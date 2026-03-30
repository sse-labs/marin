package org.tudo.sse.analyses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opalj.log.GlobalLogContext$;
import org.opalj.log.OPALLogger;
import org.tudo.sse.analyses.config.InvalidConfigurationException;
import org.tudo.sse.analyses.config.LibraryAnalysisConfigBuilder;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.LibraryResolutionContext;
import org.tudo.sse.resolution.FileNotFoundException;
import org.tudo.sse.utils.LibraryIndexIterator;
import org.tudo.sse.utils.MarinOpalLogger;
import org.tudo.sse.utils.MavenCentralRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.tudo.sse.utils.TestUtilities.testResource;

public class MavenCentralLibraryIteratorTest {

    private static final Path validGAList = testResource("library-names-valid.txt");
    private static final Path invalidGAList = testResource("library-names-invalid.txt");
    private static final Path nonExistingGAList = testResource("library-names-not-existing.txt");

    @BeforeAll
    static void setUp() {
        // Use global OPAL Logger - will only forward OPAL messages with level error or fatal
        OPALLogger.updateLogger(GlobalLogContext$.MODULE$, MarinOpalLogger.getGlobalLogger());
    }

    @Test
    @DisplayName("The library iterator must process all inputs from custom GA lists")
    void readFromFile() throws InvalidConfigurationException {
        var config = new LibraryAnalysisConfigBuilder()
                .withInputList(validGAList)
                .build();

        var iterator = new MavenCentralLibraryIterator(true, false, false, config);

        int libraryCount = 0;

        while(iterator.hasNext()){
            final LibraryResolutionContext current = iterator.next();
            final List<Artifact> releases = current.getLibraryArtifacts();

            assertFalse(releases.isEmpty());


            for(Artifact release: releases){
                assertFalse(release.hasIndexInformation());
                assertTrue(release.hasPomInformation());
                assertFalse(release.hasJarInformation());
            }


            libraryCount++;
        }

        assertEquals(5, libraryCount);
    }

    @Test
    @DisplayName("The library iterator must apply pagination to custom GA lists")
    void readFromFilePaginated() throws InvalidConfigurationException {
        var config = new LibraryAnalysisConfigBuilder()
                .withInputList(validGAList)
                .withTake(2)
                .withSkip(2)
                .build();

        var iterator = new MavenCentralLibraryIterator(false, false, true, config);

        int libraryCount = 0;

        String firstEntry = getGaInInputFile(0);
        String secondEntry = getGaInInputFile(1);

        while(iterator.hasNext()){
            final LibraryResolutionContext current = iterator.next();
            final List<Artifact> releases = current.getLibraryArtifacts();

            assertFalse(releases.isEmpty());

            for(Artifact release: releases){
                assertFalse(release.hasIndexInformation());
                assertFalse(release.hasPomInformation());
                assertTrue(release.hasJarInformation());
            }

            // Assert that we are not seeing the skipped entries
            assertNotEquals(firstEntry, current.getLibraryGA());
            assertNotEquals(secondEntry, current.getLibraryGA());

            libraryCount++;
        }

        assertEquals(2, libraryCount);
    }

    @Test
    @DisplayName("The library iterator must not produce any values on invalid input files")
    void readFromFileInvalid() throws InvalidConfigurationException {
        var config = new LibraryAnalysisConfigBuilder()
                .withInputList(invalidGAList)
                .build();

        var iterator = new MavenCentralLibraryIterator(true, true, true, config);

        // If the source file contains at least one line that is not a valid GA tuple, it must not produce any elements
        assertFalse(iterator.hasNext());
        assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test
    @DisplayName("The library iterator must apply pagination to the Central index")
    void readFromIndexPaginated() throws InvalidConfigurationException {
        var config = new LibraryAnalysisConfigBuilder()
                .withSkip(2)
                .withTake(3)
                .build();

        var iterator = new MavenCentralLibraryIterator(true, false, false, config);

        String firstEntry = getGaInIndex(0);
        String secondEntry = getGaInIndex(1);

        int artifactCount = 0;

        while(iterator.hasNext()){
            final LibraryResolutionContext current = iterator.next();
            final List<Artifact> releases = current.getLibraryArtifacts();

            assertFalse(releases.isEmpty());

            for(Artifact release: releases){
                assertFalse(release.hasIndexInformation());
                assertTrue(release.hasPomInformation());
                assertFalse(release.hasJarInformation());
            }

            // Assert that we are not seeing the skipped entries
            assertNotEquals(firstEntry, current.getLibraryGA());
            assertNotEquals(secondEntry, current.getLibraryGA());

            artifactCount++;
        }

        assertEquals(3, artifactCount);
    }

    @Test
    @DisplayName("The library iterator must invoke the registered callback on failure")
    void setCallback() throws InvalidConfigurationException {
        var config = new LibraryAnalysisConfigBuilder()
                .withInputList(nonExistingGAList)
                .build();

        var iterator = new MavenCentralLibraryIterator(true, false, false, config);

        // Define a callback for non-existing release lists
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        iterator.setFailedLibraryCallback( event -> {
            assertEquals("no", event.getGroupId());
            assertEquals("lib", event.getArtifactId());

            Throwable rootCause = event.getCause();
            while(rootCause.getCause() != null){
                rootCause = rootCause.getCause();
            }

            assertInstanceOf(FileNotFoundException.class, rootCause);

            callbackInvoked.set(true);
        });

        while(iterator.hasNext()){
            LibraryResolutionContext ctx = iterator.next();
            assertTrue(ctx.getLibraryArtifacts().isEmpty());
        }

        assertTrue(callbackInvoked.get());
    }


    private String getGaInIndex(int pos) {
        try {
            LibraryIndexIterator libIt = new LibraryIndexIterator(MavenCentralRepository.RepoBaseURI);

            int position = 0;
            while(libIt.hasNext()){
                var libGA =  libIt.next();
                if(position == pos){
                    return libGA;
                }
                position++;
            }

            libIt.close();
        } catch(IOException iox) {
            fail(iox);
        }

        return null;
    }

    private String getGaInInputFile(int pos) {
        try {
            assertNotNull(validGAList);

            var lines = Files.readAllLines(validGAList);

            if(pos >= lines.size()) fail("Invalid position in input list: " + pos);

            return lines.get(pos);
        } catch (IOException iox) {
            fail(iox);
        }

        return null;
    }
}
