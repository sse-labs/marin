package org.tudo.sse.model.pom;

import org.tudo.sse.model.*;

import java.util.List;
import java.util.Map;

/**
 * This class is where all the information collected during pom resolution is stored. From the raw features to resolved transitive dependencies.
 */
public class PomInformation extends ArtifactInformation {
    private RawPomFeatures rawPomFeatures;
    private List<Artifact> imports;
    private Artifact parent;
    private List<Dependency> resolvedDependencies;
    private List<Artifact> allTransitiveDependencies;
    private List<Artifact> effectiveTransitiveDependencies;
    private Map<String, List<ArtifactIdent>> transitiveConflicts;
    private ArtifactIdent relocation;

    protected PomInformation() {
        super();
    }

    public PomInformation(ArtifactIdent ident) {
        super(ident);
    }

    /**
     * Gets the rawPomFeatures object.
     * @return rawPomFeatures object.
     */
    public RawPomFeatures getRawPomFeatures() {
        return rawPomFeatures;
    }

    /**
     * Updates the value of the current rawPomFeatures.
     * @param rawPomFeatures new rawPomFeatures to set
     */
    public void setRawPomFeatures(RawPomFeatures rawPomFeatures) {
        this.rawPomFeatures = rawPomFeatures;
    }

    /**
     * Gets the list of imported artifacts
     * @return List of imported artifacts
     */
    public List<Artifact> getImports() {
        return imports;
    }

    /**
     * Updates the list of import artifacts
     * @param imports new list of imports to set
     */
    public void setImports(List<Artifact> imports) {
        this.imports = imports;
    }

    /**
     * Retrieves the parent artifact.
     * @return the parent artifact object
     */
    public Artifact getParent() {
        return parent;
    }

    /**
     * Updates the value of the Parent artifact.
     * @param parent new parent artifact value
     */
    public void setParent(Artifact parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the list of resolved Dependencies
     * @return a list of resolved dependency objects
     */
    public List<Dependency> getResolvedDependencies() {
        return resolvedDependencies;
    }

    /**
     * Updates the list of resolved dependencies.
     * @param resolvedDependencies new list to update value with
     */
    public void setResolvedDependencies(List<Dependency> resolvedDependencies) {
        this.resolvedDependencies = resolvedDependencies;
    }

    /**
     * Retrieves all transitive dependencies resolved, where they are nested within each other.
     * @return the list of transitive dependencies as artifact objects
     */
    public List<Artifact> getAllTransitiveDependencies() {
        return allTransitiveDependencies;
    }

    /**
     * Updates the list of transitive dependencies with a new list
     * @param allTransitiveDependencies new list of transitive dependencies to update with
     */
    public void setAllTransitiveDependencies(List<Artifact> allTransitiveDependencies) {
        this.allTransitiveDependencies = allTransitiveDependencies;
    }

    /**
     * Retrieves list of effective transitive dependencies, removing transitive dependencies that are duplicates or version conflicts.
     * @return the list of artifact objects representing the effective transitive dependencies
     */
    public List<Artifact> getEffectiveTransitiveDependencies() {
        return effectiveTransitiveDependencies;
    }

    /**
     * Updates the value of the effective transitive dependencies list
     * @param effectiveTransitiveDependencies new list to update with
     */
    public void setEffectiveTransitiveDependencies(List<Artifact> effectiveTransitiveDependencies) {
        this.effectiveTransitiveDependencies = effectiveTransitiveDependencies;
    }

    /**
     * Retrieves a map of conflicts where each (g:a) is mapped to a list of conflicts.
     * @return map containing a string (g:a) mapped to a list of conflicting identifiers
     */
    public Map<String, List<ArtifactIdent>> getTransitiveConflicts() {
        return transitiveConflicts;
    }

    /**
     * Updates the value of the transitive conflicts map
     * @param transitiveConflicts new map to update value with
     */
    public void setTransitiveConflicts(Map<String, List<ArtifactIdent>> transitiveConflicts) {
        this.transitiveConflicts = transitiveConflicts;
    }

    /**
     * Retrieves the relocation artifactIdent
     * @return artifactIdent where the current artifact information has been defined under
     */
    public ArtifactIdent getRelocation() {
        return relocation;
    }

    /**
     * Updates the value of the relocation artifact identifier
     * @param relocation new artifactIdent to update value with
     */
    public void setRelocation(ArtifactIdent relocation) {
        this.relocation = relocation;
    }
}
