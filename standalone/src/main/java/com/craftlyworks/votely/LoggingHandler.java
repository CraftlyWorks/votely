package com.craftlyworks.votely;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.util.zip.GZIPOutputStream;

public class LoggingHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void init() throws IOException {
        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        File latest = new File(logsDir, "latest.log");
        if (latest.exists() && latest.length() > 0) {
            archive(latest, logsDir);
        }

        Formatter formatter = new VotelyFormatter();

        FileHandler fileHandler = new FileHandler(latest.getPath(), true);
        fileHandler.setFormatter(formatter);
        fileHandler.setLevel(Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.ALL);

        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        root.addHandler(fileHandler);
        root.addHandler(consoleHandler);
        root.setLevel(Level.ALL);
    }

    private static void archive(File latest, File logsDir) throws IOException {
        String today = LocalDate.now().format(DATE_FMT);
        int n = 1;
        File dest;
        do {
            dest = new File(logsDir, today + "-" + n++ + ".log.gz");
        } while (dest.exists());

        try (InputStream in = new FileInputStream(latest);
             GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(dest))) {
            in.transferTo(gz);
        }
        latest.delete();
    }

    private static class VotelyFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String time = Instant.ofEpochMilli(record.getMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FMT);
            String thread = Thread.currentThread().getName();
            String level = record.getLevel().getName();
            String msg = formatMessage(record);

            StringBuilder sb = new StringBuilder()
                .append('[').append(time).append(']')
                .append(" [").append(thread).append('/').append(level).append("]: ")
                .append(msg).append('\n');

            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(sw);
            }

            return sb.toString();
        }
    }
}