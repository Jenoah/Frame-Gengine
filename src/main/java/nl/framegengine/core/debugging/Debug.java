package nl.framegengine.core.debugging;


import java.util.ArrayList;
import java.util.List;

public class Debug {

    private static final List<LogEntry> logHistory = new ArrayList<>();

    public static void log(String message){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Find the caller's class and line number (index 2 is the caller of Log())
        StackTraceElement caller = stackTrace[2];

        // Extract the class name and line number
        String className = caller.getClassName();
        int lineNumber = caller.getLineNumber();

        String debugMessage = "\u001B[35m" + "[Log] " + "\u001B[90m" + className + ":" + lineNumber + " \u001B[0m" + message;

        logHistory.add(new LogEntry(ConsoleColors.parseAnsi(debugMessage)));
        System.out.println(debugMessage);
    }

    public static void logError(String message){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        StackTraceElement caller = stackTrace[2];

        String className = caller.getClassName();
        int lineNumber = caller.getLineNumber();

        String debugMessage = "\u001B[35m" + "[Log] " + "\u001B[90m" + className + ":" + lineNumber + " \u001B[31m" + message;

        logHistory.add(new LogEntry(ConsoleColors.parseAnsi(debugMessage)));
        System.out.println(debugMessage);
    }

    public static List<LogEntry> GetLog(){
        return logHistory;
    }

    public static class LogEntry {
        public final List<ConsoleColors> segments;

        public LogEntry(List<ConsoleColors> segments) {
            this.segments = segments;
        }
    }

}
