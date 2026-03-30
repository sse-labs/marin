package org.tudo.sse.resolution;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.opalj.br.ClassFile;
import org.opalj.br.Method;
import org.opalj.br.ClassType;
import org.opalj.br.reader.BytecodeInstructionsCache;
import org.opalj.br.reader.Java17FrameworkWithCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.jar.JarInformation;
import org.tudo.sse.model.jar.ObjType;
import org.tudo.sse.model.ResolutionContext;
import org.tudo.sse.utils.MavenCentralRepository;
import scala.Tuple2;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarInputStream;

import scala.collection.JavaConverters;

/**
 * This class is set up for the resolution of jar files on the maven central repository. The information collected is then stored in a JarInformation object.
 * @see JarInformation
 */
public class JarResolver {

    private final Path pathToDirectory;
    private boolean output;
    private static final MavenCentralRepository MavenRepo = MavenCentralRepository.getInstance();
    private static final Logger log = LoggerFactory.getLogger(JarResolver.class);

    /**
     * Creates a new empty JAR resolver instance
     */
    public JarResolver() {
        output = false;
        pathToDirectory = null;
    }

    /**
     * Creates a new empty JAR resolver instance with the given configuration attributes.
     * @param output Whether this resolver should persist its processed artifacts
     * @param pathToDirectory Output directory to persist artifacts in
     */
    public JarResolver(boolean output, Path pathToDirectory) {
        this.output = output;
        this.pathToDirectory = pathToDirectory;
    }

    /**
     * Sets whether this instance shall output its processed artifacts
     * @param output True if this resolver instance should persits its processed artifacts
     */
    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * This method resolves jar artifacts from a given list of artifact identifiers.
     *
     * @param identifiers artifact identifiers used to retrieve jar artifacts to process
     * @param ctx The resolution context to use for creating artifacts
     * @return a list of resolved artifacts
     * @see Artifact
     */
    public List<Artifact> resolveJars(List<ArtifactIdent> identifiers, ResolutionContext ctx) {
        List<Artifact> toReturn = new ArrayList<>();
        int count = 0;
        for(ArtifactIdent current : identifiers) {
            try {
                toReturn.add(parseJar(current, ctx));
            } catch (JarResolutionException e) {
                log.error("Failed to resolve JAR for {}", current, e);
            }

            count++;
            if(count % 1000 == 0) {
                log.info("{} jar artifacts have been processed", count);
            }
        }
        log.info("Finished processing {} jar artifacts", count);
        log.info("Successfully collected {} jar artifacts", toReturn.size());
        return toReturn;
    }

    /**
     * This method resolve a single jar artifact given an artifact identifier
     *
     * @param identifier an artifact identifier to retrieve the jar artifact to process
     * @param ctx The resolution context to use for creating artifacts
     * @return a resolved artifact
     * @throws JarResolutionException when there is an issue resolving the given jar artifact
     */
    public Artifact parseJar(ArtifactIdent identifier, ResolutionContext ctx) throws JarResolutionException {
        final Artifact currentArtifact = ctx.createArtifact(identifier);

        // Only build jar information if it is not already present for the current resolution context
        if(currentArtifact.hasJarInformation())
            return currentArtifact;

        try {
            URL jarURL = identifier.getMavenCentralJarUri().toURL();
            InputStream jarInput = MavenRepo.openJarFileInputStream(identifier);
            if(output && pathToDirectory != null) {
                var baos = new ByteArrayOutputStream();
                var buffer = new byte[32 * 1024];

                int bytesRead = jarInput.read(buffer);

                while(bytesRead > 0){
                    baos.write(buffer, 0, bytesRead);
                    baos.close();
                    baos.flush();
                    bytesRead = jarInput.read(buffer);
                }

                byte[] jarBytes = baos.toByteArray();

                Path filePath = pathToDirectory.resolve(identifier.getGroupID() + "-" + identifier.getArtifactID() + "-" + identifier.getVersion() + ".jar");
                if(!Files.exists(filePath)) {
                    Files.createFile(filePath);
                    Files.write(filePath, jarBytes);
                }

                jarInput = new ByteArrayInputStream(jarBytes);
            }

            List<Tuple2<ClassFile, URL>> classList = readClassesFromJarStream(jarInput, jarURL);

            final JarInformation theJarInformation = parsingClassFiles(classList, identifier);

            // Finally set jar information for current artifact, then return
            currentArtifact.setJarInformation(theJarInformation);

            return currentArtifact;

        } catch (IOException e) {
            log.error("IO error while accessing JAR for {}", identifier, e);
        } catch (FileNotFoundException ignored) {}
        return null;
    }

    /**
     * This method collects jar information from the classList and stores it into a JarInformation object.
     *
     * @param classList a list of classes to be parsed
     * @param identifier the current artifacts identifier
     * @return information that was parsed from the classList
     * @see JarInformation
     */
    public JarInformation parsingClassFiles(List<Tuple2<ClassFile, URL>> classList, ArtifactIdent identifier) {
        long codeSize = 0;
        long numClasses = classList.size();
        long numMethods = 0;
        long numFields = 0;

        Map<String, List<org.tudo.sse.model.jar.ClassFile>> packages = new HashMap<>();

        for(Tuple2<ClassFile, URL> classfile : classList) {
            ClassFile current = classfile._1;

            //tally up fields and methods first
            numMethods += current.methods().size();
            numFields += current.fields().size();

            List<Method> methods =  scala.jdk.CollectionConverters.SeqHasAsJava(classfile._1.methods()).asJava();
            for(Method method : methods) {
                //tally up bytecode for each method here
                if(method.body().isDefined()) {
                    codeSize += method.body().get().codeSize();
                }
            }

            //then check the package and see if it exists in the map, if it doesn't then add it
            if(!packages.containsKey(current.thisType().packageName())) {
                packages.put(current.thisType().packageName(), new ArrayList<>());
            }

            packages.get(current.thisType().packageName()).add(processClassFile(current));
        }

        JarInformation temp = new JarInformation(identifier);

        temp.setCodesize(codeSize);
        temp.setNumClassFiles(numClasses);
        temp.setNumMethods(numMethods);
        temp.setFields(numFields);
        temp.setNumPackages(packages.size());
        temp.setPackages(packages);

        return temp;
    }

    /**
     * This method processes a single classFile converting it from the opal classfile to a custom one.
     * @param classFile opal classfile to be converted
     * @return a custom classfile object
     * @see org.tudo.sse.model.jar.ClassFile
     */
    public org.tudo.sse.model.jar.ClassFile processClassFile(ClassFile classFile) {
        ObjType thisType = new ObjType(classFile.thisType().id(), classFile.thisType().fqn(), classFile.thisType().packageName());
        ObjType superType = null;
        List<ObjType> interfaces = new ArrayList<>();

        if(classFile.superclassType().isDefined()) {
            superType = new ObjType(classFile.superclassType().get().id(), classFile.superclassType().get().fqn(), classFile.superclassType().get().packageName());
        }

        if(!classFile.interfaceTypes().isEmpty()) {
            for(ClassType type : JavaConverters.asJava(classFile.interfaceTypes())) {
                interfaces.add(new ObjType(type.id(), type.fqn(), type.packageName()));
            }
        }

        return new org.tudo.sse.model.jar.ClassFile(classFile.accessFlags(), thisType, classFile.version(), superType, interfaces);
    }

    private List<Tuple2<ClassFile, URL>> readClassesFromJarStream(InputStream jarStream, URL source) throws JarResolutionException {
        final var entries = new ArrayList<Tuple2<ClassFile, URL>>();
        final var cfReader = newClassFileReader();

        try (JarInputStream jarInputStream = new JarInputStream(jarStream)){
            var currentEntry = jarInputStream.getNextJarEntry();
            while(currentEntry != null){
                final var entryName = currentEntry.getName().toLowerCase();
                if (entryName.endsWith(".class")){
                    cfReader.ClassFile(getEntryByteStream(jarInputStream))
                            .map(cf -> {
                                try {
                                    return new Tuple2<>((ClassFile) cf, new URL("jar:" + source + "!/" + entryName));
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .foreach(entries::add);
                }

                currentEntry = jarInputStream.getNextJarEntry();
            }
        } catch (Exception e) {
            // OPAL throws some unexpected exceptions when faced with malformed JARs in the index (e.g. ArrayIndexOutOfBounds)
            // Therefore, we catch all exceptions related to the processing of class files here, and wrap them.
            throw new JarResolutionException(e.getMessage());
        }
        return entries;
    }

    /**
     * Create a new class file reader with a weak reference to a new cache, so that cached instruction objects may be
     * released at some point.
     *
     * @return New class file reader instance without rewriting
     */
    private Java17FrameworkWithCaching newClassFileReader(){
        SoftReference<BytecodeInstructionsCache> cacheRef = new SoftReference<>(new BytecodeInstructionsCache());
        return new Java17FrameworkWithCaching(cacheRef.get());
    }

    private DataInputStream getEntryByteStream(InputStream in) {
        return new DataInputStream(in);
    }

}
