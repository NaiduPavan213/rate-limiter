package com.example.ratelimiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.ratelimiter.dto.ClientConfigPatchDTO;
import com.example.ratelimiter.dto.RateLimitRequest;
import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.model.RateLimitMode;
import com.example.ratelimiter.model.RateLimitState;
import com.example.ratelimiter.model.SlidingWindowRequest;
import com.example.ratelimiter.repository.RateLimitStateRepository;
import com.example.ratelimiter.repository.SlidingWindowRequestRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimiterIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ClientConfigService clientConfigService;

    @Autowired
    private RateLimitStateRepository rateLimitStateRepository;

    @Autowired
    private SlidingWindowRequestRepository slidingWindowRequestRepository;


    @Test
    void shouldAllowRequestForValidClient() throws InterruptedException {

        ClientConfig config = new ClientConfig();
        config.setClientKey("integration-test");
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);
        config.setMode(RateLimitMode.TOKEN_BUCKET);

        clientConfigService.addClient(config);

        String url = "http://localhost:" + port + "/rate-limit/check";

        RateLimitRequest request = new RateLimitRequest();
        request.setClientKey("integration-test");

        ResponseEntity<String> response = restTemplate.postForEntity(
            url,
            request,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
            "1",
            response.getHeaders().getFirst("X-RateLimit-Limit")
        );

        assertEquals(
            "0",
            response.getHeaders().getFirst("X-RateLimit-Remaining")
        );

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
            () -> restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            )
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldHandleConcurrentRequestsCorrectly() throws InterruptedException {
        ClientConfig config = new ClientConfig();
        config.setClientKey("integration-concurrency-test");
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);
        config.setMode(RateLimitMode.TOKEN_BUCKET);

        clientConfigService.addClient(config);

        String url = "http://localhost:" + port + "/rate-limit/check";

        RateLimitRequest request = new RateLimitRequest();
        request.setClientKey("integration-concurrency-test");

        int totalRequests = 10;

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    restTemplate.postForEntity(url, request, String.class);
                    allowedCount.incrementAndGet();

                } catch (HttpClientErrorException.TooManyRequests e) {
                    rejectedCount.incrementAndGet();

                } catch (Exception e) {
                    e.printStackTrace();

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        doneLatch.await();

        executor.shutdown();

        assertEquals(totalRequests, allowedCount.get() + rejectedCount.get());
        assertEquals(1, allowedCount.get());
        assertEquals(9, rejectedCount.get());
    }

    @Test
    void shouldPersistTokenBucketStateInDatabase() {

        ClientConfig config = new ClientConfig();
        config.setClientKey("persistence-test");
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);
        config.setMode(RateLimitMode.TOKEN_BUCKET);

        clientConfigService.addClient(config);

        String url = "http://localhost:" + port + "/rate-limit/check";

        RateLimitRequest request = new RateLimitRequest();
        request.setClientKey("persistence-test");

        // First request consumes the only token.
        ResponseEntity<String> response = restTemplate.postForEntity(
                url, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify that the token state was saved in PostgreSQL.
        RateLimitState savedState = rateLimitStateRepository
                .findById("persistence-test")
                .orElseThrow();

        assertEquals(0, savedState.getCurrentTokens());

    }

    @Test
    void shouldRestoreTokenBucketStateFromDatabase() {

        ClientConfig config = new ClientConfig();
        config.setClientKey("restore-test");
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);
        config.setMode(RateLimitMode.TOKEN_BUCKET);

        clientConfigService.addClient(config);

        String url = "http://localhost:" + port + "/rate-limit/check";

        RateLimitRequest request = new RateLimitRequest();
        request.setClientKey("restore-test");

        // Consume the only token.
        restTemplate.postForEntity(url, request, String.class);

        // Load the saved state from PostgreSQL.
        RateLimitState savedState = rateLimitStateRepository
                .findById("restore-test")
                .orElseThrow();

        // Create a completely new TokenBucket using saved state.
        TokenBucket restoredBucket = new TokenBucket(savedState);

        // Verify that the restored bucket has zero tokens.
        assertEquals(0, restoredBucket.getCurrentTokens());
    }


    @Test
    void shouldPersistSlidingWindowRequestsInDatabase() {

        ClientConfig config = new ClientConfig();
        config.setClientKey("sliding-persistence-test");
        config.setMode(RateLimitMode.SLIDING_WINDOW);
        config.setRequestsPerSecond(2);
        config.setBurstSize(2);

        clientConfigService.addClient(config);

        RateLimitService service = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        RateLimitResponse firstResponse =
                service.getRateLimit("sliding-persistence-test");

        RateLimitResponse secondResponse =
                service.getRateLimit("sliding-persistence-test");

        assertEquals(true, firstResponse.isAllowed());
        assertEquals(true, secondResponse.isAllowed());

        List<SlidingWindowRequest> savedRequests =
                slidingWindowRequestRepository
                        .findByClientKeyAndRequestTimeAfter(
                                "sliding-persistence-test",
                                Instant.EPOCH
                        );

        assertEquals(2, savedRequests.size());
    }



    @Test
    void shouldRestoreTokenBucketStateInNewServiceInstance() {

        // 1. Create a client
        ClientConfig config = new ClientConfig();
        config.setClientKey("service-restore-test");
        config.setMode(RateLimitMode.TOKEN_BUCKET);
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);

        clientConfigService.addClient(config);

        // 2. Consume the only token using the original service
        RateLimitService originalService = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        RateLimitResponse firstResponse =
                originalService.getRateLimit("service-restore-test");

        assertEquals(true, firstResponse.isAllowed());

        // 3. Verify the state was persisted
        RateLimitState savedState =
                rateLimitStateRepository.findById("service-restore-test")
                        .orElseThrow();

        assertEquals(0, savedState.getCurrentTokens());

        // 4. Create a completely new service instance
        RateLimitService freshService = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        // 5. The fresh service should load the saved state
        RateLimitResponse restoredResponse =
                freshService.getRateLimit("service-restore-test");

        assertEquals(false, restoredResponse.isAllowed());
    }


    @Test
    void shouldRestoreSlidingWindowStateInNewServiceInstance() {

        // 1. Create a Sliding Window client
        ClientConfig config = new ClientConfig();
        config.setClientKey("sliding-restore-test");
        config.setMode(RateLimitMode.SLIDING_WINDOW);
        config.setRequestsPerSecond(2);
        config.setBurstSize(2);

        clientConfigService.addClient(config);

        // 2. Create the original service
        RateLimitService originalService = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        // 3. Consume both available requests
        RateLimitResponse firstResponse =
                originalService.getRateLimit("sliding-restore-test");

        RateLimitResponse secondResponse =
                originalService.getRateLimit("sliding-restore-test");

        assertEquals(true, firstResponse.isAllowed());
        assertEquals(true, secondResponse.isAllowed());

        // 4. Verify both requests were saved in PostgreSQL
        List<SlidingWindowRequest> savedRequests =
                slidingWindowRequestRepository
                        .findByClientKeyAndRequestTimeAfter(
                                "sliding-restore-test",
                                Instant.EPOCH
                        );

        assertEquals(2, savedRequests.size());

        // 5. Create a fresh service with empty in-memory maps
        RateLimitService freshService = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        // 6. The fresh service should find the previous requests
        RateLimitResponse restoredResponse =
                freshService.getRateLimit("sliding-restore-test");

        assertEquals(false, restoredResponse.isAllowed());
    }


    @Test
    void shouldKeepRateLimitsIndependentForDifferentClients() {

        // Create Client A: limit = 1 request/second
        ClientConfig clientA = new ClientConfig();
        clientA.setClientKey("isolation-client-A");
        clientA.setMode(RateLimitMode.TOKEN_BUCKET);
        clientA.setRequestsPerSecond(1);
        clientA.setBurstSize(1);

        clientConfigService.addClient(clientA);

        // Create Client B: limit = 1 request/second
        ClientConfig clientB = new ClientConfig();
        clientB.setClientKey("isolation-client-B");
        clientB.setMode(RateLimitMode.TOKEN_BUCKET);
        clientB.setRequestsPerSecond(1);
        clientB.setBurstSize(1);

        clientConfigService.addClient(clientB);

        RateLimitService service = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        // Client A consumes its token
        RateLimitResponse responseA1 =
                service.getRateLimit("isolation-client-A");

        // Client B consumes its own token
        RateLimitResponse responseB1 =
                service.getRateLimit("isolation-client-B");

        assertEquals(true, responseA1.isAllowed());
        assertEquals(true, responseB1.isAllowed());

        // Client A should now be rejected
        RateLimitResponse responseA2 =
                service.getRateLimit("isolation-client-A");

        assertEquals(false, responseA2.isAllowed());

        // Client B should also be rejected because it consumed its own token
        RateLimitResponse responseB2 =
                service.getRateLimit("isolation-client-B");

        assertEquals(false, responseB2.isAllowed());
    }


    @Test
    void shouldResetRateLimiterStateWhenModeChanges() {

        ClientConfig config = new ClientConfig();
        config.setClientKey("mode-change-test");
        config.setMode(RateLimitMode.TOKEN_BUCKET);
        config.setRequestsPerSecond(1);
        config.setBurstSize(1);

        clientConfigService.addClient(config);

        RateLimitService service = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        // Consume the only Token Bucket token
        RateLimitResponse firstResponse =
                service.getRateLimit("mode-change-test");

        assertEquals(true, firstResponse.isAllowed());

        // Confirm Token Bucket state exists
        assertEquals(
                true,
                rateLimitStateRepository.existsById("mode-change-test")
        );

        // Change the mode to Sliding Window
        ClientConfigPatchDTO patch = new ClientConfigPatchDTO();
        patch.setMode(RateLimitMode.SLIDING_WINDOW);

        service.updateClient("mode-change-test", patch);

        // Old Token Bucket state should be deleted
        assertEquals(
                false,
                rateLimitStateRepository.existsById("mode-change-test")
        );

        // New Sliding Window should allow its first request
        RateLimitResponse newResponse =
                service.getRateLimit("mode-change-test");

        assertEquals(true, newResponse.isAllowed());
    }


    @Test
    void shouldHandleConcurrentSlidingWindowRequestsCorrectly()
            throws InterruptedException {

        ClientConfig config = new ClientConfig();
        config.setClientKey("sliding-concurrency-test");
        config.setMode(RateLimitMode.SLIDING_WINDOW);
        config.setRequestsPerSecond(2);
        config.setBurstSize(2);

        clientConfigService.addClient(config);

        RateLimitService service = new RateLimitService(
                clientConfigService,
                rateLimitStateRepository,
                slidingWindowRequestRepository
        );

        int numberOfRequests = 10;

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch =
                new CountDownLatch(numberOfRequests);

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);
        AtomicInteger unexpectedErrors = new AtomicInteger(0);

        for (int i = 0; i < numberOfRequests; i++) {

            executor.submit(() -> {
                try {
                    startLatch.await();

                    RateLimitResponse response =
                            service.getRateLimit("sliding-concurrency-test");

                    if (response.isAllowed()) {
                        allowed.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }

                } catch (Exception e) {
                    unexpectedErrors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        doneLatch.await();

        executor.shutdown();

        assertEquals(2, allowed.get());
        assertEquals(8, rejected.get());
        assertEquals(0, unexpectedErrors.get());
    }

}
