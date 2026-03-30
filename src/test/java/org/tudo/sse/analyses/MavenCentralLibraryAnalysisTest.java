package org.tudo.sse.analyses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.LibraryAnalysisConfig;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.analyses.config.parsing.LibraryAnalysisConfigParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.tudo.sse.utils.TestUtilities.testResource;

public class MavenCentralLibraryAnalysisTest {

    @Test
    @DisplayName("The CLI parser must parse common argument values correctly")
    void parseCLIRegular(){
        final String validArgs = "--skip-take 20:10000 --progress-restore-file pom.xml --progress-output-file marin-progress --threads 12 --save-progress-interval 20";
        final LibraryAnalysisConfig config = parseCLI(validArgs);

        assert(config.multipleThreads);
        assertEquals(12, config.threadCount);
        assertEquals(Paths.get("pom.xml"), config.progressRestoreFile);
        assertEquals(Paths.get("marin-progress"), config.progressOutputFile);
        assertEquals(20, config.skip);
        assertEquals(10000, config.take);
        assertEquals(20, config.progressWriteInterval);
        assertNull(config.inputListFile);
    }

    @Test
    @DisplayName("The CLI parser must parse common argument values correctly using shorthand argument names")
    void parseCLIShorthands(){
        final String validArgs = "-st 20:10000 -prf pom.xml -pof marin-progress -t 12 -spi 20";
        final LibraryAnalysisConfig config = parseCLI(validArgs);

        assert(config.multipleThreads);
        assertEquals(12, config.threadCount);
        assertEquals(Paths.get("pom.xml"), config.progressRestoreFile);
        assertEquals(Paths.get("marin-progress"), config.progressOutputFile);
        assertEquals(20, config.skip);
        assertEquals(10000, config.take);
        assertEquals(20, config.progressWriteInterval);
        assertNull(config.inputListFile);
    }



    @Test
    @DisplayName("The CLI parser must set the correct defaults for empty arguments")
    void parseCLIDefaults(){
        final LibraryAnalysisConfig config = parseCLI("");

        assertFalse(config.multipleThreads);
        assertEquals(1, config.threadCount);
        assertNull(config.progressRestoreFile);
        assertEquals(Paths.get("marin-progress"), config.progressOutputFile);
        assertEquals(-1, config.skip);
        assertEquals(-1, config.take);
        assertEquals(100, config.progressWriteInterval);
        assertNull(config.inputListFile);
    }

    @Test
    @DisplayName("The CLI parser must fail if input files do not exist")
    void parseNonExistingFile(){
        final String validArgs = "--progress-restore-file foo.input";

        assertThrows(RuntimeException.class, () -> parseCLI(validArgs));
    }

    @Test
    @DisplayName("The CLI parser must accept pagination on custom GA input lists")
    void parseCustomGA(){
        final String validArgs = "-st 20:10000 --inputs pom.xml";

        final LibraryAnalysisConfig config = parseCLI(validArgs);

        assertEquals(Paths.get("pom.xml"), config.inputListFile);
        assertEquals(20, config.skip);
    }

    @Test
    @DisplayName("The CLI parser must accept setting an index position on a custom GA input list")
    void parseNonExistingIndex(){
        final String validArgs = "--progress-restore-file pom.xml --inputs pom.xml";

        final LibraryAnalysisConfig config = parseCLI(validArgs);

        assertEquals(Paths.get("pom.xml"), config.inputListFile);
        assertEquals(Paths.get("pom.xml"), config.progressRestoreFile);
    }

    @Test
    @DisplayName("An analysis with no requirements must not produce information instances")
    void simpleIndexAnalysis() throws IOException {

        final List<String> librariesHit = new java.util.ArrayList<>();

        final Path validLibraryInput = testResource("library-names-valid.txt");

        assertNotNull(validLibraryInput);
        assert(Files.exists(validLibraryInput));

        final List<String> expectedLibraries = Files.readAllLines(validLibraryInput);

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNull(releases.get(0).getPomInformation());
                assertNull(releases.get(0).getJarInformation());
                librariesHit.add(libraryGA);
            }
        };

        final String args = "-st 2:2 --inputs " + validLibraryInput.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(2, librariesHit.size());

        assertTrue(librariesHit.contains(expectedLibraries.get(2)));
        assertTrue(librariesHit.contains(expectedLibraries.get(3)));
    }

    @Test
    @DisplayName("An analysis with JAR requirements must produce JAR information instances")
    void simpleIndexWithJarAnalysis() throws IOException {

        final List<String> librariesHit = new java.util.ArrayList<>();
        final Path validLibraryInput = testResource("library-names-valid.txt");

        assertNotNull(validLibraryInput);
        assert(Files.exists(validLibraryInput));

        final List<String> expectedLibraries = Files.readAllLines(validLibraryInput);

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, true) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNull(releases.get(0).getPomInformation());
                assertNotNull(releases.get(0).getJarInformation());
                librariesHit.add(libraryGA);
            }
        };

        final String args = "-st 2:2 --inputs " + validLibraryInput.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(2, librariesHit.size());

        assertTrue(librariesHit.contains(expectedLibraries.get(2)));
        assertTrue(librariesHit.contains(expectedLibraries.get(3)));
    }

    @Test
    @DisplayName("An analysis with POM requirements must produce POM information instances")
    void simpleIndexWithPomAnalysis() throws IOException{

        final List<String> librariesHit = new java.util.ArrayList<>();

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(true, true, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNotNull(releases.get(0).getPomInformation());
                assertNull(releases.get(0).getJarInformation());
                librariesHit.add(libraryGA);
            }

            @Override
            protected void onVersionListError(String g, String a, Exception x){
                assertFalse(x.getCause().getMessage().contains("was not found"));
                String ga = g+":"+a;
                librariesHit.add(ga);
            }
        };

        final String args = "-st 5000:10 --save-progress-interval 5";
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(10, librariesHit.size());
        assertFalse(librariesHit.contains("yom:yom"));
        assertFalse(librariesHit.contains("yan:yan"));

        long progress = Long.parseLong(Files.readString(analysis.config.progressOutputFile));

        assert(progress >= 5005);
    }

    @Test
    @DisplayName("An analysis with multithreading must not miss any libraries")
    void parallelIndexAnalysis() throws IOException{

        final AtomicInteger count = new AtomicInteger(0);

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNull(releases.get(0).getPomInformation());
                assertNull(releases.get(0).getJarInformation());
                count.incrementAndGet();
            }

            @Override
            protected void onVersionListError(String g, String a, Exception x){

                assertFalse(x.getCause().getMessage().contains("was not found"));
                count.incrementAndGet();
            }
        };

        final String args = "-st 5000:500 --threads 8";
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(500, count.get());

        long progress = Long.parseLong(Files.readString(analysis.config.progressOutputFile));

        // Skip values must be part of the progress! Last progress write must have been somewhere after 5400 (interval 100)
        assert(progress >= 5400);
    }

    @Test
    @DisplayName("An analysis with input file must not miss any libraries")
    void analysisFromFile() throws IOException {

        final Path validLibraryInput = testResource("library-names-valid.txt");

        assertNotNull(validLibraryInput);
        assert(Files.exists(validLibraryInput));

        final List<String> expectedLibraries = Files.readAllLines(validLibraryInput);

        final List<String> librariesHit = new java.util.ArrayList<>();

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNull(releases.get(0).getPomInformation());
                assertNull(releases.get(0).getJarInformation());
                librariesHit.add(libraryGA);
            }
        };

        final String args = "--inputs " + validLibraryInput.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(expectedLibraries, librariesHit);
    }

    @Test
    @DisplayName("An analysis with input file must allow pagination")
    void analysisFromFileWithPagination() throws IOException {

        final Path validLibraryInput = testResource("library-names-valid.txt");

        assertNotNull(validLibraryInput);
        assert(Files.exists(validLibraryInput));

        final List<String> expectedLibraries = Files.readAllLines(validLibraryInput);

        final List<String> librariesHit = new java.util.ArrayList<>();

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                assertFalse(releases.isEmpty());
                assertNull(releases.get(0).getIndexInformation());
                assertNull(releases.get(0).getPomInformation());
                assertNull(releases.get(0).getJarInformation());
                librariesHit.add(libraryGA);
            }
        };

        final String args = "-st 1:2 --inputs " + validLibraryInput.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        assertEquals(2, librariesHit.size());
        assert(librariesHit.contains(expectedLibraries.get(1)));
        assert(librariesHit.contains(expectedLibraries.get(2)));
    }

    @Test
    @DisplayName("An analysis with input file must not fail on invalid inputs")
    void analysisFromInvalidFile() {

        final Path validLibraryInput = testResource("library-names-invalid.txt");

        assertNotNull(validLibraryInput);
        assert (Files.exists(validLibraryInput));

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                fail("Analysis should not be executed on invalid input: " + libraryGA);
            }
        };

        final String args = "--inputs " + validLibraryInput.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);
    }

    @Test
    @DisplayName("An analysis must apply pagination on top of progress restore values")
    void analysisWithRestoreAndPagination() {

        final Path restoreFile = testResource("testingIndexPosition.txt");

        assertNotNull(restoreFile);
        assert (Files.exists(restoreFile));

        final List<String> librariesHit = new java.util.ArrayList<>();

        final MavenCentralLibraryAnalysis analysis = new MavenCentralLibraryAnalysis(false, false, false) {
            @Override
            protected void analyzeLibrary(String libraryGA, List<Artifact> releases) {
                librariesHit.add(libraryGA);
            }
        };

        final String args = "-st 0:5 --progress-restore-file " + restoreFile.toAbsolutePath();
        final String[] argsArray = args.split(" ");

        analysis.runAnalysis(argsArray);

        // When restoring progress - even though we do not skip anything - we do not want to see the first index entry!
        assertEquals(5, librariesHit.size());
        assertFalse(librariesHit.contains("yom:yom"));
    }


    private LibraryAnalysisConfig parseCLI(String cli) {
        try {
            if(cli.isBlank()) return new LibraryAnalysisConfigParser().parseCommonConfig(new String[]{});
            else return new LibraryAnalysisConfigParser().parseCommonConfig(cli.split(" "));
        } catch(CLIException clix) {
            throw new RuntimeException(clix);
        }
    }

}
