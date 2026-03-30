package org.tudo.sse.analyses;

import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactResolutionContext;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.analyses.input.FileBasedArtifactIterator;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.IOException;
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
        if(this.baseConfig.hasInputList()){
            try{
                return new MavenCentralCustomListArtifactSource();
            } catch(IOException | IllegalArgumentException x){
                log.error("The given input list file is invalid", x);
                this.badSource = true;
            }
        } else {
            try {
                return new MavenCentralIndexArtifactSource();
            } catch(IOException uix){
                log.error("Failed to access Maven Central Index", uix);
                this.badSource = true;
            }
        }
        return null;
    }

    private ArtifactAnalysisConfig getConfig(){
        return (ArtifactAnalysisConfig) this.baseConfig;
    }

    private class MavenCentralIndexArtifactSource implements Iterator<ArtifactResolutionContext> {

        private final IndexIterator index;

        private boolean _needsUpdate = true;
        private boolean _hasNext = false;
        private IndexInformation _nextInfo = null;
        private boolean _indexClosed = false;

        MavenCentralIndexArtifactSource() throws IOException {
            this.index = new IndexIterator(MavenCentralRepository.RepoBaseURI);
        }

        private void findNext(){
            _hasNext = false;
            while(!_hasNext && index.hasNext()){
                var currentInfo = index.next();
                if(currentInfo != null && isValidInfo(currentInfo)){
                    _hasNext = true;
                    _nextInfo = currentInfo;
                }
            }
        }

        private boolean isValidInfo(IndexInformation indexInfo){
            if(!getConfig().hasTimeBasedFiltering()) return true;
            else {
                long timeStamp = indexInfo.getLastModified();
                return getConfig().since <= timeStamp && timeStamp <= getConfig().until;
            }
        }

        @Override
        public boolean hasNext() {
            if(_needsUpdate){
                findNext();
                _needsUpdate = false;
            }

            // If we have no more entries on the underlying index, we should close it to release resources
            if(!_hasNext && !_indexClosed){
                try { this.index.closeReader(); }
                catch (IOException ignored) {}
                this._indexClosed = true;
            }

            return _hasNext;
        }

        @Override
        public ArtifactResolutionContext next() {
            if(hasNext()){
                _needsUpdate = true;

                final ArtifactResolutionContext ctx = ArtifactResolutionContext.newInstance(_nextInfo.getIdent());
                ctx.getRootArtifact().setIndexInformation(_nextInfo);

                return ctx;
            } else throw new IllegalStateException("Call to next on empty artifact source");
        }
    }

    private class MavenCentralCustomListArtifactSource implements Iterator<ArtifactResolutionContext> {

        private final FileBasedArtifactIterator list;

        MavenCentralCustomListArtifactSource() throws IOException {
            list = new FileBasedArtifactIterator(baseConfig.inputListFile);
            list.validateInput();
        }

        @Override
        public boolean hasNext() {
            return list.hasNext();
        }

        @Override
        public ArtifactResolutionContext next() {
            if(hasNext()){
                return ArtifactResolutionContext.newInstance(list.next());
            } else throw new IllegalStateException("Call to next on empty artifact source");
        }

    }

}
