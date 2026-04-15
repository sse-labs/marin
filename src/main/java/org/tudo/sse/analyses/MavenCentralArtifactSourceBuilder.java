package org.tudo.sse.analyses;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.KillSwitches;
import org.apache.pekko.stream.OverflowStrategy;
import org.apache.pekko.stream.UniqueKillSwitch;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Source;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.input.ArtifactContextIterable;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactResolutionContext;
import org.tudo.sse.resolution.ResolverFactory;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class that can be used to obtain a Pekko Source emitting enriched Artifacts. This is analogous to the
 * MavenCentralArtifactIterator, but may be used for multithreaded stream (or actor) processing as well.
 *
 * @author Johannes Düsing
 *
 */
public class MavenCentralArtifactSourceBuilder {

    private boolean resolvePom;
    private boolean resolveTransitivePoms;
    private boolean resolveJar;

    private final ArtifactAnalysisConfig config;

    private Source<Artifact, UniqueKillSwitch> _source = null;

    private final AtomicLong currentPosition = new AtomicLong(0);
    private final AtomicLong lastPositionSaved = new AtomicLong(0);

    /**
     * Creates a new builder for a Pekko Source using the given configuration object. One builder can only create one
     * specific Source object.
     *
     * @param config The configuration to base this builder on.
     */
    public MavenCentralArtifactSourceBuilder(ArtifactAnalysisConfig config) {
        this.config = config;

        this.resolvePom = false;
        this.resolveTransitivePoms = false;
        this.resolveJar = false;
    }

    /**
     * Makes the Source object created by this builder emit artifacts that are annotated with POM information.
     * @return This builder instance
     */
    public MavenCentralArtifactSourceBuilder withPomInfo(){
        if(this._source != null)
            throw new RuntimeException("Source already built, cannot modify configuration");

        this.resolvePom = true;
        return this;
    }

    /**
     * Makes the Source object created by this builder emit artifacts that are annotated with transitive POM information.
     * @return This builder instance
     */
    public MavenCentralArtifactSourceBuilder withTransitivePomInfo(){
        if(this._source != null)
            throw new RuntimeException("Source already built, cannot modify configuration");

        this.resolvePom = true;
        this.resolveTransitivePoms = true;
        return this;
    }

    /**
     * Makes the Source object created by this builder emit artifacts that are annotated with JAR information.
     * @return This builder instance
     */
    public MavenCentralArtifactSourceBuilder withJarInfo(){
        if(this._source != null)
            throw new RuntimeException("Source already built, cannot modify configuration");

        this.resolveJar = true;
        return this;
    }

    /**
     * Builds the source object as currently defined by this builder. Subsequent calls to this method will return the
     * exact same object reference, the source object is only created once per builder.
     *
     * @return The source object that emits artifacts and materializes as a KillSwitch
     */
    public Source<Artifact, UniqueKillSwitch> buildSource(){
        if(_source == null) {
            final ResolverFactory resolverFactory = config.outputEnabled ?
                    new ResolverFactory(config.outputEnabled, config.outputDirectory, resolveTransitivePoms) :
                    new ResolverFactory(resolveTransitivePoms);

            final Iterator<ArtifactResolutionContext> sourceIterator = new ArtifactContextIterable(config).iterator();

            long initialPos = AnalysisUtils.skipInitial(sourceIterator, config);
            this.currentPosition.addAndGet(initialPos);

            Source<ArtifactResolutionContext, NotUsed> itSource = Source.fromIterator(() -> sourceIterator);

            if(config.take > 0)
                itSource = itSource.take(config.take);

            this._source = itSource
                    .mapAsync(config.threadCount, ctx -> enrichArtifact(ctx, resolverFactory, resolvePom, resolveJar))
                    .buffer(10, OverflowStrategy.backpressure())
                    .viaMat(KillSwitches.single(), Keep.right());
        }

        return this._source;
    }

    private CompletionStage<Artifact> enrichArtifact(ArtifactResolutionContext ctx,
                                                     ResolverFactory factory,
                                                     boolean resolvePom,
                                                     boolean resolveJar){
        return CompletableFuture.supplyAsync(() -> enrich(ctx, factory, resolvePom, resolveJar));
    }

    private Artifact enrich(ArtifactResolutionContext ctx,
                            ResolverFactory factory,
                            boolean resolvePom,
                            boolean resolveJar){
        final Artifact artifact = ctx.getRootArtifact();

        if(resolvePom)
            factory.runPom(artifact.getIdent(), ctx);
        if(resolveJar)
            factory.runJar(artifact.getIdent(), ctx);

        this.currentPosition.incrementAndGet();
        this.writePositionIfNeeded();

        return artifact;
    }

    private void writePositionIfNeeded(){
        // Analogous to double-locks: Check write conditions is true, lock last Position, check again and finally write
        long currPosition = this.currentPosition.get();
        long lastPosition = this.lastPositionSaved.get();

        if(currPosition - lastPosition > this.config.progressWriteInterval){
            synchronized(this.lastPositionSaved){
                currPosition = this.currentPosition.get();
                lastPosition = this.lastPositionSaved.get();
                if(currPosition - lastPosition > this.config.progressWriteInterval){
                    AnalysisUtils.writePosition(currPosition, this.config);
                    this.lastPositionSaved.set(currPosition);
                }

            }
        }
    }
}
