package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class FunctionInvocationNode extends ParseNode {

    public FunctionInvocationNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public static ParseNode withChildren(Token token, ParseNode literal, ParseNode expressionList) {
        FunctionInvocationNode result = new FunctionInvocationNode(token);
        result.appendChild(literal);
        result.appendChild(expressionList);
        return result;
    }
}
