# Banking Transaction Management System

A Java 17+ console application for managing local demo accounts and transactions. It supports account creation, deposits, withdrawals, transfers, balance lookup, and per-account transaction history. Data is saved locally in a text file and reloaded on the next run.

> This is an educational example, not production banking software. It does not connect to a bank or provide authentication, encryption, or regulatory controls. Do not store real customer or financial data in it.

## Run

From the project directory, compile the source files:

```powershell
New-Item -ItemType Directory -Force out
javac -d out (Get-ChildItem -Recurse src\main\java\*.java).FullName
```

Start the application:

```powershell
java -cp out com.example.banking.BankingApplication
```

By default, the application writes to `data\banking-data.txt`. To use another file, pass its path as the first argument:

```powershell
java -cp out com.example.banking.BankingApplication D:\path\to\bank-data.txt
```

Amounts are entered in dollars with up to two decimal places. Account balances use integer cents internally to avoid floating-point rounding errors.
