import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * AmazonWebServer
 * Built-in zero-dependency Java HTTP Server (using com.sun.net.httpserver)
 * providing an interactive Web UI for Amazon Online Shopping on http://localhost:8080.
 * Reuses Customer, Product, and CartItem JavaBeans.
 */
public class AmazonWebServer {
    private static final int PORT = 8080;
    private static final String ORDERS_FILE = "orders.txt";
    private static final List<Product> catalog = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        initializeCatalog();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Serve Frontend HTML/CSS/JS
        server.createContext("/", new StaticPageHandler());

        // API Endpoints
        server.createContext("/api/products", new ProductsHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/checkout", new CheckoutHandler());
        server.createContext("/api/orders", new OrdersHandler());
        server.createContext("/api/admin/update", new AdminUpdateHandler());
        server.createContext("/api/admin/add", new AdminAddHandler());

        server.start();
        System.out.println("==================================================");
        System.out.println(" Amazon Online Shopping Web Server is Running!    ");
        System.out.println(" Localhost URL: http://localhost:" + PORT + "     ");
        System.out.println("==================================================");
    }

    private static void initializeCatalog() {
        catalog.clear();
        catalog.add(new Product(1, "Laptop", 55000.00, 5));
        catalog.add(new Product(2, "Smartphone", 24999.00, 10));
        catalog.add(new Product(3, "Headphones", 2499.00, 15));
        catalog.add(new Product(4, "Smart Watch", 4499.00, 8));
    }

    private static Product findProductById(int id) {
        for (Product p : catalog) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // -------------------------------------------------------------
    // HTTP HANDLERS
    // -------------------------------------------------------------

    static class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }
            String html = getHtmlContent();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    static class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < catalog.size(); i++) {
                Product p = catalog.get(i);
                json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}",
                        p.getId(), escapeJson(p.getName()), p.getPrice(), p.getStock()));
                if (i < catalog.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String body = readBody(exchange);
            Map<String, String> params = parseFormOrJson(body);

            String role = params.getOrDefault("role", "customer");
            String username = params.getOrDefault("username", "");
            String password = params.getOrDefault("password", "");

            if ("admin".equalsIgnoreCase(role)) {
                if ("admin".equals(username) && "admin123".equals(password)) {
                    sendResponse(exchange, 200, "application/json", "{\"success\":true,\"role\":\"admin\",\"name\":\"Administrator\"}");
                } else {
                    sendResponse(exchange, 401, "application/json", "{\"success\":false,\"message\":\"Invalid Admin credentials! (Use admin / admin123)\"}");
                }
            } else {
                if ("amazon".equals(username) && "1234".equals(password)) {
                    sendResponse(exchange, 200, "application/json", "{\"success\":true,\"role\":\"customer\",\"username\":\"amazon\"}");
                } else {
                    sendResponse(exchange, 401, "application/json", "{\"success\":false,\"message\":\"Invalid Customer credentials! (Use amazon / 1234)\"}");
                }
            }
        }
    }

    static class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> data = parseFormOrJson(body);

            String customerName = data.getOrDefault("customerName", "Guest Customer");
            String address = data.getOrDefault("address", "Standard Delivery");
            String phone = data.getOrDefault("phone", "N/A");
            String coupon = data.getOrDefault("coupon", "").trim();
            String paymentMethod = data.getOrDefault("paymentMethod", "Cash on Delivery");
            String itemsRaw = data.getOrDefault("items", ""); // format: "id:qty,id:qty"

            Customer customer = new Customer("amazon", customerName, address, phone);
            List<CartItem> cart = new ArrayList<>();

            if (itemsRaw.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Cart is empty!\"}");
                return;
            }

            // Parse items and validate stock
            String[] itemPairs = itemsRaw.split(",");
            for (String pair : itemPairs) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    try {
                        int pid = Integer.parseInt(parts[0].trim());
                        int qty = Integer.parseInt(parts[1].trim());
                        Product prod = findProductById(pid);
                        if (prod != null) {
                            if (prod.getStock() < qty) {
                                sendResponse(exchange, 400, "application/json",
                                        String.format("{\"success\":false,\"message\":\"Only %d units left for %s!\"}",
                                                prod.getStock(), prod.getName()));
                                return;
                            }
                            cart.add(new CartItem(prod, qty));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (cart.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"No valid items in cart.\"}");
                return;
            }

            // Calculate billing
            double subtotal = 0.0;
            for (CartItem ci : cart) {
                subtotal += ci.getSubtotal();
            }

            double autoDiscount = (subtotal > 10000.0) ? (subtotal * 0.10) : 0.0;
            double couponDiscount = 0.0;
            if (coupon.equalsIgnoreCase("SAVE50")) {
                couponDiscount = 50.0;
            }

            double totalDiscount = autoDiscount + couponDiscount;
            double grandTotal = Math.max(0.0, subtotal - totalDiscount);

            // Deduct stock
            for (CartItem ci : cart) {
                ci.getProduct().reduceStock(ci.getQuantity());
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Save order to orders.txt
            saveOrderToFile(customer, cart, subtotal, totalDiscount, grandTotal, paymentMethod, timestamp);

            // Payment simulation message
            String payMessage = "";
            if (paymentMethod.equalsIgnoreCase("UPI")) {
                payMessage = "UPI payment verified successfully!";
            } else if (paymentMethod.equalsIgnoreCase("Card")) {
                payMessage = "Card authorization completed successfully!";
            } else {
                payMessage = "Order booked with Cash on Delivery. Keep exact cash ready!";
            }

            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("{")
                    .append("\"success\":true,")
                    .append("\"timestamp\":\"").append(timestamp).append("\",")
                    .append("\"customerName\":\"").append(escapeJson(customerName)).append("\",")
                    .append("\"address\":\"").append(escapeJson(address)).append("\",")
                    .append("\"phone\":\"").append(escapeJson(phone)).append("\",")
                    .append("\"subtotal\":").append(String.format(Locale.US, "%.2f", subtotal)).append(",")
                    .append("\"autoDiscount\":").append(String.format(Locale.US, "%.2f", autoDiscount)).append(",")
                    .append("\"couponDiscount\":").append(String.format(Locale.US, "%.2f", couponDiscount)).append(",")
                    .append("\"grandTotal\":").append(String.format(Locale.US, "%.2f", grandTotal)).append(",")
                    .append("\"paymentMethod\":\"").append(escapeJson(paymentMethod)).append("\",")
                    .append("\"payMessage\":\"").append(escapeJson(payMessage)).append("\"")
                    .append("}");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", jsonResponse.toString());
        }
    }

    static class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(ORDERS_FILE);
            if (!file.exists() || file.length() == 0) {
                sendResponse(exchange, 200, "text/plain; charset=UTF-8", "No past orders found in records.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            sendResponse(exchange, 200, "text/plain; charset=UTF-8", sb.toString());
        }
    }

    static class AdminUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            Map<String, String> data = parseFormOrJson(readBody(exchange));
            try {
                int id = Integer.parseInt(data.get("id"));
                Product p = findProductById(id);
                if (p != null) {
                    if (data.containsKey("price")) {
                        p.setPrice(Double.parseDouble(data.get("price")));
                    }
                    if (data.containsKey("stock")) {
                        p.setStock(Integer.parseInt(data.get("stock")));
                    }
                    sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Product updated successfully!\"}");
                    return;
                }
            } catch (Exception ignored) {}
            sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Invalid product data!\"}");
        }
    }

    static class AdminAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            Map<String, String> data = parseFormOrJson(readBody(exchange));
            try {
                String name = data.get("name");
                double price = Double.parseDouble(data.get("price"));
                int stock = Integer.parseInt(data.get("stock"));
                int newId = catalog.size() + 1;
                catalog.add(new Product(newId, name, price, stock));
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Product added successfully!\"}");
                return;
            } catch (Exception ignored) {}
            sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Invalid inputs for new product!\"}");
        }
    }

    // -------------------------------------------------------------
    // UTILITIES
    // -------------------------------------------------------------

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
            out.println();
        } catch (IOException e) {
            System.err.println("File write error: " + e.getMessage());
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseFormOrJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON parser
            String inner = body.substring(1, body.length() - 1);
            String[] tokens = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String val = kv[1].trim().replace("\"", "");
                    map.put(key, val);
                }
            }
        } else {
            // URL Encoded form
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // -------------------------------------------------------------
    // FRONTEND WEB APPLICATION (HTML + CSS + JAVASCRIPT)
    // -------------------------------------------------------------
    private static String getHtmlContent() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "  <title>Amazon Online Shopping</title>\n" +
                "  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n" +
                "  <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --amazon-dark: #131921;\n" +
                "      --amazon-nav: #232f3e;\n" +
                "      --amazon-orange: #febd69;\n" +
                "      --amazon-orange-hover: #f3a847;\n" +
                "      --amazon-yellow: #ffd814;\n" +
                "      --bg-gray: #eaeded;\n" +
                "      --card-bg: #ffffff;\n" +
                "      --text-dark: #0f1111;\n" +
                "      --text-muted: #565959;\n" +
                "      --success: #067d62;\n" +
                "      --danger: #b12704;\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }\n" +
                "    body { background-color: var(--bg-gray); color: var(--text-dark); min-height: 100vh; display: flex; flex-direction: column; }\n" +
                "    /* Header */\n" +
                "    header {\n" +
                "      background-color: var(--amazon-dark);\n" +
                "      color: white;\n" +
                "      padding: 10px 24px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: space-between;\n" +
                "      position: sticky;\n" +
                "      top: 0;\n" +
                "      z-index: 100;\n" +
                "      box-shadow: 0 2px 6px rgba(0,0,0,0.2);\n" +
                "    }\n" +
                "    .logo-container { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 22px; color: white; text-decoration: none; cursor: pointer; }\n" +
                "    .logo-container span { color: var(--amazon-orange); }\n" +
                "    .nav-actions { display: flex; align-items: center; gap: 18px; }\n" +
                "    .btn-nav {\n" +
                "      background: none;\n" +
                "      border: 1px solid rgba(255,255,255,0.3);\n" +
                "      color: white;\n" +
                "      padding: 8px 14px;\n" +
                "      border-radius: 6px;\n" +
                "      cursor: pointer;\n" +
                "      font-size: 14px;\n" +
                "      font-weight: 500;\n" +
                "      transition: all 0.2s;\n" +
                "    }\n" +
                "    .btn-nav:hover { background-color: rgba(255,255,255,0.15); border-color: white; }\n" +
                "    .cart-btn {\n" +
                "      background-color: var(--amazon-yellow);\n" +
                "      color: var(--text-dark);\n" +
                "      font-weight: 600;\n" +
                "      border: none;\n" +
                "      padding: 8px 16px;\n" +
                "      border-radius: 20px;\n" +
                "      cursor: pointer;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 8px;\n" +
                "      transition: background 0.2s;\n" +
                "    }\n" +
                "    .cart-btn:hover { background-color: var(--amazon-orange); }\n" +
                "    .cart-badge { background-color: var(--danger); color: white; border-radius: 50%; padding: 2px 7px; font-size: 12px; font-weight: 700; }\n" +
                "    /* Sub Header */\n" +
                "    .sub-bar {\n" +
                "      background-color: var(--amazon-nav);\n" +
                "      color: white;\n" +
                "      padding: 8px 24px;\n" +
                "      font-size: 13px;\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "    }\n" +
                "    .sub-bar a { color: var(--amazon-orange); text-decoration: none; font-weight: 500; cursor: pointer; }\n" +
                "    /* Main Container */\n" +
                "    main { max-width: 1200px; margin: 24px auto; padding: 0 16px; width: 100%; flex: 1; }\n" +
                "    /* Hero Banner */\n" +
                "    .banner {\n" +
                "      background: linear-gradient(135deg, #232f3e 0%, #37475a 100%);\n" +
                "      color: white;\n" +
                "      border-radius: 12px;\n" +
                "      padding: 28px;\n" +
                "      margin-bottom: 24px;\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "      box-shadow: 0 4px 12px rgba(0,0,0,0.08);\n" +
                "    }\n" +
                "    .banner h1 { font-size: 26px; margin-bottom: 8px; }\n" +
                "    .banner p { color: #d5d9d9; font-size: 14px; max-width: 600px; }\n" +
                "    .banner .offer-tag { background-color: var(--amazon-orange); color: var(--text-dark); padding: 6px 12px; border-radius: 20px; font-weight: 700; font-size: 13px; }\n" +
                "    /* Grid */\n" +
                "    .products-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; }\n" +
                "    .product-card {\n" +
                "      background: var(--card-bg);\n" +
                "      border-radius: 10px;\n" +
                "      padding: 20px;\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      justify-content: space-between;\n" +
                "      box-shadow: 0 2px 6px rgba(0,0,0,0.06);\n" +
                "      transition: transform 0.2s, box-shadow 0.2s;\n" +
                "      border: 1px solid #e3e6e6;\n" +
                "    }\n" +
                "    .product-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }\n" +
                "    .product-icon { font-size: 42px; text-align: center; margin-bottom: 14px; }\n" +
                "    .product-title { font-size: 18px; font-weight: 600; margin-bottom: 6px; }\n" +
                "    .product-price { font-size: 20px; font-weight: 700; color: var(--danger); margin-bottom: 8px; }\n" +
                "    .product-stock { font-size: 13px; margin-bottom: 16px; font-weight: 500; }\n" +
                "    .stock-in { color: var(--success); }\n" +
                "    .stock-low { color: #c45500; }\n" +
                "    .stock-out { color: var(--danger); }\n" +
                "    .card-footer { display: flex; align-items: center; gap: 10px; }\n" +
                "    .qty-select { padding: 8px 10px; border-radius: 6px; border: 1px solid #888c8c; background: #f0f2f2; font-weight: 500; }\n" +
                "    .btn-add {\n" +
                "      flex: 1;\n" +
                "      background-color: var(--amazon-yellow);\n" +
                "      border: 1px solid #fcd200;\n" +
                "      color: var(--text-dark);\n" +
                "      padding: 10px;\n" +
                "      border-radius: 20px;\n" +
                "      font-weight: 600;\n" +
                "      cursor: pointer;\n" +
                "      transition: background 0.2s;\n" +
                "    }\n" +
                "    .btn-add:hover { background-color: var(--amazon-orange); }\n" +
                "    .btn-add:disabled { background-color: #d5d9d9; border-color: #d5d9d9; cursor: not-allowed; color: #888c8c; }\n" +
                "    /* Modals & Drawers */\n" +
                "    .modal-backdrop {\n" +
                "      display: none;\n" +
                "      position: fixed;\n" +
                "      inset: 0;\n" +
                "      background: rgba(0,0,0,0.6);\n" +
                "      z-index: 200;\n" +
                "      justify-content: center;\n" +
                "      align-items: center;\n" +
                "      padding: 16px;\n" +
                "    }\n" +
                "    .modal-backdrop.active { display: flex; }\n" +
                "    .modal-box {\n" +
                "      background: white;\n" +
                "      border-radius: 12px;\n" +
                "      max-width: 540px;\n" +
                "      width: 100%;\n" +
                "      max-height: 90vh;\n" +
                "      overflow-y: auto;\n" +
                "      padding: 24px;\n" +
                "      box-shadow: 0 8px 30px rgba(0,0,0,0.25);\n" +
                "    }\n" +
                "    .modal-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e3e6e6; padding-bottom: 12px; margin-bottom: 16px; }\n" +
                "    .modal-header h2 { font-size: 20px; font-weight: 700; }\n" +
                "    .close-btn { background: none; border: none; font-size: 22px; cursor: pointer; color: var(--text-muted); }\n" +
                "    .close-btn:hover { color: black; }\n" +
                "    /* Forms */\n" +
                "    .form-group { margin-bottom: 14px; }\n" +
                "    .form-group label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }\n" +
                "    .form-group input, .form-group textarea, .form-group select {\n" +
                "      width: 100%;\n" +
                "      padding: 10px 12px;\n" +
                "      border: 1px solid #888c8c;\n" +
                "      border-radius: 6px;\n" +
                "      font-size: 14px;\n" +
                "    }\n" +
                "    .form-group input:focus, .form-group select:focus, .form-group textarea:focus {\n" +
                "      outline: none;\n" +
                "      border-color: #e77600;\n" +
                "      box-shadow: 0 0 4px rgba(228,121,17,0.5);\n" +
                "    }\n" +
                "    .btn-primary {\n" +
                "      width: 100%;\n" +
                "      background-color: var(--amazon-yellow);\n" +
                "      border: 1px solid #fcd200;\n" +
                "      color: var(--text-dark);\n" +
                "      padding: 12px;\n" +
                "      border-radius: 8px;\n" +
                "      font-size: 15px;\n" +
                "      font-weight: 700;\n" +
                "      cursor: pointer;\n" +
                "      transition: background 0.2s;\n" +
                "      margin-top: 10px;\n" +
                "    }\n" +
                "    .btn-primary:hover { background-color: var(--amazon-orange); }\n" +
                "    /* Cart Items List */\n" +
                "    .cart-item {\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "      padding: 10px 0;\n" +
                "      border-bottom: 1px solid #f0f2f2;\n" +
                "    }\n" +
                "    .cart-item-title { font-weight: 600; font-size: 14px; }\n" +
                "    .cart-item-sub { color: var(--text-muted); font-size: 12px; margin-top: 2px; }\n" +
                "    .remove-btn { color: var(--danger); background: none; border: none; font-size: 12px; cursor: pointer; text-decoration: underline; margin-top: 4px; }\n" +
                "    /* Billing Breakdown */\n" +
                "    .bill-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; }\n" +
                "    .bill-row.discount { color: var(--success); font-weight: 600; }\n" +
                "    .bill-row.total { font-size: 18px; font-weight: 700; border-top: 2px dashed #d5d9d9; padding-top: 10px; margin-top: 8px; }\n" +
                "    .alert-box { padding: 10px 14px; border-radius: 6px; font-size: 13px; margin-bottom: 14px; display: none; }\n" +
                "    .alert-success { background: #dff0d8; color: #3c763d; border: 1px solid #d6e9c6; }\n" +
                "    .alert-error { background: #f2dede; color: #a94442; border: 1px solid #ebccd1; }\n" +
                "    pre.orders-view {\n" +
                "      background: #1e1e1e;\n" +
                "      color: #76d275;\n" +
                "      padding: 16px;\n" +
                "      border-radius: 8px;\n" +
                "      font-family: monospace;\n" +
                "      font-size: 12px;\n" +
                "      overflow-x: auto;\n" +
                "      max-height: 400px;\n" +
                "      white-space: pre-wrap;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "  <header>\n" +
                "    <div class=\"logo-container\" onclick=\"location.reload()\">\n" +
                "      🛒 <span>amazon</span>.in\n" +
                "    </div>\n" +
                "    <div class=\"nav-actions\">\n" +
                "      <span id=\"userDisplay\" style=\"font-size: 13px; font-weight: 500;\">Hello, Guest</span>\n" +
                "      <button class=\"btn-nav\" id=\"loginBtn\" onclick=\"openLoginModal()\">Sign In</button>\n" +
                "      <button class=\"btn-nav\" onclick=\"viewOrderHistory()\">Past Orders</button>\n" +
                "      <button class=\"cart-btn\" onclick=\"openCartModal()\">\n" +
                "        🛒 Cart <span class=\"cart-badge\" id=\"cartBadge\">0</span>\n" +
                "      </button>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "\n" +
                "  <div class=\"sub-bar\">\n" +
                "    <div>JavaBeans Architecture • Robust Input Validation • Real-time Stock Tracking • 10% Auto Discount</div>\n" +
                "    <div><a onclick=\"openAdminQuick()\">Switch to Admin Portal</a></div>\n" +
                "  </div>\n" +
                "\n" +
                "  <main>\n" +
                "    <div class=\"banner\">\n" +
                "      <div>\n" +
                "        <span class=\"offer-tag\">🔥 Special Offer</span>\n" +
                "        <h1 style=\"margin-top: 10px;\">Great Savings Festival</h1>\n" +
                "        <p>Get an automatic <strong>10% instant discount</strong> on orders above Rs. 10,000! Plus use promo code <strong>SAVE50</strong> for extra Rs. 50 OFF.</p>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <h2 style=\"font-size: 20px; font-weight: 700; margin-bottom: 16px;\">Featured Products</h2>\n" +
                "    <div class=\"products-grid\" id=\"productsGrid\"></div>\n" +
                "  </main>\n" +
                "\n" +
                "  <!-- LOGIN MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"loginModal\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2 id=\"loginModalTitle\">Sign-In</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('loginModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <div id=\"loginAlert\" class=\"alert-box\"></div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Account Role</label>\n" +
                "        <select id=\"loginRole\" onchange=\"updateLoginTips()\">\n" +
                "          <option value=\"customer\">Customer (Credentials: amazon / 1234)</option>\n" +
                "          <option value=\"admin\">Administrator (Credentials: admin / admin123)</option>\n" +
                "        </select>\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Username</label>\n" +
                "        <input type=\"text\" id=\"loginUser\" placeholder=\"Enter username\" />\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Password</label>\n" +
                "        <input type=\"password\" id=\"loginPass\" placeholder=\"Enter password\" />\n" +
                "      </div>\n" +
                "      <button class=\"btn-primary\" onclick=\"performLogin()\">Sign In</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- CART & CHECKOUT MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"cartModal\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2>Shopping Cart</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('cartModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <div id=\"cartItemsContainer\"></div>\n" +
                "      <div id=\"cartBillDetails\" style=\"margin-top: 16px;\"></div>\n" +
                "      <button class=\"btn-primary\" id=\"checkoutBtn\" onclick=\"openCheckoutDetails()\" style=\"margin-top: 16px;\">Proceed to Checkout</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- CUSTOMER DETAILS & PAYMENT MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"checkoutModal\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2>Checkout & Delivery</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('checkoutModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <div id=\"checkoutAlert\" class=\"alert-box\"></div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Full Name</label>\n" +
                "        <input type=\"text\" id=\"custName\" value=\"Priya Sharma\" />\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Delivery Address</label>\n" +
                "        <input type=\"text\" id=\"custAddress\" value=\"14/B Green Glen Layout, Bengaluru\" />\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Contact Number</label>\n" +
                "        <input type=\"text\" id=\"custPhone\" value=\"9876543210\" />\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Coupon Code (Optional)</label>\n" +
                "        <input type=\"text\" id=\"custCoupon\" placeholder=\"Try 'SAVE50' for Rs. 50 OFF\" />\n" +
                "      </div>\n" +
                "      <div class=\"form-group\">\n" +
                "        <label>Payment Method</label>\n" +
                "        <select id=\"paymentMethod\">\n" +
                "          <option value=\"Cash on Delivery\">Cash on Delivery (COD)</option>\n" +
                "          <option value=\"Card\">Credit / Debit Card</option>\n" +
                "          <option value=\"UPI\">UPI (Google Pay / PhonePe / Paytm)</option>\n" +
                "        </select>\n" +
                "      </div>\n" +
                "      <button class=\"btn-primary\" onclick=\"submitFinalOrder()\">Place Your Order</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- ORDER CONFIRMATION MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"receiptModal\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2 style=\"color: var(--success);\">🎉 Order Placed Successfully!</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('receiptModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <div id=\"receiptContent\"></div>\n" +
                "      <button class=\"btn-primary\" onclick=\"closeModal('receiptModal')\">Continue Shopping</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- PAST ORDERS MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"ordersModal\">\n" +
                "    <div class=\"modal-box\" style=\"max-width: 680px;\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2>Past Order Records (orders.txt)</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('ordersModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <pre class=\"orders-view\" id=\"ordersFileContent\">Loading records...</pre>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- ADMIN DASHBOARD MODAL -->\n" +
                "  <div class=\"modal-backdrop\" id=\"adminModal\">\n" +
                "    <div class=\"modal-box\" style=\"max-width: 720px;\">\n" +
                "      <div class=\"modal-header\">\n" +
                "        <h2>Admin Inventory Console</h2>\n" +
                "        <button class=\"close-btn\" onclick=\"closeModal('adminModal')\">&times;</button>\n" +
                "      </div>\n" +
                "      <div id=\"adminAlert\" class=\"alert-box\"></div>\n" +
                "      <div id=\"adminProductList\"></div>\n" +
                "      <hr style=\"margin: 20px 0; border: none; border-top: 1px solid #ddd;\" />\n" +
                "      <h3 style=\"font-size: 16px; margin-bottom: 12px;\">Add New Product</h3>\n" +
                "      <div style=\"display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 10px;\">\n" +
                "        <input type=\"text\" id=\"newProdName\" placeholder=\"Product Name\" style=\"padding:8px; border:1px solid #ccc; border-radius:6px;\" />\n" +
                "        <input type=\"number\" id=\"newProdPrice\" placeholder=\"Price (Rs.)\" style=\"padding:8px; border:1px solid #ccc; border-radius:6px;\" />\n" +
                "        <input type=\"number\" id=\"newProdStock\" placeholder=\"Stock\" style=\"padding:8px; border:1px solid #ccc; border-radius:6px;\" />\n" +
                "      </div>\n" +
                "      <button class=\"btn-primary\" onclick=\"addNewProduct()\" style=\"margin-top: 10px;\">Add Product</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    let products = [];\n" +
                "    let cart = {}; // { productId: quantity }\n" +
                "    let currentUser = null;\n" +
                "    let loginAttempts = 3;\n" +
                "\n" +
                "    const productIcons = {\n" +
                "      1: '💻',\n" +
                "      2: '📱',\n" +
                "      3: '🎧',\n" +
                "      4: '⌚'\n" +
                "    };\n" +
                "\n" +
                "    async function loadProducts() {\n" +
                "      try {\n" +
                "        const res = await fetch('/api/products');\n" +
                "        products = await res.json();\n" +
                "        renderProducts();\n" +
                "      } catch (err) {\n" +
                "        console.error('Failed to load products', err);\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function renderProducts() {\n" +
                "      const grid = document.getElementById('productsGrid');\n" +
                "      grid.innerHTML = '';\n" +
                "      products.forEach(p => {\n" +
                "        const icon = productIcons[p.id] || '📦';\n" +
                "        let stockClass = 'stock-in';\n" +
                "        let stockText = `${p.stock} units in stock`;\n" +
                "        if (p.stock === 0) {\n" +
                "          stockClass = 'stock-out';\n" +
                "          stockText = 'Out of Stock';\n" +
                "        } else if (p.stock <= 3) {\n" +
                "          stockClass = 'stock-low';\n" +
                "          stockText = `Only ${p.stock} left in stock!`;\n" +
                "        }\n" +
                "\n" +
                "        const card = document.createElement('div');\n" +
                "        card.className = 'product-card';\n" +
                "        card.innerHTML = `\n" +
                "          <div>\n" +
                "            <div class=\"product-icon\">${icon}</div>\n" +
                "            <div class=\"product-title\">${p.name}</div>\n" +
                "            <div class=\"product-price\">Rs. ${p.price.toLocaleString('en-IN', {minimumFractionDigits: 2})}</div>\n" +
                "            <div class=\"product-stock ${stockClass}\">${stockText}</div>\n" +
                "          </div>\n" +
                "          <div class=\"card-footer\">\n" +
                "            <select class=\"qty-select\" id=\"qty-${p.id}\" ${p.stock === 0 ? 'disabled' : ''}>\n" +
                "              ${Array.from({length: Math.min(p.stock, 5)}, (_, i) => `<option value=\"${i+1}\">${i+1}</option>`).join('')}\n" +
                "            </select>\n" +
                "            <button class=\"btn-add\" ${p.stock === 0 ? 'disabled' : ''} onclick=\"addToCart(${p.id})\">\n" +
                "              ${p.stock === 0 ? 'Out of Stock' : 'Add to Cart'}\n" +
                "            </button>\n" +
                "          </div>\n" +
                "        `;\n" +
                "        grid.appendChild(card);\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "    function addToCart(pid) {\n" +
                "      const p = products.find(x => x.id === pid);\n" +
                "      if (!p || p.stock <= 0) return;\n" +
                "\n" +
                "      const qtySelect = document.getElementById(`qty-${pid}`);\n" +
                "      const addedQty = parseInt(qtySelect.value, 10);\n" +
                "      const currentInCart = cart[pid] || 0;\n" +
                "\n" +
                "      if (currentInCart + addedQty > p.stock) {\n" +
                "        alert(`Cannot add ${addedQty} more units. Only ${p.stock - currentInCart} available!`);\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      cart[pid] = currentInCart + addedQty;\n" +
                "      updateCartBadge();\n" +
                "      openCartModal();\n" +
                "    }\n" +
                "\n" +
                "    function updateCartBadge() {\n" +
                "      let totalCount = 0;\n" +
                "      for (let k in cart) totalCount += cart[k];\n" +
                "      document.getElementById('cartBadge').innerText = totalCount;\n" +
                "    }\n" +
                "\n" +
                "    function openCartModal() {\n" +
                "      const container = document.getElementById('cartItemsContainer');\n" +
                "      const bill = document.getElementById('cartBillDetails');\n" +
                "      const checkoutBtn = document.getElementById('checkoutBtn');\n" +
                "      container.innerHTML = '';\n" +
                "\n" +
                "      let subtotal = 0;\n" +
                "      const itemIds = Object.keys(cart);\n" +
                "\n" +
                "      if (itemIds.length === 0) {\n" +
                "        container.innerHTML = '<p style=\"color:#555; text-align:center; padding:20px;\">Your cart is empty.</p>';\n" +
                "        bill.innerHTML = '';\n" +
                "        checkoutBtn.style.display = 'none';\n" +
                "      } else {\n" +
                "        checkoutBtn.style.display = 'block';\n" +
                "        itemIds.forEach(id => {\n" +
                "          const p = products.find(x => x.id == id);\n" +
                "          if (p) {\n" +
                "            const qty = cart[id];\n" +
                "            const lineTotal = p.price * qty;\n" +
                "            subtotal += lineTotal;\n" +
                "            const itemDiv = document.createElement('div');\n" +
                "            itemDiv.className = 'cart-item';\n" +
                "            itemDiv.innerHTML = `\n" +
                "              <div>\n" +
                "                <div class=\"cart-item-title\">${p.name} (x${qty})</div>\n" +
                "                <div class=\"cart-item-sub\">Rs. ${p.price.toLocaleString('en-IN')} each</div>\n" +
                "                <button class=\"remove-btn\" onclick=\"removeFromCart(${p.id})\">Remove</button>\n" +
                "              </div>\n" +
                "              <div style=\"font-weight:700;\">Rs. ${lineTotal.toLocaleString('en-IN', {minimumFractionDigits:2})}</div>\n" +
                "            `;\n" +
                "            container.appendChild(itemDiv);\n" +
                "          }\n" +
                "        });\n" +
                "\n" +
                "        let autoDisc = (subtotal > 10000) ? (subtotal * 0.10) : 0;\n" +
                "        let estGrandTotal = subtotal - autoDisc;\n" +
                "        bill.innerHTML = `\n" +
                "          <div class=\"bill-row\"><span>Subtotal:</span> <span>Rs. ${subtotal.toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>\n" +
                "          ${autoDisc > 0 ? `<div class=\"bill-row discount\"><span>10% Discount (> Rs. 10k):</span> <span>-Rs. ${autoDisc.toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>` : ''}\n" +
                "          <div class=\"bill-row total\"><span>Estimated Total:</span> <span>Rs. ${estGrandTotal.toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>\n" +
                "        `;\n" +
                "      }\n" +
                "      document.getElementById('cartModal').classList.add('active');\n" +
                "    }\n" +
                "\n" +
                "    function removeFromCart(pid) {\n" +
                "      delete cart[pid];\n" +
                "      updateCartBadge();\n" +
                "      openCartModal();\n" +
                "    }\n" +
                "\n" +
                "    function openCheckoutDetails() {\n" +
                "      closeModal('cartModal');\n" +
                "      document.getElementById('checkoutModal').classList.add('active');\n" +
                "    }\n" +
                "\n" +
                "    async function submitFinalOrder() {\n" +
                "      const name = document.getElementById('custName').value.trim();\n" +
                "      const address = document.getElementById('custAddress').value.trim();\n" +
                "      const phone = document.getElementById('custPhone').value.trim();\n" +
                "      const coupon = document.getElementById('custCoupon').value.trim();\n" +
                "      const paymentMethod = document.getElementById('paymentMethod').value;\n" +
                "\n" +
                "      if (!name || !address || !phone) {\n" +
                "        showAlert('checkoutAlert', 'Please fill in all customer details.', true);\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      const itemsList = Object.keys(cart).map(id => `${id}:${cart[id]}`).join(',');\n" +
                "\n" +
                "      try {\n" +
                "        const res = await fetch('/api/checkout', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/json' },\n" +
                "          body: JSON.stringify({\n" +
                "            customerName: name,\n" +
                "            address: address,\n" +
                "            phone: phone,\n" +
                "            coupon: coupon,\n" +
                "            paymentMethod: paymentMethod,\n" +
                "            items: itemsList\n" +
                "          })\n" +
                "        });\n" +
                "        const data = await res.json();\n" +
                "        if (data.success) {\n" +
                "          cart = {};\n" +
                "          updateCartBadge();\n" +
                "          closeModal('checkoutModal');\n" +
                "          displayReceipt(data);\n" +
                "          loadProducts(); // Refresh stocks\n" +
                "        } else {\n" +
                "          showAlert('checkoutAlert', data.message || 'Order failed.', true);\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showAlert('checkoutAlert', 'Network error placing order.', true);\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function displayReceipt(order) {\n" +
                "      const container = document.getElementById('receiptContent');\n" +
                "      container.innerHTML = `\n" +
                "        <div style=\"background:#f9f9f9; border:1px solid #ddd; padding:16px; border-radius:8px; font-size:13px; margin: 14px 0;\">\n" +
                "          <p><strong>Order Timestamp:</strong> ${order.timestamp}</p>\n" +
                "          <p><strong>Customer:</strong> ${order.customerName} (${order.phone})</p>\n" +
                "          <p><strong>Shipping Address:</strong> ${order.address}</p>\n" +
                "          <p><strong>Payment Mode:</strong> ${order.paymentMethod}</p>\n" +
                "          <p style=\"color:var(--success); font-weight:600; margin-top:6px;\">Status: ${order.payMessage}</p>\n" +
                "          <hr style=\"margin: 10px 0; border: none; border-top: 1px solid #eee;\" />\n" +
                "          <div class=\"bill-row\"><span>Subtotal:</span> <span>Rs. ${parseFloat(order.subtotal).toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>\n" +
                "          ${order.autoDiscount > 0 ? `<div class=\"bill-row discount\"><span>10% Volume Discount:</span> <span>-Rs. ${parseFloat(order.autoDiscount).toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>` : ''}\n" +
                "          ${order.couponDiscount > 0 ? `<div class=\"bill-row discount\"><span>Coupon 'SAVE50':</span> <span>-Rs. ${parseFloat(order.couponDiscount).toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>` : ''}\n" +
                "          <div class=\"bill-row total\"><span>Grand Total Paid:</span> <span>Rs. ${parseFloat(order.grandTotal).toLocaleString('en-IN', {minimumFractionDigits:2})}</span></div>\n" +
                "        </div>\n" +
                "        <p style=\"font-size:12px; color:#666;\">Receipt has been recorded persistently to <code>orders.txt</code>.</p>\n" +
                "      `;\n" +
                "      document.getElementById('receiptModal').classList.add('active');\n" +
                "    }\n" +
                "\n" +
                "    async function viewOrderHistory() {\n" +
                "      try {\n" +
                "        const res = await fetch('/api/orders');\n" +
                "        const text = await res.text();\n" +
                "        document.getElementById('ordersFileContent').innerText = text;\n" +
                "        document.getElementById('ordersModal').classList.add('active');\n" +
                "      } catch (err) {\n" +
                "        alert('Failed to load past orders');\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function openLoginModal() {\n" +
                "      document.getElementById('loginModal').classList.add('active');\n" +
                "    }\n" +
                "\n" +
                "    function openAdminQuick() {\n" +
                "      document.getElementById('loginRole').value = 'admin';\n" +
                "      document.getElementById('loginUser').value = 'admin';\n" +
                "      document.getElementById('loginPass').value = 'admin123';\n" +
                "      openLoginModal();\n" +
                "    }\n" +
                "\n" +
                "    async function performLogin() {\n" +
                "      const role = document.getElementById('loginRole').value;\n" +
                "      const user = document.getElementById('loginUser').value.trim();\n" +
                "      const pass = document.getElementById('loginPass').value.trim();\n" +
                "\n" +
                "      try {\n" +
                "        const res = await fetch('/api/login', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/json' },\n" +
                "          body: JSON.stringify({ role: role, username: user, password: pass })\n" +
                "        });\n" +
                "        const data = await res.json();\n" +
                "        if (data.success) {\n" +
                "          currentUser = data;\n" +
                "          closeModal('loginModal');\n" +
                "          document.getElementById('userDisplay').innerText = `Hello, ${data.name || data.username} (${data.role.toUpperCase()})`;\n" +
                "          document.getElementById('loginBtn').innerText = 'Logout';\n" +
                "          document.getElementById('loginBtn').onclick = () => location.reload();\n" +
                "          if (data.role === 'admin') {\n" +
                "            openAdminDashboard();\n" +
                "          } else {\n" +
                "            alert('Welcome, Amazon Customer! You are logged in.');\n" +
                "          }\n" +
                "        } else {\n" +
                "          loginAttempts--;\n" +
                "          showAlert('loginAlert', `${data.message} (${loginAttempts} attempts left)`, true);\n" +
                "          if (loginAttempts <= 0) {\n" +
                "            alert('Maximum login attempts exceeded!');\n" +
                "            closeModal('loginModal');\n" +
                "            loginAttempts = 3;\n" +
                "          }\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showAlert('loginAlert', 'Authentication error.', true);\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function openAdminDashboard() {\n" +
                "      const list = document.getElementById('adminProductList');\n" +
                "      list.innerHTML = '';\n" +
                "      products.forEach(p => {\n" +
                "        const row = document.createElement('div');\n" +
                "        row.style = 'display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid #eee;';\n" +
                "        row.innerHTML = `\n" +
                "          <div style=\"font-weight:600;\">[${p.id}] ${p.name}</div>\n" +
                "          <div style=\"display:flex; gap:10px; align-items:center;\">\n" +
                "            <label style=\"font-size:12px;\">Price: Rs.</label>\n" +
                "            <input type=\"number\" id=\"admPrice-${p.id}\" value=\"${p.price}\" style=\"width:90px; padding:4px;\" />\n" +
                "            <label style=\"font-size:12px;\">Stock:</label>\n" +
                "            <input type=\"number\" id=\"admStock-${p.id}\" value=\"${p.stock}\" style=\"width:60px; padding:4px;\" />\n" +
                "            <button class=\"btn-nav\" style=\"background:#232f3e; color:white;\" onclick=\"saveProductChanges(${p.id})\">Save</button>\n" +
                "          </div>\n" +
                "        `;\n" +
                "        list.appendChild(row);\n" +
                "      });\n" +
                "      document.getElementById('adminModal').classList.add('active');\n" +
                "    }\n" +
                "\n" +
                "    async function saveProductChanges(id) {\n" +
                "      const price = document.getElementById(`admPrice-${id}`).value;\n" +
                "      const stock = document.getElementById(`admStock-${id}`).value;\n" +
                "      const res = await fetch('/api/admin/update', {\n" +
                "        method: 'POST',\n" +
                "        headers: { 'Content-Type': 'application/json' },\n" +
                "        body: JSON.stringify({ id: id, price: price, stock: stock })\n" +
                "      });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) {\n" +
                "        showAlert('adminAlert', 'Product updated successfully!', false);\n" +
                "        loadProducts();\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    async function addNewProduct() {\n" +
                "      const name = document.getElementById('newProdName').value.trim();\n" +
                "      const price = document.getElementById('newProdPrice').value;\n" +
                "      const stock = document.getElementById('newProdStock').value;\n" +
                "      if (!name || !price || !stock) return alert('Fill all product fields!');\n" +
                "      const res = await fetch('/api/admin/add', {\n" +
                "        method: 'POST',\n" +
                "        headers: { 'Content-Type': 'application/json' },\n" +
                "        body: JSON.stringify({ name: name, price: price, stock: stock })\n" +
                "      });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) {\n" +
                "        showAlert('adminAlert', 'New product added!', false);\n" +
                "        document.getElementById('newProdName').value = '';\n" +
                "        document.getElementById('newProdPrice').value = '';\n" +
                "        document.getElementById('newProdStock').value = '';\n" +
                "        await loadProducts();\n" +
                "        openAdminDashboard();\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function closeModal(id) {\n" +
                "      document.getElementById(id).classList.remove('active');\n" +
                "    }\n" +
                "\n" +
                "    function showAlert(elemId, msg, isError) {\n" +
                "      const el = document.getElementById(elemId);\n" +
                "      el.className = `alert-box ${isError ? 'alert-error' : 'alert-success'}`;\n" +
                "      el.innerText = msg;\n" +
                "      el.style.display = 'block';\n" +
                "      setTimeout(() => { el.style.display = 'none'; }, 4000);\n" +
                "    }\n" +
                "\n" +
                "    // Initialize\n" +
                "    loadProducts();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
