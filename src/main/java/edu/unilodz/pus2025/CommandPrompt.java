package edu.unilodz.pus2025;

import org.json.JSONObject;

import java.util.Scanner;

public class CommandPrompt {

    public static void run() {
        Scanner scanner = new Scanner(System.in);
        String line;

        boolean running = true;
        do {
            System.out.printf("[%s] %% ", Pus2025.getConfig().getName());
            try {
                line = scanner.nextLine().trim();
            } catch (Exception e) {
                break;
            }
            String[] params = line.split("\\s+");
            String cmd = params[0].toLowerCase();

            switch (cmd) {
                case "":
                    break;
                case "exit":
                    running = false;
                    break;
                case "cluster":
                    System.out.println(new JSONObject(Node.getCluster()).toString(2));
                    break;
                default:
                    System.err.println("Unknown command: " + line);
            }
        } while(running);

        scanner.close();
        System.out.println("Bye!");
    }
}
