package org.tudo.sse.model.jar;

import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ArtifactInformation;

import java.util.List;
import java.util.Map;

/**
 * This class contains all the information that is parsed during jar resolution.
 * This information contains some statistics like codesize and number of class files, as well as a map of packages, contains lists of classfiles.
 */
public class JarInformation extends ArtifactInformation {
    private long codesize;
    private long numClassFiles;
    private long numMethods;
    private long fields;
    private long numPackages;
    private Map<String, List<ClassFile>> packages;

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
}
