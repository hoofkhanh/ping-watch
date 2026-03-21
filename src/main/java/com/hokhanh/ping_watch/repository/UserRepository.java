package com.hokhanh.ping_watch.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hokhanh.ping_watch.model.User;


public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
    User findByEmail(String email);
}
