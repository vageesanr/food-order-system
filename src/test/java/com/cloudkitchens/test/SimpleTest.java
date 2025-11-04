package com.cloudkitchens.test;

import com.cloudkitchens.model.*;
import com.cloudkitchens.storage.StorageManager;
import com.cloudkitchens.service.KitchenService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simple test to verify the core functionality of the fulfillment system.
 */
public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("Testing Cloud Kitchen Fulfillment System...");
        
        // Test basic order placement and pickup
        KitchenService kitchenService = new KitchenService();
        
        try {
            // Create test orders
            Order hotOrder = new Order("hot1", "Hot Pizza", Temperature.HOT, 15.0, 120);
            Order coldOrder = new Order("cold1", "Cold Salad", Temperature.COLD, 8.0, 300);
            Order roomOrder = new Order("room1", "Sandwich", Temperature.ROOM, 6.0, 180);
            
            long currentTime = System.nanoTime() / 1000; // Convert to microseconds
            
            // Place orders
            System.out.println("Placing orders...");
            CompletableFuture<Action> place1 = kitchenService.placeOrderAsync(hotOrder, currentTime);
            CompletableFuture<Action> place2 = kitchenService.placeOrderAsync(coldOrder, currentTime + 1000);
            CompletableFuture<Action> place3 = kitchenService.placeOrderAsync(roomOrder, currentTime + 2000);
            
            // Wait for placements
            Action action1 = place1.get();
            Action action2 = place2.get();
            Action action3 = place3.get();
            
            System.out.println("Placement actions:");
            System.out.println("  " + action1);
            System.out.println("  " + action2);
            System.out.println("  " + action3);
            
            // Check storage status
            java.util.Map<com.cloudkitchens.model.StorageType, Integer> storageStatus = kitchenService.getStorageStatus();
            System.out.println("Storage status: " + storageStatus);
            
            // Pick up orders after a short delay
            Thread.sleep(1000);
            long pickupTime = System.nanoTime() / 1000;
            
            System.out.println("Picking up orders...");
            CompletableFuture<Action> pickup1 = kitchenService.pickupOrderAsync("hot1", pickupTime);
            CompletableFuture<Action> pickup2 = kitchenService.pickupOrderAsync("cold1", pickupTime + 100);
            CompletableFuture<Action> pickup3 = kitchenService.pickupOrderAsync("room1", pickupTime + 200);
            
            // Wait for pickups
            Action pickupAction1 = pickup1.get();
            Action pickupAction2 = pickup2.get();
            Action pickupAction3 = pickup3.get();
            
            System.out.println("Pickup actions:");
            System.out.println("  " + pickupAction1);
            System.out.println("  " + pickupAction2);
            System.out.println("  " + pickupAction3);
            
            // Check final storage status
            java.util.Map<com.cloudkitchens.model.StorageType, Integer> finalStatus = kitchenService.getStorageStatus();
            System.out.println("Final storage status: " + finalStatus);
            
            // Get action ledger
            List<Action> allActions = kitchenService.getActionLedger();
            System.out.println("Total actions: " + allActions.size());
            
            System.out.println("Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            kitchenService.shutdown();
        }
    }
}
