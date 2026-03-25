@echo off
echo Starting Bank Server...
java -cp out ServerJava.BankServer 2222 at-most-once 0.0 0.5
pause