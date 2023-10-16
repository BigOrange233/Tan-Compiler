package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ReturnStatementNode extends ParseNode {

    public ReturnStatementNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public Object getFunctionDefinitionNode() {
        ParseNode parent = this.getParent();
        while (parent != null && !(parent instanceof FunctionDefinitionNode)) {
            parent = parent.getParent();
        }
        assert parent != null : "Return statement not in function definition";
        return parent;
    }

}
