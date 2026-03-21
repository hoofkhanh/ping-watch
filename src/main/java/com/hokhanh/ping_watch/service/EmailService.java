package com.hokhanh.ping_watch.service;

public interface EmailService {
    void send(String to, String subject, String body);
}
