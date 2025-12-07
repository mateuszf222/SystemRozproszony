package edu.unilodz.pus2025;

import io.javalin.websocket.WsContext;
import org.json.JSONException;
import org.json.JSONObject;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static edu.unilodz.pus2025.Main.getConfig;
import static edu.unilodz.pus2025.Main.getCurrentNode;

public class HttpServer {
    private static final Log log = Log.get();

    private final int port;
    private final Set<WsContext> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public HttpServer(int port) {
        this.port = port;
    }

    public void clusterBroadcast() {
        for (WsContext client: clients) {
            if (client.session.isOpen()) {
                client.send(new JSONObject(Node.getCluster()).toString());
            }
        }
    }

    private static void handleGetApi(Context ctx) {
        JSONObject res = new JSONObject(Node.getCluster());
        ctx.contentType("application/json");
        ctx.result(res.toString());
    }

    private static void handlePostApi(Context ctx) {
        JSONObject req, res;
        String body = null;

        try {
            body = ctx.body();
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

        res = new JSONObject();
        JSONObject result = new JSONObject();
        try {
            String cmd = req.getString("cmd");
            if(cmd != null && !cmd.isEmpty()) {
                String node = req.getString("node");
                String whoami = getConfig().getString("name");
                try {
                    req.getJSONObject("args");
                } catch(JSONException e) {
                    req.put("args", new JSONObject());
                }
                if (whoami.equals(node)) {
                    result.put("node", whoami);
                    result.put("queued", true);
                    res.put("result", result);
                    new Thread(new Executor(getCurrentNode(), req)).start();
                } else {
                    Node target = Node.getCluster().get(node);
                    if(target == null) throw new JSONException("No such target node");
                    try (Socket targetSocket = new Socket()) {
                        targetSocket.connect(target.address);
                        PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream());
                        targetOutput.println(body);
                        targetOutput.flush();
                        result.put("node", node);
                        result.put("queued", true);
                        result.put("delegated", true);
                        res.put("result", result);
                    } catch (IOException ex) {
                        log.log(Level.SEVERE, ex.getMessage());
                    }
                }
            }
        }
        catch(Exception e) {
            res.put("error", "Processing error").put("message", "Cannot process " + body + ": " + e.getMessage());
            ctx.status(400);
        }

        res.put("received", req).put("ok", true);

        ctx.contentType("application/json");
        ctx.result(res.toString());
    }

    public void start() {
        Javalin app = Javalin.create(config -> config.staticFiles.add("/frontend/browser"));

        app.get("/api", HttpServer::handleGetApi);
        app.post("/api", HttpServer::handlePostApi);
        app.start(port);

        app.ws("/ws", ws -> {
            ws.onConnect(clients::add);
            ws.onMessage(ctx -> {});
            ws.onClose(clients::remove);
            ws.onError(ctx -> {});
        });
    }
}
