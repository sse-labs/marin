package org.tudo.sse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The specific type of resolution context for resolving libraries. Tracks what artifacts belong to the library.
 *
 * @author Johannes Düsing
 */
public final class LibraryResolutionContext extends ResolutionContext {

    private final String libraryGA;
    private final List<Artifact> libraryArtifacts;

    private LibraryResolutionContext(String libraryGA) {
        this.libraryGA = libraryGA;
        this.libraryArtifacts = new ArrayList<>();
    }

    /**
     * Factory method to create a new LibraryResolutionContext for the given GA.
     * @param libraryGA Maven library name (GroupID:ArtifactID)
     * @return New LibraryResolutionContext instance for the given library
     */
    public static LibraryResolutionContext newInstance(String libraryGA) {
        return new LibraryResolutionContext(libraryGA);
    }

    /**
     * Appends a new artifact to the list of library artifacts, i.e., the list of library releases.
     * @param a The new library artifact
     */
    public void addLibraryArtifact(Artifact a){
        this.libraryArtifacts.add(a);
    }

    /**
     * Returns the list of library artifacts, i.e., the list of library releases.
     * @return List of artifacts belonging to the library
     */
    public List<Artifact> getLibraryArtifacts(){
        return this.libraryArtifacts;
    }

    /**
     * Returns the library's name for which this context was created.
     * @return Library name (GroupID:ArtifactID)
     */
    public String getLibraryGA() {
        return libraryGA;
    }

}
