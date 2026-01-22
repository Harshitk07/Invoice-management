package model;

public enum InvoiceCopyType {
    ORIGINAL("ORIGINAL"),
    DUPLICATE("DUPLICATE"),
    TRIPLICATE("TRIPLICATE");

    private final String label;

    InvoiceCopyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

