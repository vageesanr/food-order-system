package com.cloudkitchens.api;

import com.cloudkitchens.api.dto.ChallengeRequest;
import com.cloudkitchens.model.Action;
import com.cloudkitchens.model.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Client for communicating with the Cloud Kitchens challenge server.
 */
public class ChallengeApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeApiClient.class);
    
    private static final String BASE_URL = "https://api.cloudkitchens.com/interview/challenge";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    
    public ChallengeApiClient(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetch a new problem from the challenge server.
     * 
     * @param seed Optional seed for reproducible test problems
     * @return ProblemResult containing the test ID and list of orders
     * @throws IOException if communication fails
     */
    public ProblemResult fetchNewProblem(Long seed) throws IOException {
        String url = BASE_URL + "/new?auth=" + authToken;
        if (seed != null) {
            url += "&seed=" + seed;
        }
        
        logger.info("Fetching new problem from: {}", url);
        
        HttpGet request = new HttpGet(url);
        
        try {
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode != 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode == 401) {
                    throw new IOException("Authentication failed (HTTP 401). Please check that you provided a valid auth_token as the first argument. " +
                                        "Usage: <auth_token> [rate_ms] [min_pickup_ms] [max_pickup_ms] [seed]");
                }
                throw new IOException("Failed to fetch problem: HTTP " + statusCode + " - " + responseBody);
            }
            
            // Extract test ID from headers for later use
            String testId = response.getFirstHeader("x-test-id") != null ? 
                          response.getFirstHeader("x-test-id").getValue() : null;
            if (testId == null || testId.isEmpty()) {
                throw new IOException("No test ID received from server");
            }
            logger.info("Received test ID: {}", testId);
            
            String responseBody = EntityUtils.toString(response.getEntity());
            List<Order> orders = objectMapper.readValue(responseBody, new TypeReference<List<Order>>() {});
            logger.info("Fetched {} orders", orders.size());
            
            return new ProblemResult(testId, orders);
            
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Request failed", e);
        }
    }
    
    /**
     * Submit a solution to the challenge server.
     * 
     * @param testId The test ID from the problem fetch
     * @param actions List of actions performed
     * @param rateMicros Order placement rate in microseconds
     * @param minPickupMicros Minimum pickup time in microseconds
     * @param maxPickupMicros Maximum pickup time in microseconds
     * @return The result string from the server
     * @throws IOException if communication fails
     */
    public String submitSolution(String testId, List<Action> actions, 
                               long rateMicros, long minPickupMicros, long maxPickupMicros) throws IOException {
        String url = BASE_URL + "/solve?auth=" + authToken;
        
        logger.info("Submitting solution for test ID: {}", testId);
        logger.info("Submitting {} actions", actions.size());
        
        // Convert internal Action objects to ChallengeAction DTOs
        List<com.cloudkitchens.api.dto.ChallengeAction> challengeActions = new java.util.ArrayList<>();
        for (Action action : actions) {
            challengeActions.add(new com.cloudkitchens.api.dto.ChallengeAction(
                    action.getTimestampMicros(),
                    action.getOrderId(),
                    action.getActionType().getValue(),
                    action.getTarget().getValue()
            ));
        }
        
        com.cloudkitchens.api.dto.ChallengeOptions options = new com.cloudkitchens.api.dto.ChallengeOptions(
                rateMicros, minPickupMicros, maxPickupMicros
        );
        
        ChallengeRequest request = new ChallengeRequest(options, challengeActions);
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpPost httpRequest = new HttpPost(url);
        httpRequest.setHeader("Content-Type", "application/json");
        httpRequest.setHeader("x-test-id", testId);
        httpRequest.setEntity(new StringEntity(requestBody, "UTF-8"));
        
        try {
            HttpResponse response = httpClient.execute(httpRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode != 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode == 409) {
                    throw new IOException("Test already submitted (HTTP 409). Test IDs can only be submitted once. " +
                                        "Use --skip-submission flag when rerunning saved tests for debugging.");
                }
                throw new IOException("Failed to submit solution: HTTP " + statusCode + " - " + responseBody);
            }
            
            String result = EntityUtils.toString(response.getEntity());
            logger.info("Solution submission result: {}", result);
            
            return result;
            
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Request failed", e);
        }
    }
}
