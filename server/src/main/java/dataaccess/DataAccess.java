package dataaccess;

import model.AuthData;
import model.UserData;

public interface DataAccess {
    void clear();
    void register(UserData userData, AuthData authData);

    UserData getUser(String username);
}
