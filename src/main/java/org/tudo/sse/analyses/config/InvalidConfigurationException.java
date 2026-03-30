package org.tudo.sse.analyses.config;

/**
 * Exception thrown when a MARIN configuration value was invalid.
 *
 * @author Johannes Düsing
 */
public class InvalidConfigurationException extends Exception {

    /**
     * Attribute the error occurred for.
     */
    private final String attrName;

    /**
     * Message describing the error.
     */
    private final String msg;

    /**
     * Creates a new instance with the given configuration attribute name and message.
     * @param attrName The configuration value that was invalid
     * @param msg A message describing the error
     */
    InvalidConfigurationException(String attrName, String msg){
        this.attrName = attrName;
        this.msg = msg;
    }

    /**
     * Returns the name of the attribute for which this exception occurred.
     * @return The attribute name
     */
    public String getAttributeName() {
        return this.attrName;
    }

    /**
     * Returns a description on the error that occurred.
     * @return The error description
     */
    public String getDescription(){
        return this.msg;
    }

    @Override
    public String getMessage() {
        return String.format("Invalid configuration value for '%s': %s", attrName, msg);
    }
}
