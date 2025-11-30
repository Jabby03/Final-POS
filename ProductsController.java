package com.newfoundsoftware.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProductsController - Fixed for your current FXML
 * Simplified version matching your updated UI
 */
public class ProductsController implements Initializable {
    
    private static final Logger LOGGER = Logger.getLogger(ProductsController.class.getName());
    
    // ========== FXML Fields - Matching YOUR current FXML ==========
    
    // Table and Columns
    @FXML private TableView<Products> tableProducts;
    @FXML private TableColumn<Products, Integer> colId;
    @FXML private TableColumn<Products, String> colBarcode;
    @FXML private TableColumn<Products, String> colDescription;
    @FXML private TableColumn<Products, Double> colPrice;
    @FXML private TableColumn<Products, String> colCategory;
    @FXML private TableColumn<Products, String> colStatus;
    
    // Form Fields (matching your FXML)
    @FXML private TextField etId;
    @FXML private TextField etBarcode;
    @FXML private TextField etDescription;
    @FXML private TextField etPrice;
    @FXML private ComboBox<String> cbStatus;
    
    // Image
    @FXML private ImageView ivProduct;
    
    // Buttons
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    
    // Data
    private ObservableList<Products> productList = FXCollections.observableArrayList();
    private Products selectedProduct = null;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupComboBoxes();
        setupEventHandlers();
        loadProducts();
        
        // Initial button states
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
    }
    
    // ==================== SETUP METHODS ====================
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Format price column with peso sign
        colPrice.setCellFactory(col -> new TableCell<Products, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", price));
                }
            }
        });
        
        // ⭐ Color-code status column
        colStatus.setCellFactory(col -> new TableCell<Products, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Inactive".equalsIgnoreCase(status)) {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #ccffcc; -fx-text-fill: #006600; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // ⭐ Gray out entire row for inactive products
        tableProducts.setRowFactory(tv -> new TableRow<Products>() {
            @Override
            protected void updateItem(Products product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setStyle("");
                } else if ("Inactive".equalsIgnoreCase(product.getStatus())) {
                    setStyle("-fx-background-color: #f5f5f5; -fx-opacity: 0.7;");
                } else {
                    setStyle("");
                }
            }
        });
        
        tableProducts.setItems(productList);
    }
    
    private void setupComboBoxes() {
        // Setup Status ComboBox
        ObservableList<String> statuses = FXCollections.observableArrayList(
            Products.STATUS_ACTIVE,
            Products.STATUS_INACTIVE
        );
        cbStatus.setItems(statuses);
        cbStatus.setValue(Products.STATUS_ACTIVE);
    }
    
    private void setupEventHandlers() {
        // Table selection handler
        tableProducts.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> handleTableSelection(newVal)
        );
        
        // Double-click to edit
        tableProducts.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && selectedProduct != null) {
                loadProductToForm(selectedProduct);
                btnUpdate.setDisable(false);
            }
        });
    }
    
    // ==================== DATABASE OPERATIONS ====================
    
    private void loadProducts() {
        productList.clear();
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) {
            showError("Database Error", "Could not connect to database");
            return;
        }
        
        String query = "SELECT * FROM products ORDER BY id DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Products product = new Products(
                    rs.getInt("id"),
                    rs.getString("barcode"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("category"),
                    rs.getString("image_path"),
                    rs.getString("status")
                );
                productList.add(product);
            }
            
            LOGGER.info("Loaded " + productList.size() + " products");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading products", e);
            showError("Database Error", "Failed to load products: " + e.getMessage());
        }
    }
    
    @FXML
    private void editEntry() {
        if (selectedProduct == null) {
            showWarning("No Selection", "Please select a product to update");
            return;
        }
        
        if (!validateInput()) return;
        
        // Confirm update
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Update");
        confirm.setHeaderText("Update Product?");
        confirm.setContentText("Are you sure you want to update: " + selectedProduct.getDescription() + "?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) {
            showError("Database Error", "Could not connect to database");
            return;
        }
        
        // ⭐ Only update status - keep other fields unchanged
        String query = "UPDATE products SET status=? WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, cbStatus.getValue());
            ps.setInt(2, selectedProduct.getId());
            
            int updateResult = ps.executeUpdate();
            
            if (updateResult > 0) {
                showInfo("Success", "Product status updated successfully!");
                loadProducts();
                clearForm();
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating product", e);
            showError("Database Error", "Failed to update product: " + e.getMessage());
        }
    }
    
    @FXML
    private void deleteEntry() {
        if (selectedProduct == null) {
            showWarning("No Selection", "Please select a product to delete");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Product?");
        confirm.setContentText("Are you sure you want to delete: " + selectedProduct.getDescription() + "?\n\nThis action cannot be undone!");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteProduct(selectedProduct.getId());
        }
    }
    
    private void deleteProduct(int productId) {
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) {
            showError("Database Error", "Could not connect to database");
            return;
        }
        
        String query = "DELETE FROM products WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, productId);
            int result = ps.executeUpdate();
            
            if (result > 0) {
                showInfo("Success", "Product deleted successfully!");
                loadProducts();
                clearForm();
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting product", e);
            showError("Database Error", "Failed to delete product: " + e.getMessage());
        }
    }
    
    // ==================== UI HELPERS ====================
    
    private void handleTableSelection(Products product) {
        selectedProduct = product;
        
        if (product != null) {
            btnUpdate.setDisable(false);
            btnDelete.setDisable(false);
            loadProductToForm(product);
        } else {
            btnUpdate.setDisable(true);
            btnDelete.setDisable(true);
        }
    }
    
    private void loadProductToForm(Products product) {
        etId.setText(String.valueOf(product.getId()));
        etBarcode.setText(product.getBarcode());
        etDescription.setText(product.getDescription());
        etPrice.setText(String.format("%.2f", product.getPrice()));
        cbStatus.setValue(product.getStatus());
        
        // Load image preview
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            try {
                InputStream imgStream = getClass().getResourceAsStream(product.getImagePath());
                if (imgStream != null) {
                    ivProduct.setImage(new Image(imgStream));
                } else {
                    LOGGER.warning("Image not found: " + product.getImagePath());
                    ivProduct.setImage(null);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load product image", e);
                ivProduct.setImage(null);
            }
        } else {
            ivProduct.setImage(null);
        }
    }
    
    private void clearForm() {
        etId.clear();
        etBarcode.clear();
        etDescription.clear();
        etPrice.clear();
        cbStatus.setValue(Products.STATUS_ACTIVE);
        ivProduct.setImage(null);
        
        selectedProduct = null;
        
        tableProducts.getSelectionModel().clearSelection();
        
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
    }
    
    // ==================== VALIDATION ====================
    
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        
        if (selectedProduct == null) {
            errors.append("• No product selected\n");
        }
        
        if (cbStatus.getValue() == null || cbStatus.getValue().isEmpty()) {
            errors.append("• Status is required\n");
        }
        
        if (errors.length() > 0) {
            showError("Validation Error", "Please fix the following errors:\n\n" + errors.toString());
            return false;
        }
        
        return true;
    }
    
    // ==================== ALERT DIALOGS ====================
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}