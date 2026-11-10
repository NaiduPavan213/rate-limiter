package com.example.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.ratelimiter.dto.RateLimitResponse;
import com.example.ratelimiter.dto.RateLimitResult;
import com.example.ratelimiter.exception.ClientNotFoundException;
import com.example.ratelimiter.dto.ClientConfigPatchDTO;
import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.model.RateLimitMode;
import com.example.ratelimiter.model.RateLimitState;
import com.example.ratelimiter.repository.RateLimitStateRepository;
import com.example.ratelimiter.repository.SlidingWindowRequestRepository;

@Service
public class RateLimitService {

    private final ClientConfigService clientConfigService;
    private final RateLimitStateRepository rateLimitStateRepository;
    private final SlidingWindowRequestRepository slidingWindowRequestRepository;

    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    private final Map<String, SlidingWindow> slidingWindows = new ConcurrentHashMap<>();

    private final Map<String, Object> clientLocks = new ConcurrentHashMap<>();

    public RateLimitService(ClientConfigService clientConfigService , RateLimitStateRepository rateLimitStateRepository , SlidingWindowRequestRepository slidingWindowRequestRepository) {
        this.clientConfigService = clientConfigService;
        this.rateLimitStateRepository = rateLimitStateRepository;
        this.slidingWindowRequestRepository = slidingWindowRequestRepository;
    }

    public RateLimitResponse getRateLimit(String clientKey) {

        Object lock = getClientLock(clientKey);

        synchronized(lock){

            ClientConfig client = clientConfigService.getClient(clientKey);

            if (client == null) {
                throw new ClientNotFoundException("Client not found");
            }

            RateLimitResult result;
            int limit;

            if (client.getMode() == RateLimitMode.TOKEN_BUCKET) {
                TokenBucket bucket = tokenBuckets.computeIfAbsent(clientKey, key -> {
                Optional<RateLimitState> state =
                        rateLimitStateRepository.findById(key);

                return state.map(TokenBucket::new)
                        .orElseGet(() -> new TokenBucket(client));
                });

                result = bucket.tryConsume(client);
                limit = client.getBurstSize();

                RateLimitState state = new RateLimitState();

                state.setClientKey(clientKey);
                state.setCurrentTokens(result.getCurrentTokens());
                state.setLastRefillTime(result.getLastRefillTime());
                state.setFractionLeft(result.getFractionLeft());

                rateLimitStateRepository.save(state);

            } else {

                SlidingWindow window = slidingWindows.computeIfAbsent(
                        clientKey,
                        key -> new SlidingWindow(slidingWindowRequestRepository)
                );

                result = window.tryConsume(client);
                limit = client.getRequestsPerSecond();
            }



            RateLimitResponse response = new RateLimitResponse();

            response.setBurstSize(client.getBurstSize());
            response.setRequestsPerSecond(client.getRequestsPerSecond());
            response.setCurrentTokens(result.getCurrentTokens());
            response.setAllowed(result.isAllowed());
            response.setResetAfterSeconds(result.getResetAfterSeconds());
            response.setLimit(limit);

            return response;

        }
    }

    public void updateClient(String clientKey , ClientConfigPatchDTO patch){

        Object lock = getClientLock(clientKey);

        synchronized(lock){

            ClientConfig client = clientConfigService.getClient(clientKey);

            if(client == null){
                throw new ClientNotFoundException("Client doesn't exist");
            }

            ClientConfig proposedConfig = new ClientConfig();

            updateProposedConfig(client , patch, proposedConfig);

            clientConfigService.validateProposedConfig(proposedConfig);

            if(client.getMode() != proposedConfig.getMode()){
                resetRateLimiterState(clientKey);
            }
            clientConfigService.updateClientConfig(clientKey, patch);
        }
    }

    private void resetRateLimiterState(String clientKey){
        tokenBuckets.remove(clientKey);
        slidingWindows.remove(clientKey);
        rateLimitStateRepository.deleteById(clientKey);
    }

    private void updateProposedConfig(ClientConfig client , ClientConfigPatchDTO patch, ClientConfig proposedConfig){
        if(patch.getRequestsPerSecond() != null){
            proposedConfig.setRequestsPerSecond(patch.getRequestsPerSecond());
        }
        else{
            proposedConfig.setRequestsPerSecond(client.getRequestsPerSecond());
        }

        if(patch.getBurstSize() != null){
            proposedConfig.setBurstSize(patch.getBurstSize());
        }
        else{
            proposedConfig.setBurstSize(client.getBurstSize());
        }

        if(patch.getMode() != null){
            proposedConfig.setMode(patch.getMode());
        }
        else{
            proposedConfig.setMode(client.getMode());
        }
    }

    private Object getClientLock(String clientKey) {
        return clientLocks.computeIfAbsent(
            clientKey,
            key -> new Object()
        );
    }
}