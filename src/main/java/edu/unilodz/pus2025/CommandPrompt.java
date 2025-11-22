package edu.unilodz.pus2025;

import org.jline.reader.*;
import org.jline.terminal.*;
import org.json.JSONObject;

import java.io.IOException;

import static edu.unilodz.pus2025.Pus2025.getConfig;

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
                    default:
                        terminal.writer().println("Unknown command: " + line);
                        terminal.writer().flush();
                }
            } while (running);
        } catch(IOException | UserInterruptException | EndOfFileException ignored) {}
    }
}
