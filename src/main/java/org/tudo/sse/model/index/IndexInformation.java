package org.tudo.sse.model.index;

import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ArtifactInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the metadata collected from walking the maven central index.
 */
public class IndexInformation extends ArtifactInformation {
    private String name;
    private long index;
    private final long lastModified;
    private final List<Package> packages;

    /**
     * Creates a new IndexInformation object for the given artifact identifier and package information.
     * @param ident The artifact identifier for which to create an IndexInformation
     * @param aPackage A package information object taken from the Maven Central Index. This must be associated with the
     *                 GAV triple represented by the given ArtifactIdent object.
     */
    public IndexInformation(ArtifactIdent ident, Package aPackage) {
        super(ident);
        packages = new ArrayList<>();
        packages.add(aPackage);
        this.lastModified = aPackage.getLastModified();
    }

    /**
     * Retrieves the last modified value
     * @return long represented the last modified value of the artifact
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Retrieves the name of the artifact
     * @return string representing the name of the artifact
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the name of the artifact
     * @param name new name to update with
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the index of the artifact
     * @return long representing the index of the artifact
     */
    public long getIndex() {
        return index;
    }

    /**
     * Updates the value of the artifact index
     * @param index new index value to set
     */
    public void setIndex(long index) {
        this.index = index;
    }

    /**
     * Adds a package which has the same identifier as the current artifact
     * @param pack package object to add
     */
    public void addAPackage(Package pack) {
        packages.add(pack);
    }

    /**
     * Retrieves all the packages under the same identifier
     * @return a list of packages
     */
    public List<Package> getPackages() {
        return packages;
    }
}
