package service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
@Slf4j
public class OpenSearchTTLService {

    private final RestTemplate restTemplate;

    @Value("${opensearch.url}")
    private String openSearchUrl;

    @Value("${opensearch.ttl}")
    private String ttlDuration;

    public OpenSearchTTLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Create index with sample data and ensure ISM policy.
     * If policy exists, use it. Else create a new one.
     *
     * @param policyName Policy to attach or create
     * @return created index name
     */
    public String createIndexWithDataAndPolicy(String policyName) {

        ensurePolicyExists(policyName);

        String indexName = "ttl-demo-" + System.currentTimeMillis();
        String indexUrl = openSearchUrl + "/" + indexName;

        String indexSettings = """
        {
          "settings": {
            "index.number_of_shards": 1,
            "opendistro.index_state_management.policy_id": "%s"
          }
        }
        """.formatted(policyName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(indexSettings, headers);

        restTemplate.exchange(indexUrl, HttpMethod.PUT, entity, String.class);
        log.info("index created {} with policy : {}", indexName, policyName);

        String docUrl = openSearchUrl + "/" + indexName + "/_doc";
        String sampleDoc = """
        {
          "name": "Suriya",
          "message": "POC TTL test",
          "created_at": "%s"
        }
        """.formatted(java.time.Instant.now().toString());

        HttpEntity<String> docEntity = new HttpEntity<>(sampleDoc, headers);
        restTemplate.postForEntity(docUrl, docEntity, String.class);
        log.info("Sample Document Inserted");

        return indexName;
    }

    /**
     * Ensure TTL policy exists; create if not.
     *
     * @param policyName Policy name
     */
    private void ensurePolicyExists(String policyName) {
        String policyUrl = openSearchUrl + "/_plugins/_ism/policies/" + policyName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.getForEntity(policyUrl, String.class);
            log.error("Policy Already exists {}", policyName);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            System.out.println("⚡ Policy not found → creating new policy: " + policyName);
            log.info("⚡ Policy not found → creating new policy: {}", policyName);

            String jsonBody = """
            {
              "policy": {
                "description": "TTL Demo Policy - delete after %s",
                "default_state": "hot",
                "states": [
                  {
                    "name": "hot",
                    "actions": [],
                    "transitions": [
                      {
                        "state_name": "delete",
                        "conditions": { "min_index_age": "%s" }
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
                  { "index_patterns": ["ttl-demo-*"], "priority": 100 }
                ]
              }
            }
            """.formatted(ttlDuration, ttlDuration);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            restTemplate.exchange(policyUrl, HttpMethod.PUT, entity, String.class);
            log.info("Policy created {}", policyName);
        }
    }

    /**
     * Explain ISM policy applied to an index
     *
     * @return
     */
    public ResponseEntity<String> explainPolicy(String indexName) {
        String url = openSearchUrl + "/_plugins/_ism/explain/" + indexName;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("ISM Explain {}", response.getBody());
        return response;
    }

    public ResponseEntity<String> getIndexData(String indexName) {
        String url = openSearchUrl + "/" + indexName + "/_search?pretty";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("Data from index {} -> {}", indexName, response.getBody());
        return response;
    }

    public ResponseEntity<String> searchByField(String indexName, String field, String value) {
        String url = openSearchUrl + "/" + indexName + "/_search";
        String query = """
    {
      "query": { "match": { "%s": "%s" } }
    }
    """.formatted(field, value);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(query, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

}
