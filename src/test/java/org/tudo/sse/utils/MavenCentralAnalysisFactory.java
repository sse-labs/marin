package org.tudo.sse.utils;

import org.tudo.sse.analyses.MavenCentralArtifactAnalysis;
import org.tudo.sse.analyses.MavenCentralLibraryAnalysis;
import org.tudo.sse.model.Artifact;

import java.util.List;

public class MavenCentralAnalysisFactory {

    public static MavenCentralArtifactAnalysis buildEmptyAnalysisWithNoRequirements() {
        return buildAnalysisWithRequirements(false, false, false, false);
    }

    public static MavenCentralArtifactAnalysis buildEmptyAnalysisWithPomRequirement() {
        return buildAnalysisWithRequirements(false, true, false, false);
    }

    public static MavenCentralArtifactAnalysis buildEmptyAnalysisWithIndexRequirement() {
        return buildAnalysisWithRequirements(true, false, false, false);
    }

    private static MavenCentralArtifactAnalysis buildAnalysisWithRequirements(boolean requiresIndex, boolean requiresPom,
                                                                              boolean requiresTransitives, boolean requiresJar) {
        return new MavenCentralArtifactAnalysis(requiresIndex, requiresPom, requiresTransitives, requiresJar) {
            @Override
            public void analyzeArtifact(Artifact current) {

            }
        };
    }

    public static MavenCentralLibraryAnalysis buildEmptyLibraryAnalysisWithNoRequirements(){
        return buildLibraryAnalysisWithRequirements(false, false, false);
    }

    private static MavenCentralLibraryAnalysis buildLibraryAnalysisWithRequirements(boolean requiresPom,
                                                                                    boolean requiresTransitives,
                                                                                    boolean requiresJar) {
        return new MavenCentralLibraryAnalysis(requiresPom, requiresTransitives, requiresJar) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {

            }
        };
    }
}
