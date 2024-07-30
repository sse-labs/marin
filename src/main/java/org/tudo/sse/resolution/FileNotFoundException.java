package org.tudo.sse.resolution;

import java.net.URL;

/**
 * Handles when the resource being retrieved cannot be found.
 */
public class FileNotFoundException  extends Exception {
    private final URL resource;


    public FileNotFoundException(URL resource) {
        this.resource = resource;
    }

    @Override
    public String getMessage() {
        return "The resource " + resource.toString() + " was not found.";
    }

}
