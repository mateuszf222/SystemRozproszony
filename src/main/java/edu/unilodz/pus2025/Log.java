package edu.unilodz.pus2025;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Log extends Logger {

    private Log(String name) {
        super(name, null);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                String time = sdf.format(new Date(record.getMillis()));
                String message = formatMessage(record);

                return String.format("[%s] %s: %s%n",
                        time,
                        record.getLevel().getName(),
                        message);
            }
        });
        this.addHandler(handler);
        this.setLevel(Level.ALL);
    }

    public static Log get() {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        return new Log(className);
    }
}
