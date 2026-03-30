package org.tudo.sse.multithreading;

import java.util.function.Supplier;

/**
 * Message used in multithreaded mode to queue processing of a callback method (for analyzing libraries).
 */
public class ProcessLibraryMessage implements WorkItem {

    private final Supplier<Void> processEntryCallback;

    /**
     * Creates a new message with the given callback method
     * @param callback The callback that analyzes a library
     */
    public ProcessLibraryMessage(Supplier<Void> callback) {
        this.processEntryCallback = callback;
    }

    /**
     * Returns the callback method stored in this message.
     * @return The callback
     */
    public Supplier<Void> getProcessEntryCallback() { return processEntryCallback; }

}
