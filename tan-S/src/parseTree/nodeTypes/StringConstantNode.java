package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.StringToken;
import tokens.Token;

public class StringConstantNode extends ParseNode {

    public StringConstantNode(Token token) {
        super(token);
    }

    public String getValue() {
        assert token instanceof StringToken;
        return ((StringToken) token).getValue();
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visit(this);
    }
}
