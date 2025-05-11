# Promotions for payment methods

Java App used application designed to optimize payment assignments and maximize
discounts using a backtracking algorithm. It processes a list of customer orders and available
payment methods (credit/debit cards and loyalty points), and computes the optimal
combination of payments to achieve the highest total savings.

# Requirements
* Java 21
* Maven 3.9.9
# Building
From the project root directory, run:
```shell
mvn clean package
```
# Running
```shell
java -jar Promotions-for-payment-methods-1.0-SNAPSHOT-fat.jar [/file/path/to/orders] [/file/path/to/payment/methods]
```
# Tests
```shell
mvn clean test
```