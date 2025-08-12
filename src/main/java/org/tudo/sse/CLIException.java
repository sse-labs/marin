package org.tudo.sse;

/**
 * Exception to be thrown when an issue arises with parsing cli passed to the program.
 */
public class CLIException extends Exception {

    /**
     * Detailed exception message.
     */
    private String message;

    /**
     * Name of the CLI parameter that caused the error.
     */
    private final String cliName;

    /**
     * Creates a new CLI Exception with a custom message and the name of the CLI parameter that caused the issue.
     * @param message A message describing the error
     * @param cliName The CLI parameter name causing the error
     */
    public CLIException(String message, String cliName) {
        this.message = message;
        this.cliName = cliName;
    }

    /**
     * Creates a new CLI Exception with the name of the CLI parameter that caused the issue.
     * @param cliName The CLI parameter name causing the error
     */
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
