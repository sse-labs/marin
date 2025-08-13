package org.tudo.sse.model.jar;

import java.util.ArrayList;
import java.util.List;

/**
 * A node for a class file for which the definition is available in the current JAR context.
 */
public class DefinedClassFileNode extends ClassFileNode {

    private final int accessFlags;
    private final long version;
    private final List<ClassFileNode> interfaceNodes;

    /**
     * Creates a new instance with the given attributes derived from the class file's definition.
     * @param accessFlags The class file access flags
     * @param thisType The class file's type definition
     * @param version The class file's version
     */
    public DefinedClassFileNode(int accessFlags, ObjType thisType, long version) {
        super(thisType);
        this.accessFlags = accessFlags;
        this.version = version;
        this.interfaceNodes = new ArrayList<>();
    }

    /**
     * Retrieves the access flags for this class file
     * @return Access flags as integer
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Retrieves the version for this class file
     * @return The class file version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Retrieves the list of interfaces for this class file node
     * @return Interface nodes implemented by this class
     */
    public List<ClassFileNode> getInterfaceNodes() {
        return interfaceNodes;
    }

    /**
     * Adds an interface that is implemented by this class
     * @param interfaceNode Class file node that represents an implemented interface
     */
    public void addInterfaceNode(ClassFileNode interfaceNode) {
        interfaceNodes.add(interfaceNode);
    }
}
