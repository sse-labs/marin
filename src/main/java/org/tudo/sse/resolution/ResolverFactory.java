package org.tudo.sse.resolution;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tudo.sse.model.ArtifactIdent;

/**
 * This class manages the pom and jar resolver, giving a way to run one or the other.
 */
public class ResolverFactory {
    private final PomResolver pomResolver;
    private final JarResolver jarResolver;

    public static final Logger log = LogManager.getLogger(ResolverFactory.class);

    public ResolverFactory(boolean pomIncludeTransitives) {
        pomResolver = new PomResolver(pomIncludeTransitives);
        jarResolver = new JarResolver();
    }

    public ResolverFactory(boolean output, Path pathToDirectory, boolean pomIncludeTransitives) {
        pomResolver = new PomResolver(output, pathToDirectory, pomIncludeTransitives);
        jarResolver = new JarResolver(output, pathToDirectory);
    }

    public void runPom(ArtifactIdent identifier) {
        try {
            pomResolver.resolveArtifact(identifier);
        } catch (FileNotFoundException | IOException | PomResolutionException e) {
            log.error(e);
        }
    }

    public void runJar(ArtifactIdent identifier) {
        try {
            jarResolver.parseJar(identifier);
        } catch (JarResolutionException e) {
            log.error(e);
        }
    }

    public void runBoth(ArtifactIdent identifier) {
        try {
            pomResolver.resolveArtifact(identifier);
        } catch (FileNotFoundException | IOException | PomResolutionException e) {
            log.error(e);
        }
        try {
            jarResolver.setOutput(false);
            jarResolver.parseJar(identifier);
        } catch (JarResolutionException e) {
            log.error(e);
        }
    }

}
