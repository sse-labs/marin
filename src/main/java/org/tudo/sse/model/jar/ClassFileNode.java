package org.tudo.sse.model.jar;

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class represents a node inside a JAR's type hierarchy.
 */
public abstract class ClassFileNode {

    private final ObjType thisType;
    private ClassFileNode superclass;
    private final List<ClassFileNode> children;

    /**
     * Creates a new class file node for the given type.
     *
     * @param thisType The type of this current node
     */
    protected ClassFileNode(ObjType thisType) {
        this.thisType = thisType;
        this.children = new ArrayList<>();
    }

    /**
     * Retrieves the parent node of this node
     * @return The parent node, or null of no parent is set
     */
    public ClassFileNode getSuperClass() {
        return superclass;
    }

    /**
     * Sets the parent node for this node
     * @param superclass The parent node
     */
    public void setSuperClass(ClassFileNode superclass) {
        this.superclass = superclass;
    }

    /**
     * Retrieves the type associated with this node
     * @return This node's type
     */
    public ObjType getThisType() {
        return thisType;
    }

    /**
     * Retrieves the list of children for this node.
     * @return The List of child nodes
     */
    public List<ClassFileNode> getChildren() {
        return children;
    }

    /**
     * Adds a child to this node
     * @param child The child node to add
     */
    public void addChild(ClassFileNode child) {
        children.add(child);
    }
}
