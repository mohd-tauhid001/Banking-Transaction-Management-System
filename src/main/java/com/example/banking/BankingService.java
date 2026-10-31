package com.example.banking;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BankingService {
    private final BankingRepository repository;
    private BankingData data;
    private long nextAccountNumber = 1;
    private long nextTransactionId = 1;

    public BankingService(BankingRepository repository) throws IOException {
        this.repository = repository;
        this.data = repository.load();
        for (String id : data.accounts().keySet()) {
            if (id.startsWith("ACC")) {
                try {
                    nextAccountNumber = Math.max(nextAccountNumber, Long.parseLong(id.substring(3)) + 1);
                } catch (NumberFormatException exception) {
                    throw new IOException("Invalid account ID in bank data: " + id, exception);
                }
            }
        }
        for (Transaction transaction : data.transactions()) {
            nextTransactionId = Math.max(nextTransactionId, transaction.id() + 1);
        }
    }

    public Account createAccount(String ownerName) throws IOException {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be blank.");
        }
        String accountId = "ACC%06d".formatted(nextAccountNumber);
        Account account = new Account(accountId, ownerName.trim(), 0);
        Map<String, Account> accounts = new LinkedHashMap<>(data.accounts());
        accounts.put(accountId, account);
        save(new BankingData(accounts, data.transactions()));
        nextAccountNumber++;
        return account;
    }

    public List<Account> getAccounts() {
        return List.copyOf(data.accounts().values());
    }

    public Account getAccount(String accountId) {
        return requireAccount(accountId);
    }

    public void deposit(String accountId, long amountCents, String note) throws IOException {
        requirePositiveAmount(amountCents);
        Account account = requireAccount(accountId);
        Account updated = new Account(account.id(), account.ownerName(),
                Math.addExact(account.balanceCents(), amountCents));
        List<Transaction> transactions = withTransaction(
                TransactionType.DEPOSIT, null, accountId, amountCents, note);
        Map<String, Account> accounts = new LinkedHashMap<>(data.accounts());
        accounts.put(accountId, updated);
        save(new BankingData(accounts, transactions));
    }

    public void withdraw(String accountId, long amountCents, String note) throws IOException {
        requirePositiveAmount(amountCents);
        Account account = requireAccount(accountId);
        if (account.balanceCents() < amountCents) {
            throw new IllegalArgumentException("Insufficient funds.");
        }
        Account updated = new Account(account.id(), account.ownerName(),
                account.balanceCents() - amountCents);
        List<Transaction> transactions = withTransaction(
                TransactionType.WITHDRAWAL, accountId, null, amountCents, note);
        Map<String, Account> accounts = new LinkedHashMap<>(data.accounts());
        accounts.put(accountId, updated);
        save(new BankingData(accounts, transactions));
    }

    public void transfer(String fromAccountId, String toAccountId, long amountCents, String note)
            throws IOException {
        requirePositiveAmount(amountCents);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        Account source = requireAccount(fromAccountId);
        Account destination = requireAccount(toAccountId);
        if (source.balanceCents() < amountCents) {
            throw new IllegalArgumentException("Insufficient funds.");
        }

        Map<String, Account> accounts = new LinkedHashMap<>(data.accounts());
        accounts.put(fromAccountId, new Account(source.id(), source.ownerName(),
                source.balanceCents() - amountCents));
        accounts.put(toAccountId, new Account(destination.id(), destination.ownerName(),
                Math.addExact(destination.balanceCents(), amountCents)));
        List<Transaction> transactions = withTransaction(
                TransactionType.TRANSFER, fromAccountId, toAccountId, amountCents, note);
        save(new BankingData(accounts, transactions));
    }

    public List<Transaction> getTransactions(String accountId) {
        requireAccount(accountId);
        return data.transactions().stream()
                .filter(transaction -> accountId.equals(transaction.sourceAccountId())
                        || accountId.equals(transaction.destinationAccountId()))
                .toList();
    }

    private Account requireAccount(String accountId) {
        Account account = data.accounts().get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return account;
    }

    private static void requirePositiveAmount(long amountCents) {
        if (amountCents < 1) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    private List<Transaction> withTransaction(
            TransactionType type, String source, String destination, long amountCents, String note) {
        List<Transaction> transactions = new ArrayList<>(data.transactions());
        transactions.add(new Transaction(nextTransactionId, Instant.now(), type,
                source, destination, amountCents, note));
        return transactions;
    }

    private void save(BankingData updatedData) throws IOException {
        repository.save(updatedData);
        data = updatedData;
        if (!updatedData.transactions().isEmpty()) {
            nextTransactionId = updatedData.transactions().get(updatedData.transactions().size() - 1).id() + 1;
        }
    }
}
