package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ArrayIndexNode extends ParseNode {

    public ArrayIndexNode(Token token) {
        super(token);
    }

    public static ParseNode withChildren(Token openBracket, ParseNode arrayNode, ParseNode indexNode) {
        ParseNode node = new ArrayIndexNode(openBracket);
        node.appendChild(arrayNode);
        node.appendChild(indexNode);
        return node;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public boolean isMutable() {
        return true;
    }
}
