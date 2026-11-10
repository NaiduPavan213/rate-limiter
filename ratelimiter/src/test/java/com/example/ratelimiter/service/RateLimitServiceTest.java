
package com.example.ratelimiter.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.model.RateLimitMode;
import com.example.ratelimiter.repository.RateLimitStateRepository;
import com.example.ratelimiter.repository.SlidingWindowRequestRepository;

public class RateLimitServiceTest {

    @Test
    void testConcurrentRequests() throws InterruptedException {

        int totalRequests = 500;

        ExecutorService executor =
                Executors.newFixedThreadPool(50);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        ClientConfigService clientConfigService =
                mock(ClientConfigService.class);

        RateLimitStateRepository repository =
                mock(RateLimitStateRepository.class);

        SlidingWindowRequestRepository slidingWindowRequestRepository =
                mock(SlidingWindowRequestRepository.class);

        RateLimitService rateLimitService =
                new RateLimitService(
                        clientConfigService,
                        repository,
                        slidingWindowRequestRepository
                );

        // Client A configuration
        ClientConfig configA = new ClientConfig();
        configA.setClientKey("client-A");
        configA.setMode(RateLimitMode.TOKEN_BUCKET);
        configA.setBurstSize(100);
        configA.setRequestsPerSecond(1);

        // Client B configuration
        ClientConfig configB = new ClientConfig();
        configB.setClientKey("client-B");
        configB.setMode(RateLimitMode.TOKEN_BUCKET);
        configB.setBurstSize(50);
        configB.setRequestsPerSecond(1);

        when(clientConfigService.getClient("client-A"))
                .thenReturn(configA);

        when(clientConfigService.getClient("client-B"))
                .thenReturn(configB);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < totalRequests; i++) {

                executor.submit(() -> {
                    try {
                        startLatch.await();

                        RateLimitResponse response =
                                rateLimitService.getRateLimit("client-A");

                        if (response.isAllowed()) {
                            allowedCount.incrementAndGet();
                        } else {
                            rejectedCount.incrementAndGet();
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failureCount.incrementAndGet();

                    } catch (Exception e) {
                        failureCount.incrementAndGet();

                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();

            System.out.println("Client A");
            System.out.println("Allowed: " + allowedCount.get());
            System.out.println("Rejected: " + rejectedCount.get());

            assertEquals(0, failureCount.get());
            assertEquals(totalRequests,
                    allowedCount.get() + rejectedCount.get());

            assertEquals(100, allowedCount.get());
            assertEquals(400, rejectedCount.get());

            RateLimitResponse responseB =
                    rateLimitService.getRateLimit("client-B");

            assertTrue(responseB.isAllowed());
            assertEquals(49, responseB.getCurrentTokens());

            System.out.println("Client B");
            System.out.println("Allowed: " + responseB.isAllowed());
            System.out.println("Remaining tokens: "
                    + responseB.getCurrentTokens());

        } finally {
            executor.shutdown();
        }
    }
}
