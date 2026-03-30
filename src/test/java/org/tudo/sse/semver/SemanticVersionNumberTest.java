package org.tudo.sse.semver;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SemanticVersionNumberTest {

    private final String[] validNumbersSimple = new String[]{"1.0.0", "3.0.0", "1.2.0", "1.1.1", "1054.20000.1"};
    private final int[] validNumbersSimple_Rank = new int[] {0, 3, 2, 1, 4};

    private final String[] preReleasesSimple = new String[]{"1.0.0", "1.0.0-gamma-1", "1.0.0-beta", "1.0.0-alpha-1"};
    private final int[] preReleasesSimple_Rank = new int[] {3, 2, 1, 0};

    // This test case is taken from: https://semver.org/#spec-item-11
    private final String[] preReleaseParts = new String[]{"1.0.0", "1.0.0-rc.1", "1.0.0-beta.11", "1.0.0-beta.2", "1.0.0-beta", "1.0.0-alpha.beta", "1.0.0-alpha.1", "1.0.0-alpha"};
    private final int[] preReleaseParts_Rank = new int[] {7,6,5,4,3,2,1,0};

    @Test
    @DisplayName("Simple version numbers must be sorted correctly")
    public void sortSimple(){
        assertSorted(validNumbersSimple, validNumbersSimple_Rank);
    }

    @Test
    @DisplayName("Prereleases must be sorted correctly")
    public void sortPrereleases(){
        assertSorted(preReleasesSimple, preReleasesSimple_Rank);
    }

    @Test
    @DisplayName("Prerelease parts must be parsed and sorted correctly")
    public void sortPrereleaseParts(){
        assertSorted(preReleaseParts, preReleaseParts_Rank);
    }

    private void assertSorted(String[] numbers, int[] ranks){
        var actual = sortSemantic(numbers);

        for(int i = 0; i < numbers.length; i++){
            var expectedNum = numbers[i];
            var expectedPos = ranks[i];

            assertEquals(expectedNum, actual.get(expectedPos).toString());
        }
    }

    private List<SemanticVersionNumber> sortSemantic(String[] numbers) {
        List<SemanticVersionNumber> list = new ArrayList<>();

        for(String number : numbers){
            var semVerOpt = SemanticVersionNumber.tryParse(number);
            assert(semVerOpt.isPresent());
            list.add(semVerOpt.get());
        }

        Collections.sort(list);

        return list;
    }

}
