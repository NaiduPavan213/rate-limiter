package com.example.ratelimiter.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RateLimitState {

    @Id
    private String clientKey;

    private int currentTokens;

    private Instant lastRefillTime;

    private double fractionLeft;

    public String getClientKey(){
        return clientKey;
    }
    public void setClientKey(String clientKey){
        this.clientKey = clientKey;
    }

    public int getCurrentTokens(){
        return currentTokens;
    }
    public void setCurrentTokens(int tokens){
        this.currentTokens = tokens;
    }

    public Instant getLastRefillTime(){
        return lastRefillTime;
    }
    public void setLastRefillTime(Instant lastRefillTime){
        this.lastRefillTime = lastRefillTime;
    }

    public double getFractionLeft(){
        return fractionLeft;
    }
    public void setFractionLeft(double fractionLeft){
        this.fractionLeft = fractionLeft;
    }

}