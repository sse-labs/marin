package org.tudo.sse.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.tudo.sse.ArtifactFactory;
import org.tudo.sse.model.jar.ClassFileNode;
import org.tudo.sse.model.jar.FoundInfoNode;
import org.tudo.sse.resolution.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TypeStructureTest {

    JarResolver resolver = new JarResolver();
    PomResolver pomResolver = new PomResolver(true);
    Map<String, Object> json;
    Gson gson = new Gson();

    private static final Logger log = LogManager.getLogger(TypeStructureTest.class);

    {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("TypeStructure.json");
        assert resource != null;
        Reader targetReader = new InputStreamReader(resource);
        json = gson.fromJson(targetReader, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @Test
    void buildTypeStructure() {

        Artifact jarArt1;
        ArtifactIdent ident = new ArtifactIdent("org.springframework", "spring-web", "6.1.11");
        Artifact jarArt2;
        jarArt2 = ArtifactFactory.getArtifact(ident);
        try {
            jarArt1 = resolver.parseJar(new ArtifactIdent("org.bouncycastle", "bcpg-lts8on", "2.73.6"));
            jarArt2 = resolver.parseJar(ident);
            pomResolver.resolveArtifact(ident);
        } catch (JarResolutionException | PomResolutionException | FileNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }

        ClassFileNode results1 = jarArt1.buildTypeStructure().get("java/lang/Object");
        Map<String, Object> expected1 = (Map<String, Object>) json.get("artifact1");

        singleChild(results1, (Map<String, Object>) expected1.get("test1"));
        interfacesCheck(results1, (Map<String, Object>) expected1.get("test2"));
        interfacesCheck(results1, (Map<String, Object>) expected1.get("test3"));

        Map<String, ClassFileNode> results2 = jarArt2.buildTypeStructure();
        Map<String, Object> expected2 = (Map<String, Object>) json.get("artifact2");


        multipleChildren(results2.get("reactor/core/observability/DefaultSignalListener"), (Map<String, Object>) expected2.get("test1"));
        multiLevelChild(results2.get("java/lang/RuntimeException"), (Map<String, Object>) expected2.get("test2"));
        interfacesCheck(results2.get("java/lang/Object"), (Map<String, Object>) expected2.get("test3"));

    }

    ClassFileNode findNode(ClassFileNode root, String toFind) {

        for(ClassFileNode cur : root.getChildren()) {
            if(cur.getThistype().getFqn().equals(toFind)) {
                return cur;
            }
        }

        throw new RuntimeException("Didn't find expected node");
    }

    void singleChild(ClassFileNode root, Map<String, Object> expected) {
        ClassFileNode toTest = findNode(root, (String) expected.get("name"));
        Map<String, Object> child = (Map<String, Object>) expected.get("child");
        assert(toTest.getChildren().get(0).getThistype().getFqn().equals((String) child.get("name")));
    }

    void interfacesCheck(ClassFileNode root, Map<String, Object> expected) {
        ClassFileNode toTest = findNode(root, (String) expected.get("name"));

        List<Map<String, String>> expectedInterfaces = (List<Map<String, String>>) expected.get("interfaces");
        List<ClassFileNode> interfaces = ((FoundInfoNode) toTest).getInterfaceNodes();

        assertEquals(interfaces.size(), expectedInterfaces.size());

        for(int i = 0; i < interfaces.size(); i++) {
            assertEquals(interfaces.get(i).getThistype().getFqn(), expectedInterfaces.get(i).get("name"));
            assertEquals(((FoundInfoNode) interfaces.get(i)).getSuperClass().getThistype().getFqn(), expectedInterfaces.get(i).get("super"));
        }
    }

    void multipleChildren(ClassFileNode root, Map<String, Object> expected) {
        assertEquals(root.getThistype().getFqn(), expected.get("name"));

        List<Map<String, String>> children = (List<Map<String, String>>) expected.get("children");
        assertEquals(root.getChildren().size(), children.size());

        for(int i = 0; i < root.getChildren().size(); i++) {
            assertEquals(children.get(i).get("name"), root.getChildren().get(i).getThistype().getFqn());
        }
        log.info("Got here");
    }

    void multiLevelChild(ClassFileNode root, Map<String, Object> expected) {
        assertEquals(root.getThistype().getFqn(), expected.get("name"));
        List<Map<String, Object>> children = (List<Map<String, Object>>) expected.get("children");
        assertEquals(root.getChildren().get(0).getThistype().getFqn(), children.get(0).get("name"));
        List<Map<String, String>> secondLevel = (List<Map<String, String>>) children.get(0).get("children");
        assertEquals(root.getChildren().get(0).getChildren().get(0).getThistype().getFqn(), secondLevel.get(0).get("name"));
    }

}