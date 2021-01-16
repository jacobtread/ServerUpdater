package me.jacobtread.updater.logging;

import java.io.PrintStream;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

    /* Console colors */
    public static final String RED = "\u001b[31m";
    private static final String YELLOW = "\u001b[33m";
    private static final String GREEN = "\u001b[32m";
    public static final String RESET = "\u001b[0m";

    private final Formatter formatter = new LogFormatter();

    @Override
    public void publish(LogRecord logRecord) {
        String text = formatter.format(logRecord);
        PrintStream out;
        Level level = logRecord.getLevel();
        if (Level.SEVERE.equals(level)) {
            out = System.err;
            text = RED + text + RESET;
            out.print (text);
            System.exit(1);
        } else {
            out = System.out;
        }
        if (Level.WARNING.equals(level)) {
            text = YELLOW + text + RESET;
        }
        if (Level.INFO.equals(level)) {
            text = GREEN + text + RESET;
        }
        out.print(text);
    }

    @Override
    public void flush() {
        System.out.flush();
        System.err.flush();
    }

    @Override
    public void close() throws SecurityException {

    }

}
