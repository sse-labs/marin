package org.tudo.sse.resolution;

import org.tudo.sse.model.ArtifactIdent;

/**
 * This class handles any issues that arise during pom resolution, giving more informative error messages.
 */
public class PomResolutionException extends Exception {

    /**
     * Detailed exception message.
     */
    private final String message;

    /**
     * The artifact identifier that caused the exception.
     */
    private final ArtifactIdent artifactIdentifier;

    /**
     * Creates a new PomResolutionException with the given attributes detailing the exception.
     * @param message A message describing the error
     * @param artifactIdentifier An artifact identifier referencing the affected artifact
     * @param cause The underlying cause, if available - else null.
     */
    public PomResolutionException(String message, ArtifactIdent artifactIdentifier, Throwable cause) {
        super(cause);
        this.message = message;
        this.artifactIdentifier = artifactIdentifier;
    }


    @Override
    public String getMessage() {
        StackTraceElement origin = null;

        for(var elem : getCause().getStackTrace()){
            if(elem != null){
                origin = elem;
                break;
            }
        }

        String location = origin == null ? "unknown" : origin.getClassName() + ":" + origin.getLineNumber();

        return "Failed to resolve " + artifactIdentifier.getCoordinates() + " : " + message + "(caused by " + getCause().getClass() + " at " + location + ")";
    }
}
