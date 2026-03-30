package org.tudo.sse.utils;

import org.tudo.sse.analyses.MavenCentralArtifactIterator;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder;
import org.tudo.sse.analyses.config.InvalidConfigurationException;
import org.tudo.sse.model.Artifact;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtilities {

    public static Path testResource(String pathToResource){
        try {
            return Path.of(Objects.requireNonNull(TestUtilities.class.getClassLoader().getResource(pathToResource)).toURI());
        } catch (Exception x){
            fail("Test setup: Failed to load resource file " + pathToResource, x);
        }
        return null;
    }

    public static List<Artifact> getFromIndex(int skip, int take, boolean resolvePom, boolean resolveTransitives, boolean resolveJar){
        try {
            final ArtifactAnalysisConfig config = new ArtifactAnalysisConfigBuilder().withSkip(skip).withTake(take).build();

            final MavenCentralArtifactIterator it = new MavenCentralArtifactIterator(resolvePom, resolveTransitives, resolveJar, config);

            List<Artifact> artifacts = new ArrayList<>();

            while(it.hasNext()){
                artifacts.add(it.next());
            }

            return artifacts;
        } catch(InvalidConfigurationException icx){
            fail(icx);
        }

        return null;
    }

}
