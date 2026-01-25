package context;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class CapabilityGate {

    private static AppMode mode = AppMode.OWNER;

    private CapabilityGate() {}

    public static void setMode(AppMode m) {
        mode = m;
    }

    public static boolean allowed(Capability capability) {
        return mode == AppMode.OWNER;
    }
    private static final BooleanProperty capabilityDirty =
            new SimpleBooleanProperty(false);

    public static ReadOnlyBooleanProperty capabilityProperty() {
        return capabilityDirty;
    }

    // call this whenever role/capabilities change
    public static void refresh() {
        capabilityDirty.set(!capabilityDirty.get());
    }

}

