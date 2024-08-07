package org.tudo.sse.model.jar;

import java.util.ArrayList;
import java.util.List;

public class FoundInfoNode extends ClassFileNode {
    private final int accessFlags;
    private final long version;
    private List<ClassFileNode> interfaceNodes;


    public FoundInfoNode(int accessFlags, ObjType thisType, long version) {
        super(thisType);
        this.accessFlags = accessFlags;
        this.version = version;
        interfaceNodes = new ArrayList<>();
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public long getVersion() {
        return version;
    }

    public List<ClassFileNode> getInterfaceNodes() {
        return interfaceNodes;
    }

    public void addInterfaceNode(ClassFileNode interfaceNode) {
        interfaceNodes.add(interfaceNode);
    }
}
