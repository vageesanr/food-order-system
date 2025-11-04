package com.cloudkitchens.api;

import com.cloudkitchens.model.Order;
import java.util.List;

/**
 * Result of fetching a new problem from the challenge server.
 */
public class ProblemResult {
    private final String testId;
    private final List<Order> orders;
    
    public ProblemResult(String testId, List<Order> orders) {
        this.testId = testId;
        this.orders = orders;
    }
    
    public String getTestId() {
        return testId;
    }
    
    public List<Order> getOrders() {
        return orders;
    }
}
