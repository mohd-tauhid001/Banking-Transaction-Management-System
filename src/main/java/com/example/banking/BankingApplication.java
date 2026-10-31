package com.example.banking;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public final class BankingApplication {
    private final BankingService bankingService;
    private final Scanner scanner;

    private BankingApplication(BankingService bankingService, Scanner scanner) {
        this.bankingService = bankingService;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        Path dataFile = args.length > 0
                ? Path.of(args[0])
                : Path.of("data", "banking-data.txt");
        try {
            BankingService service = new BankingService(new FileBankingRepository(dataFile));
            new BankingApplication(service, new Scanner(System.in)).run();
        } catch (IOException exception) {
            System.err.println("Unable to load or save banking data: " + exception.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        System.out.println("=== Banking Transaction Management System ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option: ");
            try {
                switch (choice) {
                    case "1" -> createAccount();
                    case "2" -> listAccounts();
                    case "3" -> deposit();
                    case "4" -> withdraw();
                    case "5" -> transfer();
                    case "6" -> showBalance();
                    case "7" -> showHistory();
                    case "0" -> running = false;
                    default -> System.out.println("Please choose a valid menu option.");
                }
            } catch (IllegalArgumentException | ArithmeticException exception) {
                System.out.println("Could not complete the request: " + exception.getMessage());
            } catch (IOException exception) {
                System.out.println("Could not save banking data: " + exception.getMessage());
            }
            System.out.println();
        }
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println("""
                1. Create account
                2. List accounts
                3. Deposit
                4. Withdraw
                5. Transfer
                6. Check balance
                7. Transaction history
                0. Exit""");
    }

    private void createAccount() throws IOException {
        Account account = bankingService.createAccount(prompt("Account owner name: "));
        System.out.println("Created account " + account.id() + " for " + account.ownerName() + ".");
    }

    private void listAccounts() {
        List<Account> accounts = bankingService.getAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts have been created.");
            return;
        }
        accounts.forEach(this::printAccount);
    }

    private void deposit() throws IOException {
        String accountId = prompt("Account ID: ");
        long amount = readAmount();
        bankingService.deposit(accountId, amount, prompt("Note (optional): "));
        System.out.println("Deposit completed.");
    }

    private void withdraw() throws IOException {
        String accountId = prompt("Account ID: ");
        long amount = readAmount();
        bankingService.withdraw(accountId, amount, prompt("Note (optional): "));
        System.out.println("Withdrawal completed.");
    }

    private void transfer() throws IOException {
        String from = prompt("From account ID: ");
        String to = prompt("To account ID: ");
        long amount = readAmount();
        bankingService.transfer(from, to, amount, prompt("Note (optional): "));
        System.out.println("Transfer completed.");
    }

    private void showBalance() {
        printAccount(bankingService.getAccount(prompt("Account ID: ")));
    }

    private void showHistory() {
        String accountId = prompt("Account ID: ");
        List<Transaction> transactions = bankingService.getTransactions(accountId);
        if (transactions.isEmpty()) {
            System.out.println("No transactions for this account.");
            return;
        }
        for (Transaction transaction : transactions) {
            System.out.printf("#%d | %s | %s | %s | %s%n",
                    transaction.id(),
                    transaction.timestamp(),
                    transaction.type(),
                    money(transaction.amountCents()),
                    transaction.note().isBlank() ? "-" : transaction.note());
            if (transaction.type() == TransactionType.TRANSFER) {
                System.out.printf("    %s -> %s%n",
                        transaction.sourceAccountId(), transaction.destinationAccountId());
            }
        }
    }

    private void printAccount(Account account) {
        System.out.printf("%s | %s | Balance: %s%n",
                account.id(), account.ownerName(), money(account.balanceCents()));
    }

    private long readAmount() {
        String input = prompt("Amount: ");
        try {
            BigDecimal amount = new BigDecimal(input).setScale(2, RoundingMode.UNNECESSARY);
            long cents = amount.movePointRight(2).longValueExact();
            if (cents < 1) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
            return cents;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Enter a valid amount with at most two decimal places.");
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private static String money(long amountCents) {
        return "$" + BigDecimal.valueOf(amountCents, 2).toPlainString();
    }
}
