package context.security;

import context.Capability;

public interface CapabilityGate {
    boolean has(Capability capability);
}

