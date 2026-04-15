package org.tudo.sse.analyses.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.model.ArtifactResolutionContext;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.IOException;
import java.util.Iterator;

/**
 * Class that provides an iterator over ArtifactResolutionContext instances. The iterator is, according to the
 * underlying configuration, build on top of either a custom list of input GAVs (in this case, artifacts have no index
 * information annotated), or the Maven Central index.
 *
 * @author Johannes Düsing
 */
public class ArtifactContextIterable implements Iterable<ArtifactResolutionContext> {

    private final Logger log = LoggerFactory.getLogger(ArtifactContextIterable.class);

    private final ArtifactAnalysisConfig baseConfig;

    /**
     * Creates a new ArtifactContextIterable that builds an iterator according to the given configuration.
     * @param config The configuration to use
     */
    public ArtifactContextIterable(ArtifactAnalysisConfig config) {
        this.baseConfig = config;
    }

    @Override
    public Iterator<ArtifactResolutionContext> iterator() {
        try {
            if(!baseConfig.hasInputList()){
                return new MavenCentralIndexArtifactSource();
            } else {
                return new MavenCentralCustomListArtifactSource();
            }
        } catch (IOException iox) {
            log.error("Failed to create artifact context iterator", iox);
            throw new RuntimeException(iox);
        }
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
            if(!baseConfig.hasTimeBasedFiltering()) return true;
            else {
                long timeStamp = indexInfo.getLastModified();
                return baseConfig.since <= timeStamp && timeStamp <= baseConfig.until;
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

        private final FileBasedArtifactIdentIterator list;

        MavenCentralCustomListArtifactSource() throws IOException {
            list = new FileBasedArtifactIdentIterator(baseConfig.inputListFile);
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
