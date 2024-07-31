package org.tudo.sse.multithreading;

import org.tudo.sse.MavenCentralAnalysis;
import org.tudo.sse.model.ArtifactIdent;

public class IdentPlusMCA {
    private final ArtifactIdent identifier;
    private final MavenCentralAnalysis instance;

    public IdentPlusMCA(ArtifactIdent identifier, MavenCentralAnalysis instance) {
        this.identifier = identifier;
        this.instance = instance;
    }

    public ArtifactIdent getIdentifier() {
        return identifier;
    }

    public MavenCentralAnalysis getInstance() {
        return instance;
    }
}
