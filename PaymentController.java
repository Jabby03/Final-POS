package com.newfoundsoftware.pos;

import javafx.scene.control.TextField;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * PaymentController - Redesigned Flow
 * Step 1: Show Invoice
 * Step 2: Enter Payment Amount
 * Step 3: Show Receipt
 */
public class PaymentController {

    private static final double VAT_RATE = 0.12;

    // ========== STEP 1: INVOICE PANE ==========
    @FXML private VBox invoicePane;
    @FXML private Label lblInvoiceNumber;
    @FXML private Label lblInvoiceDate;
    @FXML private TableView<DashboardController.OrderItem> tableInvoiceItems;
    @FXML private TableColumn<DashboardController.OrderItem, Integer> colInvoiceQty;
    @FXML private TableColumn<DashboardController.OrderItem, String> colInvoiceDescription;
    @FXML private TableColumn<DashboardController.OrderItem, Double> colInvoiceUnitPrice;
    @FXML private TableColumn<DashboardController.OrderItem, Double> colInvoiceTotal;
    @FXML private Label lblInvoiceSubTotal;
    @FXML private Label lblInvoiceVAT;
    @FXML private Label lblInvoiceGrandTotal;

    // ========== STEP 2: PAYMENT PANE ==========
    @FXML private VBox paymentPane;
    @FXML private Label lblPaymentTotal;
    @FXML private TextField txtAmountPaid;
    @FXML private Label lblChangePreview;

    // ========== STEP 3: RECEIPT PANE ==========
    @FXML private VBox receiptPane;  // ⭐ CHANGED FROM ScrollPane to VBox
    @FXML private Label lblReceiptNumber;
    @FXML private Label lblReceiptDate;
    @FXML private TableView<DashboardController.OrderItem> tableReceiptItems;
    @FXML private TableColumn<DashboardController.OrderItem, Integer> colReceiptQty;
    @FXML private TableColumn<DashboardController.OrderItem, String> colReceiptDescription;
    @FXML private TableColumn<DashboardController.OrderItem, Double> colReceiptUnitPrice;
    @FXML private TableColumn<DashboardController.OrderItem, Double> colReceiptTotal;
    @FXML private Label lblReceiptSubTotal;
    @FXML private Label lblReceiptVAT;
    @FXML private Label lblReceiptGrandTotal;
    @FXML private Label lblReceiptAmountPaid;
    @FXML private Label lblReceiptChange;

    // Data
    private double subTotal;
    private double vat;
    private double grandTotal;
    private double amountPaid;
    private double change;
    private ObservableList<DashboardController.OrderItem> currentOrderItems;
    private DashboardController dashboardController;
    private String invoiceNumber;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    private void initialize() {
        // Setup Invoice Table
        colInvoiceQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colInvoiceDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colInvoiceUnitPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colInvoiceTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        // Format price columns for Invoice
        colInvoiceUnitPrice.setCellFactory(col -> new TableCell<DashboardController.OrderItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₱%.2f", price));
            }
        });

        colInvoiceTotal.setCellFactory(col -> new TableCell<DashboardController.OrderItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("₱%.2f", total));
            }
        });

        // Setup Receipt Table
        colReceiptQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colReceiptDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colReceiptUnitPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colReceiptTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        // Format price columns for Receipt
        colReceiptUnitPrice.setCellFactory(col -> new TableCell<DashboardController.OrderItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₱%.2f", price));
            }
        });

        colReceiptTotal.setCellFactory(col -> new TableCell<DashboardController.OrderItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("₱%.2f", total));
            }
        });

        // Amount input listener - calculate change in real-time
        if (txtAmountPaid != null) {
            txtAmountPaid.textProperty().addListener((obs, oldVal, newVal) -> updateChangePreview());
            // Enter key on amount field -> confirm payment
            txtAmountPaid.setOnAction(e -> handleConfirmPayment(null));
        }
    }

    // ==================== PUBLIC SETTERS ====================

    public void setTotalAmount(double totalAmount) {
        this.subTotal = totalAmount;
        this.vat = totalAmount * VAT_RATE;
        this.grandTotal = totalAmount + vat;
    }

    public void setOrderItems(ObservableList<DashboardController.OrderItem> orderItems) {
        this.currentOrderItems = orderItems;
        
        // Calculate totals from order items
        this.subTotal = currentOrderItems.stream()
                .mapToDouble(DashboardController.OrderItem::getTotal)
                .sum();
        this.vat = subTotal * VAT_RATE;
        this.grandTotal = subTotal + vat;

        // Show invoice (Step 1)
        showInvoice();
    }

    // ==================== STEP 1: SHOW INVOICE ====================

    private void showInvoice() {
        invoiceNumber = generateInvoiceNumber();
        LocalDateTime now = LocalDateTime.now();

        lblInvoiceNumber.setText("Invoice No: " + invoiceNumber);
        lblInvoiceDate.setText("Date: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // Populate table
        tableInvoiceItems.setItems(currentOrderItems);

        // Display totals
        lblInvoiceSubTotal.setText(String.format("₱%.2f", subTotal));
        lblInvoiceVAT.setText(String.format("₱%.2f", vat));
        lblInvoiceGrandTotal.setText(String.format("₱%.2f", grandTotal));

        // Show invoice pane (first view)
        invoicePane.setVisible(true);
        invoicePane.setManaged(true);
        paymentPane.setVisible(false);
        paymentPane.setManaged(false);
        receiptPane.setVisible(false);
        receiptPane.setManaged(false);
    }

    // ==================== STEP 2: PAYMENT INPUT ====================

    @FXML
    private void handleProceedToPayment(ActionEvent event) {
        // Hide invoice, show payment input
        invoicePane.setVisible(false);
        invoicePane.setManaged(false);
        paymentPane.setVisible(true);
        paymentPane.setManaged(true);

        // Display total amount
        lblPaymentTotal.setText(String.format("₱%.2f", grandTotal));
        lblChangePreview.setText("₱0.00");
        txtAmountPaid.clear();
        txtAmountPaid.requestFocus();
    }

    @FXML
    private void handleBackToInvoice(ActionEvent event) {
        // Back to invoice view
        paymentPane.setVisible(false);
        paymentPane.setManaged(false);
        invoicePane.setVisible(true);
        invoicePane.setManaged(true);
    }

    private void updateChangePreview() {
        try {
            String input = txtAmountPaid.getText().trim();
            if (input.isEmpty()) {
                lblChangePreview.setText("₱0.00");
                lblChangePreview.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: green;");
                return;
            }

            double amount = Double.parseDouble(input);
            double changeAmount = amount - grandTotal;

            if (changeAmount < 0) {
                lblChangePreview.setText(String.format("₱%.2f (Insufficient)", changeAmount));
                lblChangePreview.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: red;");
            } else {
                lblChangePreview.setText(String.format("₱%.2f", changeAmount));
                lblChangePreview.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: green;");
            }
        } catch (NumberFormatException e) {
            lblChangePreview.setText("Invalid amount");
            lblChangePreview.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: red;");
        }
    }

    @FXML
    private void handleConfirmPayment(ActionEvent event) {
        try {
            // Validate amount
            if (txtAmountPaid.getText() == null || txtAmountPaid.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Missing Amount", "Please enter the payment amount!");
                return;
            }

            amountPaid = Double.parseDouble(txtAmountPaid.getText().trim());

            if (amountPaid < grandTotal) {
                showAlert(Alert.AlertType.ERROR, "Insufficient Payment",
                    String.format("Amount paid (₱%.2f) is less than total amount (₱%.2f)!",
                        amountPaid, grandTotal));
                txtAmountPaid.selectAll();
                txtAmountPaid.requestFocus();
                return;
            }

            change = amountPaid - grandTotal;

            // Check and deduct stock
            if (!checkAndDeductStock()) {
                return;
            }

            // Add to sales report
            if (currentOrderItems != null && !currentOrderItems.isEmpty()) {
                SalesReportController.addSales(currentOrderItems);
            }

            // Save PDF receipt
            saveReceiptPDF();

            // Show receipt (Step 3)
            showReceipt();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Please enter a valid amount!");
            txtAmountPaid.selectAll();
            txtAmountPaid.requestFocus();
        }
    }

    private boolean checkAndDeductStock() {
        // Check stock availability first
        StringBuilder stockErrors = new StringBuilder();
        boolean hasStockIssues = false;

        for (DashboardController.OrderItem item : currentOrderItems) {
            int currentStock = SalesInventoryController.getStock(item.getProductId());
            if (currentStock < item.getQuantity()) {
                hasStockIssues = true;
                stockErrors.append("• ").append(item.getDescription())
                          .append(" (Need: ").append(item.getQuantity())
                          .append(", Available: ").append(currentStock).append(")\n");
            }
        }

        if (hasStockIssues) {
            showAlert(Alert.AlertType.ERROR, "Insufficient Stock",
                "Cannot complete transaction. Insufficient stock:\n\n" + stockErrors.toString());
            return false;
        }

        // Deduct stock
        for (DashboardController.OrderItem item : currentOrderItems) {
            if (!SalesInventoryController.deductStock(item.getProductId(), item.getQuantity())) {
                showAlert(Alert.AlertType.ERROR, "Stock Error",
                    "Failed to update stock for: " + item.getDescription());
                return false;
            }
        }

        return true;
    }

    // ==================== STEP 3: SHOW RECEIPT ====================

    private void showReceipt() {
        LocalDateTime now = LocalDateTime.now();

        lblReceiptNumber.setText("Receipt No: " + invoiceNumber);
        lblReceiptDate.setText("Date: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // Populate table
        tableReceiptItems.setItems(currentOrderItems);

        // Display totals
        lblReceiptSubTotal.setText(String.format("₱%.2f", subTotal));
        lblReceiptVAT.setText(String.format("₱%.2f", vat));
        lblReceiptGrandTotal.setText(String.format("₱%.2f", grandTotal));
        lblReceiptAmountPaid.setText(String.format("₱%.2f", amountPaid));
        lblReceiptChange.setText(String.format("₱%.2f", change));

        // Hide payment pane, show receipt
        paymentPane.setVisible(false);
        paymentPane.setManaged(false);
        receiptPane.setVisible(true);
        receiptPane.setManaged(true);
        
        // Resize window to fit receipt
        Stage stage = (Stage) receiptPane.getScene().getWindow();
        stage.setWidth(750);
        stage.setHeight(700);
    }

    @FXML
    private void handleReceiptClose() {
        // Close window and reset dashboard
        Stage stage = (Stage) receiptPane.getScene().getWindow();
        stage.close();

        if (dashboardController != null) {
            dashboardController.resetToLandingPage();
        }
    }

    // ==================== CANCEL ====================

    @FXML
    private void handleCancel(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Transaction");
        confirm.setHeaderText("Cancel this transaction?");
        confirm.setContentText("Are you sure you want to cancel? No changes will be saved.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Stage stage = (Stage) invoicePane.getScene().getWindow();
                stage.close();
            }
        });
    }

    // ==================== PDF GENERATION ====================

    private void saveReceiptPDF() {
        try {
            File invoiceFolder = new File("invoices");
            if (!invoiceFolder.exists()) invoiceFolder.mkdir();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File file = new File(invoiceFolder, "Receipt_" + invoiceNumber + "_" + timestamp + ".pdf");

            Document document = new Document(new com.lowagie.text.Rectangle(250, 600), 10, 10, 10, 10);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Header
            Font headerFont = new Font(Font.COURIER, 10, Font.BOLD);
            Font textFont = new Font(Font.COURIER, 9);

            Paragraph header = new Paragraph("SHEGLAM COSMETICS", headerFont);
            header.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(header);

            document.add(new Paragraph("Receipt No: " + invoiceNumber, textFont));
            document.add(new Paragraph(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), textFont));
            document.add(new Paragraph(" "));

            // Items Table
            PdfPTable table = new PdfPTable(4);
            table.setWidths(new int[]{30, 100, 40, 40});
            table.setWidthPercentage(100);

            Font tableHeaderFont = new Font(Font.COURIER, 8, Font.BOLD);
            table.addCell(makeTableCell("QTY", tableHeaderFont));
            table.addCell(makeTableCell("ITEM", tableHeaderFont));
            table.addCell(makeTableCell("PRICE", tableHeaderFont));
            table.addCell(makeTableCell("TOTAL", tableHeaderFont));

            Font tableFont = new Font(Font.COURIER, 8);
            for (DashboardController.OrderItem item : currentOrderItems) {
                table.addCell(makeTableCell(String.valueOf(item.getQuantity()), tableFont));
                table.addCell(makeTableCell(item.getDescription(), tableFont));
                table.addCell(makeTableCell(String.format("%.2f", item.getPrice()), tableFont));
                table.addCell(makeTableCell(String.format("%.2f", item.getTotal()), tableFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new int[]{70, 40});

            totalsTable.addCell(makeLabelCell("SUB TOTAL:"));
            totalsTable.addCell(makeValueCell(String.format("%.2f", subTotal)));

            totalsTable.addCell(makeLabelCell("VAT (12%):"));
            totalsTable.addCell(makeValueCell(String.format("%.2f", vat)));

            totalsTable.addCell(makeLabelCell("GRAND TOTAL:"));
            totalsTable.addCell(makeValueCell(String.format("%.2f", grandTotal)));

            totalsTable.addCell(makeLabelCell("Amount Paid:"));
            totalsTable.addCell(makeValueCell(String.format("%.2f", amountPaid)));

            totalsTable.addCell(makeLabelCell("Change:"));
            totalsTable.addCell(makeValueCell(String.format("%.2f", change)));

            document.add(totalsTable);

            document.close();
            System.out.println("✓ Receipt saved: " + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("✗ Failed to save receipt PDF: " + e.getMessage());
        }
    }

    private PdfPCell makeTableCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.BOX);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell makeLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.COURIER, 9)));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell makeValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.COURIER, 9, Font.BOLD)));
        cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        return cell;
    }

    // ==================== UTILITIES ====================

    private String generateInvoiceNumber() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}