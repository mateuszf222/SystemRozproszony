package edu.unilodz.pus2025;

import java.util.Scanner;

public class CommandPrompt {

    public static void run() {
        Scanner scanner = new Scanner(System.in);
        String line;

        System.out.println("Welcome to PUS2025. Enter 'exit' to quit.");

        boolean running = true;
        do {
            System.out.print("% ");
            line = scanner.nextLine().trim();
            String[] params = line.split("\\s+");
            String cmd = params[0].toLowerCase();

            switch (cmd) {
                case "":
                    break;
                case "exit":
                    running = false;
                    break;
                case "hello":
                    System.out.println("Hello!");
                    break;
                default:
                    System.out.println("Unknown command: " + line);
            }
        } while(running);

        scanner.close();
        System.out.println("Bye!");
    }
}
