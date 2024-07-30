package org.tudo.sse.resolution;

/**
 * This class handles errors that may arise during jar resolution, giving more informative error messages.
 */
public class JarResolutionException extends Exception {
    private final String message;

    public JarResolutionException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
