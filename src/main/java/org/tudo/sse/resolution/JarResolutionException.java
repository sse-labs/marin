package org.tudo.sse.resolution;

/**
 * This class handles errors that may arise during jar resolution, giving more informative error messages.
 */
public class JarResolutionException extends Exception {
    private final String message;

    /**
     * Creates a new JarResolutionException with the given error message.
     * @param message A message describing the error during JAR resolution
     */
    public JarResolutionException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
