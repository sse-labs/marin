package org.tudo.sse.resolution;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ResolutionContext;

/**
 * This class manages the pom and jar resolver, giving a way to run one or the other.
 */
public class ResolverFactory {

    private final PomResolver pomResolver;
    private final JarResolver jarResolver;

    private static final Logger log = LoggerFactory.getLogger(ResolverFactory.class);

    /**
     * Creates a new resolver factory instance. Resolvers will not output the artifacts that they process.
     *
     * @param pomIncludeTransitives Whether this factory shall produce a PomResolver that resolves transitive files.
     */
    public ResolverFactory(boolean pomIncludeTransitives) {
        pomResolver = new PomResolver(pomIncludeTransitives);
        jarResolver = new JarResolver();
    }

    /**
     * Creates a new resolver factory instance.
     *
     * @param output Whether the resolvers produced by this factory shall output the artifacts that they process
     * @param pathToDirectory Location to which to output the artifacts processed by the resolvers
     * @param pomIncludeTransitives Whether this factory shall produce a PomResolver that resolves transitive files
     */
    public ResolverFactory(boolean output, Path pathToDirectory, boolean pomIncludeTransitives) {
        pomResolver = new PomResolver(output, pathToDirectory, pomIncludeTransitives);
        jarResolver = new JarResolver(output, pathToDirectory);
    }

    /**
     * Resolve the POM file of the given artifact.
     *
     * @param identifier Artifact identifier to resolve
     * @param ctx The resolution context for this run
     */
    public void runPom(ArtifactIdent identifier, ResolutionContext ctx) {
        try {
            pomResolver.resolveArtifact(identifier, ctx);
        } catch (IOException | PomResolutionException e) {
            log.error("Error resolving pom file for {}", identifier, e);
        } catch (FileNotFoundException ignored) {}
    }

    /**
     * Resolve the JAR file of the given artifact.
     *
     * @param identifier Artifact identifier to resolve
     * @param ctx The resolution context for this run
     */
    public void runJar(ArtifactIdent identifier, ResolutionContext ctx) {
        try {
            jarResolver.parseJar(identifier, ctx);
        } catch (JarResolutionException e) {
            log.error("Error resolving JAR file for {}", identifier, e);
        }
    }

    /**
     * Resolve both the POM and JAR file for the given artifact.
     *
     * @param identifier Artifact identifier to resolve
     * @param ctx The resolution context for this run
     */
    public void runBoth(ArtifactIdent identifier, ResolutionContext ctx) {
        try {
            pomResolver.resolveArtifact(identifier, ctx);
        } catch (IOException | PomResolutionException e) {
            log.error("Error resolving pom file for {}", identifier, e);
        } catch(FileNotFoundException ignored){}

        try {
            jarResolver.setOutput(false);
            jarResolver.parseJar(identifier, ctx);
        } catch (JarResolutionException e) {
            log.error("Error resolving JAR file for {}", identifier, e);
        }
    }

}
