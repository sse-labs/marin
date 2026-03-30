package org.tudo.sse.resolution.releases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HTMLBasedMavenReleaseListProviderTest {

    private final IReleaseListProvider provider = HTMLBasedMavenReleaseListProvider.getInstance();

    @Test
    @DisplayName("The HTML Release List Provider must extract version lists for simple cases")
    void testNoExtraFiles(){
        try {
            List<String> versions = provider.getReleases("uk.org.retep.tools", "math");
            assertTrue(versions.size() >= 6);
            assertTrue(versions.contains("9.11.1"));
        } catch (Exception x) {
            fail(x);
        }

    }

    @Test
    @DisplayName("The HTML Release List Provider must extract version lists for real use-cases")
    void testExtraFiles(){
        try {
            List<String> versions = provider.getReleases("junit", "junit");
            assertTrue(versions.size() >= 32);

            assertTrue(versions.contains("3.7"));
            assertTrue(versions.contains("4.9"));
            assertTrue(versions.contains("4.13-beta-1"));

            assertFalse(versions.contains("maven-metadata.xml"));
            assertFalse(versions.contains("maven-metadata.xml.md5"));
            assertFalse(versions.contains(".."));
        } catch (Exception x) {
            fail(x);
        }
    }

    @Test
    @DisplayName("The HTML Release List Provider must extract version lists one-element lists (with particular HTML structure)")
    void testSingletonList(){
        try {
            List<String> versions = provider.getReleases("uk.nominet", "dnsjnio");
            assertFalse(versions.isEmpty());
            assertTrue(versions.contains("1.0.3"));

        } catch (Exception x) {
            fail(x);
        }
    }
}
