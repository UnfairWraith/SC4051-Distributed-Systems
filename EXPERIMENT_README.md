# Distributed Banking System Experiment Guide

## Run Commands

### 1. Compile the Java server

```powershell
javac -d out src/ServerJava/*.java
```

### 2. Compile the C++ client

```powershell
g++ -std=c++17 src/Client/Client.cpp src/Client/ConnectionManager.cpp src/Common/Protocol.cpp -I src/Client -I src/Common -lws2_32 -o client.exe
```

Java server argument format:
`java -cp out ServerJava.BankServer <port> <semantics> <requestLossRate> <replyLossRate>`

- `port`: UDP port used by the server, normally `2222`
- `semantics`: either `at-most-once` or `at-least-once`
- `requestLossRate`: probability from `0.0` to `1.0` that an incoming request is dropped
- `replyLossRate`: probability from `0.0` to `1.0` that an outgoing reply is dropped

### 3. Run the server in at-most-once mode

```powershell
java -cp out ServerJava.BankServer 2222 at-most-once 0.0 0.5
```

### 4. Run the server in at-least-once mode

```powershell
java -cp out ServerJava.BankServer 2222 at-least-once 0.0 0.5
```

### 5. Run the client

```powershell
.\client.exe 127.0.0.1
```

## Experiment Setup

Use reply loss to trigger client retries while allowing the server to receive and process the original request.

Recommended setup:

1. Start the server with `requestLossRate=0.0` and `replyLossRate=0.5`.
2. Start the client and point it at the server IP.
3. Open two accounts with the same currency.
4. Use `Transfer Money` as the non-idempotent operation.
5. Use `Transaction History` as the idempotent operation.
6. Repeat the same experiment once with `at-least-once` and once with `at-most-once`.

Suggested account setup:

1. Create account A with balance `1000 SGD`
2. Create account B with balance `1000 SGD`
3. Transfer `100 SGD` from A to B

## Expected Results

### At-Least-Once

- If the server processes the request but the reply is lost, the client retries.
- The transfer request may be executed again.
- This can cause money to be transferred more than once.
- The source account balance may decrease multiple times for one logical user request.

### At-Most-Once

- If the server processes the request but the reply is lost, the client retries.
- The server detects the duplicate request using the same `requestId`.
- The server returns the cached reply instead of executing the transfer again.
- The source account balance changes only once.

### Idempotent Operation

When `Transaction History` is retried, the request may be executed multiple times, but it does not corrupt state because it only reads data.

## Why the Two Semantics Differ

### At-Least-Once

At-least-once means the client keeps retrying until it gets a reply or runs out of attempts.  
If a reply is lost, the same request can reach the server multiple times.  
For a non-idempotent operation like `Transfer Money`, repeated execution changes the result incorrectly.

### At-Most-Once

At-most-once also allows retries, but the server stores reply history for processed requests.  
When the same client sends the same `requestId` again, the server recognizes it as a duplicate and sends back the old reply instead of performing the operation again.  
That prevents duplicate execution of non-idempotent operations.

## Notes

- `Transfer Money` is the extra non-idempotent operation.
- `Transaction History` is the extra idempotent operation.
