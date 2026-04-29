package model;

import javafx.beans.property.*;

public class PurchaseOrderItem {

    private long id;
    private long poId;

    private final ObjectProperty<Item> item = new SimpleObjectProperty<>();
    private final DoubleProperty qty = new SimpleDoubleProperty(0);
    private final DoubleProperty rate = new SimpleDoubleProperty(0);
    private final DoubleProperty gstPercent = new SimpleDoubleProperty(0);

    // -----------------------
    // DB fields
    // -----------------------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPoId() { return poId; }
    public void setPoId(long poId) { this.poId = poId; }

    // -----------------------
    // Properties
    // -----------------------

    public ObjectProperty<Item> itemProperty() { return item; }
    public DoubleProperty qtyProperty() { return qty; }
    public DoubleProperty rateProperty() { return rate; }
    public DoubleProperty gstPercentProperty() { return gstPercent; }

    public Item getItem() { return item.get(); }
    public void setItem(Item i) { item.set(i); }

    public double getQty() { return qty.get(); }
    public void setQty(double v) { qty.set(v); }

    public double getRate() { return rate.get(); }
    public void setRate(double v) { rate.set(v); }

    public double getGstPercent() { return gstPercent.get(); }
    public void setGstPercent(double v) { gstPercent.set(v); }

    // -----------------------
    // Derived values
    // -----------------------

    public String getItemName() {
        return getItem() != null ? getItem().getName() : "";
    }

    public String getHsn() {
        return getItem() != null ? getItem().getHsn() : "";
    }

    public String getUnit() {
        return getItem() != null ? getItem().getUnit() : "";
    }

    public double calculateTaxable() {
        return Math.max(0, getQty()) * Math.max(0, getRate());
    }

    public double calculateGstAmount() {
        return calculateTaxable() * Math.max(0, getGstPercent()) / 100.0;
    }
}