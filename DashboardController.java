package com.newfoundsoftware.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String SALESINVENTORY_FXML = "SalesInventory.fxml";
    private static final String PRODUCTS_FXML = "Products.fxml";
    private static final String LOOKUP_FXML = "Lookup.fxml";
    private static final String SALES_REPORT_FXML = "SalesReport.fxml";

    @FXML private Label lblUsername;
    @FXML private Label lblGrandTotal;
    @FXML private TableView<OrderItem> orderTable;
    @FXML private TableColumn<OrderItem, String> colDescription;
    @FXML private TableColumn<OrderItem, Double> colPrice;
    @FXML private TableColumn<OrderItem, Integer> colQuantity;
    @FXML private TableColumn<OrderItem, Double> colTotal;
    @FXML private GridPane productGrid;
    @FXML private Button foundationButton, blushButton, concealerButton, lipstickButton, eyeshadowButton;
    @FXML private ImageView landingLabel;
    @FXML private ImageView imageView1, imageView2, imageView3, imageView4, imageView5, imageView6, imageView7, imageView8, imageView9;
    @FXML private Label priceLabel1, priceLabel2, priceLabel3, priceLabel4, priceLabel5, priceLabel6, priceLabel7, priceLabel8, priceLabel9;
    @FXML private Label nameLabel1, nameLabel2, nameLabel3, nameLabel4, nameLabel5, nameLabel6, nameLabel7, nameLabel8, nameLabel9;
    @FXML private Button btnManageProduct;
    @FXML private Button btnSalesInventory;
    @FXML private StackPane productPane1;
    @FXML private StackPane productPane2;
    @FXML private StackPane productPane3;
    @FXML private StackPane productPane4;
    @FXML private StackPane productPane5;
    @FXML private StackPane productPane6;
    @FXML private StackPane productPane7;
    @FXML private StackPane productPane8;
    @FXML private StackPane productPane9;

    private List<ImageView> imageViews;
    private List<Label> priceLabels;
    private List<Label> nameLabels;
    private ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private List<Product> allProducts = new ArrayList<>();
    private String currentCategory = "FOUNDATION";
    private boolean orderActive = false;
    private static Stage pStage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUsername == null || productGrid == null || orderTable == null || lblGrandTotal == null || landingLabel == null) {
            LOGGER.severe("FXML injection failed: Key components are null.");
            throw new IllegalStateException("UI components not properly injected.");
        }

        imageViews = List.of(imageView1, imageView2, imageView3, imageView4, imageView5, imageView6, imageView7, imageView8, imageView9);
        priceLabels = List.of(priceLabel1, priceLabel2, priceLabel3, priceLabel4, priceLabel5, priceLabel6, priceLabel7, priceLabel8, priceLabel9);
        nameLabels = List.of(nameLabel1, nameLabel2, nameLabel3, nameLabel4, nameLabel5, nameLabel6, nameLabel7, nameLabel8, nameLabel9);

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        orderTable.setItems(orderItems);

        // Setup GridPane constraints for proper layout
        setupGridPane();

        loadProducts();
        showLandingPage();
    }

    /**
     * Setup GridPane constraints to ensure proper sizing and spacing
     */
    private void setupGridPane() {
        // Clear existing constraints (if any)
        productGrid.getColumnConstraints().clear();
        productGrid.getRowConstraints().clear();
        
        // Create column constraints (3 columns)
        for (int i = 0; i < 3; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setMinWidth(200);
            colConstraints.setPrefWidth(220);
            colConstraints.setHgrow(Priority.SOMETIMES); // Allow horizontal growth
            productGrid.getColumnConstraints().add(colConstraints);
        }
        
        // Create row constraints (3 rows)
        for (int i = 0; i < 3; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setMinHeight(150);
            rowConstraints.setPrefHeight(170);
            rowConstraints.setVgrow(Priority.SOMETIMES); // Allow vertical growth
            productGrid.getRowConstraints().add(rowConstraints);
        }
        
        // Set alignment to center
        productGrid.setAlignment(javafx.geometry.Pos.CENTER);
    }

    private void showLandingPage() {
        orderActive = false;
        landingLabel.setVisible(true);
        productGrid.setVisible(false);
        orderTable.setVisible(true);
        lblGrandTotal.setVisible(false);
        foundationButton.setDisable(true);
        blushButton.setDisable(true);
        concealerButton.setDisable(true);
        lipstickButton.setDisable(true);
        eyeshadowButton.setDisable(true);
    }
    
    public void resetToLandingPage() {
        orderItems.clear();
        updateGrandTotal();
        orderTable.refresh();
        showLandingPage();
    }

    /**
     * Back button action - returns to landing page
     */
    @FXML
    private void backToLanding(ActionEvent event) {
        if (orderActive && !orderItems.isEmpty()) {
            // Show confirmation dialog if there are items in the order
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Back");
            alert.setHeaderText("Return to Landing Page?");
            alert.setContentText("You have items in your order. Going back will clear the current order.\n\nDo you want to continue?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                resetToLandingPage();
            }
        } else {
            resetToLandingPage();
        }
    }

    public void setUsername(String username) {
        lblUsername.setText((username != null && !username.trim().isEmpty()) ? username : "Guest");
    }

    private void setPrimaryStage(Stage pStage) {
        DashboardController.pStage = pStage;
    }

    public static Stage getPrimaryStage() {
        return pStage;
    }

    private void loadProducts() {
        allProducts.clear();
        JdbcDao jdbcDao = new JdbcDao();
        Connection conn = jdbcDao.getConnection();
    
        if (conn == null) {
            LOGGER.warning("Could not connect to database");
            return;
        }
        
        String query = "SELECT * FROM products ORDER BY category, description";
        
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int productId = rs.getInt("id");
                int stock = SalesInventoryController.getStock(productId);
                
                Product product = new Product(
                    productId,
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("image_path"),
                    rs.getString("category"),
                    rs.getString("status"),
                    stock
                );
                allProducts.add(product);
            }
            
            LOGGER.info("Loaded " + allProducts.size() + " products");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading products", e);
        }
    }

    private void displayProducts() {
        List<Product> filteredProducts = allProducts.stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(currentCategory))
                .limit(9)
                .toList();

        for (int i = 0; i < imageViews.size(); i++) {
            if (i < filteredProducts.size()) {
                Product product = filteredProducts.get(i);
                
                try {
                    var inputStream = getClass().getResourceAsStream(product.getImagePath());
                    if (inputStream != null) {
                        imageViews.get(i).setImage(new Image(inputStream));
                    } else {
                        LOGGER.warning("Image not found: " + product.getImagePath());
                    }
                    
                    // Check both status and stock
                    boolean available = product.isAvailable() && product.getStock() > 0;
                    
                    if (!available) {
                        imageViews.get(i).setOpacity(0.4);
                        imageViews.get(i).setStyle("-fx-effect: grayscale;");
                    } else {
                        imageViews.get(i).setOpacity(1.0);
                        imageViews.get(i).setStyle("");
                    }
                    
                    priceLabels.get(i).setText("₱" + product.getPrice());
                    
                    // Show stock and availability
                    if (!product.isAvailable()) {
                        nameLabels.get(i).setText(product.getName() + " - NOT AVAILABLE");
                        nameLabels.get(i).setStyle("-fx-background-color: rgba(255,0,0,0.7); -fx-text-fill: white;");
                    } else if (product.getStock() == 0) {
                        nameLabels.get(i).setText(product.getName() + " - OUT OF STOCK");
                        nameLabels.get(i).setStyle("-fx-background-color: rgba(255,0,0,0.7); -fx-text-fill: white;");
                    } else if (product.getStock() < 10) {
                        nameLabels.get(i).setText(product.getName() + " - Only " + product.getStock() + " left");
                        nameLabels.get(i).setStyle("-fx-background-color: rgba(255,165,0,0.7); -fx-text-fill: white;");
                    } else {
                        nameLabels.get(i).setText(product.getName());
                        nameLabels.get(i).setStyle("-fx-background-color: rgba(220,190,255,0.7); -fx-text-fill: black;");
                    }
                    
                    imageViews.get(i).setUserData(product);
                    
                } catch (Exception e) {
                    LOGGER.warning("Failed to update UI for: " + product.getName());
                }
            } else {
                imageViews.get(i).setImage(null);
                imageViews.get(i).setOpacity(1.0);
                imageViews.get(i).setStyle("");
                nameLabels.get(i).setText("No Product");
                nameLabels.get(i).setStyle("-fx-background-color: rgba(220,190,255,0.7); -fx-text-fill: black;");
                imageViews.get(i).setUserData(null);
            }
        }
    }

    @FXML
    private void filterProducts(ActionEvent event) {
        if (!orderActive) return;
        Button source = (Button) event.getSource();
        currentCategory = source.getText().toUpperCase();
        displayProducts();
    }

    @FXML
    private void addToOrder(MouseEvent event) {
        if (!orderActive) return;
    
        ImageView source = (ImageView) event.getSource();
        Product product = (Product) source.getUserData();
        
        if (product != null) {
            // Check if product is available
            if (!product.isAvailable()) {
                showAlert(Alert.AlertType.WARNING, "Product Not Available", 
                    product.getName() + " is currently not available.");
                return;
            }
            
            // Check if in stock
            if (product.getStock() == 0) {
                showAlert(Alert.AlertType.WARNING, "Out of Stock", 
                    product.getName() + " is out of stock.");
                return;
            }
            
            // Create custom quantity dialog
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Enter Quantity");
            dialog.setHeaderText(product.getName() + "\n\nAvailable Stock: " + product.getStock());
            
            // Set the button types
            ButtonType confirmButtonType = new ButtonType("Add to Order", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
            
            // Create the quantity input field
            TextField quantityField = new TextField("1");
            quantityField.setPromptText("Enter quantity");
            quantityField.setPrefWidth(200);
            
            VBox content = new VBox(10);
            content.getChildren().addAll(
                new Label("Enter quantity:"),
                quantityField
            );
            content.setStyle("-fx-padding: 20;");
            
            dialog.getDialogPane().setContent(content);
            
            // Request focus on the quantity field
            javafx.application.Platform.runLater(() -> quantityField.requestFocus());
            
            // Convert the result when confirm button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    try {
                        return Integer.parseInt(quantityField.getText());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            });
            
            Optional<Integer> result = dialog.showAndWait();
            
            result.ifPresent(requestedQty -> {
                if (requestedQty == null) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", 
                        "Please enter a valid number.");
                    return;
                }
                
                if (requestedQty <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Quantity", 
                        "Quantity must be greater than 0.");
                    return;
                }
                
                // Check current order quantity
                int currentOrderQty = 0;
                for (OrderItem item : orderItems) {
                    if (item.getProductId() == product.getId()) {
                        currentOrderQty = item.getQuantity();
                        break;
                    }
                }
                
                int totalQty = currentOrderQty + requestedQty;
                
                if (totalQty > product.getStock()) {
                    showAlert(Alert.AlertType.WARNING, "Insufficient Stock", 
                        "Cannot add " + requestedQty + " items!\n\n" +
                        "Available Stock: " + product.getStock() + "\n" +
                        "Already in Cart: " + currentOrderQty + "\n" +
                        "Requested: " + requestedQty + "\n" +
                        "Total Needed: " + totalQty + "\n\n" +
                        "Maximum you can add: " + (product.getStock() - currentOrderQty));
                    return;
                }
                
                // Add to order only after validation passes
                boolean found = false;
                for (OrderItem item : orderItems) {
                    if (item.getProductId() == product.getId()) {
                        item.setQuantity(item.getQuantity() + requestedQty);
                        item.setTotal(item.getPrice() * item.getQuantity());
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    orderItems.add(new OrderItem(
                        product.getId(),
                        product.getName(), 
                        product.getPrice(), 
                        requestedQty, 
                        product.getPrice() * requestedQty
                    ));
                }
                
                updateGrandTotal();
                orderTable.refresh();
            });
        }
    }

    private void updateGrandTotal() {
        double total = orderItems.stream().mapToDouble(OrderItem::getTotal).sum();
        lblGrandTotal.setText("₱" + String.format("%.2f", total));
    }

    private void openModalWindow(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            if (root == null) throw new IOException("Failed to load FXML: " + resource);
            
            Scene fxmlFile = new Scene(root);
            Stage window = new Stage();
            window.setScene(fxmlFile);
            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle(title);
            
            // Set window to maximized
            window.setMaximized(true);
            
            setPrimaryStage(window);
            window.showAndWait();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error opening modal window for " + resource, ex);
            showErrorAlert("Error", "Failed to open " + title + ": " + ex.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void newOrder(ActionEvent event) {
        orderActive = true;
        landingLabel.setVisible(false);
        productGrid.setVisible(true);
        orderTable.setVisible(true);
        lblGrandTotal.setVisible(true);
        foundationButton.setDisable(false);
        blushButton.setDisable(false);
        concealerButton.setDisable(false);
        lipstickButton.setDisable(false);
        eyeshadowButton.setDisable(false);

        loadProducts();
        displayProducts();
    }

    @FXML
    private void payment(ActionEvent event) {
        if (orderItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Order", "Please add items to the order first.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Payment.fxml"));
            Parent root = loader.load();
            
            PaymentController controller = loader.getController();
            controller.setTotalAmount(calculateTotal());
            controller.setOrderItems(orderItems);
            controller.setDashboardController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Payment");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setWidth(400);
            stage.setHeight(400);
            stage.setResizable(true);
            stage.showAndWait();

            resetToLandingPage();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Payment Error", "Failed to open payment window: " + e.getMessage());
        }
    }
    
    private double calculateTotal() {
        return orderItems.stream().mapToDouble(OrderItem::getTotal).sum();
    }

    @FXML
    private void cancelOrder(ActionEvent event) {
        ObservableList<OrderItem> selectedItems = orderTable.getSelectionModel().getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Cancel Order", "Please select item(s) to cancel.");
            return;
        }

        orderItems.removeAll(selectedItems);
        updateGrandTotal();
        orderTable.refresh();
    }

    @FXML
    private void salesReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SalesReport.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Sales Report");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void logout(ActionEvent event) {
        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("You will be returned to the login screen.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Load the login window
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/newfoundsoftware/pos/Login.fxml"));
                Parent root = loader.load();
                
                // Create new login stage
                Stage loginStage = new Stage();
                loginStage.setTitle("POS | Login");
                loginStage.setScene(new Scene(root));
                loginStage.setResizable(false);
                
                // Show login window
                loginStage.show();
                
                // Close current dashboard window
                Stage currentStage = (Stage) lblUsername.getScene().getWindow();
                currentStage.close();
                
                LOGGER.info("User logged out successfully");
                
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading login window", e);
                showErrorAlert("Logout Error", "Failed to return to login screen: " + e.getMessage());
                
                // If login window fails to load, just close the dashboard
                Stage currentStage = (Stage) lblUsername.getScene().getWindow();
                currentStage.close();
            }
        }
    }

    @FXML
    private void closeApp(ActionEvent event) {
        logout(event);
    }

    @FXML
    private void actionSalesInventory(ActionEvent event) {
        openModalWindow(SALESINVENTORY_FXML, "Sales Inventory");
        loadProducts();
        if (orderActive) displayProducts();
    }

    @FXML
    private void actionManageProduct(ActionEvent event) {
        openModalWindow(PRODUCTS_FXML, "Manage Products");
        loadProducts();
        if (orderActive) displayProducts();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode key = event.getCode();
        switch (key) {
            case F1: newOrder(null); break;
            case F2: payment(null); break;
            case F3: cancelOrder(null); break;
            case F4: actionManageProduct(null); break;
            case F5: actionSalesInventory(null); break;
            case F6: salesReport(null); break;
            case F7: logout(null); break;
            case F8: backToLanding(null); break;
        }
    }

    public static class Product {
        private int id;
        private String name;
        private double price;
        private String imagePath;
        private String category;
        private String status;
        private int stock;

        public Product(int id, String name, double price, String imagePath, String category, String status, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imagePath = imagePath;
            this.category = category;
            this.status = status;
            this.stock = stock;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getImagePath() { return imagePath; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public int getStock() { return stock; }
        
        public boolean isAvailable() {
            return "Active".equalsIgnoreCase(status);
        }
    }

    public static class OrderItem {
        private int productId;
        private String description;
        private double price;
        private int quantity;
        private double total;

        public OrderItem(int productId, String description, double price, int quantity, double total) {
            this.productId = productId;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
            this.total = total;
        }

        public int getProductId() { return productId; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }
}