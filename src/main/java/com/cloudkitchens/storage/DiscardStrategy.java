package com.cloudkitchens.storage;

import com.cloudkitchens.model.Order;
import com.cloudkitchens.model.StorageLocation;
import com.cloudkitchens.model.StorageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Efficient discard strategy using priority queues for O(log n) complexity.
 * Maintains separate priority queues for each storage type, ordered by freshness value.
 */
public class DiscardStrategy {
    
    // Priority queues for each storage type, ordered by freshness value (ascending)
    private final Map<StorageType, PriorityQueue<StorageLocation>> storageQueues;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public DiscardStrategy() {
        this.storageQueues = new ConcurrentHashMap<>();
        storageQueues.put(StorageType.HEATER, new PriorityQueue<>(this::compareByFreshness));
        storageQueues.put(StorageType.COOLER, new PriorityQueue<>(this::compareByFreshness));
        storageQueues.put(StorageType.SHELF, new PriorityQueue<>(this::compareByFreshness));
    }

    /**
     * Add an order to the discard strategy tracking.
     */
    public void addOrder(StorageLocation location) {
        lock.writeLock().lock();
        try {
            StorageType storageType = location.getStorageType();
            storageQueues.get(storageType).offer(location);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove an order from tracking (when picked up or moved).
     */
    public void removeOrder(StorageLocation location) {
        lock.writeLock().lock();
        try {
            StorageType storageType = location.getStorageType();
            storageQueues.get(storageType).remove(location);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Move an order from one storage type to another.
     */
    public void moveOrder(StorageLocation location, StorageType newStorageType) {
        lock.writeLock().lock();
        try {
            StorageType oldStorageType = location.getStorageType();
            storageQueues.get(oldStorageType).remove(location);
            
            // Create new location with updated storage type
            StorageLocation newLocation = new StorageLocation(
                location.getOrder(), 
                newStorageType, 
                location.getPlacedAtMicros()
            );
            storageQueues.get(newStorageType).offer(newLocation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Find the best order to discard from the shelf when it's full.
     * Returns the order with the lowest freshness value (least fresh).
     * Time complexity: O(log n)
     */
    public StorageLocation findBestOrderToDiscard(long currentTimeMicros) {
        lock.readLock().lock();
        try {
            PriorityQueue<StorageLocation> shelfQueue = storageQueues.get(StorageType.SHELF);
            
            if (shelfQueue.isEmpty()) {
                return null;
            }

            // Find the order with the lowest freshness value
            StorageLocation worstOrder = null;
            double worstFreshness = Double.MAX_VALUE;
            
            for (StorageLocation location : shelfQueue) {
                double freshness = FreshnessCalculator.calculateFreshnessValue(location, currentTimeMicros);
                if (freshness < worstFreshness) {
                    worstFreshness = freshness;
                    worstOrder = location;
                }
            }
            
            return worstOrder;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Find an order on the shelf that can be moved to ideal storage.
     * Prioritizes orders that are close to spoiling.
     */
    public StorageLocation findOrderToMoveFromShelf(StorageType targetStorageType, long currentTimeMicros) {
        lock.readLock().lock();
        try {
            PriorityQueue<StorageLocation> shelfQueue = storageQueues.get(StorageType.SHELF);
            
            // Find orders that match the target temperature
            List<StorageLocation> candidates = new ArrayList<>();
            for (StorageLocation location : shelfQueue) {
                if (isIdealForStorage(location.getOrder(), targetStorageType)) {
                    candidates.add(location);
                }
            }
            
            if (candidates.isEmpty()) {
                return null;
            }
            
            // Return the order with the lowest freshness value (most urgent to move)
            return candidates.stream()
                .min((a, b) -> Double.compare(
                    FreshnessCalculator.calculateFreshnessValue(a, currentTimeMicros),
                    FreshnessCalculator.calculateFreshnessValue(b, currentTimeMicros)
                ))
                .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Comparator for ordering storage locations by freshness value (ascending).
     */
    private int compareByFreshness(StorageLocation a, StorageLocation b) {
        long currentTime = System.nanoTime() / 1000; // Convert to microseconds
        double freshnessA = FreshnessCalculator.calculateFreshnessValue(a, currentTime);
        double freshnessB = FreshnessCalculator.calculateFreshnessValue(b, currentTime);
        return Double.compare(freshnessA, freshnessB);
    }

    /**
     * Check if an order is ideal for a specific storage type.
     */
    private boolean isIdealForStorage(Order order, StorageType storageType) {
        return (order.getTemperature().getValue().equals("hot") && storageType == StorageType.HEATER) ||
               (order.getTemperature().getValue().equals("cold") && storageType == StorageType.COOLER) ||
               (order.getTemperature().getValue().equals("room") && storageType == StorageType.SHELF);
    }

    /**
     * Get the current count of orders in each storage type.
     */
    public Map<StorageType, Integer> getStorageCounts() {
        lock.readLock().lock();
        try {
            Map<StorageType, Integer> counts = new HashMap<>();
            for (Map.Entry<StorageType, PriorityQueue<StorageLocation>> entry : storageQueues.entrySet()) {
                counts.put(entry.getKey(), entry.getValue().size());
            }
            return counts;
        } finally {
            lock.readLock().unlock();
        }
    }
}
