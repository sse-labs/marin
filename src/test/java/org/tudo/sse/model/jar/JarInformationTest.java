package org.tudo.sse.model.jar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opalj.br.ClassFile;
import org.opalj.br.ClassType;
import org.opalj.br.analyses.Project;
import org.opalj.log.GlobalLogContext$;
import org.opalj.log.OPALLogger;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.utils.MarinOpalLogger;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class JarInformationTest {

    private final long expectedFileSizeBytes = 79347L;
    private final ArtifactIdent ident = new ArtifactIdent("eu.sse-labs", "marin", "1.0.0");
    private final JarInformation jarInfo = new JarInformation(ident);

    @BeforeAll
    static void setup(){
        // Use global OPAL Logger - will only forward OPAL messages with level error or fatal
        OPALLogger.updateLogger(GlobalLogContext$.MODULE$, MarinOpalLogger.getGlobalLogger());
    }

    @Test
    @DisplayName("JAR information should be able to download file to temp directory")
    void testLocalFile(){
        try {
            File tempFile = jarInfo.getJarFile();
            assertTrue(tempFile.exists());
            assertTrue(tempFile.canRead());
            assertEquals(tempFile.length(), expectedFileSizeBytes);
        } catch(Exception x) {
            fail(x);
        }
    }

    @Test
    @DisplayName("JAR information should be able to open an InputStream to the actual file")
    void testInputStream(){
        try {
            InputStream is = jarInfo.getJarFileInputStream();
            byte[] jarFileBytes = is.readAllBytes();
            is.close();
            assertEquals(expectedFileSizeBytes, jarFileBytes.length);
        } catch(Exception x){
            fail(x);
        }
    }

    @Test
    @DisplayName("JAR information should be able to open a JarInputStream to the underlying JAR file")
    void testJarInputStream(){
        final String className = "org/tudo/sse/ArtifactFactory.class";
        try {
            JarInputStream jis = jarInfo.getJarInputStream();
            JarEntry entry = jis.getNextJarEntry();
            boolean classFound = false;

            while(entry != null){
                if(entry.getName().equals(className)){
                    classFound = true;
                    break;
                }
                entry = jis.getNextJarEntry();
            }

            assertTrue(classFound);
            assertNotNull(entry);
            assertEquals(className, entry.getName());
            jis.close();
        } catch (Exception x) {
            fail(x);
        }
    }

    @Test
    @DisplayName("JAR information should be able to build OPAL class file representations for the underlying JAR")
    void testOpalClassFileRepresentations(){
        try {
            List<ClassFile> cfs = jarInfo.getOpalClassFileRepresentations();
            assertNotNull(cfs);
            assertFalse(cfs.isEmpty());

            boolean foundClass = false;

            for(ClassFile cf : cfs){
                if(cf.thisType().simpleName().equals("ArtifactFactory")){
                    foundClass = true;
                    break;
                }
            }

            assertTrue(foundClass);
        } catch(Exception x){
            fail(x);
        }
    }

    @Test
    @DisplayName("JAR information should be able to initialize an OPAL project instance for the underlying JAR")
    void testOpalProjectSimple(){
        try {
            Project<?> project = jarInfo.getOpalProject();
            ClassType afType = project.allProjectClassFiles().find( cf -> cf.thisType().simpleName().equals("ArtifactFactory")).get().thisType();
            assertNotNull(afType);
            assertTrue(project.classHierarchy().isKnown(afType));
            assertTrue(project.allLibraryClassFiles().isEmpty());
        } catch (Exception x) {
            fail(x);
        }
    }

    @Test
    @DisplayName("JAR information should be able to initialize a complex OPAL project instance with dependencies")
    void testOpalProjectComplex(){
        final ArtifactIdent actualDependency = new ArtifactIdent("org.apache.maven.indexer", "indexer-reader", "7.1.3");
        final Set<ArtifactIdent> dependencies = new HashSet<>();
        dependencies.add(actualDependency);

        try {
            Project<?> project = jarInfo.getOpalProject(dependencies, true, true);

            // Check that main project is loaded
            ClassType afType = project.allProjectClassFiles().find( cf -> cf.thisType().simpleName().equals("ArtifactFactory")).get().thisType();
            assertNotNull(afType);
            assertTrue(project.classHierarchy().isKnown(afType));

            // Check that libraries are in fact loaded
            assertFalse(project.allLibraryClassFiles().isEmpty());
            ClassType irType = project.allLibraryClassFiles().find(cf -> cf.thisType().simpleName().equals("IndexReader")).get().thisType();
            assertNotNull(irType);
            assertTrue(project.classHierarchy().isKnown(irType));
        } catch (Exception x){
            fail(x);
        }
    }

}
