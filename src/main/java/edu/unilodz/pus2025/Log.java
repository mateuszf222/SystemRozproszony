package edu.unilodz.pus2025;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log extends Logger {

    private Log(String name) {
        super(name, null);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        this.addHandler(handler);
        this.setLevel(Level.ALL);
    }

    public static Log get() {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        return new Log(className);
    }
}
