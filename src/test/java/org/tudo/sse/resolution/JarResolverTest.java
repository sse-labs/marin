package org.tudo.sse.resolution;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.jar.ClassFile;
import org.tudo.sse.model.jar.JarInformation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JarResolverTest {

    JarResolver jarResolver;
    Map<String, Object> json;
    Gson gson = new Gson();

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("JarExpected.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @BeforeEach
    void setUp() {
        jarResolver = new JarResolver();
    }

    @Test
    void parseJar() {
        JarInformation toTest;
        try {
            toTest = jarResolver.parseJar(new ArtifactIdent("com.google.common.html.types", "types", "1.0.0")).getJarInformation();
        } catch (JarResolutionException e) {
            throw new RuntimeException(e);
        }

        //test statistics
        Map<String, String> stats = (Map<String, String>) json.get("Statistics");
        assertEquals(Long.parseLong(stats.get("codeSize")), toTest.getCodesize());
        assertEquals(Long.parseLong(stats.get("NumClassFiles")), toTest.getNumClassFiles());
        assertEquals(Long.parseLong(stats.get("NumMethods")), toTest.getNumMethods());
        assertEquals(Long.parseLong(stats.get("NumFields")), toTest.getFields());
        assertEquals(Long.parseLong(stats.get("NumPackages")), toTest.getNumPackages());

        //test Packages
        Map<String, List<Map<String, Map<String, String>>>> packages = (Map<String, List<Map<String, Map<String, String>>>>) json.get("Packages");

        for(Map.Entry<String, List<Map<String, Map<String, String>>>> item : packages.entrySet()) {
            String key = item.getKey();
            if(toTest.getPackages().containsKey(key)) {
                assertPackage(toTest.getPackages().get(item.getKey()),  item.getValue());
            } else {
                fail();
            }
        }


    }

    void assertPackage(List<ClassFile> toTest, List<Map<String, Map<String, String>>> item) {
        assertEquals(toTest.size(), item.size());

        for(int i = 0; i < toTest.size(); i++) {
            assertEquals(item.get(i).get("thisType").get("fqn"), toTest.get(i).getThistype().getFqn());
            assertEquals(item.get(i).get("thisType").get("packageName"), toTest.get(i).getThistype().getPackageName());

            assertEquals(item.get(i).get("superType").get("fqn"), toTest.get(i).getSuperType().getFqn());
            assertEquals(item.get(i).get("superType").get("packageName"), toTest.get(i).getSuperType().getPackageName());

        }
    }

}