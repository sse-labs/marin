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

public class IndexWalker implements Iterable<IndexInformation> {

    /**
     * The string pattern at which to split index entries.
     */
    public static final String splitPattern = Pattern.quote("|");

    private IndexIterator indexIterator;
    private boolean resetIterator;
    private final URI base;

    private static final Logger log = LogManager.getLogger(IndexWalker.class);

    /**
     * Creates a new IndexWalker with the given repository base URI.
     * @param base The repository base URI.
     */
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
     * Produces a list of all artifact identifiers for the entire repository index.
     * @return List of artifact identifiers inside the repository
     * @throws IOException If connection errors occur
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
     * Produces a list of all artifacts for the entire repository index. All artifacts are annotated with index information.
     * @return List of artifacts (with index information)
     * @throws IOException If connection errors occur
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
     * Produces a list of artifacts from the repository index with the given pagination values.
     * @param skip Number of artifact identifiers to skip
     * @param take Number of artifact identifiers to take
     * @return List of artifact identifiers
     * @throws IOException If connection errors occur
     */
    public List<ArtifactIdent> lazyWalkPaginated(long skip, long take) throws IOException {
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
     * Produces a list of artifact identifiers from the repository index with the given pagination values. All artifacts
     * are annotated with index information.
     * @param skip Number of artifacts to skip
     * @param take Number of artifacts to take
     * @return List of artifacts with index information
     * @throws IOException If connection errors occur
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
     * Produces a list of artifact identifiers from the repository index within the given time bounds.
     * @param since Timestamp marking the lower bound for release dates
     * @param until Timestamp marking the upper bound for release dates
     * @return List of artifact identifiers
     * @throws IOException If connection errors occur
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
     * Produces a list of artifacts from the repository index within the given time bounds. All artifacts
     * are annotated with index information.
     * @param since Timestamp marking the lower bound for release dates
     * @param until Timestamp marking the upper bound for release dates
     * @return List of artifacts with index information
     * @throws IOException If connection errors occur
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
