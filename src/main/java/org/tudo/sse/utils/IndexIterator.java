package org.tudo.sse.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.index.reader.IndexReader;
import org.tudo.sse.IndexWalker;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.Package;
import org.tudo.sse.model.index.IndexInformation;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

/**
* This class creates an iterator for iterating over indexes and returning IndexArtifact objects.
* These objects contain more information than just the identifiers that can be retrieved from the index.
* The information stored from the index is: GAV, Package (lastModified, source exists, javadoc exists, signature exists, packaging), size, name, sha1Checksum, and index
* There are multiple artifacts with the same GAV, but different packages, so those packages are stored in the same artifact.
*/

public class IndexIterator implements Iterator<IndexInformation> {
    private long index;

    private final URI baseUri;
    private IndexReader ir;
    private Iterator<Map<String, String>> cr;
    private IndexInformation currentArtifact;
    private IndexInformation nextArtifact;
    private boolean prevHasNext;

    private static final Logger log = LogManager.getLogger(IndexIterator.class);


    public IndexIterator(URI base) throws IOException {
        baseUri = base;
        ir = new IndexReader(null, new HttpResourceHandler(base.resolve(".index/")));
        cr = ir.iterator().next().iterator();
        index = 0;
        currentArtifact = null;
        nextArtifact = null;
    }

    public IndexIterator(URI base, long startingIndex) throws IOException {
        this(base);

        while(cr.hasNext() && index != startingIndex) {
         cr.next();
         index++;
        }
    }

    public void closeReader() throws IOException {
        ir.close();
    }

    private void recoverConnectionReset() throws IOException{
        long indexPos = getIndex();
        log.info("Recovering from connection reset at index {}", indexPos);


        ir = new IndexReader(null, new HttpResourceHandler(baseUri.resolve(".index/")));
        cr = ir.iterator().next().iterator();
        index = 0;

        while(cr.hasNext() && index < indexPos){
            cr.next();
            index++;
            if(index % 1000000 == 0) log.debug("Skipping indices for recovery, {} processed so far ...", index);
        }

        log.info("Recovery successful, reset chunk reader to index {}.", indexPos);
    }

    /**
     * This method takes in a string of the gav tuple and creates an artifactIdent from it.
     * @param gav string version of an artifact identifier "g:a:v"
     * @return an artifactIdent object
     * @see ArtifactIdent
     */
    public ArtifactIdent processArtifactIdent(String gav) {
        String[] parts = gav.split(IndexWalker.splitPattern);
        return new ArtifactIdent(parts[0], parts[1], parts[2]);
    }

    /**
     *  This method is used for creating a package obj to add to the packages list when occurrence of the same gav tuple
     *
     * @param information string of metadata to parse
     * @param checksum sha1 hash value
     * @return a Package object for different artifacts under the same identifier
     * @see Package
     */
    public Package processPackage(String information, String checksum) {
        if(information != null) {
            String[] parts = information.split(IndexWalker.splitPattern);
            return new Package(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), checksum);
        }
        return null;
    }

    /**
     * This method parsing the metadata that is present for a given index.
     * Creating an artifact identifier and compiling the rest of the information into a Package object.
     *
     * @param item a map of the metadata for the current index
     * @return information collected from the current index
     * @see IndexInformation
     * @see Package
     */
    public IndexInformation processIndex(Map<String, String> item) {
        String uVal = item.get("u");
        //process the G:A:V tuple
        if(uVal != null) {
            ArtifactIdent temp = processArtifactIdent(uVal);

            return processIndex(item, temp);
        }
        return null;
    }

    private IndexInformation processIndex(Map<String, String> item, ArtifactIdent ident){
        String iVal = item.get("i");

        //Create an artifact using the values found in the 'i' and '1' tags
        if(iVal != null) {
            String[] parts = iVal.split(IndexWalker.splitPattern);

            Package tmpPackage = new Package(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), item.get("1"));

            IndexInformation t = new IndexInformation(ident, tmpPackage);
            t.setName(item.get("n"));
            t.setIndex(index);
            index++;

            if(index != 0 && index % 500000 == 0){
                log.info("{} indexes have been processed.", index);
            }

            return t;
        }

        return null;
    }

    public long getIndex() {
        if(currentArtifact != null) {
            return currentArtifact.getIndex();
        } else {
            return index;
        }
    }

    /**
     * Moves the iterator to the next unique artifact identifier in the maven central index.
     *
     * @return whether there is another index after the current one
     */
    @Override
    public boolean hasNext() {

        //check if currentArtifact is null, and walk to next
        if(currentArtifact == null) {

            //pass nextArtifact to currentArtifact
            if(nextArtifact == null) {
                while (currentArtifact == null && cr.hasNext()) {
                    try {
                        // This may fail with an SSL connection reset exception...
                        currentArtifact = processIndex(cr.next());
                    } catch(RuntimeException rx){

                        // Try to find out if this read error was caused by an SSL connection reset.
                        boolean causedBySsl = false;
                        Throwable current = rx;
                        while(!causedBySsl && current.getCause() != null){
                            current = current.getCause();
                            causedBySsl = current instanceof SSLException;
                        }

                        if(rx.getMessage().contains("read error") && causedBySsl){
                            // If so, try to recover by re-initializing the reader (and skipping to the right position)
                            try {
                                recoverConnectionReset();
                            } catch(Exception x){
                                log.error("Recovery unsuccessful: " + x.getMessage());
                                throw new RuntimeException(x);
                            }
                        } else {
                            // Don't try to handle other exceptions with recovery
                            throw rx;
                        }
                    }

                }
            } else {
                currentArtifact = nextArtifact;
                nextArtifact = null;
            }

            //keep iterating the indexReader until the gav is different from the one in currentArtifact
            while (cr.hasNext()) {
                Map<String, String> currentEntry;

                try {
                    currentEntry = cr.next();
                } catch(RuntimeException rx){
                    log.error("Failed to get entry from index: " + rx.getMessage());
                    Throwable cause = rx.getCause();

                    if(cause != null){
                        log.error("Cause of read error: " + cause.getMessage(), cause);
                        if(cause.getMessage().toLowerCase().contains("unexpected end")){
                            try {
                                log.info("Recovering from connection interruption ... ");
                                recoverConnectionReset();
                                if(cr.hasNext()) currentArtifact = processIndex(cr.next());
                                else throw new RuntimeException("No index entry was found after recovery");
                                log.info("Recovery successful");
                            } catch(Exception x){
                                log.error("Recovery unsuccessful", x);
                            }
                        }
                    }


                    currentEntry = null;
                }

                if (currentEntry == null){
                    log.error("Aborting due to error, hasNext will be: " + cr.hasNext());
                    break;
                }

                String currentUVal = currentEntry.get("u");

                if(currentUVal == null) break;

                final String currentArtifactGAV = currentArtifact.getIdent().getCoordinates();
                final ArtifactIdent currentEntryIdent = processArtifactIdent(currentUVal);

                if(!currentArtifactGAV.equals(currentEntryIdent.getCoordinates())){
                    nextArtifact = processIndex(currentEntry, currentEntryIdent);
                    break;
                }

                String currentIVal = currentEntry.get("i");

                if(currentIVal != null) {
                    currentArtifact.addAPackage(processPackage(currentIVal, currentEntry.get("1")));
                    index++;
                }
            }

            prevHasNext = cr.hasNext();
            return cr.hasNext();

        } else {
            return prevHasNext;
        }
    }

    @Override
    public IndexInformation next() {
        if(hasNext()) {
            IndexInformation tmp = currentArtifact;
            currentArtifact = null;
            return tmp;
        } else {
            return null;
        }
    }
}
