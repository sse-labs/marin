package org.tudo.sse;

import org.tudo.sse.model.Artifact;

public class OwnImplementation extends MavenCentralAnalysis {

    public OwnImplementation() {
        super();
    }

    public OwnImplementation(boolean resolveIndex, boolean resolvePom, boolean processTransitives, boolean resolveJar) {
        super();
        this.resolveIndex = resolveIndex;
        this.resolvePom = resolvePom;
        this.processTransitives = processTransitives;
        this.resolveJar = resolveJar;
    }

    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
    }

    public void setIndex(boolean resolveIndex) {
        this.resolveIndex = resolveIndex;
    }
}
