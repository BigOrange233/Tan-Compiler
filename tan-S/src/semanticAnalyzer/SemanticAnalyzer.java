package semanticAnalyzer;

import parseTree.*;
import parseTree.nodeTypes.IdentifierNode;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;


public class SemanticAnalyzer {
    ParseNode ASTree;

    public static ParseNode analyze(ParseNode ASTree) {
        SemanticAnalyzer analyzer = new SemanticAnalyzer(ASTree);
        return analyzer.analyze();
    }

    public SemanticAnalyzer(ParseNode ASTree) {
        this.ASTree = ASTree;
    }

    public ParseNode analyze() {
        ParseNodeVisitor sav_p1 = new SementicAnalysisFirstPassVisitor();
        ASTree.accept(sav_p1);
        SemanticAnalysisVisitor sav = new SemanticAnalysisVisitor();
        ASTree.accept(sav);
        sav.promoter.promote();
        return ASTree;
    }

    public static void addBinding(IdentifierNode identifier, Type type) {
        Scope scope = identifier.getLocalScope();
        Binding binding = scope.createBinding(identifier, type);
        identifier.setBinding(binding);
    }
}
