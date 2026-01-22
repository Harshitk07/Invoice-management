package model;

public class Item {

    private int id;
    private String name;
    private String hsn;
    private String unit;
    private double rate;
    private double gstPercent;
    private boolean active;

    // -------------------------
    // REQUIRED NO-ARG CONSTRUCTOR
    // -------------------------
    public Item() {
        // leave fields empty; DAO will populate
    }

    // -------------------------
    // CONVENIENCE CONSTRUCTOR (OPTIONAL USE)
    // -------------------------
    public Item(String name, String hsn, String unit,
                double rate, double gstPercent) {

        this.name = name;
        this.hsn = hsn;
        this.unit = unit;
        this.rate = rate;
        this.gstPercent = gstPercent;
    }

    // -------------------------
    // GETTERS
    // -------------------------
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHsn() {
        return hsn;
    }

    public String getUnit() {
        return unit;
    }

    public double getRate() {
        return rate;
    }

    public double getGstPercent() {
        return gstPercent;
    }

    // -------------------------
    // SETTERS
    // -------------------------
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = safe(name);
    }

    public void setHsn(String hsn) {
        this.hsn = safe(hsn);
    }

    public void setUnit(String unit) {
        this.unit = safe(unit);
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public void setGstPercent(double gstPercent) {
        this.gstPercent = gstPercent;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // -------------------------
    // UI FRIENDLY
    // -------------------------
    @Override
    public String toString() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    // -------------------------
    // INTERNAL SAFETY
    // -------------------------
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
