package org.tudo.sse.resolution;

import java.net.URL;

/**
 * Handles when the resource being retrieved cannot be found.
 */
public class FileNotFoundException extends Exception {

    /**
     * The URL of the file that could not be found.
     */
    private final URL resource;

    /**
     * Creates a new FileNotFoundException for the given URL.
     *
     * @param resource The URL for which the corresponding file could not be found.
     */
    public FileNotFoundException(URL resource) {
        this.resource = resource;
    }

    @Override
    public String getMessage() {
        return "The resource " + resource.toString() + " was not found.";
    }

}
