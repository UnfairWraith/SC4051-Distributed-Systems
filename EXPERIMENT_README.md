# Distributed Banking System Experiment Guide

## Purpose

This experiment compares the behavior of the system under:

- `at-least-once` semantics
- `at-most-once` semantics

The experiment uses:

- `Transaction History` as the idempotent operation
- `Transfer Money` as the non-idempotent operation

The goal is to show that:

- repeated execution of an idempotent operation does not corrupt state
- repeated execution of a non-idempotent operation can produce incorrect results under `at-least-once`
- `at-most-once` avoids duplicate execution by reusing the cached reply for the same client and `requestId`

## Build Commands

### 1. Compile the Java server

```powershell
javac -d out src/ServerJava/*.java
```

### 2. Compile the C++ client

```powershell
g++ -std=c++17 src/Client/Client.cpp src/Client/ConnectionManager.cpp src/Common/Protocol.cpp -I src/Client -I src/Common -lws2_32 -o client.exe
```

## Run Commands

Java server argument format:

`java -cp out ServerJava.BankServer <port> <semantics> <requestLossRate> <replyLossRate>`

- `port`: UDP port used by the server, normally `2222`
- `semantics`: either `at-most-once` or `at-least-once`
- `requestLossRate`: probability from `0.0` to `1.0` that an incoming request is dropped
- `replyLossRate`: probability from `0.0` to `1.0` that an outgoing reply is dropped

### Run the server in at-most-once mode

```powershell
java -cp out ServerJava.BankServer 2222 at-most-once 0.0 0.5
```

### Run the server in at-least-once mode

```powershell
java -cp out ServerJava.BankServer 2222 at-least-once 0.0 0.5
```

### Run the client

```powershell
.\client.exe <server-ip>
```

- `server-ip`: IP address of the machine running the Java server, for example `127.0.0.1` when both programs run on the same machine

## Recommended Experiment Setup

Use reply loss to trigger client retries while still allowing the server to receive and process the request.

Recommended configuration:

- `requestLossRate = 0.0`
- `replyLossRate = 0.5`
- client retry limit = `5` attempts

This matches the current implementation, where the client retries when no valid reply is received before timeout.

## Suggested Account Setup

Create two accounts with the same currency:

1. Create account Alice with balance `1000 SGD`
2. Create account Bob with balance `1000 SGD`
3. Record their assigned account numbers

These two accounts will be used for the transfer experiment.

## Experiment Procedure

### Part A: Idempotent operation using `Transaction History`

1. Start the server in `at-least-once` mode:

```powershell
java -cp out ServerJava.BankServer 2222 at-least-once 0.0 0.5
```

2. Start the client:

```powershell
.\client.exe <server-ip>
```

3. Invoke `Transaction History` for one of the test accounts.
4. If the reply is lost, the client will retry automatically.
5. Observe that the operation may be executed more than once, but the account state remains unchanged because the operation only reads data.

6. Repeat the same test in `at-most-once` mode:

```powershell
java -cp out ServerJava.BankServer 2222 at-most-once 0.0 0.5
```

7. Observe that duplicate requests with the same `requestId` return the cached reply instead of being processed again.

### Part B: Non-idempotent operation using `Transfer Money`

1. Start the server in `at-least-once` mode:

```powershell
java -cp out ServerJava.BankServer 2222 at-least-once 0.0 0.5
```

2. Start the client and transfer `100 SGD` from Alice to Bob.
3. If the reply is lost, the client will retry automatically using the same request.
4. Under `at-least-once`, the server may process the same transfer more than once.
5. Check the balances or transaction history to confirm whether the transfer was applied multiple times.

6. Repeat the same transfer experiment in `at-most-once` mode:

```powershell
java -cp out ServerJava.BankServer 2222 at-most-once 0.0 0.5
```

7. Observe that the server detects duplicate requests using the same client identity and `requestId`, and returns the previously generated reply instead of applying the transfer again.

## Expected Results

### At-Least-Once

- the client retries when a reply is lost
- the server may execute the same request multiple times
- `Transaction History` remains correct because it is idempotent
- `Transfer Money` may be applied multiple times because it is non-idempotent
- this can cause the source balance to decrease multiple times and the destination balance to increase multiple times

### At-Most-Once

- the client still retries when a reply is lost
- the server keeps a reply history for processed requests
- if the same client sends the same `requestId` again, the cached reply is returned
- `Transaction History` remains correct
- `Transfer Money` is applied only once, even if the client retransmits

## Why the Two Semantics Differ

### At-Least-Once

At-least-once means the client keeps retrying until it gets a reply or runs out of attempts.  
If a reply is lost, the same request may reach the server again.  
For a non-idempotent operation like `Transfer Money`, repeated execution can change the final result incorrectly.

### At-Most-Once

At-most-once also allows retries, but the server stores reply history for processed requests.  
When the same client sends the same `requestId` again, the server recognizes it as a duplicate and returns the old reply instead of performing the operation again.  
This prevents duplicate execution of non-idempotent operations.

## Notes

- `Transaction History` is the idempotent extra operation.
- `Transfer Money` is the non-idempotent extra operation.
- For this experiment, reply loss is more useful than request loss because it allows the server to process the original request while still forcing client retries.
- If no retry happens in a particular run, repeat the operation until reply loss occurs and the retry behavior is visible.
