package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

    private String endLabel;
    private String startLabel;

    public WhileStatementNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public void setEndLabel(String endLabel) {
        this.endLabel = endLabel;
    }

    public String getEndLabel() {
        return endLabel;
    }

    public void setStartLabel(String startLabel) {
        this.startLabel = startLabel;
    }

    public String getStartLabel() {
        return startLabel;
    }
}
