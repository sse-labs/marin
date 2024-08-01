package org.tudo.sse.resolution;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opalj.br.ClassFile;
import org.opalj.br.Method;
import org.opalj.br.ObjectType;
import org.opalj.br.analyses.Project$;
import org.opalj.br.package$;
import org.opalj.br.reader.Java16LibraryFramework;
import org.opalj.bytecode.BytecodeProcessingFailedException;
import org.opalj.log.GlobalLogContext$;
import org.tudo.sse.ArtifactFactory;
import org.tudo.sse.model.Artifact;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.jar.JarInformation;
import org.tudo.sse.model.jar.ObjType;
import org.tudo.sse.utils.MavenCentralRepository;
import scala.Tuple2;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarInputStream;

import scala.collection.JavaConverters;

/**
 * This class is set up for the resolution of jar files on the maven central repository. The information collected is then stored in a JarInformation object.
 * @see JarInformation
 */
public class JarResolver {
    private final Java16LibraryFramework cfReader = Project$.MODULE$.JavaClassFileReader(GlobalLogContext$.MODULE$, package$.MODULE$.BaseConfig());
    private static final MavenCentralRepository MavenRepo = MavenCentralRepository.getInstance();
    private static final Logger log = LogManager.getLogger(JarResolver.class);

    /**
     * This method resolves jar artifacts from a given list of artifact identifiers.
     *
     * @param identifiers artifact identifiers used to retrieve jar artifacts to process
     * @return a list of resolved artifacts
     * @see Artifact
     */
    public List<Artifact> resolveJars(List<ArtifactIdent> identifiers) {
        List<Artifact> toReturn = new ArrayList<>();
        int count = 0;
        for(ArtifactIdent current : identifiers) {
            try {
                toReturn.add(parseJar(current));
            } catch (JarResolutionException e) {
                log.error(e);
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
     * @return a resolved artifact
     * @throws JarResolutionException when there is an issue resolving the given jar artifact
     */
    public Artifact parseJar(ArtifactIdent identifier) throws JarResolutionException {
        if(ArtifactFactory.getArtifact(identifier) != null && Objects.requireNonNull(ArtifactFactory.getArtifact(identifier)).getJarInformation() != null) {
            return ArtifactFactory.getArtifact(identifier);
        }

        try {
            boolean output = true;
            URL jarURL = identifier.getMavenCentralJarUri().toURL();
            InputStream jarInput = MavenRepo.openJarFileInputStream(identifier);
            //TODO: move output to mavenCentralAnalysis and into jarResolver constructor
            if(output) {
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
                //TODO: Write to a file
                jarInput = new ByteArrayInputStream(jarBytes);
            }

            List<Tuple2<ClassFile, URL>> classList = readClassesFromJarStream(jarInput, jarURL);
            return ArtifactFactory.createArtifact(parsingClassFiles(classList, identifier));

        } catch (FileNotFoundException | IOException e) {
            log.error(e);
        }
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

            List<Method> methods =  JavaConverters.seqAsJavaList(classfile._1.methods());
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
            for(ObjectType type : JavaConverters.asJava(classFile.interfaceTypes())) {
                interfaces.add(new ObjType(type.id(), type.fqn(), type.packageName()));
            }
        }

        return new org.tudo.sse.model.jar.ClassFile(classFile.accessFlags(), thisType, classFile.version(), superType, interfaces);
    }

    public List<Tuple2<ClassFile, URL>> readClassesFromJarStream(InputStream jarStream, URL source) throws JarResolutionException {
        var entries = new ArrayList<Tuple2<ClassFile, URL>>();
        JarInputStream jarInputStream;

        try {
            jarInputStream = new JarInputStream(jarStream);
            var currentEntry = jarInputStream.getNextJarEntry();
            while(currentEntry != null){
                final var entryName = currentEntry.getName().toLowerCase();
                if (entryName.endsWith(".class")){
                    cfReader.ClassFile(getEntryByteStream(jarInputStream))
                            .map(cf -> {
                                try {
                                    return new Tuple2(cf, new URL("jar:" +source + "!/" + entryName));
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .foreach(entries::add);
                }

                currentEntry = jarInputStream.getNextJarEntry();
            }
        } catch (IOException | IllegalArgumentException | BytecodeProcessingFailedException e) {
            throw new JarResolutionException(e.getMessage());
        }
            return entries;
    }

    private DataInputStream getEntryByteStream(InputStream in) throws IOException  {

        var baos = new ByteArrayOutputStream();
        var buffer = new byte[32 * 1024];

        int bytesRead = in.read(buffer);

        while(bytesRead > 0){
            baos.write(buffer, 0, bytesRead);
            baos.close();
            baos.flush();
            bytesRead = in.read(buffer);
        }

        return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

}
