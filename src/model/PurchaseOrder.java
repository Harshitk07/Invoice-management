package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {

    // =========================
    // Identity
    // =========================
    private long id;
    private String poNo;
    private LocalDate poDate;
    private LocalDate deliveryByDate;

    // =========================
    // Optional Contract Link
    // =========================
    private Long contractId;

    // =========================
    // Financial Summary
    // =========================
    private double subtotal;
    private double gstTotal;
    private double grandTotal;

    // =========================
    // Status
    // =========================
    private POStatus status;

    // =========================
    // Items
    // =========================
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public PurchaseOrder() {}

    // =========================
    // Getters & Setters
    // =========================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPoNo() {
        return poNo;
    }

    public void setPoNo(String poNo) {
        this.poNo = poNo;
    }

    public LocalDate getPoDate() {
        return poDate;
    }

    public void setPoDate(LocalDate poDate) {
        this.poDate = poDate;
    }

    public LocalDate getDeliveryByDate() {
        return deliveryByDate;
    }

    public void setDeliveryByDate(LocalDate deliveryByDate) {
        this.deliveryByDate = deliveryByDate;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getGstTotal() {
        return gstTotal;
    }

    public void setGstTotal(double gstTotal) {
        this.gstTotal = gstTotal;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
        this.grandTotal = grandTotal;
    }

    public POStatus getStatus() {
        return status;
    }

    public void setStatus(POStatus status) {
        this.status = status;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }
}