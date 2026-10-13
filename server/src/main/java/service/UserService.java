package service;

import dataaccess.DataAccess;
import model.UserData;

public class UserService {
    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }
    public void registerUser(UserData userData) {

    }
}
