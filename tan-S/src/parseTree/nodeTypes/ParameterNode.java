package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class ParameterNode extends ParseNode {

    public ParameterNode(Token token) {
        super(token);
    }

    public static ParseNode withChildren(Token token, Type type, ParseNode identifier) {
        ParameterNode result = new ParameterNode(token);
        result.setType(type);
        result.appendChild(identifier);
        return result;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

}
