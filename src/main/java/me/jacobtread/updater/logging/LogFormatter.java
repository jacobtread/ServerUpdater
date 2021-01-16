package me.jacobtread.updater.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord logRecord) {
        Date date = Date.from(Instant.ofEpochMilli(logRecord.getMillis()));
        DateFormat simple = new SimpleDateFormat("HH:mm:ss");
        String dateText = simple.format(date);
        String errorText = "";
        if (logRecord.getThrown() != null) {
            Throwable exception = logRecord.getThrown();
            StringWriter stacktrace = new StringWriter();
            PrintWriter pw = new PrintWriter(stacktrace);
            exception.printStackTrace(pw);
            errorText = "Thrown: " + exception.getMessage() + "\n" + stacktrace.toString();
        }
        return String.format("[%s] [%s] %s %s\n", dateText, logRecord.getLevel().getName(), logRecord.getMessage(), errorText);
    }

}
