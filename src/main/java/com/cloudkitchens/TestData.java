package com.cloudkitchens;

import com.cloudkitchens.model.Order;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Data class for saving and loading test data.
 */
public class TestData {
    @JsonProperty("testId")
    private String testId;
    
    @JsonProperty("orders")
    private List<Order> orders;
    
    @JsonProperty("rateMicros")
    private long rateMicros;
    
    @JsonProperty("minPickupMicros")
    private long minPickupMicros;
    
    @JsonProperty("maxPickupMicros")
    private long maxPickupMicros;
    
    @JsonProperty("seed")
    private Long seed;
    
    @JsonProperty("result")
    private String result;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("rerun_timestamp")
    private String rerunTimestamp;

    // Default constructor for Jackson
    public TestData() {}

    public TestData(String testId, List<Order> orders, long rateMicros, 
                   long minPickupMicros, long maxPickupMicros, Long seed) {
        this.testId = testId;
        this.orders = orders;
        this.rateMicros = rateMicros;
        this.minPickupMicros = minPickupMicros;
        this.maxPickupMicros = maxPickupMicros;
        this.seed = seed;
    }

    // Getters and setters
    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public long getRateMicros() {
        return rateMicros;
    }

    public void setRateMicros(long rateMicros) {
        this.rateMicros = rateMicros;
    }

    public long getMinPickupMicros() {
        return minPickupMicros;
    }

    public void setMinPickupMicros(long minPickupMicros) {
        this.minPickupMicros = minPickupMicros;
    }

    public long getMaxPickupMicros() {
        return maxPickupMicros;
    }

    public void setMaxPickupMicros(long maxPickupMicros) {
        this.maxPickupMicros = maxPickupMicros;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRerunTimestamp() {
        return rerunTimestamp;
    }

    public void setRerunTimestamp(String rerunTimestamp) {
        this.rerunTimestamp = rerunTimestamp;
    }
}
