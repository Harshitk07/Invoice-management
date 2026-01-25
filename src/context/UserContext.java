package context;

import model.UserContextData;
import service.UserContextProvider;

public final class UserContext {

    private static UserContextProvider provider;

    private UserContext() {}

    public static void init(UserContextProvider p) {
        provider = p;
    }

    public static UserContextData get() {
        if (provider == null) {
            throw new IllegalStateException("UserContext not initialized");
        }
        return provider.loadUser();
    }
}
