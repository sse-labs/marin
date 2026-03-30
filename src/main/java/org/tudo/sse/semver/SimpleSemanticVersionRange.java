package org.tudo.sse.semver;

/**
 * Simple implementation of semantic version ranges. A simple range is defined by two version numbers, a lower bound and
 * an upper bound. Both are optional, in which case we have (semi-) open ranges. Bounds are either inclusive or exclusive.
 *
 * @author Johannes Düsing
 */
public class SimpleSemanticVersionRange implements SemanticVersionRange {

    private final SemanticVersionNumber lowerBound;
    private final SemanticVersionNumber upperBound;
    private final boolean lowerBoundInclusive;
    private final boolean upperBoundInclusive;

    /**
     * Creates a new simple semantic version range with the given bounds and their inclusive-specification. Use null to
     * indicate that one (or both) bounds do not exist. If a bound is not set, its 'inclusive' value is ignored.
     * @param lowerBound The lower bound for this range, or null if there is no lower bound.
     * @param lowerBoundInclusive Whether the lower bound is inclusive. Ignored if there is no lower bound.
     * @param upperBound The upper bound for this range, or null if there is no upper bound.
     * @param upperBoundInclusive Whether the upper bound is inclusive. Ignored if there is no upper bound.
     */
    public SimpleSemanticVersionRange(SemanticVersionNumber lowerBound,
                                      boolean lowerBoundInclusive,
                                      SemanticVersionNumber upperBound,
                                      boolean upperBoundInclusive) {
        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;
    }

    /**
     * Creates a new simple semantic version range that is semi-open, i.e. a range that only has a lower bound.
     * @param lowerBound Lower bound for this range.
     * @param lowerBoundInclusive Whether the lower bound is inclusive.
     * @return Semi-Open range with only a lower bound
     */
    public static SimpleSemanticVersionRange fromLowerBound(SemanticVersionNumber lowerBound, boolean lowerBoundInclusive) {
        return new SimpleSemanticVersionRange(lowerBound, lowerBoundInclusive, null, false);
    }

    /**
     * Creates a new simple semantic version range that is semi-open, i.e. a range that only has an upper bound.
     * @param upperBound Upper bound for this range.
     * @param upperBoundInclusive Whether the upper bound is inclusive.
     * @return Semi-Open range with only an upper bound
     */
    public static SimpleSemanticVersionRange fromUpperBound(SemanticVersionNumber upperBound, boolean upperBoundInclusive) {
        return new SimpleSemanticVersionRange(null, false,  upperBound, upperBoundInclusive);
    }

    /**
     * Checks whether this range has a lower bound.
     * @return True if lower bound exists.
     */
    public boolean hasLowerBound() {
        return lowerBound != null;
    }

    /**
     * Checs whether this range has an upper bound.
     * @return True if upper bound exists.
     */
    public boolean hasUpperBound() {
        return this.upperBound != null;
    }

    /**
     * Checks whether the lower bound of this range is inclusive.
     * @return True if lower bound is inclusive, false if exclusive.
     */
    public boolean isLowerBoundInclusive() {
        return lowerBoundInclusive;
    }

    /**
     * Checks whether the upper bound of this range is inclusive.
     * @return True if upper bound is inclusive, false if exclusive.
     */
    public boolean isUpperBoundInclusive() {
        return upperBoundInclusive;
    }

    /**
     * Gets the lower bound for this range.
     * @return Lower bound, or null if non exists
     */
    public SemanticVersionNumber getLowerBound() {
        return lowerBound;
    }

    /**
     * Gets the upper bound for this range.
     * @return Upper bound, or null if non exists.
     */
    public SemanticVersionNumber getUpperBound() {
        return upperBound;
    }

    /**
     * Checks whether this range represents a (Maven Central) hard requirement. A hard requirement is a special range
     * where lower and upper bound are equal, and both are inclusive. This range contains exactly one version number.
     * @return True if this range is a hard requirement
     */
    public boolean isHardRequirement(){
        return this.lowerBound.equals(this.upperBound);
    }

    @Override
    public boolean contains(SemanticVersionNumber number) {
        if(hasLowerBound()){
            int compareResult = number.compareTo(lowerBound);
            if(compareResult < 0 || (!isLowerBoundInclusive() && compareResult == 0)) return false;
        }

        if(hasUpperBound()){
            int compareResult = number.compareTo(upperBound);
            if(compareResult > 0 || (!isUpperBoundInclusive() && compareResult == 0)) return false;
        }

        return true;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        if(isLowerBoundInclusive())
            sb.append('[');
        else
            sb.append('(');

        if(hasLowerBound())
            sb.append(lowerBound.toString());

        if(!isHardRequirement()){
            sb.append(',');
            sb.append(' ');
            if(hasUpperBound())
                sb.append(upperBound.toString());
        }

        if(isUpperBoundInclusive())
            sb.append(']');
        else
            sb.append(')');

        return sb.toString();
    }
}
