package net.unladenswallow.minecraft.autofish;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Custom Logger for ModEmeraldMaterial.  Copied and modified from FMLRelaunchLog.
 * @author FreneticFeline
 *
 */
public class AutoFishLogger {
    /**
     * Our special logger for logging issues to. We copy various assets from the
     * Minecraft logger to achieve a similar appearance.
     */
    public static AutoFishLogger log = new AutoFishLogger();

    static File minecraftHome;
    private static boolean configured;

    private Logger myLog;

    private AutoFishLogger()
    {
    }

    /**
     * Configure the FML logger and inject tracing printstreams.
     */
    private static void configureLogging()
    {
        log.myLog = LogManager.getLogger("AutoFish");
        configured = true;
    }

    public static void log(String targetLog, Level level, String format, Object... data)
    {
        LogManager.getLogger(targetLog).log(level, String.format(format, data));
    }

    public static void log(Level level, String format, Object... data)
    {
        if (!configured)
        {
            configureLogging();
        }
        log.myLog.log(level, String.format(format, data));
    }

    public static void log(String targetLog, Level level, Throwable ex, String format, Object... data)
    {
        LogManager.getLogger(targetLog).log(level, String.format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data)
    {
        if (!configured)
        {
            configureLogging();
        }
        log.myLog.log(level, String.format(format, data), ex);
    }

    public static void severe(String format, Object... data)
    {
        log(Level.ERROR, format, data);
    }

    public static void warning(String format, Object... data)
    {
        log(Level.WARN, format, data);
    }

    public static void info(String format, Object... data)
    {
        log(Level.INFO, format, data);
    }

    public static void debug(String format, Object... data)
    {
        log(Level.DEBUG, format, data);
    }

    public static void trace(String format, Object... data)
    {
        log(Level.TRACE, format, data);
    }

    public Logger getLogger()
    {
        return myLog;
    }

}
