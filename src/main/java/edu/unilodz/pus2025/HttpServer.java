package edu.unilodz.pus2025;

import io.javalin.websocket.WsContext;
import org.json.JSONException;
import org.json.JSONObject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HttpServer {
    private static final Log log = Log.get();

    private final int port;
    private final Set<WsContext> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public HttpServer(int port) {
        this.port = port;
    }

    public void broadcast(String message) {
        for (WsContext client: clients) {
            if (client.session.isOpen()) {
                client.send(message);
            }
        }
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

    public void start() {
        Javalin app = Javalin.create(config -> config.staticFiles.add("/frontend/browser"));

        app.post("/api", HttpServer::handlePostApi);
        app.start(port);

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                clients.add(ctx);
                log.log(Level.INFO, "Websocket connected: {0}", ctx.session);
            });

            ws.onMessage(ctx -> {});

            ws.onClose(ctx -> {
                clients.remove(ctx);
                log.log(Level.INFO,"Websocket closed: {0}", ctx.session);
            });

            ws.onError(ctx -> {
                log.log(Level.SEVERE, "Websocket error: {0}", ctx.error() != null ? ctx.error().getMessage() : "Unknown error");
            });
        });
    }
}
