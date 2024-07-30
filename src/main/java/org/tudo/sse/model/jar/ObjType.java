package org.tudo.sse.model.jar;

/**
 * This class contains type information for each classfile, such as an id, fully qualified name, and which package it belongs to.
 */
public class ObjType {
    private final long id;
    private final String fqn;
    private final String packageName;

    public ObjType(long id, String fqn, String packageName) {
        this.id = id;
        this.fqn = fqn;
        this.packageName = packageName;
    }

    /**
     * Retrieves the id for the classfile
     * @return long representing the id
     */
    public long getId() {
        return id;
    }

    /**
     * Retrieves the fully qualified name of the classfile
     * @return string representing the fully qualified name of the classfile
     */
    public String getFqn() {
        return fqn;
    }

    /**
     * Retrieves the name that the current classfile resides in
     * @return string containing the packagename
     */
    public String getPackageName() {
        return packageName;
    }
}
