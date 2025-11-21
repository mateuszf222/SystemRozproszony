package edu.unilodz.pus2025;

import org.json.JSONException;
import org.json.JSONObject;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class HttpServer implements Runnable {
    private final int port;
    public HttpServer(int port) {
        this.port = port;
    }

    private static void handlePostApi(Context ctx) {
        JSONObject req, res;

        try {
            String body = ctx.body();
            req = new JSONObject(body);
        } catch (JSONException e) {
            JSONObject error = new JSONObject()
                .put("error", "Invalid JSON payload")
                .put("message", e.getMessage());
            ctx.status(400);
            ctx.contentType("application/json");
            ctx.result(error.toString());
            return;
        }

        String cmd = req.getString("cmd");
        res = new JSONObject();
        switch(cmd) {
            case "cluster":
                res.put("result", new JSONObject(Node.getCluster()));
                break;
            default:
                res.put("error", "Unknown cmd").put("message", "Cannot perform " + cmd);
                ctx.status(400);
        }

        res.put("received", req).put("status", "ok");

        ctx.contentType("application/json");
        ctx.result(res.toString());
    }

    @Override
    public void run() {
        Javalin app = Javalin.create(config -> config.staticFiles.add("/frontend/browser"));

        app.post("/api", HttpServer::handlePostApi);
        app.start(port);
    }
}
