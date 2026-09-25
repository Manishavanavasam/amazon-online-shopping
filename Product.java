import java.io.Serializable;

/**
 * Class: Product
 * Description: JavaBean class representing a catalog item in the store.
 * Adheres to JavaBean standards:
 *  - Implements Serializable
 *  - Private fields (id, name, price, stock)
 *  - Default (no-arg) constructor
 *  - Parameterized constructor
 *  - Public getters and setters
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    // Private fields
    private int id;
    private String name;
    private double price;
    private int stock;

    /**
     * Default no-argument constructor (Required for JavaBean).
     */
    public Product() {
        this.id = 0;
        this.name = "";
        this.price = 0.0;
        this.stock = 0;
    }

    /**
     * Parameterized constructor to initialize product details.
     *
     * @param id    Unique product identifier
     * @param name  Display name of the product
     * @param price Unit price in INR (₹)
     * @param stock Available quantity in inventory
     */
    public Product(int id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Decrements the product stock by the specified quantity.
     *
     * @param quantity Number of units to deduct
     */
    public void reduceStock(int quantity) {
        if (quantity <= this.stock) {
            this.stock -= quantity;
        }
    }

    /**
     * Formats product information for display in catalogs.
     */
    @Override
    public String toString() {
        return String.format("[%d] %-15s - Rs. %,.2f (Stock: %d units)", id, name, price, stock);
    }
}
