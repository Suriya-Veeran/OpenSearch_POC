package runner;

import org.springframework.boot.CommandLineRunner;
import service.OpenSearchTTLService_Demo;

public class OpenSearchTTLRunner   implements CommandLineRunner {
    private final OpenSearchTTLService_Demo ttlService;

    public OpenSearchTTLRunner(OpenSearchTTLService_Demo ttlService) {
        this.ttlService = ttlService;
    }

    @Override
    public void run(String... args) {
//        ttlService.createTTLPolicy();
        ttlService.createTestIndex();
    }
}
