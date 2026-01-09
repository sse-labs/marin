package org.tudo.sse.semver;

import java.util.List;

/**
 * Implementation of composite semantic version ranges, which consist of multiple "smaller" range specifications. Those
 * ranges are concatenated in a way that this composite range is the union of all partial ranges - if a given version
 * number is contained in at least one of the partial ranges, it is also contained in this range.
 *
 * @author Johannes Düsing
 */
public class MultiPartSemanticVersionRange implements SemanticVersionRange {

    private final List<SemanticVersionRange> partialRanges;

    /**
     * Creates a new range with the given list of partial ranges. Containment of any given version number is checked
     * in the same order in which partial ranges a provided here.
     *
     * @param ranges List of partial ranges
     */
    public MultiPartSemanticVersionRange(List<SemanticVersionRange> ranges){
        this.partialRanges = ranges;
    }

    @Override
    public boolean contains(SemanticVersionNumber number) {
        for(SemanticVersionRange range : partialRanges){
            if(range.contains(number)) return true;
        }
        return false;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < partialRanges.size(); i++){
            SemanticVersionRange range = partialRanges.get(i);
            sb.append(range.toString());
            if(i != partialRanges.size()-1) sb.append(".");
        }

        return sb.toString();
    }
}
