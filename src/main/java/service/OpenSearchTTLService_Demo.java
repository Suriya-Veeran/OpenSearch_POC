package service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpenSearchTTLService_Demo {

    private final RestTemplate restTemplate;

    @Value("${opensearch.url}")
    private String openSearchUrl;

    public OpenSearchTTLService_Demo(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createOrUpdateTTLPolicyForIndex(String indexName) {
        String url = openSearchUrl + "/_plugins/_ism/policies/ttl-demo-policy";

        String jsonBody = """
        {
          "policy": {
            "description": "TTL Demo Policy - delete after 2 minutes",
            "default_state": "hot",
            "states": [
              {
                "name": "hot",
                "actions": [],
                "transitions": [
                  {
                    "state_name": "delete",
                    "conditions": { "min_index_age": "2m" }
                  }
                ]
              },
              {
                "name": "delete",
                "actions": [ { "delete": {} } ],
                "transitions": []
              }
            ],
            "ism_template": [
              { "index_patterns": ["ttl-demo-*","%s"], "priority": 100 }
            ]
          }
        }
        """.formatted(indexName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // ✅ <<< ADD TRY-CATCH HERE
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ Policy Create/Update Response: " + response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            System.out.println("⚠ Policy already exists. Updating existing policy...");
            // Retry same PUT to update policy
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ Policy Updated Response: " + response.getBody());
        }
    }




    /**
     * Create test index (auto TTL via template)
     *
     * @return
     */
    public String createTestIndex() {
        String indexName = "ttl-demo-" + System.currentTimeMillis();
        String url = openSearchUrl + "/" + indexName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"settings\":{\"index.number_of_shards\":1}}", headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        System.out.println("✅ Test Index Created: " + indexName);

        // Ensure policy attached via template
        createOrUpdateTTLPolicyForIndex(indexName);

        return indexName;
    }

    /**
     * Explain ISM policy for index
     *
     * @return
     */
    public Map explainPolicy(String indexName) {
        String url = openSearchUrl + "/_plugins/_ism/explain/" + indexName;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        System.out.println("📘 ISM Explain: " + response.getBody());
        return response.getBody();
    }

    public String createTestIndexWithData() {
        // 1️⃣ Create new index
        String indexName = "ttl-demo-" + System.currentTimeMillis();
        String url = openSearchUrl + "/" + indexName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"settings\":{\"index.number_of_shards\":1}}", headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        System.out.println("✅ Test Index Created: " + indexName);

        // 2️⃣ Ensure policy attached via template
        createOrUpdateTTLPolicyForIndex(indexName);

        // 3️⃣ Insert sample document
        String docUrl = openSearchUrl + "/" + indexName + "/_doc";
        String sampleDoc = """
        {
          "name": "Suriya",
          "message": "POC TTL test",
          "created_at": "%s"
        }
        """.formatted(java.time.Instant.now().toString());

        HttpEntity<String> docEntity = new HttpEntity<>(sampleDoc, headers);
        ResponseEntity<String> docResp = restTemplate.postForEntity(docUrl, docEntity, String.class);
        System.out.println("📄 Sample Document Inserted: " + docResp.getBody());

        return indexName;
    }


}

