package org.tudo.sse.semver;

public class SemanticVersionParsingException extends Exception {

    private final String parsedValue;
    private final String msg;
    private final int parsingPosition;

    public SemanticVersionParsingException(String value, String description) {
        this(value, description, -1);
    }

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

}
