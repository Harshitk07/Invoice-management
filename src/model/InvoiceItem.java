package model;

import javafx.beans.property.*;

public class InvoiceItem {

    /* ================= CORE FIELDS ================= */

    private final StringProperty itemName = new SimpleStringProperty("Double click to Add");
    private final StringProperty hsn = new SimpleStringProperty("");
    private final DoubleProperty qty = new SimpleDoubleProperty(1);
    private final StringProperty unit = new SimpleStringProperty("");
    private final DoubleProperty rate = new SimpleDoubleProperty(0);
    private final DoubleProperty gstPercent = new SimpleDoubleProperty(0);

    /**
     * FINAL line amount (persisted).
     * This value MUST NOT be recalculated during reprint.
     */
    private final DoubleProperty amount = new SimpleDoubleProperty(0);

    /* ================= CONSTRUCTOR ================= */

    public InvoiceItem() {
        qty.addListener((obs, o, n) -> recalcDraftAmount());
        rate.addListener((obs, o, n) -> recalcDraftAmount());
        gstPercent.addListener((obs, o, n) -> recalcDraftAmount());
        recalcDraftAmount();
    }

    /* ================= DRAFT CALCULATION ================= */

    /**
     * Used ONLY during UI editing / draft.
     * Reprint will overwrite amount from DB.
     */
    private void recalcDraftAmount() {
        double taxable = qty.get() * rate.get();
        amount.set(taxable);
    }

    /* ================= JavaFX PROPERTIES ================= */

    public StringProperty itemNameProperty() { return itemName; }
    public StringProperty hsnProperty() { return hsn; }
    public DoubleProperty qtyProperty() { return qty; }
    public StringProperty unitProperty() { return unit; }
    public DoubleProperty rateProperty() { return rate; }
    public DoubleProperty gstPercentProperty() { return gstPercent; }
    public DoubleProperty amountProperty() { return amount; }

    /* ================= PLAIN GETTERS / SETTERS ================= */

    public String getItemName() { return itemName.get(); }
    public void setItemName(String v) { itemName.set(v); }

    public String getHsn() { return hsn.get(); }
    public void setHsn(String v) { hsn.set(v); }

    public double getQty() { return qty.get(); }
    public void setQty(double v) { qty.set(v); }

    public String getUnit() { return unit.get(); }
    public void setUnit(String v) { unit.set(v); }

    public double getRate() { return rate.get(); }
    public void setRate(double v) { rate.set(v); }

    public double getGstPercent() { return gstPercent.get(); }
    public void setGstPercent(double v) { gstPercent.set(v); }

    /**
     * Amount getter returns STORED value.
     * No recalculation here.
     */
    public double getAmount() { return amount.get(); }

    /**
     * Used ONLY when loading from DB (reprint).
     */
    public void setAmount(double v) { amount.set(v); }
}
