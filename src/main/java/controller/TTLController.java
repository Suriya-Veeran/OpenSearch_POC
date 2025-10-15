package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.OpenSearchTTLService;

@RestController
@RequestMapping("/ttl")
public class TTLController {

    private final OpenSearchTTLService ttlService;

    public TTLController(OpenSearchTTLService ttlService) {
        this.ttlService = ttlService;
    }

    @PostMapping("/createIndex")
    public ResponseEntity<String> createIndexWithPolicy(@RequestParam String policyName) {
        String indexName = ttlService.createIndexWithDataAndPolicy(policyName);
        return ResponseEntity.ok("✅ Index created with TTL policy: " + indexName);
    }

    @PostMapping("/explain/{indexName}")
    public ResponseEntity<String> explainPolicy(@PathVariable String indexName) {
        return ttlService.explainPolicy(indexName);
    }
}
