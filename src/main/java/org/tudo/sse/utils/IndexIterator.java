package org.tudo.sse.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.index.reader.IndexReader;
import org.tudo.sse.IndexWalker;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.Package;
import org.tudo.sse.model.index.IndexInformation;

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
    private final IndexReader ir;
    private final Iterator<Map<String, String>> cr;
    private IndexInformation currentArtifact;
    private IndexInformation nextArtifact;
    private boolean prevHasNext;

    private static final Logger log = LogManager.getLogger(IndexIterator.class);


    public IndexIterator(URI base) throws IOException {
        ir = new IndexReader(null, new HttpResourceHandler(base.resolve(".index/")));
        cr = ir.iterator().next().iterator();
        index = 0;
        currentArtifact = null;
        nextArtifact = null;
    }

    public IndexIterator(URI base, long startingIndex) throws IOException {
        ir = new IndexReader(null, new HttpResourceHandler(base.resolve(".index/")));
        cr = ir.iterator().next().iterator();
        index = 0;

        while(cr.hasNext() && index != startingIndex) {
         cr.next();
         index++;
        }
        currentArtifact = null;
        nextArtifact = null;
    }

    public void closeReader() throws IOException {
        ir.close();
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
        //process the G:A:V tuple
        if(item.get("u") != null) {
            ArtifactIdent temp = processArtifactIdent(item.get("u"));

            //Create an artifact using the values found in the 'i' and '1' tags
            if(item.get("i") != null) {
                String[] parts = item.get("i").split(IndexWalker.splitPattern);

                Package tmpPackage = new Package(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), item.get("1"));

                IndexInformation t = new IndexInformation(temp, tmpPackage);
                t.setName(item.get("n"));
                t.setIndex(index);
                index++;

                if(index != 0 && index % 500000 == 0){
                    log.info("{} indexes have been processed.", index);
                }

                return t;
            }
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
                while (cr.hasNext() && currentArtifact == null) {
                    currentArtifact = processIndex(cr.next());
                }
            } else {
                currentArtifact = nextArtifact;
                nextArtifact = null;
            }

            //keep iterating the indexReader until the gav is different from the one in currentArtifact
            while (cr.hasNext()) {
                Map<String, String> curInfo = cr.next();

                if (curInfo.get("u") == null) {
                    break;
                }

                if (!(currentArtifact.getIdent().getCoordinates().equals(processArtifactIdent(curInfo.get("u")).getCoordinates()))) {

                    //store into additional variable
                    nextArtifact = processIndex(curInfo);
                    break;
                }

                if(curInfo.get("i") != null) {
                    currentArtifact.addAPackage(processPackage(curInfo.get("i"), curInfo.get("1")));
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
