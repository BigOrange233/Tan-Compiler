package semanticAnalyzer;

import java.util.ArrayList;
import java.util.List;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.ParameterNode;
import parseTree.nodeTypes.ProgramNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.Type;
import symbolTable.Scope;

public class SementicAnalysisFirstPassVisitor extends ParseNodeVisitor.Default {

    @Override
    public void visitEnter(ProgramNode node) {
        enterProgramScope(node);
    }

    private void enterProgramScope(ParseNode node) {
        Scope scope = Scope.createProgramScope();
        node.setScope(scope);
    }

    @Override
    public void visitLeave(FunctionDefinitionNode node) {
        IdentifierNode identifier = (IdentifierNode) node.child(0);
        ParseNode parameterListNode = node.child(1);

        Type returnType = node.getType();
        List<Type> parameterTypes = new ArrayList<Type>();
        for (ParseNode parameterNode : parameterListNode.getChildren()) {
            parameterTypes.add(parameterNode.getType());
        }

        FunctionSignature signature = new FunctionSignature(1, parameterTypes, returnType);
        node.setType(signature);

        identifier.setType(node.getType());
        // identifier.setMutable(false);
        SemanticAnalyzer.addBinding(identifier, node.getType());
    }

    @Override
    public void visitLeave(ParameterNode node) {
    }
}
