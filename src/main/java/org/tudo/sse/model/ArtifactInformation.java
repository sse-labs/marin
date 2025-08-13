package org.tudo.sse.model;

/**
 * This is an abstract class that all information type classes extend ensuring that they are attached to an artifact identifier.
 */
public abstract class ArtifactInformation {

    /**
     * The artifact identifier for this information object.
     */
    protected ArtifactIdent ident;

    /**
     * Creates an empty artifact information without any identifier. Useful for local POM files.
     */
    protected ArtifactInformation() {
    }

    /**
     * Creates a new artifact information object with the given artifact identifier.
     * @param ident Artifact identifier for this information
     */
    public ArtifactInformation(ArtifactIdent ident) {
        this.ident = ident;
    }

    /**
     * Gets the identifier for the current artifact.
     * @return the artifactIdentifier
     */
    public ArtifactIdent getIdent() {
        return ident;
    }

    /**
     * Sets a new artifactIdentifier to the current artifact.
     * @param ident new artifactIdent value
     */
    public void setIdent(ArtifactIdent ident) {
        this.ident = ident;
    }

}
