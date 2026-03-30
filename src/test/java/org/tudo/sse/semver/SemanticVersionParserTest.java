package org.tudo.sse.semver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SemanticVersionParserTest {

    private final String[] invalidSyntaxNumbers = new String []{"1a.2.3", "1.2.3.4", "foo", "1+a-n+c", "1..2", "1.0.0-"};
    private final String integerOverflow = "2131231231231231231231231231231231231231231231.1";

    private final String[] validNumbersSimple = new String[]{"1.0.0", "3.0.0", "1.2.0", "1.1.1", "1054.20000.1"};
    private final int[][] validNumbersSimple_Expected = new int[][]{new int[]{1,0,0}, new int[]{3,0,0}, new int[]{1,2,0}, new int[]{1,1,1}, new int[]{1054,20000,1}};

    private final String[] validNumbersComplex = new String[]{"1-a-valid-prerelase123+000000", "1.2.3+0a-def-----", "12-0+1", "0.0.0-0+0"};
    private final int[][] validNumbersComplex_Expected_Numbers = new int[][]{new int[]{1,0,0}, new int[]{1,2,3}, new int[]{12,0,0}, new int[]{0,0,0}};
    private final String[][] validNumbersComplex_Expected_Data = new String[][]{new String[]{"a-valid-prerelase123", "000000"}, new String[]{null, "0a-def-----"}, new String[]{"0","1"}, new String[]{"0", "0"}};

    @Test
    @DisplayName("Invalid syntax should lead to parsing exceptions")
    public void parseInvalidSyntax(){
        for(String invalidSyntaxNumber: invalidSyntaxNumbers){
            assertThrows(SemanticVersionParsingException.class, () -> SemanticVersionNumber.parse(invalidSyntaxNumber));
        }
    }

    @Test
    @DisplayName("Integer overflows should lead to parsing exceptions")
    public void parseOverflow(){
        assertThrows(SemanticVersionParsingException.class, () -> SemanticVersionNumber.parse(integerOverflow));
    }

    @Test
    @DisplayName("Simple numbers should be parsed without exception")
    public void parseSimpleNumbers(){
        try {
            for(int i = 0; i < validNumbersSimple.length; i++){
                var semVer = SemanticVersionNumber.parse(validNumbersSimple[i]);
                var expected =  validNumbersSimple_Expected[i];

                assertEquals(expected[0], semVer.getMajorVersion());
                assertEquals(expected[1], semVer.getMinorVersion());
                assertEquals(expected[2], semVer.getPatchVersion());
            }
        } catch (SemanticVersionParsingException svpx) {
            fail(svpx);
        }
    }

    @Test
    @DisplayName("Complex numbers should be parsed without exception")
    public void parseComplexNumbers(){
        try {
            for(int i = 0; i < validNumbersComplex.length; i++){
                var semVer = SemanticVersionNumber.parse(validNumbersComplex[i]);
                var expectedNumbers = validNumbersComplex_Expected_Numbers[i];
                var expectedData = validNumbersComplex_Expected_Data[i];

                assertEquals(expectedNumbers[0], semVer.getMajorVersion());
                assertEquals(expectedNumbers[1], semVer.getMinorVersion());
                assertEquals(expectedNumbers[2], semVer.getPatchVersion());

                assertEquals(expectedData[0], semVer.getPreRelease());
                assertEquals(expectedData[1], semVer.getBuildMetadata());
            }
        } catch(SemanticVersionParsingException svpx){
            fail(svpx);
        }
    }

}
