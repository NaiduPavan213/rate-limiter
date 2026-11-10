
package com.example.ratelimiter.service;

import java.time.Instant;
import java.util.List;

import com.example.ratelimiter.dto.RateLimitResult;
import com.example.ratelimiter.repository.SlidingWindowRequestRepository;
import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.model.SlidingWindowRequest;

import org.springframework.transaction.annotation.Transactional;

public class SlidingWindow {

    private final SlidingWindowRequestRepository slidingWindowRequestRepository;

    public SlidingWindow(SlidingWindowRequestRepository slidingWindowRequestRepository) {
        this.slidingWindowRequestRepository = slidingWindowRequestRepository;
    }

	@Transactional
    public synchronized RateLimitResult tryConsume(ClientConfig client) {

        Instant now = Instant.now();
        Instant cutoffTime = now.minusSeconds(1);

		slidingWindowRequestRepository.deleteByClientKeyAndRequestTimeBefore(client.getClientKey(), cutoffTime);

        // Fetch recent requests belonging to this client
        List<SlidingWindowRequest> recentRequests =
                slidingWindowRequestRepository
                        .findByClientKeyAndRequestTimeAfter(
                                client.getClientKey(), cutoffTime);

        int currentCount = recentRequests.size();
        int limit = client.getRequestsPerSecond();

        // Reject if the window is full
        if (currentCount >= limit) {

            Instant oldestRequest = recentRequests.stream()
                    .map(SlidingWindowRequest::getRequestTime)
                    .min(Instant::compareTo)
                    .orElseThrow();

            double resetAfterSeconds =
                    (oldestRequest.plusSeconds(1).toEpochMilli()
                            - now.toEpochMilli()) / 1000.0;

            return new RateLimitResult(
                    false,
                    0,
                    null,
                    0.0,
                    resetAfterSeconds
            );
        }

        // Allow the request and persist its timestamp
        SlidingWindowRequest request = new SlidingWindowRequest();
        request.setClientKey(client.getClientKey());
        request.setRequestTime(now);

        slidingWindowRequestRepository.save(request);

        int remaining = limit - (currentCount + 1);

        // Calculate when the oldest request expires
        Instant oldestRequest = recentRequests.isEmpty()
                ? now
                : recentRequests.stream()
                        .map(SlidingWindowRequest::getRequestTime)
                        .min(Instant::compareTo)
                        .orElseThrow();

        double resetAfterSeconds =
                (oldestRequest.plusSeconds(1).toEpochMilli()
                        - now.toEpochMilli()) / 1000.0;

        return new RateLimitResult(
                true,
                remaining,
                null,
                0.0,
                resetAfterSeconds
        );
    }
}
