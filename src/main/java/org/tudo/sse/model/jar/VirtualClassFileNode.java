package org.tudo.sse.model.jar;

/**
 * Artificial node representing classes that are referenced but not contained in a given JAR.
 */
public class VirtualClassFileNode extends ClassFileNode {

    /**
     * Creates a new NotFoundNode for the given type. This indicates that the type was referenced by another type or
     * class, but no definition of this type was found in the given JAR.
     *
     * @param thisType The type that was not found
     */
    public VirtualClassFileNode(ObjType thisType) {
        super(thisType);
    }
}
