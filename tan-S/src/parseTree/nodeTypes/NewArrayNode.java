package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class NewArrayNode extends ParseNode {

    public NewArrayNode(Token token) {
        super(token);
    }

    public static ParseNode withChildren(Token newToken, Type type, ParseNode expression) {
        NewArrayNode result = new NewArrayNode(newToken);
        result.setType(type);
        result.appendChild(expression);
        return result;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }
}
