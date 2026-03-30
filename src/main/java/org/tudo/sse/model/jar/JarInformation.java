package org.tudo.sse.model.jar;

import org.opalj.br.analyses.Project;
import org.opalj.br.package$;
import org.opalj.br.reader.Java17Framework$;
import org.opalj.br.reader.Java17LibraryFramework;
import org.opalj.br.reader.Java17LibraryFramework$;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ArtifactInformation;
import org.tudo.sse.resolution.FileNotFoundException;
import org.tudo.sse.utils.MarinOpalLogger;
import org.tudo.sse.utils.MavenCentralRepository;
import scala.Tuple2;
import scala.jdk.CollectionConverters;
import scala.runtime.BoxedUnit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;

/**
 * This class contains all the information that is parsed during jar resolution.
 * This information contains some statistics like codesize and number of class files, as well as a map of packages, contains lists of classfiles.
 */
public class JarInformation extends ArtifactInformation {

    private static final Logger log = LoggerFactory.getLogger(JarInformation.class);

    private long codesize;
    private long numClassFiles;
    private long numMethods;
    private long fields;
    private long numPackages;
    private Map<String, List<ClassFile>> packages;

    /**
     * Creates an empty JarInformation object for the given artifact identifier.
     * @param ident The identifier for which to create the JarInformation
     */
    public JarInformation(ArtifactIdent ident) {
        super(ident);
    }

    /**
     * Retrieves the size of the code processed
     * @return long representing the size of the code
     */
    public long getCodesize() {
        return codesize;
    }

    /**
     * Updates the value of the codeSize
     * @param codesize new codesize to update with
     */
    public void setCodesize(long codesize) {
        this.codesize = codesize;
    }

    /**
     * Retrieves the number of classfile
     * @return long of classfiles
     */
    public long getNumClassFiles() {
        return numClassFiles;
    }

    /**
     * Updates the value of number of classFiles
     * @param numClassFiles new number of classFiles
     */
    public void setNumClassFiles(long numClassFiles) {
        this.numClassFiles = numClassFiles;
    }

    /**
     * Retrieves the number of methods
     * @return long representing the number of methods
     */
    public long getNumMethods() {
        return numMethods;
    }

    /**
     * Updates the value of the number of methods
     * @param numMethods new number of methods
     */
    public void setNumMethods(long numMethods) {
        this.numMethods = numMethods;
    }

    /**
     * Retrieves the number of fields in the jar
     * @return long representing the number of fields
     */
    public long getFields() {
        return fields;
    }

    /**
     * Updates the number of fields
     * @param fields new number of fields
     */
    public void setFields(long fields) {
        this.fields = fields;
    }

    /**
     * Retrieves the number of packages in the jar
     * @return long representing the number of packages
     */
    public long getNumPackages() {
        return numPackages;
    }

    /**
     * Updates the value of number of packages
     * @param numPackages new number of packages
     */
    public void setNumPackages(long numPackages) {
        this.numPackages = numPackages;
    }

    /**
     * Retrieves a map of the packages in the jar
     * @return a map where each package name is mapped to a list of the classfiles within said package
     */
    public Map<String, List<ClassFile>> getPackages() {
        return packages;
    }

    /**
     * Updates the packages map
     * @param packages new map to update the current one with
     */
    public void setPackages(Map<String, List<ClassFile>> packages) {
        this.packages = packages;
    }

    /**
     * Downloads the JAR file associated with this ArtifactInformation into a temporary directory and returns the file
     * object representing it. Note that all temporary files will be deleted once the JVM shuts down.
     * @return The File object representing the JAR file
     * @throws IOException If any IO errors occur
     * @throws FileNotFoundException If no JAR file eixsts for this ArtifactInformation
     */
    public File getJarFile() throws IOException, FileNotFoundException {
        String prefix = ident.getGA().replace(".","_").replace(":","__");
        Path theFile = Files.createTempFile(prefix,".jar");

        try(InputStream is = getJarFileInputStream()){
            Files.copy(is, theFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return theFile.toFile();
    }

    /**
     * Creates and opens an InputStream to the JAR file referenced by this ArtifactInformation.
     * @return The JAR file InputStream
     * @throws IOException If IO errors occur while opening the stream
     * @throws FileNotFoundException If no JAR file exists for this ArtifactInformation
     */
    public InputStream getJarFileInputStream() throws IOException, FileNotFoundException {
        return MavenCentralRepository.getInstance().openJarFileInputStream(this.ident);
    }

    /**
     * Creates and opens a JarInputStream to the JAR file referenced by this ArtifactInformation.
     * @return The JarInputStream
     * @throws IOException If IO errors occur while opening the stream
     * @throws FileNotFoundException If no JAR file exists for this ArtifactInformation
     */
    public JarInputStream getJarInputStream() throws IOException, FileNotFoundException {
        return getJarInputStream(this.ident);
    }

    /**
     * <p>
     * Retrieves a list of all class files contained in JAR file referenced by this ArtifactInformation. The class
     * files are returned as represented by the OPAL framework.
     * </p>
     * <p>
     * <b>Note:</b> OPAL classes are not designed to be used in large scale analyses. Over time, OPAL's internal caches
     * will continue to claim heap space that is never freed, so eventually the program will crash with an
     * OutOfMemory error. There is currently no good way to avoid this, so use project instances only if you
     * really need them.
     * </p>
     * @return List of OPAL class file representations, or null if no JAR file was found
     * @throws IOException If reading the JAR file fails
     */
    public List<org.opalj.br.ClassFile> getOpalClassFileRepresentations() throws IOException {
        return getOpalClassFileRepresentations(this.ident);
    }

    /**
     * <p>
     * Retrieves all class files for the JAR referenced by this ArtifactInformation, and uses them to initialize an
     * OPAL project instance. This instance can be used to conduct complex static program analyses.
     * </p>
     * <p>
     * <b>Note:</b> OPAL projects are not designed to be used in large scale analyses. Over time, OPAL's internal caches
     * will continue to claim heap space that is never freed, so eventually the program will crash with an
     * OutOfMemory error. There is currently no good way to avoid this, so use project instances only if you
     * really need them.
     * </p>
     * @return The OPAL project instance, or null if no JAR file was found
     * @throws IOException If reading the JAR file fails
     */
    public Project<String> getOpalProject() throws IOException {
        return getOpalProject(MarinOpalLogger.newInfoLogger(LoggerFactory.getLogger("[OPAL-Project]")));
    }

    /**
     * <p>
     * Retrieves all class files for the JAR referenced by this ArtifactInformation, and uses them to initialize an
     * OPAL project instance. This instance can be used to conduct complex static program analyses.
     * </p>
     * <p>
     * <b>Note:</b> OPAL projects are not designed to be used in large scale analyses. Over time, OPAL's internal caches
     * will continue to claim heap space that is never freed, so eventually the program will crash with an
     * OutOfMemory error. There is currently no good way to avoid this, so use project instances only if you
     * really need them.
     * </p>
     * @return The OPAL project instance, or null if no JAR file was found
     * @param projectLogger A custom logger instance to configure the amount of output that is produced by OPAL.
     * @throws IOException If reading the JAR file fails
     */
    public Project<String> getOpalProject(MarinOpalLogger projectLogger) throws IOException {
        try(JarInputStream jarStream = getJarInputStream()){
            return Project.apply(Java17Framework$.MODULE$.ClassFiles(() -> jarStream).map( t -> new Tuple2<>((org.opalj.br.ClassFile)t._1, t._2)), projectLogger);
        } catch(FileNotFoundException fnfx){
            log.error("No JAR file found for {}", ident.getCoordinates(), fnfx);
            return null;
        }
    }

    /**
     * Initializes an OPAL project instance for the current JAR and the given set of dependencies. This project instance
     * can be used to conduct complex, static whole-program analyses. Dependencies will be loaded as interfaces only,
     * meaning no actual method implementations (i.e. bytecode) will be present for analysis.
     *
     * @param dependencies The transitive set of dependencies to use. Can be obtained using the PomInformation instance
     *                     for this artifact
     * @return The OPAL project instance, or null if no main project JAR was found
     * @throws IOException If an IO error occurs while loading the main JAR or the dependencies
     */
    public Project<String> getOpalProject(Set<ArtifactIdent> dependencies) throws IOException {
        return getOpalProject(dependencies, false, true);
    }

    /**
     * Initializes an OPAL project instance for the current JAR and the given set of dependencies. This project instance
     * can be used to conduct complex, static whole-program analyses.
     * @param dependencies The transitive set of dependencies to use. Can be obtained using the PomInformation instance
     *                     for this artifact
     * @param fullyLoadLibraries Whether to fully load the dependency's class file contents. If set to false, only their
     *                           interface definitions (class names, method signatures) are loaded, not their actual
     *                           content.
     * @param breakOnFailure If set to true, any IO exception when loading dependencies interrupts the method execution
     *                       and throws the exception up to the calling method. If set to false, dependencies will be
     *                       loaded in a best-effort fashion, and errors will only be logged to the console.
     * @return The OPAL project instance, or null if no main project JAR was found
     * @throws IOException If an IO error occurs while loading the main JAR, or the dependencies (when breakOnFailure is
     *                     enabled).
     */
    public Project<String> getOpalProject(Set<ArtifactIdent> dependencies, boolean fullyLoadLibraries, boolean breakOnFailure) throws IOException {
        return getOpalProject(dependencies, fullyLoadLibraries, breakOnFailure,
                MarinOpalLogger.newInfoLogger(LoggerFactory.getLogger("[OPAL-Project]")));
    }

    /**
     * Initializes an OPAL project instance for the current JAR and the given set of dependencies. This project instance
     * can be used to conduct complex, static whole-program analyses.
     * @param dependencies The transitive set of dependencies to use. Can be obtained using the PomInformation instance
     *                     for this artifact
     * @param fullyLoadLibraries Whether to fully load the dependency's class file contents. If set to false, only their
     *                           interface definitions (class names, method signatures) are loaded, not their actual
     *                           content.
     * @param breakOnFailure If set to true, any IO exception when loading dependencies interrupts the method execution
     *                       and throws the exception up to the calling method. If set to false, dependencies will be
     *                       loaded in a best-effort fashion, and errors will only be logged to the console.
     * @param projectLogger A custom logger instance to configure the amount of output that is produced by OPAL.
     * @return The OPAL project instance, or null if no main project JAR was found
     * @throws IOException If an IO error occurs while loading the main JAR, or the dependencies (when breakOnFailure is
     *                     enabled).
     */
    public Project<String> getOpalProject(Set<ArtifactIdent> dependencies, boolean fullyLoadLibraries,
                                          boolean breakOnFailure, MarinOpalLogger projectLogger) throws IOException {
        List<Tuple2<org.opalj.br.ClassFile, String>> allLibraryClasses = new ArrayList<>();

        for(ArtifactIdent dependency : dependencies){
            try(JarInputStream jarStream = getJarInputStream(dependency)){
                addClassFilesToList(allLibraryClasses, jarStream, fullyLoadLibraries);
            } catch (IOException iox){
                log.error("Failed to obtain dependency JAR {} for project {}", dependency, ident, iox);
                if(breakOnFailure) throw iox;
            } catch (FileNotFoundException fnfx){
                // Empty, there might be dependencies without a JAR
            }
        }

        List<Tuple2<org.opalj.br.ClassFile, String>> allProjectClasses = new ArrayList<>();
        try(JarInputStream jarStream = getJarInputStream()){
            addClassFilesToList(allProjectClasses, jarStream, true);
        } catch (FileNotFoundException fnfx){
            log.error("No JAR file found for {}", ident.getCoordinates(), fnfx);
            return null;
        }

        return Project.apply(CollectionConverters.ListHasAsScala(allProjectClasses).asScala().toSeq(),
                CollectionConverters.ListHasAsScala(allLibraryClasses).asScala().toSeq(),
                !fullyLoadLibraries,
                CollectionConverters.ListHasAsScala(new ArrayList<org.opalj.br.ClassFile>()).asScala().toSeq(),
                (a, b) -> BoxedUnit.UNIT,
                package$.MODULE$.BaseConfig(),
                projectLogger);
    }

    private List<org.opalj.br.ClassFile> getOpalClassFileRepresentations(ArtifactIdent ident) throws IOException {
        List<org.opalj.br.ClassFile> opalCFs = new ArrayList<>();

        try(JarInputStream jarStream = getJarInputStream(ident)){
            Java17LibraryFramework$.MODULE$.ClassFiles(() -> jarStream)
                    .foreach(cf -> opalCFs.add((org.opalj.br.ClassFile)cf._1));
        } catch(FileNotFoundException fnfx){
            return null;
        }

        return opalCFs;
    }

    private JarInputStream getJarInputStream(ArtifactIdent ident) throws IOException, FileNotFoundException {
        return new JarInputStream(MavenCentralRepository.getInstance().openJarFileInputStream(ident));
    }

    private void addClassFilesToList(List<Tuple2<org.opalj.br.ClassFile, String>> cfList, JarInputStream jarStream, boolean loadFully){
        Java17LibraryFramework reader = loadFully ? Java17Framework$.MODULE$ : Java17LibraryFramework$.MODULE$;
        CollectionConverters
                .SeqHasAsJava(reader.ClassFiles(() -> jarStream))
                .asJava()
                .forEach( tuple ->
                    cfList.add(new Tuple2<>((org.opalj.br.ClassFile)tuple._1, tuple._2))
                );

    }
}
