package com.cloudkitchens.storage;

import com.cloudkitchens.model.Order;
import com.cloudkitchens.model.StorageLocation;

/**
 * Calculates freshness values for orders based on time and storage conditions.
 */
public class FreshnessCalculator {

    /**
     * Calculate the freshness value (0.0 to 1.0) for an order at a given time.
     * 
     * @param location The storage location containing the order
     * @param currentTimeMicros Current time in microseconds
     * @return Freshness value between 0.0 (completely spoiled) and 1.0 (perfectly fresh)
     */
    public static double calculateFreshnessValue(StorageLocation location, long currentTimeMicros) {
        Order order = location.getOrder();
        long ageMicros = currentTimeMicros - location.getPlacedAtMicros();
        long ageSeconds = ageMicros / 1_000_000; // Convert microseconds to seconds
        
        // Apply degradation rate based on storage temperature
        double degradationRate = location.isAtIdealTemperature() ? 1.0 : 2.0;
        double effectiveAge = ageSeconds * degradationRate;
        
        // Calculate freshness ratio
        double freshnessRatio = Math.max(0.0, (order.getFreshnessSeconds() - effectiveAge) / order.getFreshnessSeconds());
        
        return Math.max(0.0, Math.min(1.0, freshnessRatio));
    }

    /**
     * Check if an order has exceeded its freshness duration.
     * 
     * @param location The storage location containing the order
     * @param currentTimeMicros Current time in microseconds
     * @return true if the order is spoiled (freshness <= 0)
     */
    public static boolean isSpoiled(StorageLocation location, long currentTimeMicros) {
        return calculateFreshnessValue(location, currentTimeMicros) <= 0.0;
    }

    /**
     * Get the remaining freshness time in seconds.
     * 
     * @param location The storage location containing the order
     * @param currentTimeMicros Current time in microseconds
     * @return Remaining freshness time in seconds (can be negative if spoiled)
     */
    public static double getRemainingFreshnessSeconds(StorageLocation location, long currentTimeMicros) {
        Order order = location.getOrder();
        long ageMicros = currentTimeMicros - location.getPlacedAtMicros();
        long ageSeconds = ageMicros / 1_000_000;
        
        double degradationRate = location.isAtIdealTemperature() ? 1.0 : 2.0;
        double effectiveAge = ageSeconds * degradationRate;
        
        return order.getFreshnessSeconds() - effectiveAge;
    }
}
