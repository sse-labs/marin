package org.tudo.sse.model.pom;

import java.util.List;
import java.util.Set;

import org.tudo.sse.model.ArtifactIdent;

/**
 * This class holds the dependency information collected from the pom file, along with some attributes to aid in dependency resolution.
 */
public class Dependency {
    private ArtifactIdent ident;
    private String scope;
    private final Set<String> exclusions;
    private final boolean optional;
    private boolean isResolved;
    private boolean isVersionRange;

    /**
     * Constructor takes in different boolean values to aid in resolution. Also including the identifier, scope, and a list of exclusions which is used for transitive dependency resolution.
     * @param ident artifact identifier
     * @param scope scope of the dependency, default is set to compile
     * @param isResolved if any parts of the identifier are missing set to false, otherwise true
     * @param isVersionRange if the version is defined by a range
     * @param optional if the dependency can be left out of resolution
     * @param exclusions list of dependencies to exclude from transitive resolution
     */
    public Dependency(ArtifactIdent ident, String scope, boolean isResolved, boolean isVersionRange, boolean optional, Set<String> exclusions) {
        this.ident = ident;
        this.scope = scope;
        this.isResolved = isResolved;
        this.isVersionRange = isVersionRange;
        this.optional = optional;
        this.exclusions = exclusions;
    }

    public Dependency(Dependency toCopy) {
        this.ident = new ArtifactIdent(toCopy.ident);
        this.scope = toCopy.scope;
        this.isResolved = toCopy.isResolved;
        this.isVersionRange = toCopy.isVersionRange;
        this.optional = toCopy.optional;
        this.exclusions = toCopy.exclusions;
    }

    /**
     * Gets the identifier.
     * @return artifactIdentifier
     */
    public ArtifactIdent getIdent() {
        return ident;
    }

    /**
     * Updates the value of the artifactIdentifier
     * @param ident new identifier value
     */
    public void setIdent(ArtifactIdent ident) {
        this.ident = ident;
    }

    /**
     * Gets the scope of the dependency.
     * @return scope value
     */
    public String getScope() {
        return scope;
    }

    /**
     * Updates the scope to the new value provided.
     * @param scope new scope value
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets if the dependency is resolved or not.
     * @return if the dependency is resolved
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Updates the resolved value
     * @param resolved new resolved value
     */
    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    /**
     * Gets if the dependency has a version range.
     * @return if there's a version range
     */
    public boolean isVersionRange() {
        return isVersionRange;
    }

    /**
     * Updates the versionRange value
     * @param versionRange the new versionRange value
     */
    public void setVersionRange(boolean versionRange) {
        isVersionRange = versionRange;
    }

    /**
     * Gets if the dependency is optional
     * @return optional value
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Gets the list of dependencies to exclude from resolution
     * @return a set of G:A identifiers to exclude from transitive dependency resolution
     */
    public Set<String> getExclusions() {
        return exclusions;
    }
}
