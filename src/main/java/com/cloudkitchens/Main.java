package com.cloudkitchens;

import com.cloudkitchens.api.ChallengeApiClient;
import com.cloudkitchens.api.ProblemResult;
import com.cloudkitchens.model.Action;
import com.cloudkitchens.model.Order;
import com.cloudkitchens.service.KitchenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main execution harness for the Cloud Kitchens fulfillment system.
 * Handles command line arguments, fetches orders from the challenge server,
 * executes the fulfillment process, and submits results.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    // Default configuration values
    private static final long DEFAULT_RATE_MICROS = 500_000; // 500ms
    private static final long DEFAULT_MIN_PICKUP_MICROS = 4_000_000; // 4 seconds
    private static final long DEFAULT_MAX_PICKUP_MICROS = 8_000_000; // 8 seconds
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        // Ignore unknown properties when loading test data (for backward compatibility)
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public static void main(String[] args) {
        // Parse arguments - support both old format and new format with --save-test/--load-test
        String authToken = null;
        long rateMicros = DEFAULT_RATE_MICROS;
        long minPickupMicros = DEFAULT_MIN_PICKUP_MICROS;
        long maxPickupMicros = DEFAULT_MAX_PICKUP_MICROS;
        Long seed = null;
        String saveTestFile = null;
        String loadTestFile = null;
        boolean skipSubmission = false;
        
        // Check if using --load-test format
        if (args.length > 0 && args[0].equals("--load-test")) {
            if (args.length < 3) {
                System.err.println("Usage: java -jar fulfillment-system.jar --load-test <file> <auth_token> [--skip-submission]");
                System.exit(1);
            }
            loadTestFile = args[1];
            authToken = args[2];
            // Check for --skip-submission flag
            for (int j = 3; j < args.length; j++) {
                if (args[j].equals("--skip-submission")) {
                    skipSubmission = true;
                    break;
                }
            }
        } else {
            // First pass: extract flags (--save-test, --skip-submission)
            List<String> positionalArgs = new ArrayList<>();
            int i = 0;
            while (i < args.length) {
                if (args[i].equals("--save-test") && i + 1 < args.length) {
                    saveTestFile = args[++i]; // Skip both flag and value
                } else if (args[i].equals("--skip-submission")) {
                    skipSubmission = true; // Skip submission flag
                } else {
                    positionalArgs.add(args[i]);
                }
                i++;
            }
            
            // Second pass: parse positional arguments
            if (positionalArgs.size() > 0) {
                authToken = positionalArgs.get(0);
            }
            if (positionalArgs.size() > 1) {
                rateMicros = Long.parseLong(positionalArgs.get(1)) * 1000;
            }
            if (positionalArgs.size() > 2) {
                minPickupMicros = Long.parseLong(positionalArgs.get(2)) * 1000;
            }
            if (positionalArgs.size() > 3) {
                maxPickupMicros = Long.parseLong(positionalArgs.get(3)) * 1000;
            }
            if (positionalArgs.size() > 4) {
                seed = Long.parseLong(positionalArgs.get(4));
            }
        }
        
        if (authToken == null && loadTestFile == null) {
            System.err.println("Usage: java -jar fulfillment-system.jar <auth_token> [rate_ms] [min_pickup_ms] [max_pickup_ms] [seed] [--save-test <file>] [--skip-submission]");
            System.err.println("   OR: java -jar fulfillment-system.jar --load-test <file> <auth_token> [--skip-submission]");
            System.err.println("  auth_token: Authentication token for the challenge server");
            System.err.println("  rate_ms: Order placement rate in milliseconds (default: 500)");
            System.err.println("  min_pickup_ms: Minimum pickup time in milliseconds (default: 4000)");
            System.err.println("  max_pickup_ms: Maximum pickup time in milliseconds (default: 8000)");
            System.err.println("  seed: Optional seed for reproducible test problems");
            System.err.println("  --save-test <file>: Save test data to JSON file");
            System.err.println("  --load-test <file>: Load test data from JSON file (requires auth_token for submission)");
            System.err.println("  --skip-submission: Skip submitting to server (useful for debugging saved tests)");
            System.exit(1);
        }
        
        logger.info("Starting Cloud Kitchens Fulfillment System");
        logger.info("Configuration: rate={}μs, pickup={}-{}μs, seed={}", 
                   rateMicros, minPickupMicros, maxPickupMicros, seed);
        
        KitchenService kitchenService = new KitchenService();
        ChallengeApiClient apiClient = authToken != null ? new ChallengeApiClient(authToken) : null;
        
        try {
            ProblemResult problemResult;
            String testId;
            List<Order> orders;
            
            // Load test data from file if specified
            if (loadTestFile != null) {
                logger.info("Loading test data from: {}", loadTestFile);
                TestData testData = loadTestData(loadTestFile);
                testId = testData.getTestId();
                orders = testData.getOrders();
                rateMicros = testData.getRateMicros();
                minPickupMicros = testData.getMinPickupMicros();
                maxPickupMicros = testData.getMaxPickupMicros();
                seed = testData.getSeed();
                logger.info("Loaded test ID: {}, {} orders", testId, orders.size());
            } else {
                // Fetch orders from challenge server
                logger.info("Fetching orders from challenge server...");
                problemResult = apiClient.fetchNewProblem(seed);
                testId = problemResult.getTestId();
                orders = problemResult.getOrders();
                logger.info("Received {} orders to process", orders.size());
                
                // Save test data if requested
                if (saveTestFile != null) {
                    logger.info("Saving test data to: {}", saveTestFile);
                    TestData testData = new TestData(testId, orders, rateMicros, minPickupMicros, maxPickupMicros, seed);
                    saveTestData(testData, saveTestFile);
                }
            }
            
            // Process orders
            processOrders(kitchenService, orders, rateMicros, minPickupMicros, maxPickupMicros);
            
            // Submit solution (skip if flag is set)
            String result;
            if (skipSubmission) {
                logger.info("Skipping submission to challenge server (--skip-submission flag set)");
                List<Action> actions = kitchenService.getActionLedger();
                logger.info("Generated {} actions but not submitting", actions.size());
                result = "skipped (not submitted)";
            } else {
                logger.info("Submitting solution to challenge server...");
                List<Action> actions = kitchenService.getActionLedger();
                result = apiClient.submitSolution(testId, actions, rateMicros, minPickupMicros, maxPickupMicros);
                logger.info("Challenge result: {}", result);
            }
            
            System.out.println("RESULT: " + result);
            
            // Update saved test data with result if loading from file
            if (loadTestFile != null) {
                updateTestResult(loadTestFile, result);
            }
            
        } catch (IOException e) {
            logger.error("Failed to communicate with challenge server", e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error during execution", e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } finally {
            kitchenService.shutdown();
        }
    }
    
    /**
     * Save test data to a JSON file.
     */
    private static void saveTestData(TestData testData, String filename) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), testData);
        logger.info("Test data saved to: {}", filename);
    }
    
    /**
     * Load test data from a JSON file.
     */
    private static TestData loadTestData(String filename) throws IOException {
        return objectMapper.readValue(new File(filename), TestData.class);
    }
    
    /**
     * Update the result in a saved test file.
     */
    private static void updateTestResult(String filename, String result) {
        try {
            TestData testData = loadTestData(filename);
            testData.setResult(result);
            saveTestData(testData, filename);
        } catch (IOException e) {
            logger.warn("Failed to update test result in file: {}", filename, e);
        }
    }
    
    private static void processOrders(KitchenService kitchenService, List<Order> orders, 
                                    long rateMicros, long minPickupMicros, long maxPickupMicros) {
        logger.info("Starting order processing...");
        
        long startTime = System.currentTimeMillis() * 1000;
        List<CompletableFuture<Void>> pickupFutures = new java.util.ArrayList<>();
        
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            
            long placementTime = startTime + (i * rateMicros);
            
            logger.info("Placing order {} at time {}", order.getId(), placementTime);
            CompletableFuture<Action> placeFuture = kitchenService.placeOrderAsync(order, placementTime);
            
            // Wait for placement to complete before scheduling pickup and moving to next order
            // This ensures actions are executed in timestamp order and state checks are accurate
            try {
                placeFuture.get(); // Wait for placement to complete
            } catch (Exception e) {
                logger.error("Failed to place order {}: {}", order.getId(), e);
                throw new RuntimeException("Order placement failed", e);
            }
            
            CompletableFuture<Void> pickupFuture = kitchenService.schedulePickup(
                order.getId(), placementTime, minPickupMicros, maxPickupMicros);
            pickupFutures.add(pickupFuture);
            
            if (i < orders.size() - 1) {
                try {
                    Thread.sleep(rateMicros / 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Order placement interrupted", e);
                    break;
                }
            }
        }
        
        logger.info("All orders placed, waiting for pickups to complete...");
        
        CompletableFuture<Void> allPickups = CompletableFuture.allOf(
            pickupFutures.toArray(new CompletableFuture[0]));
        
        try {
            allPickups.get(60, TimeUnit.SECONDS); // Wait up to 60 seconds
            logger.info("All pickups completed successfully");
        } catch (Exception e) {
            logger.error("Error waiting for pickups to complete", e);
            throw new RuntimeException("Pickup completion failed", e);
        }
        
        // Log final storage status
        java.util.Map<com.cloudkitchens.model.StorageType, Integer> storageStatus = kitchenService.getStorageStatus();
        logger.info("Final storage status: {}", storageStatus);
        
        // Log action summary
        List<Action> actions = kitchenService.getActionLedger();
        logger.info("Total actions recorded: {}", actions.size());
        
        // Count actions by type
        long placeCount = actions.stream().mapToLong(a -> a.getActionType().getValue().equals("place") ? 1 : 0).sum();
        long pickupCount = actions.stream().mapToLong(a -> a.getActionType().getValue().equals("pickup") ? 1 : 0).sum();
        long moveCount = actions.stream().mapToLong(a -> a.getActionType().getValue().equals("move") ? 1 : 0).sum();
        long discardCount = actions.stream().mapToLong(a -> a.getActionType().getValue().equals("discard") ? 1 : 0).sum();
        
        logger.info("Action summary: place={}, pickup={}, move={}, discard={}", 
                   placeCount, pickupCount, moveCount, discardCount);
    }
}
