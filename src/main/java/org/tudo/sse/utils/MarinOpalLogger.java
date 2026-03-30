package org.tudo.sse.utils;

import org.opalj.log.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acts as a logging bridge between SLF4J and OPAL. A MarinOpalLogger instance can be passed when creating an OPAL
 * project, and can be configured to selectively enable certain log levels. You can pass an existing SLF4J logger
 * to use as the underlying backend, otherwise the bridge will create its own SLF4J logger backend.
 */
public class MarinOpalLogger implements OPALLogger {

    private final Logger internalLog;
    private boolean infoEnabled = true, warnEnabled = true, errorEnabled = true;

    private static final MarinOpalLogger _globalInstance = newErrorOnlyLogger();

    /**
     * Create a new instance that creates a new underlying Log4j backend.
     */
    public MarinOpalLogger(){
        this.internalLog = LoggerFactory.getLogger(MarinOpalLogger.class);
    }

    /**
     * Creates a new instance that uses the given Log4j backend.
     * @param logger The Log4j logger to use as a backend
     */
    public MarinOpalLogger(Logger logger) {
        this.internalLog = logger;
    }

    /**
     * Sets whether the info level of OPAL shall be forwarded to the backend.
     * @param enabled True if info level shall be forwarded, false otherwise
     */
    public void setInfoEnabled(boolean enabled) { this.infoEnabled = enabled; }
    /**
     * Sets whether the warning level of OPAL shall be forwarded to the backend.
     * @param enabled True if warning level shall be forwarded, false otherwise
     */
    public void setWarnEnabled(boolean enabled) { this.warnEnabled = enabled; }
    /**
     * Sets whether the error level of OPAL shall be forwarded to the backend.
     * @param enabled True if error level shall be forwarded, false otherwise
     */
    public void setErrorEnabled(boolean enabled) { this.errorEnabled = enabled; }

    /**
     * Creates a new logger bridge instance that only forwards error messages. Uses a new Log4j backend.
     * @return New logger bridge instance
     */
    public static MarinOpalLogger newErrorOnlyLogger(){
        MarinOpalLogger logger = new MarinOpalLogger();
        logger.setInfoEnabled(false);
        logger.setWarnEnabled(false);
        logger.setErrorEnabled(true);
        return logger;
    }

    /**
     * Creates a new logger bridge instance that forwards info, warning and error messages. Uses the given Log4j backend.
     * @param log The Log4j backend to use
     * @return New logger bridge instance
     */
    public static MarinOpalLogger newInfoLogger(Logger log){
        MarinOpalLogger logger = new MarinOpalLogger(log);
        logger.setInfoEnabled(true);
        logger.setWarnEnabled(true);
        logger.setErrorEnabled(true);
        return logger;
    }

    /**
     * Gets the singleton logger bridge that is used as the OPAL global logger for MARIN.
     * @return The global logging bridge
     */
    public static MarinOpalLogger getGlobalLogger(){
        return _globalInstance;
    }


    @Override
    public void log(LogMessage message, LogContext ctx) {
        if(message.level() == Error$.MODULE$ && errorEnabled){
            internalLog.error(message.message());
        } else if(message.level() == Warn$.MODULE$ && warnEnabled){
            internalLog.warn(message.message());
        } else if(message.level() == Info$.MODULE$ && infoEnabled){
            internalLog.info(message.message());
        } else if(message.level() == Fatal$.MODULE$){
            internalLog.error("[FATAL] {}", message.message());
        }
    }

    @Override
    public void logOnce(LogMessage message, LogContext ctx) {
        OPALLogger.super.logOnce(message, ctx);
    }
}
