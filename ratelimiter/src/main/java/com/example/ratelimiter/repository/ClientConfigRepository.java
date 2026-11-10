package com.example.ratelimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ratelimiter.model.ClientConfig;

public interface ClientConfigRepository extends JpaRepository<ClientConfig , String>{

}
