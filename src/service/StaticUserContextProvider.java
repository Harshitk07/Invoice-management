package service;

import model.UserContextData;

public final class StaticUserContextProvider
        implements UserContextProvider {

    @Override
    public UserContextData loadUser() {
        return new UserContextData("Harshit", "IT Officer");
    }
}
