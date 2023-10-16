import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface Type {
    // 分配内存时需要用到
    int getSize();
}

enum PrimitiveType implements Type {
    BOOLEAN(1),
    CHARACTER(1),
    INTEGER(4),
    FLOAT(8),
    STRING(4),
    ERROR(0), // use as a value when a syntax error has occurred
    NO_TYPE(0); // use as a value when no type has been assigned.

    private int sizeInBytes;

    private PrimitiveType(int size) {
        this.sizeInBytes = size;
    }

    @Override
    public int getSize() {
        return sizeInBytes;
    }
}

class Signature {

}

interface Node {
    void accept(NodeVisitor visitor);
    void appendChild(Node child);
}

class NodeOne implements Node {
    private List<Node> children = new ArrayList<Node>();

    public void accept(NodeVisitor visitor) {
        visitor.visitEnter(this);
        for (Node child : children) {
            child.accept(visitor);
        }
        visitor.visitLeave(this);
    }

    public void appendChild(Node child) {
        children.add(child);
    }
}

class NodeTwo implements Node {
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    public void appendChild(Node child) {
        throw new UnsupportedOperationException();
    }
}

interface NodeVisitor {
    void visit(NodeOne node);
    void visitLeave(NodeOne nodeOne);
    void visitEnter(NodeOne nodeOne);
    void visit(NodeTwo node);

    static class Default implements NodeVisitor {

        @Override
        public void visit(NodeOne node) {}

        @Override
        public void visitLeave(NodeOne nodeOne) {}

        @Override
        public void visitEnter(NodeOne nodeOne) {}

        @Override
        public void visit(NodeTwo node) {}

    }
}

class VisitorOne extends NodeVisitor.Default {

    @Override
    public void visit(NodeTwo node) {
        System.out.println("VisitorOne: NodeTwo");
    }

    @Override
    public void visitEnter(NodeOne nodeOne) {
        // pre-order traversal
        System.out.println("VisitorOne: NodeOne");
    }
}

public class demo {
    
    public static void main(String[] args) {
        System.out.println("Hello, world!");
        Node nodeOne = new NodeOne();
        nodeOne.appendChild(new NodeTwo());
        NodeVisitor visitorOne = new VisitorOne();
        nodeOne.accept(visitorOne);
    }
}
