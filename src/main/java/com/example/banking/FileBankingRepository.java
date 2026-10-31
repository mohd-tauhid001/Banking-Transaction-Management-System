package com.example.banking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileBankingRepository implements BankingRepository {
    private static final String HEADER = "BANKING_DATA_V1";
    private final Path file;

    public FileBankingRepository(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    @Override
    public BankingData load() throws IOException {
        if (!Files.exists(file)) {
            return BankingData.empty();
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new IOException("Bank data file has an invalid or unsupported format: " + file);
        }

        Map<String, Account> accounts = new LinkedHashMap<>();
        List<Transaction> transactions = new ArrayList<>();
        try {
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\\|", -1);
                switch (fields[0]) {
                    case "A" -> {
                        requireFieldCount(fields, 4);
                        String id = fields[1];
                        Account account = new Account(
                                id,
                                decode(fields[2]),
                                Long.parseLong(fields[3]));
                        if (accounts.putIfAbsent(id, account) != null) {
                            throw new IOException("Duplicate account ID in bank data file: " + id);
                        }
                    }
                    case "T" -> {
                        requireFieldCount(fields, 8);
                        transactions.add(new Transaction(
                                Long.parseLong(fields[1]),
                                Instant.parse(fields[2]),
                                TransactionType.valueOf(fields[3]),
                                emptyToNull(fields[4]),
                                emptyToNull(fields[5]),
                                Long.parseLong(fields[6]),
                                decode(fields[7])));
                    }
                    default -> throw new IOException("Unknown record on line " + (i + 1) + ".");
                }
            }
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new IOException("Bank data file contains invalid data: " + file, exception);
        }
        return new BankingData(accounts, transactions);
    }

    @Override
    public void save(BankingData data) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        String prefix = file.getFileName().toString();
        if (prefix.length() < 3) {
            prefix = (prefix + "___").substring(0, 3);
        }
        Path tempFile = Files.createTempFile(parent, prefix, ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (Account account : data.accounts().values()) {
                lines.add(String.join("|",
                        "A",
                        account.id(),
                        encode(account.ownerName()),
                        Long.toString(account.balanceCents())));
            }
            for (Transaction transaction : data.transactions()) {
                lines.add(String.join("|",
                        "T",
                        Long.toString(transaction.id()),
                        transaction.timestamp().toString(),
                        transaction.type().name(),
                        nullToEmpty(transaction.sourceAccountId()),
                        nullToEmpty(transaction.destinationAccountId()),
                        Long.toString(transaction.amountCents()),
                        encode(transaction.note())));
            }
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void requireFieldCount(String[] fields, int expected) throws IOException {
        if (fields.length != expected) {
            throw new IOException("Invalid record field count in bank data file.");
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
