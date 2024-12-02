package org.tudo.sse.testutils;

import org.tudo.sse.MavenCentralAnalysis;
import org.tudo.sse.model.Artifact;

import java.util.concurrent.atomic.AtomicInteger;

public class DummyEvaluationAnalysis extends MavenCentralAnalysis {

    private final AtomicInteger artifactCnt = new AtomicInteger(0);

    public DummyEvaluationAnalysis() {
        super();
    }

    public DummyEvaluationAnalysis(boolean resolveIndex, boolean resolvePom, boolean processTransitives, boolean resolveJar) {
        super();
        this.resolveIndex = resolveIndex;
        this.resolvePom = resolvePom;
        this.processTransitives = processTransitives;
        this.resolveJar = resolveJar;
    }

    @Override
    public void analyzeArtifact(Artifact toAnalyze) {
        if(artifactCnt.incrementAndGet() % 100 == 0){
            System.out.println("Client analysis implementation got " + artifactCnt.get() + " artifacts so far");
        }
    }

    public void setIndex(boolean resolveIndex) {
        this.resolveIndex = resolveIndex;
    }
}
