package org.tudo.sse.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.pom.Dependency;
import org.tudo.sse.model.pom.LocalPomInformation;
import org.tudo.sse.model.pom.RawPomFeatures;
import org.tudo.sse.resolution.PomResolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalPomInformationTest {

    Gson gson = new Gson();
    Map<String, Object> json;
    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("localPomExpected.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @Test
    void localPom() {
        List<LocalPomInformation> tests = new ArrayList<>();
        PomResolver resolver = new PomResolver(true);

        tests.add(new LocalPomInformation("src/test/resources/localPom.xml", resolver));
        tests.add(new LocalPomInformation(new File("src/test/resources/localPom.xml"), resolver));
        try {
            tests.add(new LocalPomInformation(new FileInputStream("src/test/resources/localPom.xml"), resolver));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        testFeatureExtraction(tests);

    }

    @Test
    void localPomException() {
        PomResolver resolver = new PomResolver(true);
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