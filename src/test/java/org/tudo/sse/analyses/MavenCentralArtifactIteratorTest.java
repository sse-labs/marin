package org.tudo.sse.analyses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opalj.log.GlobalLogContext$;
import org.opalj.log.OPALLogger;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfigBuilder;
import org.tudo.sse.analyses.config.InvalidConfigurationException;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.utils.MarinOpalLogger;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.tudo.sse.utils.TestUtilities.testResource;

public class MavenCentralArtifactIteratorTest {

    private final Path gavInputList = testResource("artifact-names-valid.txt");

    @BeforeAll
    static void setUp() {
        // Use global OPAL Logger - will only forward OPAL messages with level error or fatal
        OPALLogger.updateLogger(GlobalLogContext$.MODULE$, MarinOpalLogger.getGlobalLogger());
    }

    @Test
    @DisplayName("The artifact iterator must process all inputs from custom GAV lists")
    void readFromFile() throws InvalidConfigurationException {
        var config = new ArtifactAnalysisConfigBuilder()
                .withInputList(gavInputList)
                .build();

        var iterator = new MavenCentralArtifactIterator(true, false, true, config);

        int artifactCount = 0;

        while(iterator.hasNext()){
            Artifact current = iterator.next();
            assertFalse(current.hasIndexInformation());
            assertTrue(current.hasPomInformation());
            assertTrue(current.hasJarInformation());

            artifactCount++;
        }

        assertEquals(10, artifactCount);
    }

    @Test
    @DisplayName("The artifact iterator must apply pagination to custom GAV lists")
    void readFromFilePaginated() throws InvalidConfigurationException {
        var config = new ArtifactAnalysisConfigBuilder()
                .withInputList(gavInputList)
                .withSkip(2)
                .withTake(5)
                .build();

        var iterator = new MavenCentralArtifactIterator(true, true, false, config);

        ArtifactIdent firstEntry = getIdentInInputFile(0);
        ArtifactIdent secondEntry = getIdentInInputFile(1);

        int artifactCount = 0;

        while(iterator.hasNext()){
            Artifact current = iterator.next();
            assertFalse(current.hasIndexInformation());
            assertTrue(current.hasPomInformation());
            assertFalse(current.hasJarInformation());

            // Assert that we are not seeing the skipped entries
            assertNotEquals(firstEntry, current.getIdent());
            assertNotEquals(secondEntry, current.getIdent());

            artifactCount++;
        }

        assertEquals(5, artifactCount);
    }

    @Test
    @DisplayName("The artifact iterator must not produce any values on invalid input files")
    void readFromFileInvalid() throws InvalidConfigurationException {
        var config = new ArtifactAnalysisConfigBuilder()
                .withInputList(testResource("artifact-names-invalid.txt"))
                .build();

        var iterator = new MavenCentralArtifactIterator(true, true, true, config);

        // If the source file contains at least one line that is not a valid GAV triple, it must not produce any elements
        assertFalse(iterator.hasNext());
        assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test
    @DisplayName("The artifact iterator must apply pagination to the Central index")
    void readFromIndexPaginated() throws InvalidConfigurationException {
        var config = new ArtifactAnalysisConfigBuilder()
                .withSkip(2)
                .withTake(5)
                .build();

        var iterator = new MavenCentralArtifactIterator(false, false, true, config);

        ArtifactIdent firstEntry = getIdentInIndex(0);
        ArtifactIdent secondEntry = getIdentInIndex(1);

        int artifactCount = 0;

        while(iterator.hasNext()){
            Artifact current = iterator.next();
            assertTrue(current.hasIndexInformation());
            assertFalse(current.hasPomInformation());
            assertTrue(current.hasJarInformation());

            // Assert that we are not seeing the skipped entries
            assertNotEquals(firstEntry, current.getIdent());
            assertNotEquals(secondEntry, current.getIdent());

            artifactCount++;
        }

        assertEquals(5, artifactCount);
    }

    @Test
    @DisplayName("The artifact iterator must apply time range filtering to the Central index")
    void readFromIndexFiltered() throws InvalidConfigurationException {
        // We expect two entries in this range
        long since = 1106902436000L;
        long until = 1106902438001L;

        var config = new ArtifactAnalysisConfigBuilder()
                .withSinceUtil(since, until)
                .build();

        var iterator = new MavenCentralArtifactIterator(false, false, false, config);

        int artifactCount = 0;

        while(iterator.hasNext() && artifactCount < 2){
            Artifact current = iterator.next();
            assertTrue(current.hasIndexInformation());
            assertFalse(current.hasPomInformation());
            assertFalse(current.hasJarInformation());

            assertTrue(since <= current.getIndexInformation().getLastModified());
            assertTrue(current.getIndexInformation().getLastModified() <= until);

            assertEquals("ymsg", current.getIdent().getGroupID());

            artifactCount++;
        }
    }

    private ArtifactIdent getIdentInInputFile(int pos) {
        try {
            var lines = Files.readAllLines(gavInputList);

            if(pos >= lines.size()) fail("Invalid position in input list: " + pos);

            var parts = lines.get(pos).split(":");

            if(parts.length != 3) fail("Invalid GAV in input list: " + pos);

            return new ArtifactIdent(parts[0], parts[1], parts[2]);
        } catch (IOException iox) {
            fail(iox);
        }

        return null;
    }

    private ArtifactIdent getIdentInIndex(int pos){
        try {
            IndexIterator ii = new IndexIterator(MavenCentralRepository.RepoBaseURI);

            int position = 0;

            while(ii.hasNext()){
                var ident = ii.next().getIdent();
                if(position == pos){
                    return ident;
                }
                position++;
            }

            ii.closeReader();

        } catch (IOException iox) {
            fail(iox);
        }

        return null;
    }
}
