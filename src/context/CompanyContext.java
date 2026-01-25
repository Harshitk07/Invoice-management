package context;

import model.CompanyProfile;
import service.CompanyProfileProvider;

public final class CompanyContext {

    private static CompanyProfileProvider provider;

    private CompanyContext() {}

    public static void init(CompanyProfileProvider p) {
        provider = p;
    }

    public static CompanyProfile get() {
        if (provider == null) {
            throw new IllegalStateException("CompanyContext not initialized");
        }
        return provider.loadProfile();
    }
}
