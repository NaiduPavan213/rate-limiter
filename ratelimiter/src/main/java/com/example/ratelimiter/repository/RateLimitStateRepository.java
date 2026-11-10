package com.example.ratelimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ratelimiter.model.RateLimitState;

public interface RateLimitStateRepository extends JpaRepository<RateLimitState, String> {
}