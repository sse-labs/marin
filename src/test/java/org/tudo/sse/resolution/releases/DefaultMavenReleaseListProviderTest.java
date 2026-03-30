package org.tudo.sse.resolution.releases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DefaultMavenReleaseListProviderTest {

    private final IReleaseListProvider provider = DefaultMavenReleaseListProvider.getInstance();

    @Test
    @DisplayName("The Default Release List Provider must fall back to the HTML provider if no metadata file is present")
    void testNoExtraFiles(){
        try {
            List<String> versions = provider.getReleases("uk.org.retep.tools", "math");
            assertTrue(versions.size() >= 6);
            assertTrue(versions.contains("9.11.1"));
        } catch (Exception x) {
            fail(x);
        }

    }

}
