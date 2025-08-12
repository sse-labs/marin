package org.tudo.sse.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.pom.Dependency;
import org.tudo.sse.model.pom.LocalPomInformation;
import org.tudo.sse.model.pom.RawPomFeatures;
import org.tudo.sse.resolution.PomResolver;
import org.tudo.sse.resolution.releases.IReleaseListProvider;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalPomInformationTest {

    final Gson gson = new Gson();
    final Map<String, Object> json;
    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("localPomExpected.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    final IReleaseListProvider mockProvider = new IReleaseListProvider() {

        private Map<String, List<String>> releaseListData = new HashMap<>();

        private void buildMap(){
            for(Map<String, Object> entry : (List<Map<String, Object>>)json.get("versionLists")){
                String g = (String)entry.get("g");
                String a = (String)entry.get("a");
                List<String> versions = (List<String>)entry.get("v");
                String ga = g + ":" + a;

                releaseListData.put(ga, versions);
            }
        }

        @Override
        public List<String> getReleases(ArtifactIdent identifier) throws IOException {
            if(releaseListData.isEmpty()){
                buildMap();
            }

            if(releaseListData.containsKey(identifier.getGA())){
                return releaseListData.get(identifier.getGA());
            } else {
                fail("No mock release data available for " + identifier.getGA());
                return null;
            }
        }
    };

    @Test
    void localPom() {
        List<LocalPomInformation> tests = new ArrayList<>();
        PomResolver resolver = new PomResolver(true, mockProvider);

        try {
            tests.add(new LocalPomInformation("src/test/resources/localPom.xml", resolver));
            tests.add(new LocalPomInformation(new File("src/test/resources/localPom.xml"), resolver));
            tests.add(new LocalPomInformation(new FileInputStream("src/test/resources/localPom.xml"), resolver));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        testFeatureExtraction(tests);

    }

    @Test
    void localPomException() {
        PomResolver resolver = new PomResolver(true, mockProvider);
        assertThrows(RuntimeException.class, () -> new LocalPomInformation("src/test/resources/brokenlocalPom.xml", resolver));
        assertThrows(RuntimeException.class, () -> new LocalPomInformation(new File("src/test/resources/brokenlocalPom.xml"), resolver));
        assertThrows(RuntimeException.class, () -> new LocalPomInformation(new FileInputStream("src/test/resources/brokenlocalPom.xml"), resolver));
    }

    void testFeatureExtraction(List<LocalPomInformation> tests) {
        for(LocalPomInformation test : tests) {
            RawPomFeatures actual = test.getRawPomFeatures();
            Map<String, String> parentVals = (Map<String, String>) json.get("parent");

            assertEquals(json.get("name"), actual.getName());
            assertEquals(json.get("ident"), test.getIdent().getCoordinates());
            assertEquals(json.get("description"), actual.getDescription());
            assertEquals(json.get("packaging"), actual.getPackaging());

            assertEquals(parentVals.get("name"), test.getParent().getPomInformation().getRawPomFeatures().getName());
            assertEquals(parentVals.get("ident"), test.getRawPomFeatures().getParent().getCoordinates());
            assertEquals(parentVals.get("description"), test.getParent().getPomInformation().getRawPomFeatures().getDescription());
            assertEquals(parentVals.get("packaging"), test.getParent().getPomInformation().getRawPomFeatures().getPackaging());

            List<String> expectedDependencies = (List<String>) json.get("dependencies");
            List<Dependency> dependencies = actual.getDependencies();

            for (int i = 0; i < expectedDependencies.size(); i++) {
                assertEquals(expectedDependencies.get(i), dependencies.get(i).getIdent().getCoordinates() + ":" + dependencies.get(i).getScope());
            }


            List<String> expectedResolvedDependencies = (List<String>) json.get("resolvedDependencies");
            List<Dependency> resolvedDependencies = test.getResolvedDependencies();

            for (int i = 0; i < expectedResolvedDependencies.size(); i++) {
                assertEquals(expectedResolvedDependencies.get(i), resolvedDependencies.get(i).getIdent().getCoordinates() + ":" + resolvedDependencies.get(i).getScope());
            }
        }
    }

}