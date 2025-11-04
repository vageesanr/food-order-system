package com.cloudkitchens.model;

/**
 * Represents where an order is stored and when it was placed there.
 */
public class StorageLocation {
    private final Order order;
    private final StorageType storageType;
    private final long placedAtMicros;

    public StorageLocation(Order order, StorageType storageType, long placedAtMicros) {
        this.order = order;
        this.storageType = storageType;
        this.placedAtMicros = placedAtMicros;
    }

    public Order getOrder() {
        return order;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public long getPlacedAtMicros() {
        return placedAtMicros;
    }

    /**
     * Check if the order is stored at its ideal temperature.
     */
    public boolean isAtIdealTemperature() {
        return (order.getTemperature() == Temperature.HOT && storageType == StorageType.HEATER) ||
               (order.getTemperature() == Temperature.COLD && storageType == StorageType.COOLER) ||
               (order.getTemperature() == Temperature.ROOM && storageType == StorageType.SHELF);
    }

    @Override
    public String toString() {
        return String.format("StorageLocation{order=%s, storage=%s, placedAt=%d}", 
                           order.getId(), storageType.getValue(), placedAtMicros);
    }
}
