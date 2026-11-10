package com.example.ratelimiter.service;

import java.time.Instant;
import com.example.ratelimiter.model.ClientConfig;
import com.example.ratelimiter.model.RateLimitState;
import com.example.ratelimiter.dto.RateLimitResult;


public class TokenBucket {
    private int currentTokens;
    private Instant lastRefillTime = Instant.now();
    private double fractionLeft;
    private double resetAfterSeconds;

    public TokenBucket(ClientConfig config){
        this.currentTokens = config.getBurstSize();
    }

    public TokenBucket(RateLimitState rateLimitState){
        this.currentTokens = rateLimitState.getCurrentTokens();
        this.lastRefillTime = rateLimitState.getLastRefillTime();
        this.fractionLeft = rateLimitState.getFractionLeft();
    }

    public synchronized int getCurrentTokens(){
        return currentTokens;
    }


    private boolean consumeToken(){
        if(this.currentTokens > 0){
            this.currentTokens--;
            return true;
        }
        return false;
    }

    public synchronized RateLimitResult tryConsume(ClientConfig client){
        refill(client);

        boolean allowed = consumeToken();

        if(this.currentTokens > 0){
            this.resetAfterSeconds = 0;
        }
        else{
            this.resetAfterSeconds = (1 - fractionLeft) / client.getRequestsPerSecond();
        }

        RateLimitResult res = new RateLimitResult(allowed, currentTokens , lastRefillTime , fractionLeft , resetAfterSeconds);
        return res;
    }

    private void refill(ClientConfig config){

        Instant current = Instant.now();

        long elapsedMillis = current.toEpochMilli() - this.lastRefillTime.toEpochMilli();

        double elapsedSeconds = elapsedMillis / 1000.0;

        double tokensEarned = elapsedSeconds * config.getRequestsPerSecond();

        tokensEarned += this.fractionLeft;

        int tokensToAdd = (int) tokensEarned;

        this.fractionLeft = tokensEarned - tokensToAdd;

        this.currentTokens = Math.min(config.getBurstSize() , tokensToAdd + this.currentTokens);

        this.lastRefillTime = current;
    }
}
