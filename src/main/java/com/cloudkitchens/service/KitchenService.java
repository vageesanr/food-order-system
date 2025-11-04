package com.cloudkitchens.service;

import com.cloudkitchens.model.Action;
import com.cloudkitchens.model.Order;
import com.cloudkitchens.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KitchenService {
    private static final Logger logger = LoggerFactory.getLogger(KitchenService.class);
    
    private final StorageManager storageManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final List<Action> actionLedger;
    private final Object ledgerLock = new Object();
    
    public KitchenService() {
        this.storageManager = new StorageManager();
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);
        this.actionLedger = new ArrayList<>();
    }
    
    public CompletableFuture<Action> placeOrderAsync(Order order, long timestampMicros) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Action action = storageManager.placeOrder(order, timestampMicros);
                recordAction(action);
                logger.info("Order placed: {} -> {}", order.getId(), action.getTarget().getValue());
                return action;
            } catch (Exception e) {
                logger.error("Failed to place order: {}", order.getId(), e);
                throw new RuntimeException("Failed to place order: " + order.getId(), e);
            }
        }, executorService);
    }
    
    public CompletableFuture<Action> pickupOrderAsync(String orderId, long timestampMicros) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Action action = storageManager.pickupOrder(orderId, timestampMicros);
                if (action != null) {
                    recordAction(action);
                    logger.info("Order picked up: {} from {}", orderId, action.getTarget().getValue());
                } else {
                    logger.warn("Order not found for pickup: {}", orderId);
                }
                return action;
            } catch (Exception e) {
                logger.error("Failed to pickup order: {}", orderId, e);
                throw new RuntimeException("Failed to pickup order: " + orderId, e);
            }
        }, executorService);
    }
    
    public CompletableFuture<Void> schedulePickup(String orderId, long placementTime, long minDelayMicros, long maxDelayMicros) {
        long delayMicros = minDelayMicros + (long) (Math.random() * (maxDelayMicros - minDelayMicros));
        long pickupTime = placementTime + delayMicros; // Calculate absolute pickup time
        long delayMillis = delayMicros / 1000; // Convert to milliseconds for scheduling
        
        logger.info("Scheduling pickup for order {} at absolute time {} (delay {}ms)", orderId, pickupTime, delayMillis);
        
        // Register the scheduled pickup so capacity checks can account for it
        storageManager.registerScheduledPickup(orderId, pickupTime);
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        scheduledExecutor.schedule(() -> {
            try {
                pickupOrderAsync(orderId, pickupTime).thenAccept(action -> {
                    if (action != null) {
                        logger.info("Scheduled pickup completed for order: {}", orderId);
                    }
                    future.complete(null);
                }).exceptionally(throwable -> {
                    logger.error("Scheduled pickup failed for order: {}", orderId, throwable);
                    future.completeExceptionally(throwable);
                    return null;
                });
            } catch (Exception e) {
                logger.error("Error in scheduled pickup for order: {}", orderId, e);
                future.completeExceptionally(e);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
        
        return future;
    }
    
    private void recordAction(Action action) {
        synchronized (ledgerLock) {
            actionLedger.add(action);
        }
    }
    
    public List<Action> getActionLedger() {
        synchronized (ledgerLock) {
            List<Action> sortedActions = new ArrayList<>(actionLedger);
            // Sort by timestamp to ensure monotonic order
            Collections.sort(sortedActions, new Comparator<Action>() {
                @Override
                public int compare(Action a, Action b) {
                    return Long.compare(a.getTimestampMicros(), b.getTimestampMicros());
                }
            });
            return sortedActions;
        }
    }
    
    public java.util.Map<com.cloudkitchens.model.StorageType, Integer> getStorageStatus() {
        return storageManager.getStorageStatus();
    }
    
    public void shutdown() {
        logger.info("Shutting down KitchenService...");
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
        }
        
        logger.info("KitchenService shutdown complete");
    }
}
