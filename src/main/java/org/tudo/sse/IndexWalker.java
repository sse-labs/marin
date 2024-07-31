package org.tudo.sse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.utils.IndexIterator;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
*  This class creates an object that manages retrieving indexes from the Maven Central repository.
* Either the Identifiers can be retrieved lazily or be fully parsed, utilizing the IndexIterator.
* This class also allows for the ability to walk all indexes or walk them in a paginated fashion.
*/

public class IndexWalker implements Iterable<IndexInformation>{
    public static final String splitPattern = Pattern.quote("|");
    private IndexIterator indexIterator;
    private boolean resetIterator;
    private final URI base;

    private static final Logger log = LogManager.getLogger(IndexWalker.class);

    public IndexWalker(URI base) {
        this.base = base;
        resetIterator = true;
    }

    /**
     * Moves the iterator to a specified index.
     *
     * @param position which index to move to
     * @throws IOException when there is an issue opening a file
     */
    public void moveIterator(long position) throws IOException {
        indexIterator = new IndexIterator(base, position);
        this.resetIterator = false;
    }

    /**
     * {@link MavenCentralAnalysis#lazyWalkAllIndexes(IndexIterator)}
     */
    public List<ArtifactIdent> lazyWalkAllIndexes() throws IOException {
        if(resetIterator) {
            indexIterator = new IndexIterator(base);
        }

        List<ArtifactIdent> idents = new ArrayList<>();
        while(indexIterator.hasNext()) {
            idents.add(indexIterator.next().getIdent());
        }

        indexIterator.closeReader();
        return idents;
    }

    /**
     * {@link MavenCentralAnalysis#walkAllIndexes(IndexIterator)}
     */
    public List<Artifact> walkAllIndexes() throws IOException {
        if(resetIterator) {
            indexIterator = new IndexIterator(base);
        }

        List<Artifact> artifacts = new ArrayList<>();
        while(indexIterator.hasNext()) {
            artifacts.add(ArtifactFactory.createArtifact(indexIterator.next()));
        }

        if(artifacts.size() % 500000 == 0) {
            log.info("{} artifacts have been parsed.", artifacts.size());
        }
        indexIterator.closeReader();
        return artifacts;
    }

    /**
     * {@link MavenCentralAnalysis#lazyWalkPaginated(long, IndexIterator)}
     *
     * @param skip how many indexes to skip from the beginning of the index
     */
    public List<ArtifactIdent> lazyWalkPaginated(long skip, long take) throws IOException{
        if(resetIterator) {
            indexIterator = new IndexIterator(base);
        }
        List<ArtifactIdent> idents = new ArrayList<>();

        int count = 0;
        int fromFront = 0;
        while(indexIterator.hasNext() && count < take) {
            if(fromFront >= skip) {
                idents.add(indexIterator.next().getIdent());
                count++;
            } else {
                indexIterator.next();
            }
            fromFront++;
        }
        indexIterator.closeReader();

        return idents;
    }

    /**
     * {@link MavenCentralAnalysis#walkPaginated(long, IndexIterator)}
     *
     * @param skip how many indexes to skip from the beginning of the index
     */
    public List<Artifact> walkPaginated(long skip, long take) throws IOException {
        if(resetIterator) {
            indexIterator = new IndexIterator(base);
        }
        List<Artifact> artifacts = new ArrayList<>();

        int count = 0;
        int fromFront = 0;
        while(indexIterator.hasNext() && count < take) {
            if(fromFront >= skip) {
                artifacts.add(ArtifactFactory.createArtifact(indexIterator.next()));
                count++;
            } else {
                indexIterator.next();
            }
            fromFront++;
        }
        indexIterator.closeReader();
        return artifacts;
    }

    /**
     * {@link MavenCentralAnalysis#walkDates(long, long, IndexIterator)}
     */
    public List<Artifact> walkDates(long since, long until) throws IOException {
            if(resetIterator) {
                indexIterator = new IndexIterator(base);
            }
            List<Artifact> artifacts = new ArrayList<>();

            long currentToSince = 0;
            long sinceToUntil = since;
            while(indexIterator.hasNext() && sinceToUntil < until) {
                IndexInformation temp = indexIterator.next();
                if(currentToSince >= since) {
                    sinceToUntil = temp.getLastModified();
                    artifacts.add(ArtifactFactory.createArtifact(temp));
                } else {
                    currentToSince = temp.getLastModified();
                }
            }
            indexIterator.closeReader();
            return artifacts;
        }

    /**
     * {@link MavenCentralAnalysis#lazyWalkDates(long, long, IndexIterator)}
     */
    public List<ArtifactIdent> lazyWalkDates(long since, long until) throws IOException{
        if(resetIterator) {
            indexIterator = new IndexIterator(base);
        }
        List<ArtifactIdent> idents = new ArrayList<>();

        long currentToSince = 0;
        long sinceToUntil = since;
        while(indexIterator.hasNext() && sinceToUntil < until) {
            IndexInformation temp = indexIterator.next();
            if(currentToSince >= since) {
                sinceToUntil = temp.getLastModified();
                idents.add(temp.getIdent());
            } else {
                currentToSince = temp.getLastModified();
            }
        }
        indexIterator.closeReader();

        return idents;
    }

    @Override
    public Iterator<IndexInformation> iterator() {
        try {
            return new IndexIterator(base);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
