package model;

import javafx.beans.property.*;

public class Customer {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty gstNo = new SimpleStringProperty();
    private final StringProperty state = new SimpleStringProperty();
    private final StringProperty stateCode = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    // ===== ID =====
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // ===== NAME =====
    public String getName() { return name.get(); }
    public void setName(String n) { name.set(n); }
    public StringProperty nameProperty() { return name; }

    // ===== ADDRESS =====
    public String getAddress() { return address.get(); }
    public void setAddress(String a) { address.set(a); }
    public StringProperty addressProperty() { return address; }

    // ===== GST =====
    public String getGstNo() { return gstNo.get(); }
    public void setGstNo(String g) { gstNo.set(g); }
    public StringProperty gstNoProperty() { return gstNo; }

    // ===== STATE =====
    public String getState() { return state.get(); }
    public void setState(String s) { state.set(s); }
    public StringProperty stateProperty() { return state; }

    public String getStateCode() { return stateCode.get(); }
    public void setStateCode(String sc) { stateCode.set(sc); }
    public StringProperty stateCodeProperty() { return stateCode; }

    // ===== ACTIVE (CRITICAL) =====
    public boolean isActive() { return active.get(); }
    public void setActive(boolean a) { active.set(a); }
    public BooleanProperty activeProperty() { return active; }

    @Override
    public String toString() {
        return getName();
    }
}
