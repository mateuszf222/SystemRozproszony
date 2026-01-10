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
            int code = -1;
            String description = "no cmd in task";
            long executionTime = System.currentTimeMillis();
            switch (cmd) {
                case "sleep": {
                    long ms = args.getLong("ms");
                    log.log(Level.INFO, "executor: sleep {0}", ms);
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException ignored) {
                    }
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
        long timeStart = System.currentTimeMillis();
        // processing
        Node node = Node.getCluster().get(response.getString("node"));
        new Thread(new Executor(node, response)).start();
        // end of processing
        System.out.println(inputLine);
        long processingTime = System.currentTimeMillis() - timeStart;
        JSONObject processed = new JSONObject();
        processed.put("session", uuid);
        processed.put("processing_time", processingTime);
        response.put("processed", processed);
        try {
            Database.communicationLog(timeStart, true, inputLine, response.toString());
        } catch(SQLException ex) {
            log.log(Level.SEVERE, "Error while logging communication: {0}", ex.getMessage());
        }
        return response;
    }
}
