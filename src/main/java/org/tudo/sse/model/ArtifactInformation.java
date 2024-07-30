package org.tudo.sse.model;

/**
 * This is an abstract class that all information type classes extend ensuring that they are attached to an artifact identifier.
 */
public abstract class ArtifactInformation {
    protected ArtifactIdent ident;

    protected ArtifactInformation() {
    }

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
