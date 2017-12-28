package Hydra.Logging;

import org.apache.log4j.PropertyConfigurator;

import java.util.Date;
import java.util.Properties;

public class Logger {

    private static org.apache.log4j.Logger logger;

    public static org.apache.log4j.Logger getLogger(){
        return logger;
    }

    public static void info(Object ob){
        logger.info(ob);
    }

    public static void info(Object ob, Object ob2){
        logger.info(ob + " | " + ob2);
    }

    public static void setPreMessage(Object ob){
        logger.info(ob);
    }

    public static void error(Object message, Exception ex)
    {
        logger.error(message, ex);
    }

    public static void initialize() {
        Properties properties = new Properties();
        properties.setProperty("log4j.rootLogger", "INFO,stdout,MyFile");
        properties.setProperty("log4j.rootCategory", "INFO");

        properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{yyyy/MM/dd HH:mm:ss.SSS} [%5p] %t (%F) - %m%n");

        properties.setProperty("log4j.appender.MyFile", "org.apache.log4j.RollingFileAppender");
        properties.setProperty("log4j.appender.MyFile.File", new Date().getTime() + "_RUDP_Client.log");
        properties.setProperty("log4j.appender.MyFile.MaxFileSize", "100000KB");
        properties.setProperty("log4j.appender.MyFile.MaxBackupIndex", "1");
        properties.setProperty("log4j.appender.MyFile.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.MyFile.layout.ConversionPattern", "%d{yyyy/MM/dd HH:mm:ss.SSS} [%5p] %t (%F) - %m%n");
        properties.setProperty("log4j.appender.BAR.Threshold", "ERROR");
        properties.setProperty("log4j.appender.BAR.Threshold", "INFO");

        PropertyConfigurator.configure(properties);

        logger = org.apache.log4j.Logger.getLogger("R-UDP Client Log");

        logger.fatal("This is a FATAL message.");
        logger.error("This is an ERROR message.");
        logger.warn("This is a WARN message.");
        info("This is an INFO message.");
        logger.debug("This is a DEBUG message.");
        logger.trace("This is a TRACE message.");
    }
}


