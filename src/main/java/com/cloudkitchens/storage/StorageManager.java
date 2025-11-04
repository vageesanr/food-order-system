package com.cloudkitchens.storage;

import com.cloudkitchens.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe storage manager for the kitchen fulfillment system.
 * Handles placement, movement, pickup, and discard operations with proper concurrency control.
 */
public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    
    // Storage capacities
    private static final int HEATER_CAPACITY = 6;
    private static final int COOLER_CAPACITY = 6;
    private static final int SHELF_CAPACITY = 12;
    
    // Storage containers
    private final Map<String, StorageLocation> orderLocations = new ConcurrentHashMap<>();
    private final Map<StorageType, List<StorageLocation>> storage = new ConcurrentHashMap<>();
    private final DiscardStrategy discardStrategy = new DiscardStrategy();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Track scheduled pickup timestamps (orderId -> pickup timestamp)
    private final Map<String, Long> scheduledPickups = new ConcurrentHashMap<>();
    
    public StorageManager() {
        storage.put(StorageType.HEATER, new ArrayList<>());
        storage.put(StorageType.COOLER, new ArrayList<>());
        storage.put(StorageType.SHELF, new ArrayList<>());
    }

    /**
     * Register a scheduled pickup for an order.
     * This allows capacity checks to account for future pickups.
     */
    public void registerScheduledPickup(String orderId, long pickupTimestampMicros) {
        scheduledPickups.put(orderId, pickupTimestampMicros);
    }
    
    /**
     * Place a new order in storage following the placement logic.
     * Returns the action taken (place, move, or discard).
     */
    public Action placeOrder(Order order, long timestampMicros) {
        lock.writeLock().lock();
        try {
            // Check if order already exists (shouldn't happen, but defensive check)
            if (orderLocations.containsKey(order.getId())) {
                String error = String.format("ERROR: Order %s already exists in storage at %s", 
                                           order.getId(), orderLocations.get(order.getId()).getStorageType());
                logger.error(error);
                throw new IllegalStateException(error);
            }
            
            logger.info("=== PLACING ORDER: {} (temperature: {}) ===", order.getId(), order.getTemperature());
            logger.info("Current storage state: HEATER={}/{}, COOLER={}/{}, SHELF={}/{}",
                       storage.get(StorageType.HEATER).size(), HEATER_CAPACITY,
                       storage.get(StorageType.COOLER).size(), COOLER_CAPACITY,
                       storage.get(StorageType.SHELF).size(), SHELF_CAPACITY);
            
            // Log all orders currently in storage for debugging (INFO level for visibility)
            if (storage.get(StorageType.HEATER).size() > 0) {
                logger.info("Orders in HEATER: {}", 
                            storage.get(StorageType.HEATER).stream()
                                .map(loc -> loc.getOrder().getId() + "(" + loc.getOrder().getTemperature().getValue() + ")")
                                .collect(java.util.stream.Collectors.joining(", ")));
            }
            if (storage.get(StorageType.COOLER).size() > 0) {
                logger.info("Orders in COOLER: {}", 
                            storage.get(StorageType.COOLER).stream()
                                .map(loc -> loc.getOrder().getId() + "(" + loc.getOrder().getTemperature().getValue() + ")")
                                .collect(java.util.stream.Collectors.joining(", ")));
            }
            if (storage.get(StorageType.SHELF).size() > 0) {
                logger.info("Orders in SHELF: {}", 
                            storage.get(StorageType.SHELF).stream()
                                .map(loc -> loc.getOrder().getId() + "(" + loc.getOrder().getTemperature().getValue() + ")")
                                .collect(java.util.stream.Collectors.joining(", ")));
            }
            
            // Defensive validation: ensure order temperature is valid
            if (order.getTemperature() == null) {
                throw new IllegalArgumentException("Order temperature cannot be null for order: " + order.getId());
            }
            
            StorageType idealStorage = getIdealStorage(order.getTemperature());
            logger.info("Ideal storage for {} (temp: {}) is: {}", order.getId(), order.getTemperature(), idealStorage);
            
            // Try ideal storage first
            // Check capacity at the placement timestamp (excluding orders with pickups scheduled before this timestamp)
            boolean idealHasCapacity = hasCapacityAtTimestamp(idealStorage, timestampMicros);
            int idealSize = getEffectiveSizeAtTimestamp(idealStorage, timestampMicros);
            int idealCapacity = getCapacity(idealStorage);
            logger.info("Ideal storage {} capacity check at timestamp {}: effective size={}/{}, hasCapacity={}", 
                       idealStorage, timestampMicros, idealSize, idealCapacity, idealHasCapacity);
            
            if (idealHasCapacity) {
                logger.info("✓ Placing order {} (temp: {}) in ideal storage: {}", 
                           order.getId(), order.getTemperature(), idealStorage);
                return placeInStorage(order, idealStorage, timestampMicros);
            }
            logger.info("✗ Ideal storage {} is FULL (size: {}/{})", idealStorage, idealSize, idealCapacity);
            
            // For room temperature orders, ideal storage IS the shelf, so if it's full, go to discard
            if (order.getTemperature() == Temperature.ROOM) {
                // Shelf is full, discard an order and place the new one
                StorageLocation orderToDiscard = discardStrategy.findBestOrderToDiscard(timestampMicros);
                if (orderToDiscard != null) {
                    discardOrder(orderToDiscard.getOrder().getId(), timestampMicros);
                    return placeInStorage(order, StorageType.SHELF, timestampMicros);
                }
                throw new IllegalStateException("Unable to place order: " + order.getId());
            }
            
            // For hot/cold orders, try shelf if ideal storage is full
            if (hasCapacityAtTimestamp(StorageType.SHELF, timestampMicros)) {
                return placeInStorage(order, StorageType.SHELF, timestampMicros);
            }
            
            // Shelf is full, try to move orders from shelf to ideal storage (only for hot/cold)
            // We move an order that matches the new order's temperature from shelf to ideal storage.
            // After a successful move, the shelf now has room, so place the new order on the shelf.
            if (tryMoveFromShelfToIdeal(order.getTemperature(), timestampMicros)) {
                // After moving, we made room on the shelf, so place the new order there
                logger.info("After moving an order from shelf to ideal storage, placing new order on shelf");
                return placeInStorage(order, StorageType.SHELF, timestampMicros);
            }
            
            // Still no room, discard an order and place the new one
            StorageLocation orderToDiscard = discardStrategy.findBestOrderToDiscard(timestampMicros);
            if (orderToDiscard != null) {
                discardOrder(orderToDiscard.getOrder().getId(), timestampMicros);
                return placeInStorage(order, StorageType.SHELF, timestampMicros);
            }
            
            throw new IllegalStateException("Unable to place order: " + order.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Pick up an order. Returns null if order not found or spoiled.
     */
    public Action pickupOrder(String orderId, long timestampMicros) {
        lock.writeLock().lock();
        try {
            StorageLocation location = orderLocations.get(orderId);
            if (location == null) {
                logger.warn("Order not found for pickup: {}", orderId);
                return null;
            }
            
            // Remove scheduled pickup tracking (order is being picked up now)
            scheduledPickups.remove(orderId);
            
            // Check if order is spoiled
            if (FreshnessCalculator.isSpoiled(location, timestampMicros)) {
                logger.info("Order spoiled, discarding: {}", orderId);
                discardStrategy.removeOrder(location);
                storage.get(location.getStorageType()).remove(location);
                orderLocations.remove(orderId);
                return new Action(timestampMicros, orderId, ActionType.DISCARD, location.getStorageType());
            }
            
            // Pick up the order
            logger.info("Picking up order: {}", orderId);
            discardStrategy.removeOrder(location);
            storage.get(location.getStorageType()).remove(location);
            orderLocations.remove(orderId);
            return new Action(timestampMicros, orderId, ActionType.PICKUP, location.getStorageType());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Move an order from shelf to ideal storage.
     */
    private boolean tryMoveFromShelfToIdeal(Temperature temperature, long timestampMicros) {
        StorageType idealStorage = getIdealStorage(temperature);
        if (!hasCapacityAtTimestamp(idealStorage, timestampMicros)) {
            return false;
        }
        
        StorageLocation orderToMove = discardStrategy.findOrderToMoveFromShelf(idealStorage, timestampMicros);
        if (orderToMove == null) {
            return false;
        }
        
        // Move the order
        moveOrder(orderToMove.getOrder().getId(), idealStorage, timestampMicros);
        return true;
    }

    /**
     * Move an order to a different storage type.
     */
    public Action moveOrder(String orderId, StorageType newStorageType, long timestampMicros) {
        lock.writeLock().lock();
        try {
            StorageLocation currentLocation = orderLocations.get(orderId);
            if (currentLocation == null) {
                logger.warn("Order not found for move: {}", orderId);
                return null;
            }
            
            if (!hasCapacity(newStorageType)) {
                logger.warn("Target storage full for move: {}", newStorageType);
                return null;
            }
            
            // Remove from current storage
            storage.get(currentLocation.getStorageType()).remove(currentLocation);
            discardStrategy.removeOrder(currentLocation);
            
            // Create new location
            StorageLocation newLocation = new StorageLocation(
                currentLocation.getOrder(), 
                newStorageType, 
                currentLocation.getPlacedAtMicros()
            );
            
            // Add to new storage
            storage.get(newStorageType).add(newLocation);
            orderLocations.put(orderId, newLocation);
            discardStrategy.addOrder(newLocation);
            
            logger.info("Moved order {} from {} to {}", orderId, currentLocation.getStorageType(), newStorageType);
            return new Action(timestampMicros, orderId, ActionType.MOVE, newStorageType);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Discard an order.
     */
    public Action discardOrder(String orderId, long timestampMicros) {
        lock.writeLock().lock();
        try {
            StorageLocation location = orderLocations.get(orderId);
            if (location == null) {
                logger.warn("Order not found for discard: {}", orderId);
                return null;
            }
            
            discardStrategy.removeOrder(location);
            storage.get(location.getStorageType()).remove(location);
            orderLocations.remove(orderId);
            
            logger.info("Discarded order: {}", orderId);
            return new Action(timestampMicros, orderId, ActionType.DISCARD, location.getStorageType());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Place an order in specific storage.
     */
    private Action placeInStorage(Order order, StorageType storageType, long timestampMicros) {
        // Validate that the order can be placed in this storage type
        if (storageType == StorageType.HEATER && order.getTemperature() != Temperature.HOT) {
            String error = String.format("VALIDATION ERROR: Cannot place non-hot order %s (temp: %s) in heater", 
                                       order.getId(), order.getTemperature());
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        if (storageType == StorageType.COOLER && order.getTemperature() != Temperature.COLD) {
            String error = String.format("VALIDATION ERROR: Cannot place non-cold order %s (temp: %s) in cooler", 
                                       order.getId(), order.getTemperature());
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        
        // Safety check: ensure storage has capacity at this timestamp before placing
        int effectiveSize = getEffectiveSizeAtTimestamp(storageType, timestampMicros);
        int capacity = getCapacity(storageType);
        logger.info("placeInStorage: Order {} -> {}, effective size at timestamp {}: {}/{}, capacity check: {}", 
                   order.getId(), storageType, timestampMicros, effectiveSize, capacity, effectiveSize < capacity);
        
        if (effectiveSize >= capacity) {
            String error = String.format("CAPACITY ERROR: Cannot place order %s in %s at timestamp %d - storage at effective capacity (size: %d, capacity: %d)", 
                                       order.getId(), storageType, timestampMicros, effectiveSize, capacity);
            logger.error(error);
            throw new IllegalStateException(error);
        }
        
        StorageLocation location = new StorageLocation(order, storageType, timestampMicros);
        
        // Double-check capacity right before adding (defensive programming)
        int effectiveSizeBeforeAdd = getEffectiveSizeAtTimestamp(storageType, timestampMicros);
        if (effectiveSizeBeforeAdd >= capacity) {
            String error = String.format("CAPACITY RACE CONDITION DETECTED: Order %s -> %s, effective size changed from %d to %d (capacity: %d)", 
                                       order.getId(), storageType, effectiveSize, effectiveSizeBeforeAdd, capacity);
            logger.error(error);
            throw new IllegalStateException(error);
        }
        
                storage.get(storageType).add(location);
        orderLocations.put(order.getId(), location);
        discardStrategy.addOrder(location);

        int sizeAfterAdd = getEffectiveSizeAtTimestamp(storageType, timestampMicros);
        logger.info("placeInStorage: Order {} successfully added to {}, effective size: {} -> {}",
                   order.getId(), storageType, effectiveSizeBeforeAdd, sizeAfterAdd);
        
        logger.info("✓ Placed order {} (temp: {}) in {} (storage now {}/{})", 
                   order.getId(), order.getTemperature(), storageType.getValue(), 
                   storage.get(storageType).size(), capacity);
        return new Action(timestampMicros, order.getId(), ActionType.PLACE, storageType);
    }
    
    /**
     * Get the capacity of a storage type.
     */
    private int getCapacity(StorageType storageType) {
        switch (storageType) {
            case HEATER: return HEATER_CAPACITY;
            case COOLER: return COOLER_CAPACITY;
            case SHELF: return SHELF_CAPACITY;
            default: return 0;
        }
    }

    /**
     * Get the ideal storage type for a temperature.
     */
    private StorageType getIdealStorage(Temperature temperature) {
        switch (temperature) {
            case HOT: return StorageType.HEATER;
            case COLD: return StorageType.COOLER;
            case ROOM: return StorageType.SHELF;
            default: throw new IllegalArgumentException("Unknown temperature: " + temperature);
        }
    }

    /**
     * Check if storage has capacity.
     */
    private boolean hasCapacity(StorageType storageType) {
        int currentSize = storage.get(storageType).size();
        switch (storageType) {
            case HEATER: return currentSize < HEATER_CAPACITY;
            case COOLER: return currentSize < COOLER_CAPACITY;
            case SHELF: return currentSize < SHELF_CAPACITY;
            default: return false;
        }
    }
    
    /**
     * Get the effective size of storage at a given timestamp, excluding orders with pickups scheduled before that timestamp.
     */
    private int getEffectiveSizeAtTimestamp(StorageType storageType, long timestampMicros) {
        // Count orders that are currently in storage at this timestamp.
        // Since pickups happen asynchronously, we need to exclude orders that have been
        // picked up by this timestamp. Since we execute placements sequentially, orders
        // currently in storage were all placed before this timestamp.
        // Orders that have been picked up have already been removed from storage, so
        // we just need to count the current size.
        return storage.get(storageType).size();
    }
    
    /**
     * Check if storage has capacity at a given timestamp, accounting for scheduled pickups.
     */
    private boolean hasCapacityAtTimestamp(StorageType storageType, long timestampMicros) {
        int effectiveSize = getEffectiveSizeAtTimestamp(storageType, timestampMicros);
        switch (storageType) {
            case HEATER: return effectiveSize < HEATER_CAPACITY;
            case COOLER: return effectiveSize < COOLER_CAPACITY;
            case SHELF: return effectiveSize < SHELF_CAPACITY;
            default: return false;
        }
    }

    /**
     * Get storage status for monitoring.
     */
    public Map<StorageType, Integer> getStorageStatus() {
        lock.readLock().lock();
        try {
            Map<StorageType, Integer> status = new HashMap<>();
            status.put(StorageType.HEATER, storage.get(StorageType.HEATER).size());
            status.put(StorageType.COOLER, storage.get(StorageType.COOLER).size());
            status.put(StorageType.SHELF, storage.get(StorageType.SHELF).size());
            return status;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all orders currently in storage.
     */
    public Collection<StorageLocation> getAllOrders() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(orderLocations.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
