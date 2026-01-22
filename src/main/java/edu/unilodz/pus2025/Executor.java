package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.logging.Level;

public class Executor implements Runnable {
    private static final Log log = Log.get();

    private final Node node;
    private final JSONObject task;
    public Executor(Node node, JSONObject task) {
        this.node = node;
        this.task = task;
    }

    @Override
    public void run() {
        node.incTasks();
        try {
            String cmd = task.getString("cmd");
            JSONObject args = task.getJSONObject("args");
            String argsStr = args.toString();
            int code;
            String description = "no cmd in task";
            long executionTime = System.currentTimeMillis();
            switch (cmd) {
                case "sleep": {
                        long ms = args.getLong("ms");
                        log.log(Level.INFO, "executor: sleep {0}", ms);
                        try {
                            Thread.sleep(ms);
                        } catch (InterruptedException ignored) {}
                    }
                    code = 0;
                    break;
                default:
                    log.log(Level.SEVERE, "Cannot execute the task {0}", task);
                    code = -2;
                    description = "unknown cmd " + cmd;
            }
            executionTime = System.currentTimeMillis() - executionTime;
            try {
                Database.executionLog(cmd, argsStr, executionTime, code, code != 0 ? description : "ok");
            } catch (SQLException ignore) {}
        } catch(Exception e) {
            log.log(Level.SEVERE, "Error during execution {0}: {1}", new Object[]{task, e.getMessage()});
        }
        node.decTasks();
    }

    public static JSONObject process(String uuid, String inputLine) {
        JSONObject response = new JSONObject(inputLine);
        String nodeName = response.getString("node");
        Node node = Node.getCluster().get(nodeName);
        if (node == null) {
            log.log(Level.SEVERE, "Executor.process: Node {0} not found in cluster", nodeName);
            response.put("error", "Node not found: " + nodeName);
            return response;
        }
        new Thread(new Executor(node, response)).start();
        JSONObject processing = new JSONObject();
        processing.put("session", uuid);
        processing.put("node", node.toString());
        response.put("processing", processing);
        try {
            Database.communicationLog(System.currentTimeMillis(), true, inputLine, response.toString());
        } catch(SQLException ex) {
            log.log(Level.SEVERE, "Error while logging communication: {0}", ex.getMessage());
        }
        return response;
    }
}
