package com.example.ratelimiter.repository;

import java.util.List;
import java.time.Instant;


import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ratelimiter.model.SlidingWindowRequest;
import org.springframework.transaction.annotation.Transactional;





public interface SlidingWindowRequestRepository extends JpaRepository<SlidingWindowRequest, Long>{
    List<SlidingWindowRequest> findByClientKeyAndRequestTimeAfter(String clientKey, Instant requestTime);

    @Transactional
    void deleteByClientKeyAndRequestTimeBefore(String clientKey, Instant requestTime);
}
