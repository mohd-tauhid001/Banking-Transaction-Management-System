package com.example.banking;

import java.time.Instant;

public record Transaction(
        long id,
        Instant timestamp,
        TransactionType type,
        String sourceAccountId,
        String destinationAccountId,
        long amountCents,
        String note) {
    public Transaction {
        if (id < 1) {
            throw new IllegalArgumentException("Transaction ID must be positive.");
        }
        if (timestamp == null || type == null) {
            throw new IllegalArgumentException("Transaction timestamp and type are required.");
        }
        if (amountCents < 1) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }
        if (note == null) {
            note = "";
        }
    }
}
