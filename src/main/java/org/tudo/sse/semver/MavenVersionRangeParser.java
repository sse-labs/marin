package org.tudo.sse.semver;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for semantic version ranges as specified by the Maven build system. See <a href="https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification">the specification</a>
 * for details on the supported range types and syntaxes.
 *
 * @author Johannes Düsing
 */
public class MavenVersionRangeParser {

    private static final char START_INCLUSIVE =  '[';
    private static final char END_INCLUSIVE = ']';

    private static final char START_EXCLUSIVE = '(';
    private static final char END_EXCLUSIVE = ')';

    private static final char RANGE_SEPARATOR = ',';

    private MavenVersionRangeParser(){}

    /**
     * Parses the given textual representation of a Maven version range into the corresponding version range object.
     * @param value Value to parse
     * @return Parsed version range, if successful
     * @throws SemanticVersionParsingException If either the range syntax or the version number syntax were invalid.
     */
    public static SemanticVersionRange parseRange(String value) throws SemanticVersionParsingException {
        final char[] chars = value.toCharArray();
        final StringBuilder currentValue = new StringBuilder();

        boolean inRange = false;
        boolean sawSeparator = false;
        boolean lowerInclusive = false;
        SemanticVersionNumber lowerBound = null;

        List<SemanticVersionRange> ranges = new ArrayList<>();

        for(int i = 0; i < chars.length; i++){
            char currentChar = chars[i];

            // Ignore whitespaces
            if(Character.isWhitespace(currentChar)){
                continue;
            }

            if(!inRange){
                if(currentChar == START_INCLUSIVE){
                    inRange = true;
                    lowerInclusive = true;
                } else if(currentChar == START_EXCLUSIVE){
                    inRange = true;
                    lowerInclusive = false;
                } else if(currentChar != RANGE_SEPARATOR){
                    throw new SemanticVersionParsingException(value, "Expected begin of new range or range separator but got " + currentChar, i);
                }
            } else {
                if(currentChar == RANGE_SEPARATOR){
                    String lowerBoundStr = currentValue.toString();
                    currentValue.setLength(0);

                    sawSeparator = true;

                    if(lowerBoundStr.isBlank()) lowerBound = null;
                    else lowerBound = SemanticVersionNumber.parse(lowerBoundStr);
                } else if(currentChar == END_INCLUSIVE || currentChar == END_EXCLUSIVE){
                    String upperBoundStr = currentValue.toString();
                    currentValue.setLength(0);

                    SemanticVersionNumber upperBound = null;
                    if(!upperBoundStr.isBlank()) upperBound = SemanticVersionNumber.parse(upperBoundStr);
                    boolean upperInclusive = (currentChar == END_INCLUSIVE);

                    // If we do not see a separator (e.g. range '[1.0]'), we have a hard requirement for one specific
                    // version. This means lower and upper bound are equal, and both bounds must be inclusive
                    if(!sawSeparator){
                        lowerBound = upperBound;

                        if(!lowerInclusive || !upperInclusive){
                            throw new SemanticVersionParsingException(value, "Hard requirements for one specific version must have inclusive range delimiters.", i);
                        }
                    }

                    SimpleSemanticVersionRange range = new SimpleSemanticVersionRange(lowerBound, lowerInclusive, upperBound, upperInclusive);
                    ranges.add(range);

                    inRange = false;
                    sawSeparator = false;
                } else {
                    currentValue.append(currentChar);
                }
            }
        }

        if(inRange)
            throw new SemanticVersionParsingException(value, "Input ended with range not being closed");

        if(ranges.isEmpty())
            throw new SemanticVersionParsingException(value, "No ranges found in expression: " + value);
        else if(ranges.size() == 1)
            return ranges.get(0);
        else
            return new MultiPartSemanticVersionRange(ranges);
    }
}
