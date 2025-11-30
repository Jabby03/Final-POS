package com.newfoundsoftware.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced SalesReportController
 * Features: Filtering, Summary Stats, Real-time Updates
 */
public class SalesReportController {
    
    private static final Logger LOGGER = Logger.getLogger(SalesReportController.class.getName());
    
    // Table and Columns
    @FXML private TableView<SalesItem> salesTable;
    @FXML private TableColumn<SalesItem, String> colDate;
    @FXML private TableColumn<SalesItem, String> colProduct;
    @FXML private TableColumn<SalesItem, Integer> colQty;
    @FXML private TableColumn<SalesItem, Double> colPrice;
    @FXML private TableColumn<SalesItem, Double> colTotal;
    @FXML private TableColumn<SalesItem, String> colCategory;
    
    // Filters
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private TextField txtSearchProduct;
    
    // Summary Labels
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalItems;
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblRecordCount;
    
    // Buttons
    @FXML private Button btnRefresh;
    
    // Data
    private static final ObservableList<SalesItem> allSalesData = FXCollections.observableArrayList();
    private final ObservableList<SalesItem> filteredSalesData = FXCollections.observableArrayList();
    private final JdbcDao jdbcDao = new JdbcDao();
    
    @FXML
    public void initialize() {
        setupTableColumns();
        createSalesTable();
        loadSalesData();
        updateSummary();
        setupSearchListener();
    }
    
    // ==================== SETUP ====================
    
    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        // Format price columns
        colPrice.setCellFactory(col -> new TableCell<SalesItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₱%.2f", price));
            }
        });
        
        colTotal.setCellFactory(col -> new TableCell<SalesItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("₱%.2f", total));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                }
            }
        });
        
        salesTable.setItems(filteredSalesData);
    }
    
    private void setupSearchListener() {
        if (txtSearchProduct != null) {
            txtSearchProduct.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }
    
    // ==================== DATABASE ====================
    
    private void createSalesTable() {
        try (Connection conn = jdbcDao.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String createTable = "CREATE TABLE IF NOT EXISTS sales (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sale_date DATE NOT NULL," +
                    "product VARCHAR(100) NOT NULL," +
                    "quantity INT NOT NULL," +
                    "unit_price DOUBLE NOT NULL," +
                    "total DOUBLE NOT NULL," +
                    "category VARCHAR(50)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.executeUpdate(createTable);
            
            LOGGER.info("Sales table created/verified");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating sales table", e);
            showError("Database Error", "Failed to create sales table: " + e.getMessage());
        }
    }
    
    private void loadSalesData() {
        allSalesData.clear();
        
        try (Connection conn = jdbcDao.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // First, check what columns exist in the sales table
            String query = "SELECT * FROM sales ORDER BY sale_date DESC, id DESC LIMIT 1";
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData metaData = rs.getMetaData();
            
            // Determine the correct column names
            String productCol = null;
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String colName = metaData.getColumnName(i).toLowerCase();
                if (colName.equals("product_name") || colName.equals("product") || colName.equals("productname")) {
                    productCol = metaData.getColumnName(i);
                    break;
                }
            }
            
            if (productCol == null) {
                throw new SQLException("Could not find product column in sales table");
            }
            
            // Now load all data with the correct column name
            query = "SELECT sale_date, " + productCol + " as product_name, quantity, unit_price, total, category " +
                    "FROM sales ORDER BY sale_date DESC, id DESC";
            
            rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                SalesItem item = new SalesItem(
                    rs.getString("sale_date"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getDouble("total"),
                    rs.getString("category")
                );
                allSalesData.add(item);
            }
            
            applyFilters();
            LOGGER.info("Loaded " + allSalesData.size() + " sales records");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading sales data", e);
            showError("Database Error", "Failed to load sales data: " + e.getMessage());
        }
    }
    
    // ==================== FILTERING ====================
    
    @FXML
    private void handleApplyFilter() {
        applyFilters();
    }
    
    @FXML
    private void handleClearFilter() {
        if (dateFrom != null) dateFrom.setValue(null);
        if (dateTo != null) dateTo.setValue(null);
        if (txtSearchProduct != null) txtSearchProduct.clear();
        applyFilters();
    }
    
    private void applyFilters() {
        filteredSalesData.clear();
        
        LocalDate fromDate = dateFrom != null ? dateFrom.getValue() : null;
        LocalDate toDate = dateTo != null ? dateTo.getValue() : null;
        String searchText = txtSearchProduct != null ? txtSearchProduct.getText().toLowerCase().trim() : "";
        
        for (SalesItem item : allSalesData) {
            // Date filter
            if (fromDate != null && toDate != null) {
                try {
                    LocalDate itemDate = LocalDate.parse(item.getDate());
                    if (itemDate.isBefore(fromDate) || itemDate.isAfter(toDate)) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            
            // Product search filter
            if (!searchText.isEmpty() && !item.getProduct().toLowerCase().contains(searchText)) {
                continue;
            }
            
            filteredSalesData.add(item);
        }
        
        updateSummary();
        updateRecordCount();
    }
    
    // ==================== SUMMARY STATS ====================
    
    private void updateSummary() {
        double totalSales = filteredSalesData.stream()
                .mapToDouble(SalesItem::getTotal)
                .sum();
        
        int totalItems = filteredSalesData.stream()
                .mapToInt(SalesItem::getQuantity)
                .sum();
        
        int totalTransactions = filteredSalesData.size();
        
        if (lblTotalSales != null) {
            lblTotalSales.setText(String.format("₱%.2f", totalSales));
        }
        if (lblTotalItems != null) {
            lblTotalItems.setText(String.valueOf(totalItems));
        }
        if (lblTotalTransactions != null) {
            lblTotalTransactions.setText(String.valueOf(totalTransactions));
        }
    }
    
    private void updateRecordCount() {
        if (lblRecordCount != null) {
            lblRecordCount.setText(filteredSalesData.size() + " of " + allSalesData.size());
        }
    }
    
    // ==================== ACTIONS ====================
    
    @FXML
    private void handleRefresh() {
        loadSalesData();
        showInfo("Refreshed", "Sales data has been refreshed successfully!");
    }
    
    // ==================== STATIC ADD SALES ====================
    
    /**
     * Called from PaymentController to add new sales
     */
    public static void addSales(ObservableList<DashboardController.OrderItem> orderItems) {
        String date = LocalDate.now().toString();
        JdbcDao dao = new JdbcDao();
        
        try (Connection conn = dao.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO sales (sale_date, product, quantity, unit_price, total, category) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {
            
            for (DashboardController.OrderItem item : orderItems) {
                String category = getCategoryFromDatabase(conn, item.getDescription());
                
                ps.setString(1, date);
                ps.setString(2, item.getDescription());
                ps.setInt(3, item.getQuantity());
                ps.setDouble(4, item.getPrice());
                ps.setDouble(5, item.getTotal());
                ps.setString(6, category);
                ps.addBatch();
                
                // Also add to in-memory list for real-time update
                SalesItem newSale = new SalesItem(
                    date,
                    item.getDescription(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getTotal(),
                    category
                );
                allSalesData.add(0, newSale); // Add at top (most recent)
            }
            
            ps.executeBatch();
            LOGGER.info("Added " + orderItems.size() + " sales records");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding sales", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Get category from products table based on product name
     */
    private static String getCategoryFromDatabase(Connection conn, String productName) {
        // First, let's see what columns exist in the products table
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            
            System.out.println("=== Products Table Columns ===");
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                System.out.println("Column " + i + ": " + metaData.getColumnName(i));
            }
            System.out.println("==============================");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not inspect products table", e);
        }
        
        // Try different possible column names for product
        String[] productColumns = {"product_name", "product", "productname", "name", "productName", "description"};
        
        for (String colName : productColumns) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT category FROM products WHERE " + colName + " = ?")) {
                ps.setString(1, productName);
                ResultSet rs = ps.executeQuery();
                
                if (rs.next()) {
                    String category = rs.getString("category");
                    System.out.println("✓ Found category for '" + productName + "': " + category + " (using column: " + colName + ")");
                    return category != null && !category.trim().isEmpty() ? category : "OTHER";
                }
            } catch (SQLException e) {
                // Column doesn't exist, try next one
                continue;
            }
        }
        
        System.out.println("✗ Could not find category for product: " + productName);
        LOGGER.log(Level.WARNING, "Could not find category for product: " + productName);
        return "OTHER";
    }
    
    // ==================== ALERTS ====================
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
    
    // ==================== SALES ITEM MODEL ====================
    
    public static class SalesItem {
        private final String date;
        private final String product;
        private final int quantity;
        private final double unitPrice;
        private final double total;
        private final String category;
        
        public SalesItem(String date, String product, int quantity, double unitPrice, double total, String category) {
            this.date = date;
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = total;
            this.category = category != null ? category : "OTHER";
        }
        
        // Getters
        public String getDate() { return date; }
        public String getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getTotal() { return total; }
        public String getCategory() { return category; }
    }
}