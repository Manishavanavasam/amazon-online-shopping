import java.io.Serializable;

/**
 * Class: Customer
 * Description: JavaBean class representing an Amazon customer.
 * Adheres to JavaBean standards:
 *  - Implements Serializable
 *  - Private fields
 *  - Default (no-arg) constructor
 *  - Parameterized constructor
 *  - Public getters and setters
 */
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    // Private fields (encapsulation)
    private String username;
    private String fullName;
    private String deliveryAddress;
    private String contactNumber;

    /**
     * Default no-argument constructor (Required for JavaBean).
     */
    public Customer() {
        this.username = "";
        this.fullName = "";
        this.deliveryAddress = "";
        this.contactNumber = "";
    }

    /**
     * Parameterized constructor to initialize a customer with details.
     *
     * @param username        The user account handle
     * @param fullName        Customer's full name
     * @param deliveryAddress Customer's complete shipping address
     * @param contactNumber   Customer's phone contact number
     */
    public Customer(String username, String fullName, String deliveryAddress, String contactNumber) {
        this.username = username;
        this.fullName = fullName;
        this.deliveryAddress = deliveryAddress;
        this.contactNumber = contactNumber;
    }

    // --- Getters and Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    /**
     * Returns a formatted string representation of customer information.
     */
    @Override
    public String toString() {
        return "Customer Details: [Name: " + fullName + 
               ", Address: " + deliveryAddress + 
               ", Phone: " + contactNumber + 
               ", Username: " + username + "]";
    }
}
