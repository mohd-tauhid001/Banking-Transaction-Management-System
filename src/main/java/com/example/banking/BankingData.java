package com.example.banking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BankingData(Map<String, Account> accounts, List<Transaction> transactions) {
    public BankingData {
        accounts = Collections.unmodifiableMap(new LinkedHashMap<>(accounts));
        transactions = List.copyOf(transactions);
    }

    public static BankingData empty() {
        return new BankingData(Map.of(), List.of());
    }
}
