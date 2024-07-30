package org.tudo.sse.resolution;

import org.tudo.sse.model.ArtifactIdent;

/**
 * This class handles any issues that arise during pom resolution, giving more informative error messages.
 */
public class PomResolutionException extends Exception {
    private final String message;
    private final ArtifactIdent artifactIdentifier;


    public PomResolutionException(String message, ArtifactIdent artifactIdentifier) {
        this.message = message;
        this.artifactIdentifier = artifactIdentifier;
    }


    @Override
    public String getMessage() {
        return "Failed to resolve " + artifactIdentifier.getCoordinates() + " : " + message;
    }
}
