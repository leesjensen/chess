package dataaccess;

import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryDataAccessTest {

    private UserData registerRandomUser(DataAccess da) {
        var user = new UserData("cow", "cow@byu.edu", "moo");
        da.register(user, null);
        return user;
    }

    @Test
    void clear() {
        var da = new MemoryDataAccess();
        var user = registerRandomUser(da);
        assertEquals(user, da.getUser(user.username()));
        da.clear();
        assertNull(da.getUser(user.username()));
    }


    @Test
    void getUser() {
        var da = new MemoryDataAccess();
        var user = registerRandomUser(da);
        assertEquals(user, da.getUser(user.username()));
    }
}