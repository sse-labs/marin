package org.tudo.sse.multiThreading;

/**
 * This class contains a message sent from the Index to the Queue class to signal that it finished processing indexes.
 */
public class IndexProcessingMessage {
    private final String message;

    public IndexProcessingMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
