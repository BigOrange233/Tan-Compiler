package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.CharToken;
import tokens.Token;

public class CharConstantNode extends ParseNode {

    public CharConstantNode(Token token) {
        super(token);
    }

    public char getValue() {
        return ((CharToken) token).getValue();
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }
}
