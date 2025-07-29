// WebhookService.java
package com.assignment.WebhookApp;

import com.assignment.WebhookApp.model.WebhookRequest;
import com.assignment.WebhookApp.model.WebhookResponse;
import com.assignment.WebhookApp.model.FinalQueryRequest;
import javax.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String name = "John Doe";
    private final String regNo = "REG12347";  // Ends with 7 (odd)
    private final String email = "john@example.com";

    @PostConstruct
    public void runOnStartup() {
        System.out.println("runOnStartup is called");
        try {
            // 1. Generate Webhook
            WebhookRequest request = new WebhookRequest();
            request.setName(name);
            request.setRegNo(regNo);
            request.setEmail(email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<WebhookResponse> response = restTemplate.exchange(
                    "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA",
                    HttpMethod.POST,
                    entity,
                    WebhookResponse.class
            );

            WebhookResponse webhookResponse = response.getBody();
            if (webhookResponse == null) {
                System.out.println("Invalid response from webhook generation");
                return;
            }

            String webhookUrl = webhookResponse.getWebhook();
            String accessToken = webhookResponse.getAccessToken();

            System.out.println("webhookUrl url: " + webhookUrl);
            System.out.println("accessToken: " + accessToken);

            // 2. Determine SQL question & solve
            String finalQuery = solveQuestion();

            // 3. Send final query
            sendFinalQuery(webhookUrl, accessToken, finalQuery);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   private String solveQuestion() {
    return  "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
    "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
    "FROM EMPLOYEE e1 " +
    "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
    "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
    "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
    "ORDER BY e1.EMP_ID DESC";
}


  private void sendFinalQuery(String url, String accessToken, String finalQuery) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
     headers.set("Authorization", accessToken);

    // Clean up finalQuery string to single line
    String cleanQuery = finalQuery.replace("\n", " ").replace("\"", "\\\"");
    String body = "{\"finalQuery\":\"" + cleanQuery + "\"}";

    System.out.println("POST URL: " + url);
    System.out.println("Headers: " + headers);
    System.out.println("Request body: " + body);

    HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

    System.out.println("Submission response: " + response.getStatusCode());
    System.out.println("Response body: " + response.getBody());
}

}