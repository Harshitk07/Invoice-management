package service;

import model.UserContextData;

public interface UserContextProvider {
    UserContextData loadUser();
}

