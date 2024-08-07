package org.tudo.sse.model.jar;

import java.util.ArrayList;
import java.util.List;

public class ClassFileNode {
    private final ObjType thistype;
    private ClassFileNode superclass;
    private List<ClassFileNode> children;

    public ClassFileNode(ObjType thistype) {
        this.thistype = thistype;
        children = new ArrayList<>();
    }

    public ClassFileNode getSuperClass() {
        return superclass;
    }

    public void setSuperClass(ClassFileNode superclass) {
        this.superclass = superclass;
    }

    public ObjType getThistype() {
        return thistype;
    }

    public List<ClassFileNode> getChildren() {
        return children;
    }

    public void addChild(ClassFileNode child) {
        children.add(child);
    }
}
