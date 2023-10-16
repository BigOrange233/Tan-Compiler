package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ForStatementNode extends ParseNode {

    private String idAddrLabel;
    private String toValueLabel;
    private String startLabel;
    private String endLabel;
    private String continueLabel;

    public ForStatementNode(Token token) {
        super(token);
    }

    public void accept(ParseNodeVisitor visitor) {
        visitor.visitEnter(this);
        visitChildren(visitor);
        visitor.visitLeave(this);
    }

    public static ParseNode withChildren(Token forToken, ParseNode id, ParseNode from, ParseNode to,
            ParseNode body) {
        ParseNode result = new ForStatementNode(forToken);
        result.appendChild(id);
        result.appendChild(from);
        result.appendChild(to);
        result.appendChild(body);
        return result;
    }

    public void setIdAddrLabel(String idAddrLabel) {
        this.idAddrLabel = idAddrLabel;
    }

    public void setToValueLabel(String toValueLabel) {
        this.toValueLabel = toValueLabel;
    }

    public void setStartLabel(String startLabel) {
        this.startLabel = startLabel;
    }

    public void setEndLabel(String endLabel) {
        this.endLabel = endLabel;
    }

    public void setContinueLabel(String continueLabel) {
        this.continueLabel = continueLabel;
    }

    public String getIdAddrLabel() {
        return idAddrLabel;
    }

    public String getToValueLabel() {
        return toValueLabel;
    }

    public String getEndLabel() {
        return endLabel;
    }

    public String getContinueLabel() {
        return continueLabel;
    }

    public String getStartLabel() {
        return startLabel;
    }
}
