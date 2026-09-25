import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ============================================================================
 * Application: Amazon Online Shopping Console Application
 * Description: Simulates a complete e-commerce ordering system adhering to
 *              JavaBean architecture, robust input handling, shopping cart,
 *              inventory decrement, tiered discounts, payment simulation,
 *              order history persistence (orders.txt), and admin management.
 * ============================================================================
 */
public class AmazonShopping {

    // File name for persistent order records
    private static final String ORDERS_FILE = "orders.txt";

    // In-memory catalog of products
    private static List<Product> catalog = new ArrayList<>();

    // Global scanner for user input
    private static Scanner scanner = new Scanner(System.in);

    /**
     * Application entry point.
     * Initializes default catalog and enters the main application loop.
     */
    public static void main(String[] args) {
        initializeCatalog();

        System.out.println("=================================================");
        System.out.println("      WELCOME TO AMAZON ONLINE SHOPPING          ");
        System.out.println("=================================================");

        boolean exitApp = false;
        while (!exitApp) {
            System.out.println("\n---------------- MAIN MENU ----------------");
            System.out.println("1. Customer Login & Shopping");
            System.out.println("2. Admin Portal (Manage Catalog & Stock)");
            System.out.println("3. View Past Order History");
            System.out.println("4. Exit Application");
            System.out.println("-------------------------------------------");

            int choice = readIntChoice("Enter your choice (1-4): ", 1, 4);

            switch (choice) {
                case 1:
                    handleCustomerSession();
                    break;
                case 2:
                    handleAdminSession();
                    break;
                case 3:
                    viewOrderHistory();
                    break;
                case 4:
                    System.out.println("\nThank you for visiting Amazon Online Shopping! Goodbye.");
                    exitApp = true;
                    break;
            }
        }
        scanner.close();
    }

    // =========================================================================
    // CATALOG INITIALIZATION
    // =========================================================================

    /**
     * Populates the in-memory product catalog with core e-commerce items.
     * Initialized with ID, Name, Price (₹), and Initial Stock.
     */
    private static void initializeCatalog() {
        catalog.clear();
        catalog.add(new Product(1, "Laptop", 55000.00, 5));
        catalog.add(new Product(2, "Smartphone", 24999.00, 10));
        catalog.add(new Product(3, "Headphones", 2499.00, 15));
        catalog.add(new Product(4, "Smart Watch", 4499.00, 8));
    }

    // =========================================================================
    // CUSTOMER FLOW & AUTHENTICATION
    // =========================================================================

    /**
     * Handles the customer login flow with up to 3 attempts.
     * Core Requirement 1: username 'amazon', password '1234'.
     */
    private static void handleCustomerSession() {
        System.out.println("\n--- CUSTOMER LOGIN ---");
        boolean authenticated = false;
        int attemptsLeft = 3;

        while (attemptsLeft > 0) {
            System.out.print("Enter Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter Password: ");
            String password = scanner.nextLine().trim();

            if (username.equals("amazon") && password.equals("1234")) {
                authenticated = true;
                System.out.println("\nLogin Successful! Welcome to Amazon Shopping.");
                break;
            } else {
                attemptsLeft--;
                System.out.println("Invalid credentials! Attempts remaining: " + attemptsLeft);
            }
        }

        if (!authenticated) {
            System.out.println("Access Denied: Maximum login attempts exceeded. Returning to main menu.");
            return;
        }

        // Core Requirement 2: Collect customer name and delivery address
        System.out.println("\n--- CUSTOMER DETAILS ---");
        String name = readNonEmptyString("Enter your full name: ");
        String address = readNonEmptyString("Enter your delivery address: ");
        String phone = readNonEmptyString("Enter your contact number: ");

        // Instantiate Customer JavaBean
        Customer customer = new Customer("amazon", name, address, phone);

        // Shopping session loop (allows multiple transactions or cart operations)
        runShoppingSession(customer);
    }

    /**
     * Executes the shopping cart interactions for the authenticated customer.
     * Core Requirements 3, 4, 5, 6 & Advanced Features 11, 12, 13, 14, 15.
     */
    private static void runShoppingSession(Customer customer) {
        List<CartItem> cart = new ArrayList<>();
        boolean continueShopping = true;

        while (continueShopping) {
            System.out.println("\n=========================================");
            System.out.println("CUSTOMER PORTAL - " + customer.getFullName());
            System.out.println("=========================================");
            System.out.println("1. Browse Catalog & Add Item to Cart");
            System.out.println("2. View Current Cart");
            System.out.println("3. Proceed to Checkout");
            System.out.println("4. Clear Cart");
            System.out.println("5. Return to Main Menu");
            System.out.println("-----------------------------------------");

            int option = readIntChoice("Select an option (1-5): ", 1, 5);

            switch (option) {
                case 1:
                    addToCartFlow(cart);
                    break;
                case 2:
                    displayCart(cart);
                    break;
                case 3:
                    if (cart.isEmpty()) {
                        System.out.println("\nYour cart is empty! Add items from the catalog before checking out.");
                    } else {
                        boolean orderPlaced = checkoutFlow(customer, cart);
                        if (orderPlaced) {
                            cart.clear(); // Empty cart after successful placement
                            // Core Requirement 6: y/n prompt to continue shopping
                            System.out.print("\nDo you want to continue shopping for more items? (y/n): ");
                            String ans = scanner.nextLine().trim();
                            if (!ans.equalsIgnoreCase("y")) {
                                continueShopping = false;
                            }
                        }
                    }
                    break;
                case 4:
                    cart.clear();
                    System.out.println("\nShopping cart has been cleared.");
                    break;
                case 5:
                    continueShopping = false;
                    System.out.println("\nLogging out from customer session...");
                    break;
            }
        }
    }

    // =========================================================================
    // CATALOG & CART MANAGEMENT
    // =========================================================================

    /**
     * Displays product catalog and allows adding items to cart.
     * Core Requirement 3: Switch-case selection for catalog assignment.
     * Advanced Feature 12: Stock validation & decrement preparation.
     */
    private static void addToCartFlow(List<CartItem> cart) {
        System.out.println("\n============== PRODUCT CATALOG ==============");
        System.out.println(String.format("%-5s %-16s %-14s %-8s", "ID", "Product Name", "Price (Rs.)", "Stock"));
        System.out.println("---------------------------------------------");
        for (Product p : catalog) {
            System.out.println(String.format("%-5d %-16s Rs. %-10.2f %-8d", 
                    p.getId(), p.getName(), p.getPrice(), p.getStock()));
        }
        System.out.println("---------------------------------------------");

        int maxId = catalog.size();
        int selectedId = readIntChoice("Enter the Product ID you wish to purchase (0 to cancel): ", 0, maxId);

        if (selectedId == 0) {
            System.out.println("Item selection cancelled.");
            return;
        }

        // Core Requirement 3: switch-case to assign product name and price
        Product chosenProduct = null;
        switch (selectedId) {
            case 1:
                chosenProduct = findProductById(1);
                break;
            case 2:
                chosenProduct = findProductById(2);
                break;
            case 3:
                chosenProduct = findProductById(3);
                break;
            case 4:
                chosenProduct = findProductById(4);
                break;
            default:
                chosenProduct = findProductById(selectedId);
                break;
        }

        if (chosenProduct == null) {
            System.out.println("Product not found!");
            return;
        }

        // Inventory check
        if (chosenProduct.getStock() <= 0) {
            System.out.println("Sorry, '" + chosenProduct.getName() + "' is currently Out of Stock!");
            return;
        }

        // Determine quantity already in cart for this product
        int existingInCart = 0;
        CartItem existingItem = null;
        for (CartItem item : cart) {
            if (item.getProduct().getId() == chosenProduct.getId()) {
                existingInCart = item.getQuantity();
                existingItem = item;
                break;
            }
        }

        int availableStock = chosenProduct.getStock() - existingInCart;
        if (availableStock <= 0) {
            System.out.println("You already have all available " + chosenProduct.getStock() + 
                               " unit(s) of '" + chosenProduct.getName() + "' in your cart!");
            return;
        }

        // Core Requirement 4: Accept quantity input and validate positive integer
        System.out.println("Available stock for this order: " + availableStock);
        int quantity = readIntChoice("Enter quantity to add: ", 1, availableStock);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            cart.add(new CartItem(chosenProduct, quantity));
        }

        System.out.println("\nSUCCESS: Added " + quantity + " x " + chosenProduct.getName() + " to your cart.");
    }

    /**
     * Displays all items currently held in the shopping cart.
     */
    private static void displayCart(List<CartItem> cart) {
        if (cart.isEmpty()) {
            System.out.println("\nYour shopping cart is empty.");
            return;
        }

        System.out.println("\n---------------- CURRENT CART ----------------");
        double subtotal = 0.0;
        int index = 1;
        for (CartItem item : cart) {
            System.out.println(String.format("%d. %-15s x %-2d @ Rs. %,.2f = Rs. %,.2f",
                    index++, item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getPrice(), item.getSubtotal()));
            subtotal += item.getSubtotal();
        }
        System.out.println("----------------------------------------------");
        System.out.println(String.format("Current Subtotal: Rs. %,.2f", subtotal));
    }

    // =========================================================================
    // CHECKOUT, DISCOUNTS, PAYMENT & PERSISTENCE
    // =========================================================================

    /**
     * Executes the checkout process:
     * - Itemized billing
     * - Tiered discounts (10% over ₹10,000 + coupon code 'SAVE50')
     * - Payment simulation (COD, Card, UPI)
     * - Inventory stock decrement
     * - Order summary display & file logging (orders.txt)
     */
    private static boolean checkoutFlow(Customer customer, List<CartItem> cart) {
        System.out.println("\n==============================================");
        System.out.println("               CHECKOUT BILLING               ");
        System.out.println("==============================================");

        double subtotal = 0.0;
        System.out.println(String.format("%-3s %-16s %-14s %-6s %-14s", "#", "Item", "Price (Rs.)", "Qty", "Subtotal (Rs.)"));
        System.out.println("----------------------------------------------");
        int count = 1;
        for (CartItem item : cart) {
            System.out.println(String.format("%-3d %-16s %-14.2f %-6d %-14.2f",
                    count++, item.getProduct().getName(), item.getProduct().getPrice(),
                    item.getQuantity(), item.getSubtotal()));
            subtotal += item.getSubtotal();
        }
        System.out.println("----------------------------------------------");
        System.out.println(String.format("Cart Subtotal:                            Rs. %,.2f", subtotal));

        // Advanced Feature 13: 10% discount if subtotal > ₹10,000
        double autoDiscount = 0.0;
        if (subtotal > 10000.0) {
            autoDiscount = subtotal * 0.10;
            System.out.println(String.format("Cart Value Discount (10%% off > Rs. 10,000): -Rs. %,.2f", autoDiscount));
        }

        // Advanced Feature 13: Hardcoded coupon code (e.g., 'SAVE50')
        System.out.print("\nDo you have a promo/coupon code? (Press Enter to skip or enter code): ");
        String couponCode = scanner.nextLine().trim();
        double couponDiscount = 0.0;

        if (!couponCode.isEmpty()) {
            if (couponCode.equalsIgnoreCase("SAVE50")) {
                couponDiscount = 50.0;
                System.out.println("Coupon 'SAVE50' applied successfully! Flat Rs. 50.00 OFF.");
            } else {
                System.out.println("Invalid coupon code entered. No additional coupon discount applied.");
            }
        }

        double totalDiscount = autoDiscount + couponDiscount;
        double grandTotal = subtotal - totalDiscount;
        if (grandTotal < 0) {
            grandTotal = 0.0;
        }

        System.out.println(String.format("Total Discounts Applied:                  -Rs. %,.2f", totalDiscount));
        System.out.println(String.format("GRAND TOTAL TO PAY:                       Rs. %,.2f", grandTotal));
        System.out.println("==============================================");

        // Advanced Feature 14: Payment simulation
        System.out.println("\nSelect Payment Method:");
        System.out.println("1. Cash on Delivery (COD)");
        System.out.println("2. Credit / Debit Card");
        System.out.println("3. UPI (Google Pay / PhonePe / Paytm)");

        int payChoice = readIntChoice("Choose payment method (1-3): ", 1, 3);
        String paymentMethodStr = "";

        switch (payChoice) {
            case 1:
                paymentMethodStr = "Cash on Delivery";
                System.out.println("\n[PAYMENT STATUS] Selected Cash on Delivery.");
                System.out.println(">> Please keep exact cash of Rs. " + String.format("%,.2f", grandTotal) + " ready at delivery!");
                break;
            case 2:
                paymentMethodStr = "Credit/Debit Card";
                System.out.print("Enter 16-digit Card Number (Mock): ");
                String cardNum = scanner.nextLine().trim();
                System.out.print("Enter Card Expiry (MM/YY): ");
                String expiry = scanner.nextLine().trim();
                System.out.println("\n[PAYMENT STATUS] Contacting Bank Payment Gateway...");
                System.out.println(">> Card ending with " + (cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "XXXX") + 
                                   " authorized successfully. Payment of Rs. " + String.format("%,.2f", grandTotal) + " confirmed!");
                break;
            case 3:
                paymentMethodStr = "UPI";
                System.out.print("Enter your UPI ID (e.g. user@oksbi): ");
                String upiId = scanner.nextLine().trim();
                System.out.println("\n[PAYMENT STATUS] Sending payment request to " + upiId + "...");
                System.out.println(">> UPI Transaction Approved! Payment of Rs. " + String.format("%,.2f", grandTotal) + " received.");
                break;
        }

        // Advanced Feature 12: Decrement inventory stock upon successful purchase
        for (CartItem item : cart) {
            item.getProduct().reduceStock(item.getQuantity());
        }

        // Format order timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Core Requirement 5: Display full order summary
        displayOrderSummary(customer, cart, subtotal, autoDiscount, couponDiscount, grandTotal, paymentMethodStr, timestamp);

        // Advanced Feature 15: Save order details to text file (orders.txt)
        saveOrderToFile(customer, cart, subtotal, totalDiscount, grandTotal, paymentMethodStr, timestamp);

        return true;
    }

    /**
     * Displays a clean, formatted receipt summarizing the completed order.
     */
    private static void displayOrderSummary(Customer customer, List<CartItem> cart, double subtotal,
                                            double autoDisc, double couponDisc, double grandTotal,
                                            String paymentMethod, String timestamp) {
        System.out.println("\n=======================================================");
        System.out.println("                 FINAL ORDER SUMMARY                   ");
        System.out.println("=======================================================");
        System.out.println("Date & Time      : " + timestamp);
        System.out.println("Customer Name    : " + customer.getFullName());
        System.out.println("Contact Number   : " + customer.getContactNumber());
        System.out.println("Delivery Address : " + customer.getDeliveryAddress());
        System.out.println("Payment Method   : " + paymentMethod);
        System.out.println("-------------------------------------------------------");
        System.out.println(String.format("%-18s %-5s %-14s %-14s", "Item", "Qty", "Price (Rs.)", "Subtotal (Rs.)"));
        System.out.println("-------------------------------------------------------");
        for (CartItem item : cart) {
            System.out.println(String.format("%-18s %-5d %-14.2f %-14.2f",
                    item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getPrice(), item.getSubtotal()));
        }
        System.out.println("-------------------------------------------------------");
        System.out.println(String.format("Subtotal         : Rs. %,.2f", subtotal));
        if (autoDisc > 0) {
            System.out.println(String.format("10%% Volume Disc  : -Rs. %,.2f", autoDisc));
        }
        if (couponDisc > 0) {
            System.out.println(String.format("Coupon Discount  : -Rs. %,.2f", couponDisc));
        }
        System.out.println(String.format("Grand Total Paid : Rs. %,.2f", grandTotal));
        System.out.println("=======================================================");
        System.out.println(">> Status: Confirmed & Dispatched for Delivery!");
    }

    /**
     * Appends completed order details to the persistent text file 'orders.txt'.
     */
    private static void saveOrderToFile(Customer customer, List<CartItem> cart, double subtotal,
                                        double totalDiscount, double grandTotal,
                                        String paymentMethod, String timestamp) {
        try (FileWriter fw = new FileWriter(ORDERS_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("=======================================================");
            out.println("ORDER RECORD - " + timestamp);
            out.println("Customer Name    : " + customer.getFullName());
            out.println("Contact Number   : " + customer.getContactNumber());
            out.println("Delivery Address : " + customer.getDeliveryAddress());
            out.println("Payment Method   : " + paymentMethod);
            out.println("Purchased Items  :");
            for (CartItem item : cart) {
                out.println(String.format("   - %-15s x %-2d @ Rs. %,.2f = Rs. %,.2f",
                        item.getProduct().getName(), item.getQuantity(),
                        item.getProduct().getPrice(), item.getSubtotal()));
            }
            out.println(String.format("Subtotal         : Rs. %,.2f", subtotal));
            out.println(String.format("Total Discount   : -Rs. %,.2f", totalDiscount));
            out.println(String.format("Grand Total Paid : Rs. %,.2f", grandTotal));
            out.println("=======================================================");
            out.println(); // Blank line between orders

            System.out.println("\n[PERSISTENCE] Order details successfully recorded in '" + ORDERS_FILE + "'.");
        } catch (IOException e) {
            System.out.println("\n[ERROR] Unable to write order to history file: " + e.getMessage());
        }
    }

    /**
     * Reads and displays all previous orders stored inside 'orders.txt'.
     */
    private static void viewOrderHistory() {
        System.out.println("\n=============== PAST ORDER HISTORY ===============");
        File file = new File(ORDERS_FILE);
        if (!file.exists() || file.length() == 0) {
            System.out.println("No past orders found in records.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading past orders: " + e.getMessage());
        }
        System.out.println("==================================================");
    }

    // =========================================================================
    // ADMIN PORTAL (ADVANCED FEATURE 16)
    // =========================================================================

    /**
     * Admin portal allowing administrators to view, add, and edit product stock and prices.
     */
    private static void handleAdminSession() {
        System.out.println("\n--- ADMIN PORTAL LOGIN ---");
        System.out.print("Enter Admin Username: ");
        String adminUser = scanner.nextLine().trim();
        System.out.print("Enter Admin Password: ");
        String adminPass = scanner.nextLine().trim();

        // Default admin credentials
        if (!adminUser.equals("admin") || !adminPass.equals("admin123")) {
            System.out.println("Authentication Failed: Invalid admin credentials!");
            return;
        }

        System.out.println("\nAdmin login authorized. Welcome to Catalog & Inventory Management.");
        boolean adminActive = true;

        while (adminActive) {
            System.out.println("\n----------- ADMIN CONSOLE -----------");
            System.out.println("1. View Current Inventory");
            System.out.println("2. Update Product Price / Stock");
            System.out.println("3. Add New Product to Catalog");
            System.out.println("4. View All Past Customer Orders");
            System.out.println("5. Exit Admin Console");
            System.out.println("-------------------------------------");

            int adminChoice = readIntChoice("Enter choice (1-5): ", 1, 5);

            switch (adminChoice) {
                case 1:
                    System.out.println("\n--- CURRENT INVENTORY ---");
                    for (Product p : catalog) {
                        System.out.println(p);
                    }
                    break;
                case 2:
                    updateProductInventory();
                    break;
                case 3:
                    addNewProduct();
                    break;
                case 4:
                    viewOrderHistory();
                    break;
                case 5:
                    adminActive = false;
                    System.out.println("Exiting Admin Console...");
                    break;
            }
        }
    }

    /**
     * Allows admin to modify existing product price or replenish stock.
     */
    private static void updateProductInventory() {
        System.out.println("\nSelect a product to update:");
        for (Product p : catalog) {
            System.out.println(p);
        }

        int pid = readIntChoice("Enter Product ID to modify (0 to cancel): ", 0, catalog.size());
        if (pid == 0) return;

        Product p = findProductById(pid);
        if (p == null) {
            System.out.println("Product not found!");
            return;
        }

        System.out.println("\nModifying Product: " + p.getName());
        System.out.println("1. Update Price (Current: Rs. " + p.getPrice() + ")");
        System.out.println("2. Update Stock (Current: " + p.getStock() + " units)");
        int subChoice = readIntChoice("Enter choice (1-2): ", 1, 2);

        if (subChoice == 1) {
            double newPrice = readDoublePositive("Enter new unit price (Rs.): ");
            p.setPrice(newPrice);
            System.out.println("SUCCESS: Price updated to Rs. " + newPrice);
        } else {
            int newStock = readIntChoice("Enter new stock level (0 to 1000): ", 0, 1000);
            p.setStock(newStock);
            System.out.println("SUCCESS: Stock updated to " + newStock + " units");
        }
    }

    /**
     * Allows admin to introduce a brand new product to the catalog.
     */
    private static void addNewProduct() {
        System.out.println("\n--- ADD NEW PRODUCT ---");
        String name = readNonEmptyString("Enter Product Name: ");
        double price = readDoublePositive("Enter Unit Price (Rs.): ");
        int stock = readIntChoice("Enter Initial Stock quantity: ", 1, 1000);

        int newId = catalog.size() + 1;
        Product newProduct = new Product(newId, name, price, stock);
        catalog.add(newProduct);
        System.out.println("SUCCESS: Product '" + name + "' added with ID " + newId);
    }

    // =========================================================================
    // ROBUST INPUT HANDLING UTILITIES (CORE REQUIREMENT 8 & ADVANCED 17)
    // =========================================================================

    /**
     * Safely reads an integer from console within [min, max] range.
     * Uses try/catch for InputMismatchException to ensure application never crashes.
     */
    private static int readIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a valid number.");
                continue;
            }

            try {
                // Wrap in dedicated token scanner to catch InputMismatchException
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

    /**
     * Safely reads a positive double value (for price entries).
     */
    private static double readDoublePositive(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a valid decimal number.");
                continue;
            }

            try {
                Scanner tokenScanner = new Scanner(input);
                double value = tokenScanner.nextDouble();

                if (value > 0) {
                    return value;
                } else {
                    System.out.println("Price must be greater than zero.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a valid decimal number.");
            } catch (NoSuchElementException e) {
                System.out.println("No input detected. Please enter a valid decimal number.");
            }
        }
    }

    /**
     * Safely prompts user for a non-blank string.
     */
    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String val = scanner.nextLine().trim();
            if (!val.isEmpty()) {
                return val;
            }
            System.out.println("Field cannot be empty. Please enter a valid response.");
        }
    }

    /**
     * Helper method to locate a product in catalog by its ID.
     */
    private static Product findProductById(int id) {
        for (Product p : catalog) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }
}
