package org.tudo.sse.model.jar;

import java.util.List;

/**
 * This class represents a JVM class file contained within a JAR. Information contained here are parsed from OPAL's
 * class file representation.
 */
public class ClassFile {
    private final int accessFlags;
    private final ObjType thistype;
    private final long version;
    private final ObjType superType;
    private final List<ObjType> interfaceTypes;

    /**
     * Creates a new ClassFile instance with the given attributes.
     *
     * @param accessFlags The classes access flags encoded as integer
     * @param thisType The type defined within the class
     * @param version The class file version
     * @param superType The classes super type
     * @param interfaceTypes A list of all interface types implemented by this class
     */
    public ClassFile(int accessFlags, ObjType thisType, long version, ObjType superType, List<ObjType> interfaceTypes) {
        this.accessFlags = accessFlags;
        this.thistype = thisType;
        this.version = version;
        this.superType = superType;
        this.interfaceTypes = interfaceTypes;
    }

    /**
     * Retrieves the access flags
     * @return an int representing the different accessflags for the classfile
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Retrieves the objType object for this classfile
     * @return objtype object for this classfile
     */
    public ObjType getThistype() {
        return thistype;
    }

    /**
     * Retrieves the classfile version
     * @return long representing the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Retrieves the objType for the superclass
     * @return objType object of the superclass
     */
    public ObjType getSuperType() {
        return superType;
    }

    /**
     * Retrieves the objtypes for interfaces
     * @return a list of interface objtypes
     */
    public List<ObjType> getInterfaceTypes() {
        return interfaceTypes;
    }
}
