package org.tudo.sse.semver;

/**
 * Class representing an exception while parsing a semantic version number.
 *
 * @author Johannes Düsing
 */
public class SemanticVersionParsingException extends Exception {

    /**
     * Value that was being parsed
     */
    private final String parsedValue;

    /**
     * Message describing the error
     */
    private final String msg;

    /**
     * Position at which the error occurred
     */
    private final int parsingPosition;

    /**
     * Create a new parsing exception with the given description while parsing the given value.
     * @param value The value that was being parsed
     * @param description A description of the error that was encountered
     */
    public SemanticVersionParsingException(String value, String description) {
        this(value, description, -1);
    }

    /**
     * Create a new parsing exception with the given description, parsing position and parsed value.
     * @param value The value that was being parsed
     * @param description A description of the error that was encountered
     * @param parsingPosition The parsing position at which the error occurred
     */
    public SemanticVersionParsingException(String value, String description, int parsingPosition) {
        this.parsedValue = value;
        this.msg = description;
        this.parsingPosition = parsingPosition;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Error parsing value '");
        sb.append(parsedValue);
        sb.append("'");

        if(parsingPosition != -1){
            sb.append(" at position ").append(parsingPosition);
        }

        sb.append(" : ");
        sb.append(msg);
        return sb.toString();
    }

    /**
     * Returns the position at which the error occurred in the original value.
     * @return Parsing position
     */
    public int getParsingPosition() {
        return this.parsingPosition;
    }

    /**
     * Returns the value that was being parsed when the error occurred.
     * @return The parsed value
     */
    public String getParsedValue(){
        return this.parsedValue;
    }

}
