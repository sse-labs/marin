package org.tudo.sse.multithreading;

/**
 * This class contains a plain message sent from the Index to the Queue class to signal that it finished processing indexes.
 */
public class IndexProcessingMessage {

    private final String message;

    /**
     * Creates a new instance of this class with the given message
     *
     * @param message The message to pass to the processing queue
     */
    public IndexProcessingMessage(String message) {
        this.message = message;
    }

    /**
     * Retrieves the message string contained in this class
     * @return Message as plain string
     */
    public String getMessage() {
        return message;
    }
}
