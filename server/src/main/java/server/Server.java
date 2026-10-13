package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import io.javalin.*;
import io.javalin.http.Context;
import model.UserData;
import service.AdminService;
import service.UserService;

public class Server {

    private final Javalin javalin;
    private final AdminService adminService;
    private final UserService userService;

    public Server() {
        var dataAccess = new MemoryDataAccess();
        adminService = new AdminService(dataAccess);
        userService = new UserService(dataAccess);

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.delete("/db", this::clearHandler);
        javalin.post("/user", this::registerHandler);
        // Register your endpoints and exception handlers here.

    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    private void clearHandler(Context ctx) {
        adminService.clear();
    }

    private void registerHandler(Context ctx) {
        var userData = new Gson().fromJson(ctx.body(), UserData.class);
        userService.registerUser(userData);
    }
}
