package org.tudo.sse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.index.Package;
import org.tudo.sse.model.index.IndexInformation;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexWalkerTest {

    IndexWalker indexWalker;

    Map<String, Object> json;
    Gson gson = new Gson();

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("IndexInputs.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @BeforeEach
    void setUp() {
        try {
            indexWalker = new IndexWalker(new URI("https://repo1.maven.org/maven2/"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void walkPaginatedEndings() {
        int[][] startNtake = {{20, 30}, {20, 1}, {100000, 200000}, {500, 6000}, {6000, 10}};
        List<ArtifactIdent> idents;
        List<Artifact> indexArtifacts;

        for (int[] ints : startNtake) {
            try {
                indexArtifacts = indexWalker.walkPaginated(ints[0], ints[1]);
                idents = indexWalker.lazyWalkPaginated(ints[0], ints[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertEquals(ints[1], indexArtifacts.size());
            assertEquals(ints[1], idents.size());
        }
    }

    @Test
    void walkPaginated() {
        ArrayList<Map<String, Object>> expectedValues = (ArrayList<Map<String, Object>>) json.get("walkPaginated");
        List<Artifact> results;
        try {
            results = indexWalker.walkPaginated(1000, 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int i = 3;
        for(Map<String, Object> value: expectedValues) {
            IndexInformation currentArtifact = results.get(i).getIndexInformation();

            assertEquals(value.get("ident"), currentArtifact.getIdent().getCoordinates());
            assertEquals(value.get("name"), currentArtifact.getName());
            assertEquals(Long.parseLong((String) value.get("index")), currentArtifact.getIndex());

            ArrayList<Map<String, String>> expectedpackages = (ArrayList<Map<String, String>>) value.get("packages");
            List<Package> packages = currentArtifact.getPackages();
            for(int j = 0; j < expectedpackages.size(); j++) {
                assertEquals(expectedpackages.get(j).get("packaging"), packages.get(j).getPackaging());
                assertEquals(Long.parseLong(expectedpackages.get(j).get("lastModified")), packages.get(j).getLastModified());
                assertEquals(Long.parseLong(expectedpackages.get(j).get("size")), packages.get(j).getSize());
                assertEquals(Integer.parseInt(expectedpackages.get(j).get("sourcesExist")), packages.get(j).getSourcesExist());
                assertEquals(Integer.parseInt(expectedpackages.get(j).get("javadocExist")), packages.get(j).getJavadocExists());
                assertEquals(Integer.parseInt(expectedpackages.get(j).get("signatureExist")), packages.get(j).getSignatureExists());
                assertEquals(expectedpackages.get(j).get("sha1checksum"), packages.get(j).getSha1checksum());
            }
            i += 2;
        }

    }
}