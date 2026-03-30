package org.tudo.sse.semver;

/**
 * Interface that represents a range of semantic version numbers. The only requirement for implementations is that they
 * must be able to decide whether any given semantic version number is contained within a range or not.
 *
 * @author Johannes Düsing
 */
public interface SemanticVersionRange {

    /**
     * Checks whether the given semantic version number falls within this range
     * @param number The number to check
     * @return True if the number is contained within this range, false otherwise
     */
    boolean contains(SemanticVersionNumber number);
}
