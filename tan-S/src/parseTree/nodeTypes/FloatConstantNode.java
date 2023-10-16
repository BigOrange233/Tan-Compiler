package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.FloatToken;
import tokens.Token;

public class FloatConstantNode extends ParseNode {

    public FloatConstantNode(Token token) {
        super(token);
    }

    public double getValue() {
        return ((FloatToken) token).getValue();
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }

}
