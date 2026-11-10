package com.example.ratelimiter.dto;

import java.time.Instant;

public class RateLimitResult{
    private final boolean allowed;
    private final int currentTokens;
    private final Instant lastRefillTime;
    private final double fractionLeft;
    private final double resetAfterSeconds;

    public RateLimitResult(boolean allowed , int currentTokens , double resetAfterSeconds){
        this.allowed = allowed;
        this.currentTokens = currentTokens;
        this.resetAfterSeconds = resetAfterSeconds;
        this.lastRefillTime = null;
        this.fractionLeft = 0.0;
    }

    public RateLimitResult(boolean allowed , int currentTokens , Instant lastRefillTime , double fractionLeft , double resetAfterSeconds){
        this.allowed = allowed;
        this.currentTokens = currentTokens;
        this.lastRefillTime = lastRefillTime;
        this.fractionLeft = fractionLeft;
        this.resetAfterSeconds = resetAfterSeconds;

    }

    public boolean isAllowed(){
        return allowed;
    }

    public int getCurrentTokens(){
        return currentTokens;
    }

    public Instant getLastRefillTime(){
        return lastRefillTime;
    }

    public double getFractionLeft(){
        return fractionLeft;
    }

    public double getResetAfterSeconds() {
        return resetAfterSeconds;
    }
}