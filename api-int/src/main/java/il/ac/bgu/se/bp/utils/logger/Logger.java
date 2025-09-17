package il.ac.bgu.se.bp.utils.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
    private enum LOG_LEVELS { DEBUG, INFO, WARN, ERROR }

    private final String className;

    public Logger(Class<?> clazz) {
        this.className = "[" + clazz.getSimpleName() + "]";
    }

    public Logger(Class<?> clazz, String debuggerId) {
        this.className = "[" + clazz.getSimpleName() + "] [" + debuggerId + "]";
    }

    public void debug(String msg, Object... args) {
        loggerPrinter(msg, LOG_LEVELS.DEBUG, args);
    }

    public void info(String msg, Object... args) {
        loggerPrinter(msg, LOG_LEVELS.INFO, args);
    }

    public void warning(String msg, Object... args) {
        loggerPrinter(msg, LOG_LEVELS.WARN, args);
    }

    public void error(String msg, Object... args) {
        loggerPrinter(msg, LOG_LEVELS.ERROR, args);
    }

    public void error(String msg, Throwable e, Object... args) {
        loggerPrinter(msg, LOG_LEVELS.ERROR, args);
        e.printStackTrace();
    }

    private void loggerPrinter(String msg, LOG_LEVELS logLevel, Object... args) {
        String additionalInfo = replacePlaceHolders("{0}  {1} {2} --- {3} ", getDate(), logLevel, "Thread" + Thread.currentThread().getId(), className);
        String replacedMsg = replacePlaceHolders(msg, args);

        System.out.println(additionalInfo + replacedMsg);
    }

    private String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(new Date());
    }

    private String replacePlaceHolders(String msg, Object... args) {
        if (msg == null) {
            return "";
        }
        if (args == null) {
            return msg;
        }
        String newMsg = msg;
        for (int i = 0; i < args.length; i++) {
            newMsg = newMsg.replace("{" + i + "}", args[i] == null ? "null" : args[i].toString());
        }
        return newMsg;
    }

}
