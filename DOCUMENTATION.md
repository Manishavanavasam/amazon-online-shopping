# Amazon Online Shopping - Project Documentation & Lab Report

**Project Title:** Amazon Online Shopping Console & Web Simulation  
**Architecture:** JavaBean Design Patterns (Encapsulation, Constructors, Getters/Setters, Serializable)  
**Language:** Java (JDK 21+)  
**Repository:** https://github.com/Manishavanavasam/amazon-online-shopping.git  

---

## 1. Project Abstract & Objectives

The **Amazon Online Shopping** application simulates an end-to-end e-commerce order management lifecycle. The project is engineered for academic rigor and software design best practices, emphasizing:

1. **Object-Oriented Programming (OOP) & JavaBeans**: Proper encapsulation, no-arg and parameterized constructors, explicit property getters and setters, and `Serializable` implementation.
2. **Robust Exception Handling**: Total immunity against crashing from invalid user inputs (wrapping Scanner reads in `try-catch` blocks catching `InputMismatchException` and `NumberFormatException`).
3. **Cart & Inventory Management**: Accumulating multiple distinct products into an itemized shopping cart, decrementing product inventory stock upon confirmed checkout, and blocking out-of-stock orders.
4. **Business Logic & Pricing Engine**: Multi-tiered discounts, including an automatic 10% volume discount on cart totals exceeding Rs. 10,000, and coupon codes (`SAVE50`).
5. **Persistence & Auditing**: Automatic logging and appending of order receipts to `orders.txt` for audit trails and historical inspection.
6. **Dual Role Access Control**: Role-separated workflows for Customers (shopping & checkout) and Administrators (live stock management, price updates, catalog additions).
7. **Dual-Mode User Interface**: Interactive CLI application (`AmazonShopping`) and a lightweight, zero-dependency Web UI (`AmazonWebServer`) running on `http://localhost:8080`.

---

## 2. System Architecture & JavaBean Compliance

```
+-------------------------------------------------------------------------------+
|                             Presentation Layer                                |
|  [AmazonShopping Console Interface]   |   [AmazonWebServer Localhost UI]      |
+-------------------------------------------------------------------------------+
                                        |
+-------------------------------------------------------------------------------+
|                              Business Logic Layer                             |
|  - Authentication (3 attempts max)    |   - Discount & Coupon Engine          |
|  - Inventory & Stock Management       |   - Payment Simulation (COD/Card/UPI) |
+-------------------------------------------------------------------------------+
                                        |
+-------------------------------------------------------------------------------+
|                                JavaBeans Layer                                |
|  - Customer.java (Serializable)       |   - Product.java (Serializable)       |
|  - CartItem.java (Serializable)       |                                       |
+-------------------------------------------------------------------------------+
                                        |
+-------------------------------------------------------------------------------+
|                              Persistence Layer                                |
|  - orders.txt (Appended Receipt File)                                         |
+-------------------------------------------------------------------------------+
```

### 2.1. JavaBean Specifications

#### A. `Customer.java`
- **Fields**: `username`, `fullName`, `deliveryAddress`, `contactNumber` (all private).
- **Constructors**: No-argument constructor initializing default blank strings, plus parameterized constructor.
- **Accessors/Mutators**: Standard public getters and setters for all attributes.
- **Serialization**: Implements `java.io.Serializable` with `serialVersionUID`.

#### B. `Product.java`
- **Fields**: `id`, `name`, `price`, `stock` (all private).
- **Constructors**: Default no-argument constructor and parameterized constructor.
- **Business Operations**: `reduceStock(int quantity)` for inventory decrementing.
- **Serialization**: Implements `java.io.Serializable`.

#### C. `CartItem.java`
- **Fields**: `product` (reference to `Product`), `quantity` (int).
- **Subtotal Calculation**: `getSubtotal()` returns `product.getPrice() * quantity`.

---

## 3. Requirements Compliance Matrix

| Requirement | Implementation Detail | Status |
| :--- | :--- | :--- |
| **1. Login System** | Enforces credentials (`amazon` / `1234`). Allows up to 3 attempts with graceful rejection. | **Passed** |
| **2. Customer Details** | Collects customer name, delivery address, and phone number post-authentication. | **Passed** |
| **3. Product Catalog** | Catalog menu using `switch-case` to assign product and pricing details. | **Passed** |
| **4. Quantity Validation** | Validates positive integers and prevents ordering more units than in stock. | **Passed** |
| **5. Order Summary** | Full formatted receipt displaying customer info, line items, subtotals, and totals. | **Passed** |
| **6. Multi-Purchase Loop** | `(y/n)` loop permitting consecutive shopping transactions in one session. | **Passed** |
| **7. JavaBean Design** | `Customer` & `Product` strictly follow JavaBean standards and implement `Serializable`. | **Passed** |
| **8. Robust Input Resilience**| Explicit `try-catch` catching `InputMismatchException` so bad inputs never crash the app. | **Passed** |
| **9. Inline Lab Comments** | Detailed code documentation explaining blocks, parameters, and algorithms. | **Passed** |
| **10. Sample Run Output** | Documented execution trace included in report and console logs. | **Passed** |
| **11. Shopping Cart** | `CartItem` list tracking multiple items before finalizing order. | **Passed** |
| **12. Inventory Stock** | Stock decrements upon checkout; warns "Out of stock" / "Only X left". | **Passed** |
| **13. Discounts & Coupons** | Automatic 10% discount for orders > Rs. 10,000 + flat Rs. 50 off with `SAVE50`. | **Passed** |
| **14. Payment Simulation** | Interactive simulation of Cash on Delivery (COD), Card, and UPI payments. | **Passed** |
| **15. Order Persistence** | Completed orders appended to `orders.txt` with past order inspection feature. | **Passed** |
| **16. Admin Console** | Second role (`admin` / `admin123`) to view stock, edit prices, and add products. | **Passed** |
| **17. Zero-Crash Guarantees** | Every token/line read guarded against blank inputs and format mismatches. | **Passed** |

---

## 4. Input Handling & Exception Architecture

A critical objective is zero-crash execution. The program solves standard Scanner pitfalls (such as trailing newline skipping) through encapsulated validation methods:

```java
private static int readIntChoice(String prompt, int min, int max) {
    while (true) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Input cannot be empty. Please enter a valid number.");
            continue;
        }
        try {
            Scanner tokenScanner = new Scanner(input);
            int value = tokenScanner.nextInt();
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println("Out of bounds! Please enter a number between " + min + " and " + max + ".");
            }
        } catch (InputMismatchException e) {
            System.out.println("Invalid input! Non-numeric characters entered. Please input a valid integer.");
        } catch (NoSuchElementException e) {
            System.out.println("No input detected. Please enter a valid integer.");
        }
    }
}
```

---

## 5. Sample Execution Log

```
=======================================================
FINAL ORDER SUMMARY
=======================================================
Date & Time      : 2026-09-25 22:18:59
Customer Name    : Priya Sharma
Contact Number   : 9876543210
Delivery Address : 14/B Green Glen Layout, Bellandur, Bengaluru
Payment Method   : UPI
-------------------------------------------------------
Item               Qty   Price (Rs.)    Subtotal (Rs.)
-------------------------------------------------------
Laptop             1     55000.00       55000.00      
Headphones         2     2499.00        4998.00       
-------------------------------------------------------
Subtotal         : Rs. 59,998.00
10% Volume Disc  : -Rs. 5,999.80
Coupon Discount  : -Rs. 50.00
Grand Total Paid : Rs. 53,948.20
=======================================================
>> Status: Confirmed & Dispatched for Delivery!
[PERSISTENCE] Order details successfully recorded in 'orders.txt'.
```

---

## 6. How to Run

### Console Application
```bash
javac Customer.java Product.java CartItem.java AmazonShopping.java
java AmazonShopping
```

### Web Application (Localhost:8080)
```bash
javac Customer.java Product.java CartItem.java AmazonWebServer.java
java AmazonWebServer
```
Navigate to: `http://localhost:8080`

---

## 7. Future Enhancements

1. **Database Integration**: Migrating from in-memory catalog and `orders.txt` to an ACID-compliant relational database (MySQL/PostgreSQL) using JDBC or Hibernate.
2. **Security & Authentication**: Adding salted hashing (BCrypt) for passwords and JWT/session tokens.
3. **Third-Party Payment Gateways**: Integrating sandbox environments for Razorpay or Stripe.
4. **Automated Invoicing**: Generating signed PDF invoices via Apache PDFBox or iText.
