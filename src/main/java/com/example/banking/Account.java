package com.example.banking;

public record Account(String id, String ownerName, long balanceCents) {
    public Account {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be blank.");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Account owner cannot be blank.");
        }
        if (balanceCents < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative.");
        }
    }
}
