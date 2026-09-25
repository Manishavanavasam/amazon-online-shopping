# Amazon Online Shopping - Console E-Commerce Application

A modular, console-based Java application simulating a complete e-commerce ordering system built using **JavaBean design principles**, robust input validation, shopping cart functionality, inventory tracking, tiered discounts, simulated payments, file persistence, and an administrative console.

---

## 🛠 Project Structure

- [Customer.java](Customer.java): JavaBean representing customer profile (encapsulated private fields, no-arg and parameterized constructors, getters/setters, `Serializable`).
- [Product.java](Product.java): JavaBean representing items in the catalog (ID, name, price, stock, getters/setters, `Serializable`).
- [CartItem.java](CartItem.java): JavaBean representing items in a user's active shopping cart (holds `Product` reference and quantity, computes subtotals).
- [AmazonShopping.java](AmazonShopping.java): Main console driver class managing authentication, menus, cart, checkout, discounts, and admin operations.
- [AmazonWebServer.java](AmazonWebServer.java): Built-in zero-dependency Java HTTP server providing a full interactive Web UI on `http://localhost:8080`.
- `orders.txt`: Persistent text file storing all completed orders (appended automatically).

---

## 🚀 How to Run

### Option 1: Web Interface (Localhost)
1. Run the built-in Java web server:
   ```bash
   java AmazonWebServer
   ```
2. Open your web browser:
   👉 **[http://localhost:8080](http://localhost:8080)**

### Option 2: Console Interface
1. Run the interactive console application:
   ```bash
   java AmazonShopping
   ```

---

## 🔑 Login Credentials

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Customer** | `amazon` | `1234` | Max 3 login attempts. Allows shopping, cart management, and checkout. |
| **Admin** | `admin` | `admin123` | Allows viewing stock, replenishing stock, updating prices, and adding products. |

---

## 🌟 Key Features

1. **Authentication & Graceful Error Recovery**: Rejects invalid credentials gracefully and enforces a 3-attempt limit.
2. **Robust Input Handling**: Scanner input is wrapped in `try-catch` blocks catching `InputMismatchException` and formatting errors; non-numeric inputs and out-of-bounds options never crash the program.
3. **Multi-Item Shopping Cart**: Add multiple distinct items and quantities before final checkout.
4. **Inventory Management**: Real-time stock decrementing upon successful purchase; prevents ordering more items than available.
5. **Tiered Discounts & Coupons**:
   - Automatic 10% volume discount on cart subtotals exceeding Rs. 10,000.
   - Promotional coupon code `SAVE50` for an additional flat Rs. 50 discount.
6. **Payment Simulation**: Support for Cash on Delivery (COD), Credit/Debit Card, and UPI with custom mock authorizations.
7. **Order History & Persistence**: Appends formatted order receipts with timestamps to `orders.txt`.
8. **Admin Portal**: In-memory management to view stock levels, modify unit prices, update inventory, and add new items.
