package com.newfoundsoftware.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SalesInventoryController - Manages inventory with stock tracking
 */
public class SalesInventoryController implements Initializable {
    
    private static final Logger LOGGER = Logger.getLogger(SalesInventoryController.class.getName());
    private static final int DEFAULT_STOCK = 100;
    
    // ========== FXML Fields ==========
    @FXML private TextField searchField;
    @FXML private Button btnSearch;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockCheck;
    @FXML private Spinner<Integer> lowStockSpinner;
    @FXML private Button btnApplyFilters;
    @FXML private Button btnClearFilters;
    @FXML private Button btnUpdateStock;
    @FXML private Label totalProductsLabel;
    @FXML private TableView<InventoryItem> productTable;
    @FXML private TableColumn<InventoryItem, Integer> colId;
    @FXML private TableColumn<InventoryItem, String> colName;
    @FXML private TableColumn<InventoryItem, String> colCategory;
    @FXML private TableColumn<InventoryItem, Integer> colStock;
    @FXML private TableColumn<InventoryItem, Double> colUnitPrice;
    @FXML private TableColumn<InventoryItem, String> colStatus;
    @FXML private Button btnCloseDetails;
    @FXML private ImageView detailImage;
    @FXML private Label noImageLabel;
    @FXML private Label detailId;
    @FXML private Label detailName;
    @FXML private Label detailCategory;
    @FXML private Label detailStock;
    @FXML private Label detailUnitPrice;
    @FXML private Label detailStatus;
    
    private ObservableList<InventoryItem> inventoryList = FXCollections.observableArrayList();
    private ObservableList<InventoryItem> filteredList = FXCollections.observableArrayList();
    private InventoryItem selectedItem = null;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupFilters();
        setupSpinner();
        setupEventHandlers();
        initializeStockTable();
        loadInventoryData();
        hideDetailPanel();
    }
    
    // Create stock table if not exists
    private void initializeStockTable() {
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) return;
        
        String createTableSQL = "CREATE TABLE IF NOT EXISTS product_stock (" +
                "product_id INT PRIMARY KEY, " +
                "stock INT NOT NULL DEFAULT " + DEFAULT_STOCK + ", " +
                "FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            LOGGER.info("Stock table initialized");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating stock table", e);
        }
    }
    
    // Get stock for a product (creates entry if not exists)
    public static int getStock(int productId) {
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) return 0;
        
        // Try to get existing stock
        String selectSQL = "SELECT stock FROM product_stock WHERE product_id=?";
        try (PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } catch (SQLException e) {
            Logger.getLogger(SalesInventoryController.class.getName()).log(Level.WARNING, "Error getting stock", e);
        }
        
        // Create default stock entry if not exists
        String insertSQL = "INSERT INTO product_stock (product_id, stock) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
            ps.setInt(1, productId);
            ps.setInt(2, DEFAULT_STOCK);
            ps.executeUpdate();
            return DEFAULT_STOCK;
        } catch (SQLException e) {
            Logger.getLogger(SalesInventoryController.class.getName()).log(Level.SEVERE, "Error creating stock entry", e);
            return 0;
        }
    }
    
    // Update stock
    public static boolean updateStock(int productId, int newStock) {
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) return false;
        
        String updateSQL = "INSERT INTO product_stock (product_id, stock) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE stock=?";
        
        try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
            ps.setInt(1, productId);
            ps.setInt(2, newStock);
            ps.setInt(3, newStock);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.getLogger(SalesInventoryController.class.getName()).log(Level.SEVERE, "Error updating stock", e);
            return false;
        }
    }
    
    // Deduct stock (called when order is placed)
    public static boolean deductStock(int productId, int quantity) {
        int currentStock = getStock(productId);
        if (currentStock >= quantity) {
            return updateStock(productId, currentStock - quantity);
        }
        return false;
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colUnitPrice.setCellFactory(col -> new TableCell<InventoryItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₱%.2f", price));
            }
        });
        
        colStock.setCellFactory(col -> new TableCell<InventoryItem, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(stock));
                    if (stock == 0) {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-font-weight: bold;");
                    } else if (stock < 10) {
                        setStyle("-fx-background-color: #fff4cc; -fx-text-fill: #ff9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        colStatus.setCellFactory(col -> new TableCell<InventoryItem, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equalsIgnoreCase(status)) {
                        setStyle("-fx-background-color: #ccffcc; -fx-text-fill: #006600; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        productTable.setItems(filteredList);
    }
    
    private void setupFilters() {
        ObservableList<String> categories = FXCollections.observableArrayList(
            "All Categories", "FOUNDATION", "BLUSH", "CONCEALER", "LIPSTICK", "EYESHADOW"
        );
        categoryFilter.setItems(categories);
        categoryFilter.setValue("All Categories");
    }
    
    private void setupSpinner() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 10, 5);
        lowStockSpinner.setValueFactory(valueFactory);
    }
    
    private void setupEventHandlers() {
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> handleTableSelection(newVal)
        );
        
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (btnSearch != null) btnSearch.setOnAction(e -> applyFilters());
        if (btnApplyFilters != null) btnApplyFilters.setOnAction(e -> applyFilters());
        if (btnClearFilters != null) btnClearFilters.setOnAction(e -> clearFilters());
        if (btnCloseDetails != null) btnCloseDetails.setOnAction(e -> hideDetailPanel());
        if (btnUpdateStock != null) btnUpdateStock.setOnAction(e -> handleAddStock());
    }
    
    public void loadInventoryData() {
        inventoryList.clear();
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
        
        if (conn == null) {
            showError("Database Error", "Could not connect to database");
            return;
        }
        
        String query = "SELECT * FROM products ORDER BY id ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int productId = rs.getInt("id");
                int stock = getStock(productId);
                
                InventoryItem item = new InventoryItem(
                    productId,
                    rs.getString("description"),
                    rs.getString("category"),
                    stock,
                    rs.getDouble("price"),
                    rs.getString("status"),
                    rs.getString("image_path")
                );
                inventoryList.add(item);
            }
            
            applyFilters();
            updateTotalLabel();
            LOGGER.info("Loaded " + inventoryList.size() + " inventory items");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading inventory", e);
            showError("Database Error", "Failed to load inventory: " + e.getMessage());
        }
    }
    
    @FXML
    private void applyFilters() {
        filteredList.clear();
        
        String searchText = "";
        if (searchField != null && searchField.getText() != null) {
            searchText = searchField.getText().toLowerCase().trim();
        }
        
        String selectedCategory = categoryFilter.getValue();
        boolean filterLowStock = lowStockCheck.isSelected();
        int lowStockThreshold = lowStockSpinner.getValue();
        
        for (InventoryItem item : inventoryList) {
            boolean matchesSearch = searchText.isEmpty() ||
                item.getName().toLowerCase().contains(searchText) ||
                item.getCategory().toLowerCase().contains(searchText) ||
                String.valueOf(item.getId()).contains(searchText);
            
            boolean matchesCategory = "All Categories".equals(selectedCategory) ||
                item.getCategory().equalsIgnoreCase(selectedCategory);
            
            boolean matchesStock = !filterLowStock || item.getStock() < lowStockThreshold;
            
            if (matchesSearch && matchesCategory && matchesStock) {
                filteredList.add(item);
            }
        }
        
        updateTotalLabel();
        productTable.refresh();
    }
    
    @FXML
    private void clearFilters() {
        if (searchField != null) searchField.clear();
        categoryFilter.setValue("All Categories");
        lowStockCheck.setSelected(false);
        lowStockSpinner.getValueFactory().setValue(10);
        applyFilters();
    }
    
    private void updateTotalLabel() {
        if (totalProductsLabel != null) {
            totalProductsLabel.setText("Showing " + filteredList.size() + " products");
        }
    }
    
    private void handleTableSelection(InventoryItem item) {
        selectedItem = item;
        if (item != null) showDetailPanel(item);
    }
    
    private void showDetailPanel(InventoryItem item) {
        if (detailId != null) detailId.setText(String.valueOf(item.getId()));
        if (detailName != null) detailName.setText(item.getName());
        if (detailCategory != null) detailCategory.setText(item.getCategory());
        if (detailStock != null) detailStock.setText(String.valueOf(item.getStock()));
        if (detailUnitPrice != null) detailUnitPrice.setText(String.format("₱%.2f", item.getUnitPrice()));
        if (detailStatus != null) detailStatus.setText(item.getStatus());
        
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            try {
                InputStream imgStream = getClass().getResourceAsStream(item.getImagePath());
                if (imgStream != null && detailImage != null) {
                    detailImage.setImage(new Image(imgStream));
                    detailImage.setVisible(true);
                    if (noImageLabel != null) noImageLabel.setVisible(false);
                } else {
                    if (detailImage != null) detailImage.setVisible(false);
                    if (noImageLabel != null) noImageLabel.setVisible(true);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load image", e);
                if (detailImage != null) detailImage.setVisible(false);
                if (noImageLabel != null) noImageLabel.setVisible(true);
            }
        } else {
            if (detailImage != null) detailImage.setVisible(false);
            if (noImageLabel != null) noImageLabel.setVisible(true);
        }
        
        if (btnCloseDetails != null) btnCloseDetails.setVisible(true);
    }
    
    private void hideDetailPanel() {
        productTable.getSelectionModel().clearSelection();
        selectedItem = null;
        
        if (detailId != null) detailId.setText("-");
        if (detailName != null) detailName.setText("-");
        if (detailCategory != null) detailCategory.setText("-");
        if (detailStock != null) detailStock.setText("-");
        if (detailUnitPrice != null) detailUnitPrice.setText("-");
        if (detailStatus != null) detailStatus.setText("-");
        if (detailImage != null) detailImage.setVisible(false);
        if (noImageLabel != null) noImageLabel.setVisible(true);
        if (btnCloseDetails != null) btnCloseDetails.setVisible(false);
    }
    
    private void handleAddStock() {
        if (selectedItem == null) {
            showWarning("No Selection", "Please select a product to add stock.");
            return;
        }
        
        int currentStock = selectedItem.getStock();
        
        // Check if stock is already full
        if (currentStock >= DEFAULT_STOCK) {
            showWarning("Stock is Full", 
                "Cannot add more stock!\n\n" +
                "Current Stock: " + currentStock + "\n" +
                "Maximum Stock: " + DEFAULT_STOCK + "\n\n" +
                "Stock is already at maximum capacity.");
            return;
        }
        
        int availableSpace = DEFAULT_STOCK - currentStock;
        
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Add Stock");
        dialog.setHeaderText(
            "Add stock for: " + selectedItem.getName() + "\n" +
            "Current Stock: " + currentStock + "/" + DEFAULT_STOCK + "\n" +
            "Available Space: " + availableSpace
        );
        dialog.setContentText("Enter quantity to add:");
        
        dialog.showAndWait().ifPresent(input -> {
            try {
                int quantityToAdd = Integer.parseInt(input);
                
                if (quantityToAdd < 0) {
                    showError("Invalid Input", "Quantity to add cannot be negative!");
                    return;
                }
                
                if (quantityToAdd == 0) {
                    showWarning("No Change", "Please enter a quantity greater than 0.");
                    return;
                }
                
                int newStock = currentStock + quantityToAdd;
                
                // Check if adding would exceed maximum
                if (newStock > DEFAULT_STOCK) {
                    showWarning("Stock Limit Exceeded", 
                        "Cannot add " + quantityToAdd + " items!\n\n" +
                        "Current Stock: " + currentStock + "\n" +
                        "Trying to Add: +" + quantityToAdd + "\n" +
                        "Would Result In: " + newStock + "\n" +
                        "Maximum Allowed: " + DEFAULT_STOCK + "\n\n" +
                        "Maximum space available: " + availableSpace + " items");
                    return;
                }
                
                if (updateStock(selectedItem.getId(), newStock)) {
                    selectedItem.setStock(newStock);
                    productTable.refresh();
                    showDetailPanel(selectedItem);
                    
                    String message = String.format(
                        "Stock updated successfully!\n\n" +
                        "Previous: %d\n" +
                        "Added: +%d\n" +
                        "New Stock: %d/%d",
                        currentStock, quantityToAdd, newStock, DEFAULT_STOCK
                    );
                    
                    // Show special message if stock is now full
                    if (newStock == DEFAULT_STOCK) {
                        message += "\n\n✓ Stock is now FULL!";
                    }
                    
                    showInfo("Success", message);
                } else {
                    showError("Error", "Failed to update stock in database.");
                }
                
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid number!");
            }
        });
    }
    
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
    
    public static class InventoryItem {
        private int id;
        private String name;
        private String category;
        private int stock;
        private double unitPrice;
        private String status;
        private String imagePath;
        
        public InventoryItem(int id, String name, String category, int stock, 
                           double unitPrice, String status, String imagePath) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.stock = stock;
            this.unitPrice = unitPrice;
            this.status = status;
            this.imagePath = imagePath;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getStock() { return stock; }
        public double getUnitPrice() { return unitPrice; }
        public String getStatus() { return status; }
        public String getImagePath() { return imagePath; }
        
        public void setStock(int stock) { this.stock = stock; }
        public void setStatus(String status) { this.status = status; }
    }
}