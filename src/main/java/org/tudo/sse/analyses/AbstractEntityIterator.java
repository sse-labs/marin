package org.tudo.sse.analyses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ResolutionContext;
import org.tudo.sse.resolution.ResolverFactory;

import java.util.Iterator;

/**
 * An abstract base class for analysis-equivalent iterators. This class assumes an underlying source iterator that
 * produces a generic kind of source object (an "identifier" for the entity), and then transforms the source object into
 * a target entity of generic type. The iterator enforces pagination and supports progress dumps and progress restores.
 *
 * @param <S> Type of source objects as produced by the underlying iterator
 * @param <T> Type of target objects that are created by this iterator
 *
 * @author Johannes Düsing
 */
abstract class AbstractEntityIterator<S, T> implements Iterator<T> {

    /**
     * Logger instance for this iterator
     */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The base configuration for this iterator instance - subclasses may have a more specific configuration type.
     */
    protected final LibraryAnalysisConfig baseConfig;

    /**
     * Flag indicating whether the underlying source is invalid, i.e. because a connection attempt failed
     */
    protected boolean badSource = false;

    private final Iterator<S> source;

    private boolean sourceClosed = false;

    private long currentPosition = 0L;
    private long entitiesTaken = 0L;
    private long lastPositionSaved = 0L;

    private final ResolverFactory resolverFactory;
    private final boolean resolvePom;
    private final boolean resolveJar;

    /**
     * Builds the underlying source iterator for this entity iterator instance.
     * @return The source iterator
     */
    protected abstract Iterator<S> buildSource();

    /**
     * Transforms a source object into the target entity.
     * @param source The source object as produced by the underlying iterator
     * @return The target entity
     */
    protected abstract T buildEntity(S source);

    /**
     * Creates a new entity iterator instance with the given configuration. Immediately sets the underlying source and
     * skips all initial entities according to the configuration.
     *
     * @param resolvePom Whether information on artifact pom files shall be resolved
     * @param resolveTransitivePoms Whether transitive poms should also be resolved
     * @param resolveJar Whether information on jar files shall be resolved
     * @param baseConfig The analysis configuration, see {@link org.tudo.sse.analyses.config.LibraryAnalysisConfigBuilder}
     */
    AbstractEntityIterator(boolean resolvePom,
                           boolean resolveTransitivePoms,
                           boolean resolveJar,
                           LibraryAnalysisConfig baseConfig){
        this.baseConfig = baseConfig;
        this.resolvePom = resolvePom;
        this.resolveJar = resolveJar;

        this.source = buildSource();

        if(baseConfig instanceof ArtifactAnalysisConfig && ((ArtifactAnalysisConfig)this.baseConfig).outputEnabled){
            this.resolverFactory = new ResolverFactory(true,
                    ((ArtifactAnalysisConfig)this.baseConfig).outputDirectory,
                    resolveTransitivePoms);
        } else {
            this.resolverFactory = new ResolverFactory(resolveTransitivePoms);
        }

        try {
            if(!this.badSource)
                this.skipInitial();
        } catch(Exception x){
            log.error("Failed to initialize iterator", x);
            this.badSource = true;
        }
    }

    @Override
    public final boolean hasNext() {
        // If the underlying source is invalid, we have no next element
        if(this.badSource) return false;

        // If the config specifies a limited amount of entities to take, and that amount is reached, we have no next element
        if(this.baseConfig.hasTake() && this.entitiesTaken >= this.baseConfig.take){
            closeSourceIfNeeded();
            return false;
        }

        // Otherwise, we have a next entity if the underlying source has a next element
        try {
            boolean hasNext = this.source.hasNext();

            if(!hasNext)
                closeSourceIfNeeded();

            return hasNext;
        } catch(Exception x){
            // If accessing the source fails, we report an error and mark the source as bad - also we assume we have no
            // next element.
            log.error("Failed to access source", x);
            this.badSource = true;
            return false;
        }
    }

    @Override
    public final T next() {
        if(!hasNext())
            throw new IllegalStateException("No more entities available");

        final S sourceEntity = this.source.next();
        final T entity = this.buildEntity(sourceEntity);

        this.entitiesTaken += 1L;
        this.currentPosition += 1L;

        writePositionIfNeeded();

        return entity;
    }

    /**
     * Enriches the given artifact with ArtifactInformation as defined by the current configuration.
     * @param a The artifact to enrich
     * @param ctx The current artifact resolution context
     */
    protected final void enrichArtifact(Artifact a, ResolutionContext ctx){
        if(this.resolvePom)
            resolverFactory.runPom(a.getIdent(), ctx);

        if(this.resolveJar)
            resolverFactory.runJar(a.getIdent(), ctx);
    }

    private void writePositionIfNeeded(){
        if(this.currentPosition - this.lastPositionSaved > this.baseConfig.progressWriteInterval){
            AnalysisUtils.writePosition(this.currentPosition, this.baseConfig);
            this.lastPositionSaved = this.currentPosition;
        }
    }

    private void closeSourceIfNeeded(){
        if(!this.sourceClosed && !this.badSource && this.source instanceof AutoCloseable){
            try {((AutoCloseable)this.source).close();}
            catch(Exception ignored){}
            this.sourceClosed = true;
        }
    }

    private void skipInitial(){
        final long entitiesToSkip = AnalysisUtils.getInitialPosition(this.baseConfig);

        if(entitiesToSkip > 0L && this.badSource)
            return;

        for(int i = 0; i < entitiesToSkip && this.source.hasNext(); i++){
            this.source.next();
            this.currentPosition += 1L;
        }

        log.debug("Successfully skipped to position {}", this.currentPosition);

        if(!this.source.hasNext()){
            log.warn("Reached end of input source while skipping to position {}", entitiesToSkip);
        }
    }
}
