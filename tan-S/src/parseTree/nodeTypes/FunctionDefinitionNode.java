package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.Token;

public class FunctionDefinitionNode extends ParseNode {

    private String epilogueLabel;
    private String startLabel;

    public FunctionDefinitionNode(Token token) {
        super(token);
    }

    public static ParseNode withChildren(Token token, Type type,
            ParseNode id, ParseNode parameters, ParseNode block) {
        FunctionDefinitionNode result = new FunctionDefinitionNode(token);
        result.setType(type);
        result.appendChild(id);
        result.appendChild(parameters);
        result.appendChild(block);
        return result;
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public void setEpilogueLabel(String epilogueLabel) {
        this.epilogueLabel = epilogueLabel;
    }

    public String getEpilogueLabel() {
        return epilogueLabel;
    }

    public void setStartLabel(String startLabel) {
        this.startLabel = startLabel;
    }

    public String getStartLabel() {
        return startLabel;
    }
}
