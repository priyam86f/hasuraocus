package com.example.demo.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final WebClient webClient;

    @Value("${hasura.graphql.endpoint}")
    private String hasuraEndpoint;

    @Value("${HASURA_GRAPHQL_ADMIN_SECRET}")
    private String hasuraAdminSecret;  // Reference the correct property

    public WebhookController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/home")
    public String hello() {
        logger.info("Received request to /home endpoint");
        return "ok";
    }

    @PostMapping("/insertUserDetailsWithBackup")
    public ResponseEntity<Map<String, Object>> insertUserDetailsWithBackup(@RequestBody Map<String, Object> requestBody) {
        logger.info("Received request body: {}", requestBody);

        Map<String, Object> input = (Map<String, Object>) requestBody.get("input");

        try {
            // Parse age and id as integers
            int age = Integer.parseInt(input.get("age").toString());
            int id = Integer.parseInt(input.get("id").toString());

            // Log and execute the main mutation
            String mainMutation = String.format("""
                mutation InsertMain {
                    insert_user_details(objects: {first_name: "%s", last_name: "%s", age: %d, id: %d}) {
                        affected_rows
                    }
                }
            """, input.get("first_name"), input.get("last_name"), age, id);
            logger.info("Executing main mutation: {}", mainMutation);
            Map<String, Object> mainResponse = executeMutation(mainMutation);
            logger.info("Main mutation response: {}", mainResponse);

            // Log and execute backup mutations
            String backup1Mutation = String.format("""
                mutation InsertBackup1 {
                    insert_user_details_backup1(objects: {first_name: "%s", last_name: "%s", age: %d, id: %d}) {
                        affected_rows
                    }
                }
            """, input.get("first_name"), input.get("last_name"), age, id);
            logger.info("Executing backup1 mutation: {}", backup1Mutation);
            Map<String, Object> backup1Response = executeMutation(backup1Mutation);
            logger.info("Backup1 mutation response: {}", backup1Response);

            String backup2Mutation = String.format("""
                mutation InsertBackup2 {
                    insert_user_details_backup2(objects: {first_name: "%s", last_name: "%s", age: %d, id: %d}) {
                        affected_rows
                    }
                }
            """, input.get("first_name"), input.get("last_name"), age, id);
            logger.info("Executing backup2 mutation: {}", backup2Mutation);
            Map<String, Object> backup2Response = executeMutation(backup2Mutation);
            logger.info("Backup2 mutation response: {}", backup2Response);

            return ResponseEntity.ok(Map.of("success", true, "message", "Data inserted into all tables"));

        } catch (NumberFormatException e) {
            logger.error("Invalid number format for age or id: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("success", false, "error", "Invalid number format for age or id"));
        } catch (Exception e) {
            logger.error("Error occurred while processing request: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Map<String, Object> executeMutation(String mutation) {
        logger.info("Sending mutation to endpoint: {}", hasuraEndpoint);
        return this.webClient.post()
                .uri(hasuraEndpoint)
                .header("x-hasura-admin-secret", hasuraAdminSecret)  // Add the admin secret header
                .bodyValue(Map.of("query", mutation))
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> logger.info("Received response: {}", response))
                .doOnError(error -> logger.error("Error during API call: {}", error.getMessage(), error))
                .block();
    }
}
