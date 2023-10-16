package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class CastNode extends ParseNode {

    Type castType;
    FunctionSignature signature;

    public CastNode(Token token) {
        super(token);
        signature = FunctionSignature.nullInstance();
    }

    public static CastNode withChildren(Token castToken, Type type, ParseNode expression) {
        CastNode result = new CastNode(castToken);
        result.castType = type;
        result.appendChild(expression);
        return result;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public Type getCastType() {
        return castType;
    }

    public void setSignature(FunctionSignature signature) {
        this.signature = signature;
    }

    public FunctionSignature getSignature() {
        return signature;
    }
    
}
