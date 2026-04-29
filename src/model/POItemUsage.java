package model;

public class POItemUsage {

    private String itemName;
    private double orderedQty;
    private double billedQty;
    private double remainingQty;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(double orderedQty) {
        this.orderedQty = orderedQty;
    }

    public double getBilledQty() {
        return billedQty;
    }

    public void setBilledQty(double billedQty) {
        this.billedQty = billedQty;
    }

    public double getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(double remainingQty) {
        this.remainingQty = remainingQty;
    }
}