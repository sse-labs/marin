package org.tudo.sse.resolution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.io.*;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.tudo.sse.model.pom.Dependency;
import org.tudo.sse.model.pom.License;
import org.tudo.sse.model.pom.RawPomFeatures;
import org.tudo.sse.model.ResolutionContext;
import org.tudo.sse.resolution.releases.IReleaseListProvider;
import org.tudo.sse.utils.JsonExporter;


import static org.junit.jupiter.api.Assertions.*;
import static org.tudo.sse.utils.TestUtilities.testResource;

@SuppressWarnings("unchecked")
class PomResolverTest {

    PomResolver pomResolver;
    ResolutionContext testContext;

    Map<String, Object> json;
    Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(PomResolverTest.class);

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("PomInputs.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @BeforeEach
    void setUp() {
           pomResolver = new PomResolver(true);
           testContext = ResolutionContext.createAnonymousContext();
    }

    @Test
    void processRawPomFeatures() {
        Map<String, Map<String, Object>> temp = (Map<String, Map<String, Object>>) json.get("processRawFeatures");
        ArrayList<String> inputs = (ArrayList<String>) temp.get("inputs");
        ArrayList<Map<String, Object>> expected = (ArrayList<Map<String, Object>>) temp.get("expected");

        int i = 0;
        for(String input : inputs) {
            RawPomFeatures current = null;
            try {
                current = pomResolver.processRawPomFeatures(IOUtils.toInputStream(input, StandardCharsets.UTF_8), null);
            } catch (PomResolutionException e) {
                fail(e);
            }
            checkRawFeatures(current, expected, i);
            i++;
        }

    }

    @Test
    @DisplayName("Assert correct raw pom feature contents")
    void processArtifacts() throws IOException {
        List<String> identStrings = Files.readAllLines(Objects.requireNonNull(testResource("artifact-names-valid.txt")));

        List<ArtifactIdent> idents = new ArrayList<>();

        for(String ident : identStrings) {
            String[] parts = ident.split(":");
            idents.add(new ArtifactIdent(parts[0], parts[1], parts[2]));
        }

        ArrayList<Map<String, Object>> expected = (ArrayList<Map<String, Object>>) json.get("resolveArtifacts");

        List<Artifact> poms = pomResolver.resolveArtifacts(idents, testContext);

        assertEquals(10, poms.size());

        //check to see that the parsed data from the POM for these indexes are correct for actually retrieving the files from url
        for(int i = 0; i < expected.size(); i++) {
            assertNotNull(poms.get(i).getPomInformation());
            assertNotNull(poms.get(i).getPomInformation().getRawPomFeatures());
            checkRawFeatures(poms.get(i).getPomInformation().getRawPomFeatures(), expected, i);
        }
    }

    @Test
    @DisplayName("Export new expected values for: Assert correct raw pom feature contents")
    @Disabled
    void exportRawPomFeatures() throws IOException {
        final Gson exportGson = new GsonBuilder().setPrettyPrinting().create();
        List<String> identStrings = Files.readAllLines(Objects.requireNonNull(testResource("artifact-names-valid.txt")));

        List<ArtifactIdent> idents = new ArrayList<>();

        for(String ident : identStrings) {
            String[] parts = ident.split(":");
            idents.add(new ArtifactIdent(parts[0], parts[1], parts[2]));
        }

        ArrayList<Map<String, Object>> expected = (ArrayList<Map<String, Object>>) json.get("resolveArtifacts");

        List<Artifact> poms = pomResolver.resolveArtifacts(idents, testContext);

        assertEquals(10, poms.size());

        JsonArray exportArray = new JsonArray();

        //check to see that the parsed data from the POM for these indexes are correct for actually retrieving the files from url
        for(int i = 0; i < expected.size(); i++) {
            assertNotNull(poms.get(i).getPomInformation());
            assertNotNull(poms.get(i).getPomInformation().getRawPomFeatures());
            JsonObject exportObject = JsonExporter.exportRawPomFeatures(poms.get(i).getPomInformation().getRawPomFeatures());
            exportArray.add(exportObject);
        }

        exportGson.toJson(exportArray, System.out);
    }

    @Test
    void resolveDependencies() {
        Map<String, Object> allTestData = (Map<String, Object>) json.get("resolveDependencies");

        Map<String, List<String>> releaseListData = new HashMap<>();
        for(Map<String, Object> entry : (List<Map<String, Object>>)allTestData.get("versionLists")){
            String g = (String)entry.get("g");
            String a = (String)entry.get("a");
            List<String> versions = (List<String>)entry.get("v");
            String ga = g + ":" + a;

            releaseListData.put(ga, versions);
        }

        IReleaseListProvider mockProvider = new IReleaseListProvider() {
            @Override
            public List<String> getReleases(String groupId, String artifactId) throws IOException {
                final String ga =  groupId + ":" + artifactId;
                if(releaseListData.containsKey(ga)){
                    return releaseListData.get(ga);
                } else {
                    fail("No mock release data available for " + ga);
                    return null;
                }
            }
        };

        pomResolver = new PomResolver(true, mockProvider);

        List<ArtifactIdent> idents = new ArrayList<>();

        //get three idents with complex resolution cases
        idents.add(new ArtifactIdent("com.google.common.html.types", "types", "1.0.0"));
        idents.add(new ArtifactIdent("org.springframework.cloud", "spring-cloud-bus", "1.0.0.RELEASE"));
        idents.add(new ArtifactIdent("org.springframework.boot", "spring-boot-starter-jersey", "1.2.0.RELEASE"));
        idents.add(new ArtifactIdent("org.openengsb", "openengsb-maven-plugin", "1.3.1"));
        idents.add(new ArtifactIdent("org.opencoweb", "cowebx-apps", "0.4"));
        idents.add(new ArtifactIdent("com.io7m.junreachable", "io7m-junreachable-documentation", "1.0.0"));
        idents.add(new ArtifactIdent("org.eclipse.xtext", "org.eclipse.xtext.builder.standalone", "2.6.2"));

        //set up expected values from mvn dependency tree to the json file
        ArrayList<ArrayList<String>> temp = (ArrayList<ArrayList<String>>) allTestData.get("tests");

        //check expected values against processed ones?
        List<Artifact> results = pomResolver.resolveArtifacts(idents, testContext);

        for(int i = 0; i < results.size(); i++) {
            List<Dependency> current = results.get(i).getPomInformation().getResolvedDependencies();
            ArrayList<String> expectedDependencies = temp.get(i);
            //add something to bypass version range resolution checks for the time being
            for(int j = 0; j < current.size(); j++) {
                String actual = current.get(j).getIdent().getCoordinates() + ":" + current.get(j).getScope();
                assertEquals(expectedDependencies.get(j), actual);
            }
        }
    }

    void checkRawFeatures(RawPomFeatures current, ArrayList<Map<String, Object>> expected, int i) {

        if(current.getParent() != null) {
            assertEquals(expected.get(i).get("parent"), current.getParent().getCoordinates());
        } else {
            assertEquals(expected.get(i).get("parent"),"null");
        }

        if(current.getName() != null) {
            assertEquals(expected.get(i).get("name"), current.getName());
        } else {
            assertEquals(expected.get(i).get("name"),"null");
        }

        if(current.getDescription() != null) {
            assertEquals(expected.get(i).get("description"), current.getDescription());
        } else {
            assertEquals(expected.get(i).get("description"),"null");
        }

        if(current.getUrl() != null) {
            assertEquals(expected.get(i).get("url"), current.getUrl());
        } else {
            assertEquals(expected.get(i).get("url"),"null");
        }

        if(current.getPackaging() != null) {
            assertEquals(expected.get(i).get("packaging"), current.getPackaging());
        } else {
            assertEquals(expected.get(i).get("packaging"),"null");
        }

        if(current.getInceptionYear() != null) {
            assertEquals(expected.get(i).get("inceptionYear"), current.getInceptionYear());
        } else {
            assertEquals(expected.get(i).get("inceptionYear"),"null");
        }

        if(!current.getProperties().isEmpty()) {

            //check properties for each value pair in the expected
            ArrayList<ArrayList<String>> props = (ArrayList<ArrayList<String>>) expected.get(i).get("properties");
            IntStream.range(0, props.size()).forEach(j -> assertEquals(props.get(j).get(1), current.getProperties().get(props.get(j).get(0))));
        }

        ArrayList<String> expectedDeps = (ArrayList<String>) expected.get(i).get("dependencies");
        if(!current.getDependencies().isEmpty()) {
            int j = 0;
            for(Dependency dependency : current.getDependencies()) {
                assertEquals(expectedDeps.get(j), dependency.getIdent().getCoordinates());
                j++;
            }
        } else {
            assert(expectedDeps.isEmpty());
        }

        ArrayList<String> expectedLicenses = (ArrayList<String>) expected.get(i).get("licenses");
        if(!current.getLicenses().isEmpty()) {
            int j = 0;
            for(License license : current.getLicenses()) {
                assertEquals(expectedLicenses.get(j), license.getUrl());
                j++;
            }
        } else {
            assert(expectedLicenses.isEmpty());
        }


        if(current.getDependencyManagement() != null) {
            ArrayList<String> expectedDepManagement = (ArrayList<String>) expected.get(i).get("dependencyManagement");
            int j = 0;
            for(Dependency dependency : current.getDependencyManagement()) {
                assertEquals(expectedDepManagement.get(j), dependency.getIdent().getCoordinates());
                j++;
            }
        } else {
            assertEquals(expected.get(i).get("dependencyManagement"), "null");
        }
    }


    @Test
    void resolveVersionRanges() {
        ArrayList<Map<String, Object>> temp = (ArrayList<Map<String, Object>>) json.get("versionRanges");

        for(Map<String, Object> testCase : temp){
            String rangeExpr = (String) testCase.get("expr");
            List<String> versionsAvailable = (List<String>) testCase.get("avail");
            String expected = (String) testCase.get("res");

            IReleaseListProvider mockProvider = new IReleaseListProvider() {
                @Override
                public List<String> getReleases(String groupId, String artifactId) throws IOException {
                    return versionsAvailable;
                }
            };

            PomResolver mockResolver = new PomResolver(true, mockProvider);
            String[] parts = rangeExpr.split(":");
            Dependency inputDependency = new Dependency(new ArtifactIdent(parts[0], parts[1], parts[2]), parts[3], false, true, false, null);

            String result = mockResolver.resolveVersionRange(inputDependency);
            inputDependency.getIdent().setVersion(result);

            assertEquals(expected, inputDependency.getIdent().getCoordinates() + ":" + inputDependency.getScope());
        }

    }

    @Test
    void resolveTransitiveDependencies() {
        Map<String, Object> allTransitives = (Map<String, Object>) json.get("allTransitives");
        ArrayList<String> currentDependencies = (ArrayList<String>) allTransitives.get("dependencies");
        List<ArtifactIdent> idents = new ArrayList<>();
        idents.add(new ArtifactIdent("org.openengsb", "openengsb-maven-plugin", "1.3.1"));
        List<Artifact> results = pomResolver.resolveArtifacts(idents, testContext);

        //create a recursive driver, that takes in the main map and the dependencies list, so it can recur to each level of the tree
        for(Artifact current : results) {
            recursiveIteration(allTransitives, currentDependencies, current);
        }
    }

    void recursiveIteration(Map<String, Object> allTransitives, ArrayList<String> currentDependencies, Artifact current) {
        int i = 0;
        assertEquals(currentDependencies.size(), current.getPomInformation().getAllTransitiveDependencies().size());
        for(String dependency : currentDependencies) {
            assertEquals(dependency, current.getPomInformation().getAllTransitiveDependencies().get(i).getIdent().getCoordinates());
            if(allTransitives.containsKey(dependency)) {
                Map<String, Object> currentLevel = (Map<String, Object>) allTransitives.get(dependency);
                recursiveIteration(currentLevel, (ArrayList<String>) currentLevel.get("dependencies"), current.getPomInformation().getAllTransitiveDependencies().get(i));
            }
            i++;
        }
    }

    @Test
    void resolveEffectiveTransitiveDependencies() {
        List<ArtifactIdent> idents = new ArrayList<>();

        //get three idents with complex resolution cases
//        idents.add(new ArtifactIdent("xom", "xom", "1.1d2"));
        idents.add(new ArtifactIdent("org.openengsb", "openengsb-maven-plugin", "1.3.1"));

        //set up expected values from mvn dependency tree to the json file
        ArrayList<ArrayList<String>> temp = (ArrayList<ArrayList<String>>) json.get("effectiveTransitives");
        ArrayList<Map<String, ArrayList<String>>> conflicts = (ArrayList<Map<String, ArrayList<String>>>) json.get("conflicts");

        //check expected values against processed ones?
        List<Artifact> results = pomResolver.resolveArtifacts(idents, testContext);

        for(int i = 0; i < results.size(); i++) {

            List<Artifact> current = results.get(i).getPomInformation().getEffectiveTransitiveDependencies();
            ArrayList<String> expectedDependencies = temp.get(i);
            for(Artifact art : current) {
                log.info(art.getIdent().getCoordinates());
            }

            assertEquals(expectedDependencies.size(), current.size());

            for(int j = 0; j < current.size(); j++) {
                String actual = current.get(j).getIdent().getCoordinates();
                if(current.get(j).relocation != null) {
                    actual = current.get(j).getRelocation().getCoordinates();
                }
                assertEquals(expectedDependencies.get(j), actual);
            }


            Map<String, ArrayList<String>> currentConflicts = conflicts.get(i);
            Map<String, List<ArtifactIdent>> transitiveConflicts = results.get(i).getPomInformation().getTransitiveConflicts();

           for(Map.Entry<String, ArrayList<String>> cur : currentConflicts.entrySet()) {
               assert(transitiveConflicts.containsKey(cur.getKey()));
               ArrayList<String> expected = cur.getValue();
               List<ArtifactIdent> actual = transitiveConflicts.get(cur.getKey());

               assertEquals(expected.size(), actual.size());

               for(int k = 0; k < expected.size(); k++) {
                   assertEquals(expected.get(k), actual.get(k).getCoordinates());
               }
           }
        }
    }

    @Test
    void resolveFromSecondaryRepository() {
        List<ArtifactIdent> idents = new ArrayList<>();

        //set idents up here
        idents.add(new ArtifactIdent("com.walmartlabs.concord.plugins", "git", "1.44.0"));

        //set up expected values from mvn dependency tree to the json file
        ArrayList<String> expectedDeps = (ArrayList<String>) json.get("2ndRepo");
        List<Artifact> results = pomResolver.resolveArtifacts(idents, testContext);

        for(int i = 0; i < results.size(); i++) {
            List<Artifact> current = results.get(i).getPomInformation().getEffectiveTransitiveDependencies();
            assertEquals(expectedDeps.size(), current.size());

            for(int j = 0; j < current.size(); j++) {
                String actual = current.get(j).getIdent().getCoordinates() + ":" + current.get(j).getIdent().getRepository();
                if(current.get(j).relocation != null) {
                    actual = current.get(j).getRelocation().getCoordinates() + ":" + current.get(j).getIdent().getRepository();
                }
                assertEquals(expectedDeps.get(j), actual);
            }
        }
    }

    @Test
    @DisplayName("The POM resolver must not loop for circular dependencies")
    void resolveDependencyLoop() {
        // Based on https://github.com/sse-labs/marin/issues/52
        final ArtifactIdent loopingArtifact = new ArtifactIdent("net.wicp.tams", "ts-maven-plugin", "8.0.2");
        final ArtifactResolutionContext freshCtx = ArtifactResolutionContext.newInstance(loopingArtifact);

        final Artifact artifact = pomResolver.resolveArtifacts(List.of(loopingArtifact), freshCtx).get(0);

        assertNotNull(artifact);


    }
}