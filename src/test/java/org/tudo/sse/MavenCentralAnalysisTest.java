package org.tudo.sse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.Package;
import org.tudo.sse.model.pom.License;
import org.tudo.sse.model.pom.PomInformation;
import org.tudo.sse.utils.IndexIterator;
import scala.Tuple2;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MavenCentralAnalysisTest {
    OwnImplementation tester = new OwnImplementation();
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
    void parseCmdLinePositive() {
        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"-st", "500:223"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/test/resources/localPom.xml"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/test/resources/localPom.xml", "-ip", "src/test/resources/localPom.xml"};
        cliInputs.add(args);
        args = new String[]{"-su", "53245:13243"};
        cliInputs.add(args);
        args = new String[]{"-ip", "src/test/resources/localPom.xml", "--coordinates", "src/test/resources/localPom.xml"};
        cliInputs.add(args);
        args = new String[]{"-ip", "src/test/resources/localPom.xml"};
        cliInputs.add(args);

        try {
            Path tmpDir = Files.createTempDirectory("maven-resolution-files");
            args = new String[]{"--output", tmpDir.toString()};
            cliInputs.add(args);
            List<List<String>> expected = (List<List<String>>) json.get("cliParsePos");

            int i = 0;
            for(String[] input : cliInputs) {
                MavenCentralAnalysis tester = new MavenCentralAnalysis() {
                    @Override
                    public void analyzeArtifact(Artifact current) {

                    }
                };
                tester.parseCmdLine(input);
                CliInformation result = tester.getSetupInfo();
                List<String> currentExp = expected.get(i);
                //run asserts here
                assertEquals(Integer.parseInt(currentExp.get(0)), result.getSkip());
                assertEquals(Integer.parseInt(currentExp.get(1)), result.getTake());
                assertEquals(Integer.parseInt(currentExp.get(2)), result.getSince());
                assertEquals(Integer.parseInt(currentExp.get(3)), result.getUntil());

                if(result.getToCoordinates() != null) {
                    assertEquals(currentExp.get(4), result.getToCoordinates().toString().replace("\\","/"));
                } else {
                    assertEquals(currentExp.get(4), "null");
                }

                if(result.getToIndexPos() != null) {
                    assertEquals(currentExp.get(5), result.getToIndexPos().toString().replace("\\","/"));
                } else {
                    assertEquals(currentExp.get(5), "null");
                }

                assertEquals(Boolean.parseBoolean(currentExp.get(6)), result.isOutput());
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void parseCmdLineNegative() {
        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"-st", "500:22:3", "--index", "--pom"};
        cliInputs.add(args);
        args = new String[]{"--jar", "--coordinates", "-/xcd/"};
        cliInputs.add(args);
        args = new String[]{"-st", "88880000", "-su", "--coordinates", "path/to/file", "-ip", "path/to/file", "--index", "--pom", "--jar"};
        cliInputs.add(args);
        args = new String[]{"--pom", "53245:13243", "--index"};
        cliInputs.add(args);
        args = new String[]{"--jr", "--index"};
        cliInputs.add(args);
        args = new String[]{"-ip", "--coordinates", "file/to/path"};
        cliInputs.add(args);
        args = new String[]{"--coordinates"};
        cliInputs.add(args);

        for(String[] input : cliInputs) {
            assertThrows(RuntimeException.class, () -> tester.parseCmdLine(input));
        }
    }

    @Test
    void walkPaginated() {
        List<Tuple2<Integer, Integer>> inputs = new ArrayList<>();
        inputs.add(new Tuple2<>(500, 10));
        inputs.add(new Tuple2<>(0, 10));
        inputs.add(new Tuple2<>(50000, 100));
        inputs.add(new Tuple2<>(763, 20));

        for(Tuple2 input : inputs) {
            int start1 = (int) input._1;
            int take = (int) input._2;

            int start2 = (start1 + take) - 1;

            try {
                IndexIterator iterator = new IndexIterator(new URI(base), start1);
                List<Artifact> collected1 = new ArrayList<>(tester.walkPaginated(take, iterator));

                Artifact lastOne = collected1.get(collected1.size() - 1);
                int i = 2;
                while(lastOne.getIndexInformation().getIndex() > start2) {
                    lastOne = collected1.get(collected1.size() - i);
                    i++;
                }

                iterator = new IndexIterator(new URI(base), start2);

                List<Artifact> collected2 = new ArrayList<>(tester.walkPaginated(1, iterator));
                assertEquals(lastOne.getIndexInformation().getIndex(), collected2.get(0).getIndexInformation().getIndex());
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void indexProcessor() {
        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"-st", "200:70"};
        cliInputs.add(args);
        args = new String[] {"-st", "499:11", "--name", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"-st", "500:10"};
        cliInputs.add(args);
        args = new String[]{"-st", "275:70", "--name", "src/test/resources/stop.txt"};
        cliInputs.add(args);

        int[] expectedEndings = {270, 510, 510, 345};

        int i = 0;
        for(String[] arg : cliInputs) {
            try {
                if(i == 2) {
                    tester.setIndex(true);
                }
                tester.parseCmdLine(arg);
                CliInformation current = tester.getSetupInfo();
                tester.indexProcessor();

                long ending = getEndingIndex(current.getName());
                assertEquals(expectedEndings[i], ending);

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
            i++;
        }
    }

    @Test
    void readIdentsIn() {
        List<String[]> cliInputs = new ArrayList<>();
        String[] args = {"--coordinates", "src/main/resources/coordinates.txt"};
        cliInputs.add(args);
        args = new String[] {"--coordinates", "src/main/resources/coordinates.txt", "--name", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/main/resources/coordinates.txt", "-st", "4:5"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/main/resources/coordinates.txt", "-st", "4:5", "--name", "src/test/resources/stop.txt"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/main/resources/coordinates.txt", "-ip", "src/test/resources/testingIndexPosition.txt"};
        cliInputs.add(args);
        args = new String[]{"--coordinates", "src/main/resources/coordinates.txt", "-ip", "src/test/resources/testingIndexPosition.txt", "--name", "src/test/resources/stop.txt"};
        cliInputs.add(args);

        List<List<String>> expected = (List<List<String>>) json.get("readIdentsIn");
        int[] expectedEndings = {9, 9, 8, 8, 9, 9};

       int i = 0;
        for(String[] arg : cliInputs) {
            List<String> curExpected = expected.get(i);
            MavenCentralAnalysis tester = new MavenCentralAnalysis() {
                @Override
                public void analyzeArtifact(Artifact current) {}
            };
            tester.parseCmdLine(arg);
            CliInformation current = tester.getSetupInfo();
            List<ArtifactIdent> idents = tester.readIdentsIn();

            assertEquals(curExpected.size(), idents.size());

            long ending = getEndingIndex(current.getName());

            assertEquals(expectedEndings[i], ending);

            for(int j = 0; j < curExpected.size(); j++) {
                assertEquals(curExpected.get(j), idents.get(j).getCoordinates());
            }
            i++;
        }

    }

    public long getEndingIndex(Path fileName) {
        BufferedReader indexReader;
        try {
            indexReader = new BufferedReader(new FileReader(fileName.toFile()));
            String line = indexReader.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void checkMultiThreading() {
        List<String[]> singleArgs = new ArrayList<>();
        List<String[]> multiArgs = new ArrayList<>();

        singleArgs.add(new String[]{"-st", "10:1000"});
        multiArgs.add(new String[]{"--multi", "5", "-st", "10:1000"});

        singleArgs.add(new String[]{"--coordinates", "src/main/resources/coordinates.txt"});
        multiArgs.add(new String[]{"--multi", "5", "--coordinates", "src/main/resources/coordinates.txt"});

        for(int i = 0; i < singleArgs.size(); i++) {
         try {
             MavenCentralAnalysis tester = new MavenCentralAnalysis() {
                 @Override
                 public void analyzeArtifact(Artifact current) {

                 }
             };
             tester.resolvePom = true;
             Map<ArtifactIdent, Artifact> singleResult = tester.runAnalysis(singleArgs.get(i));
             Map<ArtifactIdent, Artifact> multiResult = tester.runAnalysis(multiArgs.get(i));
             assertEquals(singleResult.size(), multiResult.size());
             for(Map.Entry<ArtifactIdent, Artifact> entry : singleResult.entrySet()) {
                 assert(multiResult.containsKey(entry.getKey()));
             }
         } catch(URISyntaxException | IOException e) {
            fail("Threw an exception");
         }

        }
    }
    @Test
    void checkUseCases() {
        MavenCentralAnalysis jarUseCase = new MavenCentralAnalysis() {
            public long numberOfClassfiles = 0;
            @Override
            public void analyzeArtifact(Artifact current) {
                if(current.getJarInformation() != null) {
                    numberOfClassfiles += current.getJarInformation().getNumClassFiles();
                }
            }

        };

        jarUseCase.resolveJar = true;
        assertDoesNotThrow( () -> jarUseCase.runAnalysis(new String[]{"-st", "0:1000"}));

        MavenCentralAnalysis pomUseCase = new MavenCentralAnalysis() {
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

        pomUseCase.resolvePom = true;
        assertDoesNotThrow( () -> pomUseCase.runAnalysis(new String[]{"-st", "0:1000"}));

        MavenCentralAnalysis indexUseCase = new MavenCentralAnalysis() {
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

        indexUseCase.resolveIndex = true;
        assertDoesNotThrow( () -> indexUseCase.runAnalysis(new String[]{"-st", "0:1000"}));
    }

}