package org.tudo.sse;

/**
 * Exception to be thrown when an issue arises with parsing cli passed to the program.
 */
public class CLIException extends Exception {
    private String message;
    private final String cliName;


    public CLIException(String message, String cliName) {
        this.message = message;
        this.cliName = cliName;
    }

    public CLIException(String cliName) {
        this.cliName = cliName;
    }

    /**
     * Gets the message of the exception thrown.
     * @return A string containing the message.
     */
    @Override
    public String getMessage() {
        if(cliName != null && message != null) {
            return "Trouble processing the " + cliName + " flag : " + message;
        } else if(cliName != null) {
            return "Unknown cli : " + cliName;
        } else {
            return "Unknown cli";
        }

    }

}
