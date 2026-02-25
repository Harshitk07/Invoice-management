package context.security;

public final class CapabilityContext {

    private static CapabilityGate gate;

    private CapabilityContext() {}

    public static void init(CapabilityGate g) {
        gate = g;
    }

    public static CapabilityGate get() {
        if (gate == null) {
            throw new IllegalStateException("CapabilityContext not initialized");
        }
        return gate;
    }
}
