import java.io.Serializable;

/**
 * Class: CartItem
 * Description: Represents an entry inside the shopping cart consisting of a Product
 * and the quantity selected by the customer.
 */
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    // Private fields
    private Product product;
    private int quantity;

    /**
     * Default no-argument constructor (JavaBean compliance).
     */
    public CartItem() {
        this.product = null;
        this.quantity = 0;
    }

    /**
     * Parameterized constructor.
     *
     * @param product  Selected Product
     * @param quantity Number of units added to cart
     */
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // --- Getters and Setters ---

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Calculates the subtotal for this cart item (unit price * quantity).
     *
     * @return Subtotal in INR (₹)
     */
    public double getSubtotal() {
        if (product == null) {
            return 0.0;
        }
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        if (product == null) {
            return "Empty Item";
        }
        return String.format("%-15s x %-3d @ Rs. %,.2f each = Rs. %,.2f",
                product.getName(), quantity, product.getPrice(), getSubtotal());
    }
}
