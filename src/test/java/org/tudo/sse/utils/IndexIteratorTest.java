package org.tudo.sse.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.index.Package;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexIteratorTest {

    IndexIterator indexIterator;
    final Map<String, Object> json;
    final Gson gson = new Gson();

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("IndexInputs.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @BeforeEach
    void setIndexIterator() {
        try {
            indexIterator = new IndexIterator(new URI("https://repo1.maven.org/maven2/"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void processArtifactIdent() {
        Map<String, ArrayList<String>> inputsAndExpected  = (Map<String, ArrayList<String>>) json.get("processArtifactIdent");
            for (int j = 0; j < inputsAndExpected.get("inputs").size(); j++) {
                assertEquals(inputsAndExpected.get("expected").get(j), indexIterator.processArtifactIdent(inputsAndExpected.get("inputs").get(j)).getCoordinates());
            }
    }

    @Test
    void processPackage() {
        Map<String, Object> inputsAndExpected  = (Map<String, Object>) json.get("processPackage");
        ArrayList<ArrayList<String>> inputs = (ArrayList<ArrayList<String>>) inputsAndExpected.get("inputs");
        ArrayList<ArrayList<String>> expected = (ArrayList<ArrayList<String>>) inputsAndExpected.get("expected");
        for(int i = 0; i < inputs.size(); i++) {
            Package current = indexIterator.processPackage(inputs.get(i).get(0), inputs.get(i).get(1));

            assertEquals(expected.get(i).get(0), current.getPackaging());
            assertEquals(Long.parseLong(expected.get(i).get(1)), current.getLastModified());
            assertEquals(Long.parseLong(expected.get(i).get(2)), current.getSourcesExist());
            assertEquals(Long.parseLong(expected.get(i).get(3)), current.getJavadocExists());
            assertEquals(Long.parseLong(expected.get(i).get(4)), current.getSignatureExists());
            assertEquals(expected.get(i).get(5), current.getSha1checksum());
        }

    }
}