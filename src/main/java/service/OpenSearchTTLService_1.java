package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpenSearchTTLService_1 {

    private final RestTemplate restTemplate;

    @Value("${opensearch.url}")
    private String openSearchUrl;

    public OpenSearchTTLService_1(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Create a new index with sample data and attach TTL ISM policy.
     * If the policy already exists → reuse it
     * Else → create a new TTL policy and attach
     *
     * @return created index name
     */
    public String createIndexWithDataAndEnsurePolicy() {
        String indexName = "ttl-demo-" + System.currentTimeMillis();
        String indexUrl = openSearchUrl + "/" + indexName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String indexBody = "{\"settings\":{\"index.number_of_shards\":1}}";
        HttpEntity<String> indexEntity = new HttpEntity<>(indexBody, headers);

        ResponseEntity<String> indexResp = restTemplate.exchange(indexUrl, HttpMethod.PUT, indexEntity, String.class);
        System.out.println("✅ Index Created: " + indexName);

        // 2️⃣ Ensure TTL Policy exists
        ensureTTLPolicyExists(indexName);

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

    /**
     * Ensure TTL policy exists
     * If exists → reuse
     * Else → create new
     */
    private void ensureTTLPolicyExists(String indexName) {
        String policyUrl = openSearchUrl + "/_plugins/_ism/policies/ttl-demo-policy";

        try {
            restTemplate.getForEntity(policyUrl, String.class);
            System.out.println("✅ TTL Policy already exists → using existing policy");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            System.out.println("⚡ TTL Policy not found → creating new policy");

            // Create new policy
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

            ResponseEntity<String> response = restTemplate.exchange(policyUrl, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ TTL Policy Created: " + response.getBody());
        }
    }

    /**
     * Explain ISM policy for an index
     */
    public Map explainPolicy(String indexName) {
        String url = openSearchUrl + "/_plugins/_ism/explain/" + indexName;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        System.out.println("📘 ISM Explain: " + response.getBody());
        return response.getBody();
    }
}
