package util;

import context.Capability;
import context.security.CapabilityContext;
import javafx.scene.Node;

public final class CapabilityBinder {

    private CapabilityBinder() {}

    public static void bindDisable(Node node, Capability capability) {
        node.setDisable(!CapabilityContext.get().has(capability));
    }
}
