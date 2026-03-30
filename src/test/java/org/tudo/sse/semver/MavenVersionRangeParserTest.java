package org.tudo.sse.semver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for version range parsing are based on <a href="https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification">the specification</a>
 */
public class MavenVersionRangeParserTest {

    @Test
    @DisplayName("The parser must parse hard requirements correctly")
    void testHardRequirement_Valid(){
        SimpleSemanticVersionRange simpleRange = assertSimpleRange("[1.0]");

        assertNotNull(simpleRange);
        assertTrue(simpleRange.isHardRequirement());
        assertEquals("1.0.0", simpleRange.getLowerBound().toString());
    }

    @Test
    @DisplayName("The parser must parse semi-open simple ranges like (,1.0]")
    void testSemiRange1(){
        SimpleSemanticVersionRange simpleRange = assertSimpleRange("(,1.0]");
        assertNotNull(simpleRange);
        assertFalse(simpleRange.hasLowerBound());
        assertTrue(simpleRange.hasUpperBound());
        assertFalse(simpleRange.isLowerBoundInclusive());
        assertTrue(simpleRange.isUpperBoundInclusive());
        assertEquals("1.0.0", simpleRange.getUpperBound().toString());
    }

    @Test
    @DisplayName("The parser must parse regular simple ranges")
    void testFullRange1(){
        SimpleSemanticVersionRange simpleRange = assertSimpleRange("[1.2,1.3.3-Preview+Snapshot.1]");
        assertNotNull(simpleRange);
        assertTrue(simpleRange.hasLowerBound());
        assertTrue(simpleRange.hasUpperBound());
        assertTrue(simpleRange.isLowerBoundInclusive());
        assertTrue(simpleRange.isUpperBoundInclusive());
        assertTrue(simpleRange.getUpperBound().hasPreRelease());
        assertTrue(simpleRange.getUpperBound().getBuildMetadata().endsWith(".1"));
    }

    @Test
    @DisplayName("The parser must parse inclusivity correctly for simple ranges")
    void testFullRange2(){
        SimpleSemanticVersionRange simpleRange = assertSimpleRange("[1.0,   2.0)");
        assertNotNull(simpleRange);
        assertTrue(simpleRange.hasLowerBound());
        assertTrue(simpleRange.isLowerBoundInclusive());
        assertTrue(simpleRange.hasUpperBound());
        assertFalse(simpleRange.isUpperBoundInclusive());
        assertEquals(2, simpleRange.getUpperBound().getMajorVersion());
    }

    @Test
    @DisplayName("The parser must parse semi-open simple ranges like [1.5,)")
    void testSemiRange2(){
        SimpleSemanticVersionRange simpleRange = assertSimpleRange("[1.5,)");
        assertNotNull(simpleRange);
        assertTrue(simpleRange.hasLowerBound());
        assertTrue(simpleRange.isLowerBoundInclusive());
        assertFalse(simpleRange.hasUpperBound());
        assertFalse(simpleRange.isUpperBoundInclusive());
        assertEquals(5, simpleRange.getLowerBound().getMinorVersion());
    }

    @Test
    @DisplayName("The parser must parse complex composite ranges correctly")
    void complexRange1(){
        MultiPartSemanticVersionRange multiRange = assertMultiRange("(,1.0],[1.2,)");
        assertNotNull(multiRange);

        var notContained = asSemVer("1.0.1");
        var contained1 = asSemVer("0.0.1");
        var contained2 = asSemVer("1.2");

        assertFalse(multiRange.contains(notContained));
        assertTrue(multiRange.contains(contained1));
        assertTrue(multiRange.contains(contained2));
    }

    @Test
    @DisplayName("The parser must handle inverse hard requirements correctly")
    void testInverseHardRequirement(){
        MultiPartSemanticVersionRange multiRange = assertMultiRange("(,1.1),(1.1,)");
        assertNotNull(multiRange);

        var notContained = asSemVer("1.1");
        var contained = asSemVer("1.2");

        assertFalse(multiRange.contains(notContained));
        assertTrue(multiRange.contains(contained));
    }


    private SimpleSemanticVersionRange assertSimpleRange(String versionRange){
        try {
            var range = MavenVersionRangeParser.parseRange(versionRange);

            assertInstanceOf(SimpleSemanticVersionRange.class, range);

            return (SimpleSemanticVersionRange)range;
        } catch (SemanticVersionParsingException svpx){
            fail(svpx);
        }
        return null;
    }

    private MultiPartSemanticVersionRange assertMultiRange(String versionRange){
        try {
            var range = MavenVersionRangeParser.parseRange(versionRange);

            assertInstanceOf(MultiPartSemanticVersionRange.class, range);

            return (MultiPartSemanticVersionRange) range;
        } catch (SemanticVersionParsingException svpx){
            fail(svpx);
        }
        return null;
    }

    private SemanticVersionNumber asSemVer(String value){
        try {
            return SemanticVersionNumber.parse(value);
        } catch (SemanticVersionParsingException svpx){
            fail(svpx);
        }
        return null;
    }
}
