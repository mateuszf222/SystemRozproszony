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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    // Obsługa zwykłych zadań (exec, sleep)
    private static void handlePostApi(Context ctx) {
        JSONObject req, res;
        String body;

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
                String whoami = getConfig().getName();
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
                        Database.communicationLog(System.currentTimeMillis(), false, body, target.toString());
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

    // --- ZMIANA: Obsługa PUT /api (Join logic) ---
    private static void handlePutApi(Context ctx) {
        try {
            JSONObject body = new JSONObject(ctx.body());

            // Oczekujemy payloadu: { "url": "http://target:port/api" }
            if (!body.has("url")) {
                ctx.status(400).result("Missing 'url' field in payload");
                return;
            }

            String targetUrl = body.getString("url");

            // 1. Pobieramy config zdalnego węzła
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject root = new JSONObject(response.body());

            // 2. Dodajemy do klastra
            for (String key : root.keySet()) {
                JSONObject nodeJson = root.getJSONObject(key);
                if (nodeJson.optBoolean("me", false)) {
                    String address = nodeJson.optString("address", null);
                    if (address != null) {
                        new Node(key, address);
                        log.log(Level.INFO, "Joined cluster via PUT: " + key);
                    }
                }
            }

            // 3. Wymuszamy UDP
            Heartbeat.perform(true);

            ctx.result("Joined successfully via PUT");
        } catch (Exception e) {
            log.log(Level.SEVERE, "PUT Join error: " + e.getMessage());
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    public void start() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/frontend/browser");
        });

        // REJESTRACJA ENDPOINTÓW
        app.get("/api", HttpServer::handleGetApi);   // Pobranie listy
        app.post("/api", HttpServer::handlePostApi); // Wykonywanie zadań (exec)

        // ZMIANA: Zamiast POST /join, mamy PUT /api
        app.put("/api", HttpServer::handlePutApi);   // Dołączanie do sieci (join)

        app.start(port);

        app.ws("/ws", ws -> {
            ws.onConnect(clients::add);
            ws.onMessage(ctx -> {});
            ws.onClose(clients::remove);
            ws.onError(ctx -> {});
        });
    }
}