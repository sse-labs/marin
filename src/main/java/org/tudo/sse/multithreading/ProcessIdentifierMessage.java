package org.tudo.sse.multithreading;

import org.tudo.sse.analyses.MavenCentralArtifactAnalysis;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ArtifactResolutionContext;

/**
 * A message passed to the processing queue to indicate that a given analysis instance must process the given
 * artifact identifier.
 */
public class ProcessIdentifierMessage implements WorkItem {

    private final ArtifactResolutionContext artifactCtx;
    private final MavenCentralArtifactAnalysis instance;

    /**
     * Creates a new message with the given artifact identifier and analysis instance.
     * @param resolutionContext The artifact resolution context to process
     * @param instance The analysis instance that must process the identifier
     */
    public ProcessIdentifierMessage(ArtifactResolutionContext resolutionContext, MavenCentralArtifactAnalysis instance) {
        this.artifactCtx = resolutionContext;
        this.instance = instance;
    }

    /**
     * Retrieves the identifier that must be processed
     * @return The artifact identifier
     */
    public ArtifactIdent getIdentifier() {
        return this.artifactCtx.getRootArtifactIdentifier();
    }

    /**
     * Retrieves the resolution context of this message
     * @return ResolutionContext contained
     */
    public ArtifactResolutionContext getArtifactResolutionContext() {
        return this.artifactCtx;
    }

    /**
     * Retrieves the analysis instance that must process the identifier
     * @return The analysis instance
     */
    public MavenCentralArtifactAnalysis getInstance() {
        return instance;
    }
}
