package edu.unilodz.pus2025;

import io.javalin.http.UploadedFile;
import io.javalin.websocket.WsContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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
                client.send(Node.getClusterJson().toString());
            }
        }
    }

    private static void handleGetApi(Context ctx) {
        JSONObject res = Node.getClusterJson();
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
                
                // --- ADDED: Auto-selection for least loaded node ---
                if ("auto".equalsIgnoreCase(node)) {
                    node = Node.getLeastLoadedNode();
                    if (node == null) {
                         // Fallback to self if cluster is empty/broken
                         node = getConfig().getName();
                    }
                    req.put("node", node); // Update request with chosen node
                }
                // ---------------------------------------------------

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
                    Database.communicationLog(System.currentTimeMillis(), true, req.toString(), res.toString());
                    new Thread(new Executor(getCurrentNode(), req)).start();
                } else {
                    Node target = Node.getCluster().get(node);
                    if(target == null) throw new JSONException("No such target node");
                    try (Socket targetSocket = new Socket()) {
                        targetSocket.connect(target.address);
                        PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream());
                        String requestBody = req.toString();
                        Database.communicationLog(System.currentTimeMillis(), false, requestBody, target.toString());
                        targetOutput.println(requestBody);
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
                        Node newNode = new Node(key, address);
                        String httpAddress = nodeJson.optString("httpAddress", null);
                        if (httpAddress != null) {
                            newNode.setHttpAddress(httpAddress);
                        }
                        log.log(Level.INFO, "Joined cluster via PUT: " + key);
                    }
                }
            }

            // 3. Wymuszamy UDP
            Heartbeat.perform(true);

            String responseMsg = "Joined successfully via PUT";
            Database.communicationLog(System.currentTimeMillis(), true, ctx.body(), responseMsg);
            ctx.result(responseMsg);
        } catch (Exception e) {
            log.log(Level.SEVERE, "PUT Join error: " + e.getMessage());
            ctx.status(500).result("Error: " + e.getMessage());
        }
    }

    private static void handleGetExecutionLogs(Context ctx) {
        String targetNodeName = ctx.queryParam("node");
        String myName = getConfig().getName();

        if (targetNodeName == null || targetNodeName.isEmpty() || targetNodeName.equals(myName)) {
            try {
                JSONArray logs = Database.getLastExecutions();
                ctx.contentType("application/json");
                ctx.result(logs.toString());
            } catch (SQLException e) {
                log.log(Level.SEVERE, "DB Error: " + e.getMessage());
                ctx.status(500).result("Database error");
            }
        } else {
            Node target = Node.getCluster().get(targetNodeName);
            if (target == null) {
                ctx.status(404).result("Node not found");
                return;
            }
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(target.getHttpAddress() + "/api/logs/execution"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                ctx.contentType("application/json");
                ctx.result(response.body());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Proxy Error: " + e.getMessage());
                ctx.status(500).result("Proxy error: " + e.getMessage());
            }
        }
    }

    private static void handleGetCommunicationLogs(Context ctx) {
        String targetNodeName = ctx.queryParam("node");
        String myName = getConfig().getName();

        if (targetNodeName == null || targetNodeName.isEmpty() || targetNodeName.equals(myName)) {
            try {
                JSONArray logs = Database.getLastCommunications();
                ctx.contentType("application/json");
                ctx.result(logs.toString());
            } catch (SQLException e) {
                log.log(Level.SEVERE, "DB Error: " + e.getMessage());
                ctx.status(500).result("Database error");
            }
        } else {
            Node target = Node.getCluster().get(targetNodeName);
            if (target == null) {
                ctx.status(404).result("Node not found");
                return;
            }
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(target.getHttpAddress() + "/api/logs/communication"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                ctx.contentType("application/json");
                ctx.result(response.body());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Proxy Error: " + e.getMessage());
                ctx.status(500).result("Proxy error: " + e.getMessage());
            }
        }
    }

    private static final String FILES_DIR = "./files";

    private static void initFilesDir() {
        File folder = new File(FILES_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private static void handleGetFiles(Context ctx) {
        initFilesDir();
        JSONArray files = new JSONArray();
        try (Stream<Path> paths = Files.walk(Paths.get(FILES_DIR))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        JSONObject file = new JSONObject();
                        file.put("name", path.getFileName().toString());
                        try {
                            file.put("size", Files.size(path));
                        } catch (IOException e) {
                            file.put("size", -1);
                        }
                        files.put(file);
                    });
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error listing files: " + e.getMessage());
            ctx.status(500).result("Error listing files");
            return;
        }
        ctx.contentType("application/json");
        ctx.result(files.toString());
    }

    private static void handlePostFilesUpload(Context ctx) {
        initFilesDir();
        UploadedFile uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile != null) {
            try {
                Path targetPath = Paths.get(FILES_DIR, uploadedFile.filename());
                try (InputStream content = uploadedFile.content()) {
                    Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                ctx.result("File uploaded");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Upload error: " + e.getMessage());
                ctx.status(500).result("Upload failed");
            }
        } else {
            ctx.status(400).result("No file uploaded");
        }
    }

    private static void handleGetFileDownload(Context ctx) {
        String filename = ctx.pathParam("filename");
        Path filePath = Paths.get(FILES_DIR, filename);
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            try {
                ctx.result(Files.newInputStream(filePath));
                ctx.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            } catch (IOException e) {
                ctx.status(500);
            }
        } else {
            ctx.status(404).result("File not found");
        }
    }

    private static void handlePutFileInternal(Context ctx) {
        initFilesDir();
        String filename = ctx.pathParam("filename");
        try {
            Path targetPath = Paths.get(FILES_DIR, filename);
            Files.copy(ctx.bodyInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            ctx.result("File received via internal transfer");
            log.log(Level.INFO, "Received file transfer: " + filename);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Internal upload error: " + e.getMessage());
            ctx.status(500).result("Internal upload failed");
        }
    }

    private static void handlePostFilesTransfer(Context ctx) {
        try {
            JSONObject body = new JSONObject(ctx.body());
            String filename = body.getString("filename");
            String targetNodeName = body.getString("targetNode");

            Path filePath = Paths.get(FILES_DIR, filename);
            if (!Files.exists(filePath)) {
                ctx.status(404).result("File not found locally");
                return;
            }

            Node target = Node.getCluster().get(targetNodeName);
            if (target == null) {
                ctx.status(404).result("Target node not found");
                return;
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target.getHttpAddress() + "/api/files/" + filename))
                    .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ctx.result("Transfer successful");
                Database.communicationLog(System.currentTimeMillis(), false, "Transfer file " + filename, targetNodeName);
            } else {
                ctx.status(500).result("Transfer failed: " + response.body());
                log.log(Level.SEVERE, "Transfer failed: " + response.body());
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Transfer error: " + e.getMessage());
            ctx.status(500).result("Transfer error: " + e.getMessage());
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

        // Nowe endpointy logów
        app.get("/api/logs/execution", HttpServer::handleGetExecutionLogs);
        app.get("/api/logs/communication", HttpServer::handleGetCommunicationLogs);

        // Files
        app.get("/api/files", HttpServer::handleGetFiles);
        app.post("/api/files", HttpServer::handlePostFilesUpload);
        app.get("/api/files/{filename}", HttpServer::handleGetFileDownload);
        app.put("/api/files/{filename}", HttpServer::handlePutFileInternal);
        app.post("/api/files/transfer", HttpServer::handlePostFilesTransfer);

        app.start(port);

        app.ws("/ws", ws -> {
            ws.onConnect(clients::add);
            ws.onMessage(ctx -> {});
            ws.onClose(clients::remove);
            ws.onError(ctx -> {});
        });
    }
}