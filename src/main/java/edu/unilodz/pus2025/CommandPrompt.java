package edu.unilodz.pus2025;

import org.jline.reader.*;
import org.jline.terminal.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.io.IOException;
import java.sql.SQLException;

import static edu.unilodz.pus2025.Main.getConfig;
import static edu.unilodz.pus2025.Main.getCurrentNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
public class CommandPrompt {

    public static void start() {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            LineReader reader = LineReaderBuilder.
                    builder().terminal(terminal).build();

            String prompt = "[" + getConfig().getName() + "]%% ";
            boolean running = true;
            do {
                String line = reader.readLine(prompt);

                String[] params = line.split("\\s+");
                String cmd = params[0].toLowerCase();

                switch (cmd) {
                    case "":
                        break;
                    case "exit":
                        running = false;
                        break;
                    case "cluster":
                        terminal.writer().println(new JSONObject(Node.getCluster()).toString(2));
                        terminal.writer().flush();
                        break;
                    case "log": {
                            int limit = 1;
                            if(params.length > 1) {
                                try {
                                    limit = Integer.parseInt(params[1]);
                                } catch(NumberFormatException ignored) {}
                            }
                            JSONArray lastExecutions = Database.getLastExecutions(limit);
                            terminal.writer().println(lastExecutions.toString(2));
                            terminal.writer().flush();
                        } break;
                    case "exec": {
                            if(params.length > 2) {
                                String xCmd = params[1];
                                JSONObject payload = new JSONObject();
                                String argsStr = line.replaceFirst("^\\S+\\s+\\S+\\s+", "");
                                try {
                                    JSONObject args = new JSONObject(argsStr);
                                    payload.put("args", args);
                                } catch(JSONException e) {
                                    terminal.writer().println("Arguments have to be a proper JSON");
                                    terminal.writer().flush();
                                    break;
                                }
                                payload.put("node", getCurrentNode());
                                payload.put("cmd", xCmd);
                                new Thread(new Executor(getCurrentNode(), payload)).start();
                            } else {
                                terminal.writer().println("Use exec cmd arg_json");
                                terminal.writer().flush();
                            }
                        }
                        break;
                    case "join":
                        if (params.length < 2) {
                            terminal.writer().println("Use join <apiUrl>");
                            terminal.writer().flush();
                            break;
                        }

                        try {
                            HttpClient client = HttpClient.newHttpClient();
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(params[1])).GET().build();
                            HttpResponse<String> response = client.send(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            );
                            if (response.statusCode() != 200) throw new Exception("No valid response");
                            JSONObject root = new JSONObject(response.body());
                            boolean nodeAdded = false;
                            for (Iterator<String> it = root.keys(); it.hasNext(); ) {
                                String key = it.next();
                                JSONObject node = root.getJSONObject(key);

                                if (node.optBoolean("me", false)) {
                                    String address = node.optString("address", null);
                                    if(address == null) throw new Exception("no node address defined");
                                    new Node(key, address);
                                    Heartbeat.perform(true);
                                    nodeAdded = true;
                                    break;
                                }
                            }
                            if(!nodeAdded) throw new Exception("no node added");
                        } catch (Exception e) {
                            terminal.writer().println(e.getMessage());
                            terminal.writer().flush();
                        }
                        break;
                    default:
                        terminal.writer().println("Unknown command: " + line);
                        terminal.writer().flush();
                }
            } while (running);
        } catch(IOException | UserInterruptException | EndOfFileException | SQLException ignored) {}
    }
}
