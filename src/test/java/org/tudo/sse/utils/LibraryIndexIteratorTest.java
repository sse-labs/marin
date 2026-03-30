package org.tudo.sse.utils;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

class LibraryIndexIteratorTest {

    private LibraryIndexIterator iteratorUnderTest;

    @BeforeEach
    void setIndexIterator() {
        try {
            iteratorUnderTest = new LibraryIndexIterator(new URI("https://repo1.maven.org/maven2/"));
        } catch (Exception x) { fail(x); }
    }

    @Test
    @DisplayName("The Library Index Iterator must produce valid GAs")
    void validGAFormat() {
        int cutoff = 10000;
        int idx = 0;

        while(iteratorUnderTest.hasNext() && idx < cutoff) {
            final String ga = iteratorUnderTest.next();
            final String[] parts = ga.split(":");

            assert(parts.length == 2);
            assert(!parts[0].isBlank() && !parts[1].isBlank());

            idx += 1;

            if(idx % 100 == 0){
                System.out.println("Got " + idx + " unique library names so far");
            }
        }
    }

    @Test
    @DisplayName("The Library Index Iterator must produce unique GAs")
    void uniqueGAs() {
        int cutoff = 100000;
        Set<String> gasSeen = new HashSet<>();

        while(iteratorUnderTest.hasNext() && gasSeen.size() < cutoff) {
            final String ga = iteratorUnderTest.next();

            assert(!gasSeen.contains(ga));

            gasSeen.add(ga);

            if(gasSeen.size() % 1000 == 0){
                System.out.println("Got " + gasSeen.size() + " unique library names so far");
            }
        }
    }

    @Test
    @Disabled
    @DisplayName("The Library Index Iterator must terminate")
    void terminate() {
        int idx = 0;
        while(iteratorUnderTest.hasNext()){
            iteratorUnderTest.next();
            idx += 1;

            if(idx % 10000 == 0){
                System.out.println("Got " + idx + " unique library names so far");
            }
        }
        // There are more than 600.000 libraries on maven central
        assert(idx > 600000);
    }


    @AfterEach
    void resetIndexIterator() {
        if(iteratorUnderTest != null) {
            try { iteratorUnderTest.close(); } catch (Exception x) { fail (x); }
        }
        iteratorUnderTest = null;
    }

}
