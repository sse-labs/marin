package org.tudo.sse.model.jar;

import java.util.List;

/**
 * This class contains the information parsed from the opal classfiles.
 */
public class ClassFile {
    private final int accessFlags;
    private final ObjType thistype;
    private final long version;
    private final ObjType superType;
    private final List<ObjType> interfaceTypes;

    public ClassFile(int accessFlags, ObjType thistype, long version, ObjType superType, List<ObjType> interfaceTypes) {
        this.accessFlags = accessFlags;
        this.thistype = thistype;
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
