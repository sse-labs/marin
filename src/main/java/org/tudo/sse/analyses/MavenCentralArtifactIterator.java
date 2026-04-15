package org.tudo.sse.analyses;

import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.input.ArtifactContextIterable;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactResolutionContext;

import java.util.Iterator;

/**
 * Iterator that produces artifacts that are enriched with index-, pom- or jar information, as configured. This is the
 * iterator equivalent to the {@link MavenCentralArtifactAnalysis}. Only supports single-threaded resolution.
 *
 * @author Johannes Düsing
 */
public final class MavenCentralArtifactIterator extends AbstractEntityIterator<ArtifactResolutionContext, Artifact> {

    /**
     * Creates a new iterator instance with the given configuration values.
     *
     * @param resolvePom Whether information on artifact pom files shall be resolved
     * @param resolveTransitivePoms Whether transitive poms should also be resolved
     * @param resolveJar Whether information on jar files shall be resolved
     * @param config The analysis configuration, see {@link org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder}
     */
    public MavenCentralArtifactIterator(boolean resolvePom, boolean resolveTransitivePoms, boolean resolveJar, ArtifactAnalysisConfig config) {
        super(resolvePom, resolveTransitivePoms, resolveJar, config);

        if(this.baseConfig.multipleThreads){
            log.warn("Artifact iterator does no support multiple threads - using a single thread for resolution");
        }
    }

    @Override
    protected Artifact buildEntity(ArtifactResolutionContext ctx){
        final Artifact artifact = ctx.getRootArtifact();

        this.enrichArtifact(artifact, ctx);

        return artifact;
    }


    @Override
    protected Iterator<ArtifactResolutionContext> buildSource(){

        try {
            return new ArtifactContextIterable(getConfig()).iterator();
        } catch(RuntimeException rx){
            log.error("Could not create artifact iterator", rx);
            this.badSource = true;
        }

        return null;
    }

    private ArtifactAnalysisConfig getConfig(){
        return (ArtifactAnalysisConfig) this.baseConfig;
    }

}
