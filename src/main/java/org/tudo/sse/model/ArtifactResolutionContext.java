package org.tudo.sse.model;

/**
 * The specific type of resolution context for resolving individual artifact, i.e. *not libraries*.
 *
 * @author Johannes Düsing
 */
public final class ArtifactResolutionContext extends ResolutionContext {

    private final ArtifactIdent identifier;

    private ArtifactResolutionContext(ArtifactIdent identifier) {
        this.identifier = identifier;
    }

    /**
     * Factory method to create a new ArtifactResolutionContext for the given identifier.
     * @param identifier Artifact identifier to create a context for
     * @return New ArtifactResolutionContext instance for the given artifact
     */
    public static ArtifactResolutionContext newInstance(ArtifactIdent identifier) {
        return new ArtifactResolutionContext(identifier);
    }

    /**
     * Returns the identifier of the artifact for which this context was created.
     * @return ArtifactIdent for this context
     */
    public ArtifactIdent getRootArtifactIdentifier() {
        return identifier;
    }

    /**
     * Returns the artifact for which this context was initially created
     * @return The root artifact of this context
     */
    public Artifact getRootArtifact() {
        return this.createArtifact(getRootArtifactIdentifier());
    }

}
