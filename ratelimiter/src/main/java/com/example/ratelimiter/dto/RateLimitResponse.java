package com.example.ratelimiter.dto;

public class RateLimitResponse {
    private int requestsPerSecond;
    private int burstSize;
    private int currentTokens;
    private boolean allowed;
    private double resetAfterSeconds;
    private int limit;

    public int getRequestsPerSecond(){
        return requestsPerSecond;
    }

    public int getBurstSize(){
        return burstSize;
    }

    public int getCurrentTokens(){
        return currentTokens;
    }

    public boolean isAllowed(){
        return allowed;
    }

    public double getResetAfterSeconds() {
        return resetAfterSeconds;
    }

    public int getLimit(){
        return limit;
    }

    public void setLimit(int limit){
        this.limit = limit;
    }

    public void setRequestsPerSecond(int requestsPerSecond){
        this.requestsPerSecond = requestsPerSecond;
    }

    public void setBurstSize(int burstSize){
        this.burstSize = burstSize;
    }

    public void setCurrentTokens(int currentTokens){
        this.currentTokens = currentTokens;
    }

    public void setAllowed(boolean allowed){
        this.allowed = allowed;
    }

    public void setResetAfterSeconds(double resetAfterSeconds) {
        this.resetAfterSeconds = resetAfterSeconds;
    }
}
