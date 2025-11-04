package com.cloudkitchens.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a food order with all its attributes.
 */
public class Order {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("temp")
    private Temperature temperature;
    
    @JsonProperty("price")
    private double price;
    
    @JsonProperty("freshness")
    private int freshnessSeconds;

    // Default constructor for Jackson
    public Order() {}

    public Order(String id, String name, Temperature temperature, double price, int freshnessSeconds) {
        this.id = id;
        this.name = name;
        this.temperature = temperature;
        this.price = price;
        this.freshnessSeconds = freshnessSeconds;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getFreshnessSeconds() {
        return freshnessSeconds;
    }

    public void setFreshnessSeconds(int freshnessSeconds) {
        this.freshnessSeconds = freshnessSeconds;
    }

    @Override
    public String toString() {
        return String.format("Order{id='%s', name='%s', temp=%s, price=%.2f, freshness=%ds}", 
                           id, name, temperature.getValue(), price, freshnessSeconds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return id != null ? id.equals(order.id) : order.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
