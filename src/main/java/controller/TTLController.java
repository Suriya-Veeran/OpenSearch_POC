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


    /**
     * Fetch all data from a given index
     */
    @GetMapping("/data/{indexName}")
    public ResponseEntity<String> getIndexData(
            @PathVariable String indexName,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String value) {

        if (field != null && value != null) {
            return ttlService.searchByField(indexName, field, value);
        }
        return ttlService.getIndexData(indexName);
    }
}
