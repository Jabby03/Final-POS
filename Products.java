package com.newfoundsoftware.pos;

import javafx.beans.property.*;
import java.sql.Blob;
import java.io.InputStream;

/**
 * Enhanced Products Model Class
 * Represents a product in the POS system with JavaFX properties for TableView binding
 * 
 * @author Administrator
 */
public class Products {
    
    // Using JavaFX properties for automatic UI updates
    private final IntegerProperty id;
    private final StringProperty barcode;
    private final StringProperty description;
    private final DoubleProperty price;
    private final StringProperty category;
    private final StringProperty imagePath;
    private final StringProperty status;
    private Blob imageBlob; // Keep blob for database operations
    
    // Status constants
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
    
    // Category constants (matching your categories)
    public static final String CATEGORY_FOUNDATION = "FOUNDATION";
    public static final String CATEGORY_BLUSH = "BLUSH";
    public static final String CATEGORY_CONCEALER = "CONCEALER";
    public static final String CATEGORY_LIPSTICK = "LIPSTICK";
    public static final String CATEGORY_EYESHADOW = "EYESHADOW";
    
    /**
     * Full Constructor - used when loading from database
     */
    public Products(int id, String barcode, String description, double price, 
                   String category, String imagePath, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.barcode = new SimpleStringProperty(barcode);
        this.description = new SimpleStringProperty(description);
        this.price = new SimpleDoubleProperty(price);
        this.category = new SimpleStringProperty(category);
        this.imagePath = new SimpleStringProperty(imagePath);
        this.status = new SimpleStringProperty(status);
        this.imageBlob = null;
    }
    
    /**
     * Constructor with Blob - for database operations
     */
    public Products(int id, String barcode, String description, double price, 
                   String category, Blob imageBlob, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.barcode = new SimpleStringProperty(barcode);
        this.description = new SimpleStringProperty(description);
        this.price = new SimpleDoubleProperty(price);
        this.category = new SimpleStringProperty(category);
        this.imagePath = new SimpleStringProperty("");
        this.status = new SimpleStringProperty(status);
        this.imageBlob = imageBlob;
    }
    
    /**
     * Simple Constructor - for creating new products
     */
    public Products(String barcode, String description, double price, 
                   String category, String imagePath) {
        this(0, barcode, description, price, category, imagePath, STATUS_ACTIVE);
    }
    
    // ==================== GETTERS ====================
    
    public int getId() {
        return id.get();
    }
    
    public IntegerProperty idProperty() {
        return id;
    }
    
    public String getBarcode() {
        return barcode.get();
    }
    
    public StringProperty barcodeProperty() {
        return barcode;
    }
    
    public String getDescription() {
        return description.get();
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    public double getPrice() {
        return price.get();
    }
    
    public DoubleProperty priceProperty() {
        return price;
    }
    
    public String getCategory() {
        return category.get();
    }
    
    public StringProperty categoryProperty() {
        return category;
    }
    
    public String getImagePath() {
        return imagePath.get();
    }
    
    public StringProperty imagePathProperty() {
        return imagePath;
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public StringProperty statusProperty() {
        return status;
    }
    
    public Blob getImageBlob() {
        return imageBlob;
    }
    
    // ==================== SETTERS ====================
    
    public void setId(int id) {
        this.id.set(id);
    }
    
    public void setBarcode(String barcode) {
        this.barcode.set(barcode);
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    public void setPrice(double price) {
        this.price.set(price);
    }
    
    public void setCategory(String category) {
        this.category.set(category);
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath.set(imagePath);
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    public void setImageBlob(Blob imageBlob) {
        this.imageBlob = imageBlob;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if product is active
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(getStatus());
    }
    
    /**
     * Toggle product status
     */
    public void toggleStatus() {
        setStatus(isActive() ? STATUS_INACTIVE : STATUS_ACTIVE);
    }
    
    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        return String.format("₱%.2f", getPrice());
    }
    
    /**
     * Validate product data
     */
    public boolean isValid() {
        return getBarcode() != null && !getBarcode().trim().isEmpty() &&
               getDescription() != null && !getDescription().trim().isEmpty() &&
               getPrice() > 0 &&
               getCategory() != null && !getCategory().trim().isEmpty();
    }
    
    /**
     * Get InputStream from Blob for image loading
     */
    public InputStream getImageStream() {
        try {
            if (imageBlob != null) {
                return imageBlob.getBinaryStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("Product[id=%d, barcode=%s, description=%s, price=%.2f, category=%s, status=%s]",
                getId(), getBarcode(), getDescription(), getPrice(), getCategory(), getStatus());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Products product = (Products) obj;
        return getId() == product.getId();
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}