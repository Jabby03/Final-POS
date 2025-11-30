/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.newfoundsoftware.pos;

/**
 *
 * @author shereli
 */
public class SalesReport {

    private String date;
    private String product;
    private int quantity;
    private double total;

    public SalesReport(String date, String product, int quantity, double total) {
        this.date = date;
        this.product = product;
        this.quantity = quantity;
        this.total = total;
    }

    public String getDate() { return date; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }

}