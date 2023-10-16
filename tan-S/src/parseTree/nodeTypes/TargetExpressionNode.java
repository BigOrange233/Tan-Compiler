package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class TargetExpressionNode extends ParseNode {

    public TargetExpressionNode(Token token) {
        super(token);
    }

    public boolean isMutable() {
        assert this.nChildren() == 1;
        return this.child(0).isMutable();
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

}
