package context.security;

import context.Capability;
import java.util.Set;

public final class StaticCapabilityGate implements CapabilityGate {

    private final Set<Capability> allowed;

    public StaticCapabilityGate(Set<Capability> allowed) {
        this.allowed = allowed;
    }

    @Override
    public boolean has(Capability capability) {
        return allowed.contains(capability);
    }
}
