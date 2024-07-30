package org.tudo.sse.model;

import org.tudo.sse.model.index.IndexInformation;
import org.tudo.sse.model.jar.JarInformation;
import org.tudo.sse.model.pom.PomInformation;

/**
 * This class holds all the artifact information. For each artifact index, jar, and pom information can be defined.
 * Two identifiers are also defined as some artifact are relocated under new identifiers.
 */
public class Artifact {
    /**
     * The identifier object for the artifact.
     */
    public ArtifactIdent ident;
    /**
     * A secondary identifier, for if its pom information has been moved on the maven central repository.
     */
    public ArtifactIdent relocation;
    private IndexInformation indexInformation;
    private PomInformation pomInformation;
    private JarInformation jarInformation;

    public Artifact(IndexInformation indexInformation) {
        this.indexInformation = indexInformation;
        this.ident = indexInformation.getIdent();
        pomInformation = null;
        jarInformation = null;
    }

    public Artifact(PomInformation pomInformation) {
        this.pomInformation = pomInformation;
        this.ident = pomInformation.getIdent();
        this.relocation = pomInformation.getRelocation();
        indexInformation = null;
        jarInformation = null;
    }

    public Artifact(JarInformation jarInformation) {
        this.jarInformation = jarInformation;
        this.ident = jarInformation.getIdent();
        indexInformation = null;
        pomInformation = null;
    }

    public IndexInformation getIndexInformation() {
        return indexInformation;
    }

    public ArtifactIdent getIdent() {
        return ident;
    }

    public ArtifactIdent getRelocation() {
        return relocation;
    }

    public void setIndexInformation(IndexInformation indexInformation) {
        this.indexInformation = indexInformation;
    }

    public PomInformation getPomInformation() {
        return pomInformation;
    }

    public void setPomInformation(PomInformation pomInformation) {
        this.pomInformation = pomInformation;
    }

    public JarInformation getJarInformation() {
        return jarInformation;
    }

    public void setJarInformation(JarInformation jarInformation) {
        this.jarInformation = jarInformation;
    }

}
