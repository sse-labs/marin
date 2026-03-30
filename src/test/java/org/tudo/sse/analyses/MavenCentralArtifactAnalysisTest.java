package org.tudo.sse.analyses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tudo.sse.CLIException;
import org.tudo.sse.analyses.config.ArtifactAnalysisConfig;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.Package;
import org.tudo.sse.model.pom.License;
import org.tudo.sse.model.pom.PomInformation;
import org.tudo.sse.analyses.config.parsing.ArtifactAnalysisConfigParser;
import org.tudo.sse.utils.IndexIterator;
import org.tudo.sse.utils.MavenCentralAnalysisFactory;
import scala.Tuple2;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class MavenCentralArtifactAnalysisTest {
    final MavenCentralArtifactAnalysis analysisUnderTest = MavenCentralAnalysisFactory.buildEmptyAnalysisWithIndexRequirement();
    final String base = "https://repo1.maven.org/maven2/";
    final Map<String, Object> json;
    final Gson gson = new Gson();

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("MavenAnalysis.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @Test
    @DisplayName("The CLI parser must parse common argument values correctly")
    void parseCLIRegular() throws IOException{
        Path tmpDir = Files.createTempDirectory("maven-resolution-files");

        ArtifactAnalysisConfig[] configs = new  ArtifactAnalysisConfig[]{
                parseCLI("--skip-take 500:223"),
                parseCLI("--inputs src/test/resources/localPom.xml"),
                parseCLI("--progress-restore-file src/test/resources/localPom.xml --inputs src/test/resources/localPom.xml"),
                parseCLI("--since-until 53245:53246"),
                parseCLI("--inputs src/test/resources/localPom.xml --progress-restore-file src/test/resources/localPom.xml"),
                parseCLI("--progress-restore-file src/test/resources/localPom.xml"),
                parseCLI("--output " + tmpDir.toString())
        };

        List<List<String>> expected = (List<List<String>>) json.get("cliParsePos");

        for(int i = 0; i < configs.length; i++){
            final List<String> currExpected = expected.get(i);
            final ArtifactAnalysisConfig currConfig = configs[i];

            assertEquals(asInt(currExpected.get(0)), currConfig.skip);
            assertEquals(asInt(currExpected.get(1)), currConfig.take);
            assertEquals(asInt(currExpected.get(2)), currConfig.since);
            assertEquals(asInt(currExpected.get(3)), currConfig.until);

            final String expectedInputFile = currExpected.get(4);

            if(expectedInputFile.equalsIgnoreCase("null")){
                assertNull(currConfig.inputListFile);
            } else {
                assertEquals(expectedInputFile, currConfig.inputListFile.toString().replace("\\","/"));
            }

            final String expectedIndexFile = currExpected.get(5);

            if(expectedIndexFile.equalsIgnoreCase("null")){
                assertNull(currConfig.progressRestoreFile);
            } else {
                assertEquals(expectedIndexFile, currConfig.progressRestoreFile.toString().replace("\\","/"));
            }

            assertEquals(Boolean.parseBoolean(currExpected.get(6)), currConfig.outputEnabled);
        }
    }

    @Test
    @DisplayName("The CLI parser must parse timestamps YYYY-MM-DD format")
    void parseCLITimestamps1() {
        var config = parseCLI("-su 2025-12-01:2025-12-31");

        var since = asDate(config.since);
        var until = asDate(config.until);

        assertEquals(2025, since.getYear());
        assertEquals(12, since.getMonthValue());
        assertEquals(1, since.getDayOfMonth());
        assertEquals(0, since.getHour());
        assertEquals(0, since.getMinute());

        assertEquals(2025, until.getYear());
        assertEquals(12, until.getMonthValue());
        assertEquals(31, until.getDayOfMonth());
        assertEquals(23, until.getHour());
        assertEquals(59, until.getMinute());
    }

    @Test
    @DisplayName("The CLI parser must parse UNIX timestamps")
    void parseCLITimestamps2() {
        var config = parseCLI("-su 2010-10-10:1321009871");

        var since = asDate(config.since);
        var until = asDate(config.until);

        assertEquals(1321009871000L, config.until);

        assertEquals(2010, since.getYear());
        assertEquals(10, since.getMonthValue());
        assertEquals(10, since.getDayOfMonth());
        assertEquals(0, since.getHour());
        assertEquals(0, since.getMinute());

        assertEquals(2011, until.getYear());
        assertEquals(11, until.getMonthValue());
        assertEquals(11, until.getDayOfMonth());
        assertEquals(11, until.getHour());
        assertEquals(11, until.getMinute());
        assertEquals(11, until.getSecond());
    }

    @Test
    @DisplayName("The CLI parser must reject invalid ranges")
    void parseCLIInvalidRanges() {
        try {
            parseCLI("-su 1321006271:2010-10-10");
            fail("The CLI parser must reject invalid ranges");
        } catch(Exception x){
            assertInstanceOf(RuntimeException.class, x);
            assertInstanceOf(CLIException.class, x.getCause());
        }
    }

    @Test
    @DisplayName("The CLI parser must parse common argument values correctly using shorthand argument names")
    void parseCLIShorthands() throws IOException{
        Path tmpDir = Files.createTempDirectory("maven-resolution-files");

        ArtifactAnalysisConfig[] configs = new  ArtifactAnalysisConfig[]{
                parseCLI("-st 500:223"),
                parseCLI("-i src/test/resources/localPom.xml"),
                parseCLI("-prf src/test/resources/localPom.xml -i src/test/resources/localPom.xml"),
                parseCLI("-su 53245:53246"),
                parseCLI("-i src/test/resources/localPom.xml -prf src/test/resources/localPom.xml"),
                parseCLI("-prf src/test/resources/localPom.xml"),
                parseCLI("-o " + tmpDir.toString())
        };

        List<List<String>> expected = (List<List<String>>) json.get("cliParsePos");

        for(int i = 0; i < configs.length; i++){
            final List<String> currExpected = expected.get(i);
            final ArtifactAnalysisConfig currConfig = configs[i];

            assertEquals(asInt(currExpected.get(0)), currConfig.skip);
            assertEquals(asInt(currExpected.get(1)), currConfig.take);
            assertEquals(asInt(currExpected.get(2)), currConfig.since);
            assertEquals(asInt(currExpected.get(3)), currConfig.until);

            final String expectedInputFile = currExpected.get(4);

            if(expectedInputFile.equalsIgnoreCase("null")){
                assertNull(currConfig.inputListFile);
            } else {
                assertEquals(expectedInputFile, currConfig.inputListFile.toString().replace("\\","/"));
            }

            final String expectedIndexFile = currExpected.get(5);

            if(expectedIndexFile.equalsIgnoreCase("null")){
                assertNull(currConfig.progressRestoreFile);
            } else {
                assertEquals(expectedIndexFile, currConfig.progressRestoreFile.toString().replace("\\","/"));
            }

            assertEquals(Boolean.parseBoolean(currExpected.get(6)), currConfig.outputEnabled);
        }
    }

    @Test
    @DisplayName("The CLI parser must reject invalid inputs")
    void parseCmdLineNegative() {
        List<String> cliInputs = new ArrayList<>();
        cliInputs.add("--skip-take 500:223:3");
        cliInputs.add("--inputs -/xcd/");
        cliInputs.add("-st 88880000 -su --inputs path/to/file -ip path/to/file");
        cliInputs.add("--inputs");
        cliInputs.add("--since-until 53245:13243");


        for(String input : cliInputs) {
            assertThrows(RuntimeException.class, () -> parseCLI(input));
        }
    }

    @Test
    @DisplayName("An analysis must adhere to pagination configurations")
    void walkPaginated() {

        final List<Artifact> artifactsSeen = new ArrayList<>();
        final MavenCentralArtifactAnalysis theAnalysis = new MavenCentralArtifactAnalysis(true, false, false, false) {
            @Override
            public void analyzeArtifact(Artifact current) {
                artifactsSeen.add(current);
            }
        };

        List<Tuple2<Integer, Integer>> inputs = new ArrayList<>();
        inputs.add(new Tuple2<>(500, 10));
        inputs.add(new Tuple2<>(0, 10));
        inputs.add(new Tuple2<>(50000, 100));
        inputs.add(new Tuple2<>(763, 20));

        for(Tuple2<Integer, Integer> input : inputs) {
            int skip = input._1;
            int take = input._2;

            try {
                IndexIterator iterator = new IndexIterator(new URI(base), skip);
                theAnalysis.walkPaginated(take, iterator);

                assertFalse(artifactsSeen.isEmpty());
                assertEquals(take, artifactsSeen.size());

                for(Artifact a : artifactsSeen){
                    assertTrue(a.hasIndexInformation());
                    assertTrue(a.getIndexInformation().getIndex() >= (skip - 1));
                }

                artifactsSeen.clear();
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @DisplayName("An analysis must store its last processed index regardless of the store interval")
    void indexProcessorProgress() {

        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"-st", "200:70"};
        cliInputs.add(args);
        args = new String[] {"-st", "499:11", "-pof", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"-st", "500:10"};
        cliInputs.add(args);
        args = new String[]{"-st", "275:70", "-pof", "src/test/resources/stop.txt", "-spi", "1000000"};
        cliInputs.add(args);

        int[] expectedEndings = {270, 510, 510, 345};

        int i = 0;
        for(String[] arg : cliInputs) {
            MavenCentralArtifactAnalysis tester = MavenCentralAnalysisFactory.buildEmptyAnalysisWithNoRequirements();
            tester.runAnalysis(arg);

            Path indexPath = tester.getSetupInfo().progressOutputFile;
            assert(indexPath != null);
            long ending = getEndingIndex(indexPath);

            assertEquals(expectedEndings[i], ending);

            i++;
        }
    }

    @Test
    @DisplayName("An analysis must correctly apply CLI options when reading custom input lists")
    void readIdentsIn() {
        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"--inputs", "src/test/resources/artifact-names-valid.txt"};
        cliInputs.add(args);
        args = new String[] {"--inputs", "src/test/resources/artifact-names-valid.txt", "-pof", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"--inputs", "src/test/resources/artifact-names-valid.txt", "-st", "4:5"};
        cliInputs.add(args);
        args = new String[]{"--inputs", "src/test/resources/artifact-names-valid.txt", "-st", "4:5", "-pof", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"--inputs", "src/test/resources/artifact-names-valid.txt", "-prf", "src/test/resources/testingIndexPosition.txt"};
        cliInputs.add(args);
        args = new String[]{"--inputs", "src/test/resources/artifact-names-valid.txt", "-prf", "src/test/resources/testingIndexPosition.txt", "-pof", "src/test/resources/stop.txt"};
        cliInputs.add(args);

        List<List<String>> expected = (List<List<String>>) json.get("readIdentsIn");
        int[] expectedEndings = {10, 10, 9, 9, 10, 10};

        int i = 0;
        for(String[] arg : cliInputs) {
            List<String> curExpected = expected.get(i);
            List<Artifact> artifactsSeen = new ArrayList<>();
            MavenCentralArtifactAnalysis collectorAnalysis = new MavenCentralArtifactAnalysis(false, false, false, false) {
                @Override
                public void analyzeArtifact(Artifact current) {
                    artifactsSeen.add(current);
                }
            };

            // Apply arguments, check progress is stored correctly
            collectorAnalysis.runAnalysis(arg);
            Path progressFile = collectorAnalysis.getSetupInfo().progressOutputFile;
            long ending = getEndingIndex(progressFile);
            assertEquals(expectedEndings[i], ending);

            assertEquals(curExpected.size(), artifactsSeen.size());



            for(Artifact actual: artifactsSeen) {
                final String actualCoordinates = actual.getIdent().getCoordinates();
                assert(curExpected.contains(actualCoordinates));
            }
            i++;
        }
    }

    @Test
    @DisplayName("An analysis must produce the same results in multithreaded mode")
    void checkMultiThreading() {
        List<String[]> singleArgs = new ArrayList<>();
        List<String[]> multiArgs = new ArrayList<>();

        singleArgs.add(new String[]{"-st", "10:200"});
        multiArgs.add(new String[]{"--threads", "5", "-st", "10:200"});

        singleArgs.add(new String[]{"--inputs", "src/test/resources/artifact-names-valid.txt"});
        multiArgs.add(new String[]{"--threads", "5", "--inputs", "src/test/resources/artifact-names-valid.txt"});

        for(int i = 0; i < singleArgs.size(); i++) {

            Set<ArtifactIdent> identifiersSeen = new HashSet<>();
            MavenCentralArtifactAnalysis tester = new MavenCentralArtifactAnalysis(false, true, false, false) {
                @Override
                public void analyzeArtifact(Artifact current) {
                    identifiersSeen.add(current.ident);
                }
            };

            tester.runAnalysis(singleArgs.get(i));
            Set<ArtifactIdent> singleResult = new HashSet<>(identifiersSeen);

            identifiersSeen.clear();

            tester.runAnalysis(multiArgs.get(i));
            Set<ArtifactIdent> multiResult = new HashSet<>(identifiersSeen);

            assertEquals(singleResult.size(), multiResult.size());

            for(ArtifactIdent single : singleResult) {
                assert(multiResult.contains(single));
            }
        }
    }

    @Test
    @DisplayName("An analysis must execute without exception for basic use cases")
    void checkUseCases() {
        MavenCentralArtifactAnalysis jarUseCase = new MavenCentralArtifactAnalysis(false, false, false, true) {
            public long numberOfClassfiles = 0;
            @Override
            public void analyzeArtifact(Artifact current) {
                if(current.getJarInformation() != null) {
                    numberOfClassfiles += current.getJarInformation().getNumClassFiles();
                }
            }

        };
        assertDoesNotThrow( () -> jarUseCase.runAnalysis(new String[]{"-st", "0:300"}));

        MavenCentralArtifactAnalysis pomUseCase = new MavenCentralArtifactAnalysis(false, true, false, false) {
            public final Set<License> uniqueLicenses = new HashSet<>();
            @Override
            public void analyzeArtifact(Artifact toAnalyze) {
                if(toAnalyze.getPomInformation() != null) {
                    PomInformation info = toAnalyze.getPomInformation();
                    if(!info.getRawPomFeatures().getLicenses().isEmpty()) {
                        for(License license : info.getRawPomFeatures().getLicenses()) {
                            if(!uniqueLicenses.contains(license)) {
                                uniqueLicenses.add(license);
                            }
                        }
                    }
                }
            }
        };

        assertDoesNotThrow( () -> pomUseCase.runAnalysis(new String[]{"-st", "0:300"}));

        MavenCentralArtifactAnalysis indexUseCase = new MavenCentralArtifactAnalysis(true, false, false, false) {
            public final Set<Artifact> hasJavadocs = new HashSet<>();
            @Override
            public void analyzeArtifact(Artifact toAnalyze) {
                if(toAnalyze.getIndexInformation() != null) {
                    List<Package> packages = toAnalyze.getIndexInformation().getPackages();
                    for(Package current : packages) {
                        if(current.getJavadocExists() > 0) {
                            hasJavadocs.add(toAnalyze);
                            break;
                        }
                    }
                }
            }
        };

        assertDoesNotThrow( () -> indexUseCase.runAnalysis(new String[]{"-st", "0:300"}));
    }

    private long getEndingIndex(Path fileName) {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(fileName.toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArtifactAnalysisConfig parseCLI(String cli) {
        try {
            final ArtifactAnalysisConfigParser parser = new ArtifactAnalysisConfigParser();
            if(cli.isBlank()) return parser.parseArtifactConfig(new String[] {});
            else return parser.parseArtifactConfig(cli.split(" "));
        } catch(CLIException clix){
            throw new RuntimeException(clix);
        }

    }

    private int asInt(String s){
        return Integer.parseInt(s);
    }

    private ZonedDateTime asDate(long timestamp){
        Instant i = Instant.ofEpochMilli(timestamp);
        return ZonedDateTime.ofInstant(i, ZoneId.of("GMT"));
    }
}