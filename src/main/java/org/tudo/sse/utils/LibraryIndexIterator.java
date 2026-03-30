package org.tudo.sse.utils;

import org.apache.maven.index.reader.ChunkReader;
import org.apache.maven.index.reader.IndexReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Iterator over all unique library names in the Maven Central index. A library name is a tuple of GroupId:ArtifactId,
 * separated by a colon.
 */
public class LibraryIndexIterator implements Iterator<String>, AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<Integer> libraryHashesSeen;
    private final Iterator<Map<String, String>> entryIterator;

    private final IndexReader mavenIndexReader;
    private final ChunkReader mavenChunkReader;

    private long currentPosition;

    private String currentLibraryGA;
    private String nextLibraryGA;

    /**
     * Creates a new iterator instance for the given Maven repository.
     * @param baseUri URI of the Maven-complient repository to iterator over
     * @throws IOException If accessing the repository fails.
     */
    public LibraryIndexIterator(URI baseUri) throws IOException {
        this.libraryHashesSeen = new HashSet<>();

        // Build intermediate readers that must be kept open until iterator is done and resources can be closed
        this.mavenIndexReader = new IndexReader(null, new HttpResourceHandler(baseUri.resolve(".index/")));
        this.mavenChunkReader = this.mavenIndexReader.iterator().next();

        // Build the actual iterator for entries in the Maven Central index
        this.entryIterator = this.mavenChunkReader.iterator();

        this.currentPosition = -1;
        this.currentLibraryGA = null;
    }


    /**
     * Sets this iterator to a specific position by skipping over the given amount of entries. Aborts early if no more
     * entries are available on the iterator.
     *
     * @param startIdx The iterator position to skip to
     */
    public void setPosition(long startIdx){
        while(this.currentPosition < startIdx && this.hasNext()){
            this.next();
        }
    }

    @Override
    public boolean hasNext() {
        // If currentLibraryGA is null, we have the first hasNext() call after a call to next(). We must find our new
        // currentLibraryGA first.
        if(currentLibraryGA == null){

            // If nextLibraryGA is null, we have no information stored from a previous iteration about the next artifact.
            // We thus must determine the current artifact ourselves and, while we are there, detect the new next artifact
            // as well.
            if(this.nextLibraryGA == null){
                // Walk to the first actual entry
                while(currentLibraryGA == null && entryIterator.hasNext()){
                    currentLibraryGA = getGAFromEntry(nextEntry());
                }
            } else {
                // If we have information about the next artifact, we just copy it
                this.currentLibraryGA = this.nextLibraryGA;
                this.nextLibraryGA = null;
            }

            findNextGA();
        }

        return this.nextLibraryGA != null;
    }

    @Override
    public String next() {
        if(hasNext()){
            final String valueToReturn = this.currentLibraryGA;
            this.libraryHashesSeen.add(this.currentLibraryGA.hashCode());
            this.currentLibraryGA = null;

            currentPosition += 1;

            return valueToReturn;
        } else throw new IllegalStateException("No libraries left on iterator (position " + currentPosition + ")");
    }



    private boolean isNewLibrary(String libraryGA){
        final int hash = libraryGA.hashCode();
        return !libraryHashesSeen.contains(hash);
    }

    private String getGAFromEntry(Map<String, String> entry){
        final String uVal = entry.get("u");
        if(uVal != null){
            final StringBuilder gaBuilder = new StringBuilder();
            boolean patternSeen = false;
            for(char current: uVal.toCharArray()){
                if(current == '|'){
                    if(patternSeen) break;

                    patternSeen = true;
                    gaBuilder.append(':');
                } else {
                    gaBuilder.append(current);
                }
            }

            return gaBuilder.toString();
        } else return null;
    }

    private void findNextGA(){
        // Walk up to the next entry that has a different GA (is a different library)
        String nextNewGA = null;

        while(entryIterator.hasNext()){
            final Map<String, String> nextEntry = nextEntry();
            final String nextGA =  getGAFromEntry(nextEntry);

            // Silently ignore malformed entries
            if(nextGA == null) continue;

            if(!currentLibraryGA.equals(nextGA) && isNewLibrary(nextGA)){
                nextNewGA = nextGA;
                break;
            }
        }

        this.nextLibraryGA = nextNewGA;
    }

    private Map<String, String> nextEntry(){
        return entryIterator.next();
    }


    @Override
    public void close(){
        try {
            if(this.mavenChunkReader != null){
                this.mavenChunkReader.close();
            }
            if(this.mavenIndexReader != null) {
                this.mavenIndexReader.close();
            }
        } catch(IOException iox){
            logger.warn("Exception while closing maven index reader", iox);
        }


    }
}
