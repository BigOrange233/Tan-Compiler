package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import asmCodeGenerator.operators.LengthArraySCG;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.PseudoOperator;
import logging.TanLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.CharConstantNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.ArrayIndexNode;
import parseTree.nodeTypes.AssignmentStatementNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatConstantNode;
import parseTree.nodeTypes.ForStatementNode;
import parseTree.nodeTypes.FunctionDefinitionNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewArrayNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.OperatorNode;
import parseTree.nodeTypes.ParameterNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TargetExpressionNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {

    public Promoter promoter = new Promoter();

    @Override
    public void visitLeave(ParseNode node) {
        throw new RuntimeException(
                "Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
    }

    ///////////////////////////////////////////////////////////////////////////
    // constructs larger than statements

    public void visitLeave(ProgramNode node) {
        leaveScope(node);
    }

    public void visitEnter(BlockStatementNode node) {
        if (node.getParent() instanceof FunctionDefinitionNode) {
            Scope parameterScope = node.getLocalScope();
            node.setScope(parameterScope.createProcedureScope());
        } else {
            enterSubscope(node);
        }
    }

    public void visitLeave(BlockStatementNode node) {
        leaveScope(node);
    }


    ///////////////////////////////////////////////////////////////////////////
    // helper methods for scoping.

    private void enterSubscope(ParseNode node) {
        Scope baseScope = node.getLocalScope();
        Scope scope = baseScope.createSubscope();
        node.setScope(scope);
    }

    private void leaveScope(ParseNode node) {
        node.getScope().leave();
    }

    ///////////////////////////////////////////////////////////////////////////
    // statements and declarations
    @Override
    public void visitLeave(PrintStatementNode node) {}

    @Override
    public void visitLeave(DeclarationNode node) {
        if (node.child(0) instanceof ErrorNode) {
            node.setType(PrimitiveType.ERROR);
            return;
        }

        IdentifierNode identifier = (IdentifierNode) node.child(0);
        ParseNode initializer = node.child(1);

        Type declarationType = initializer.getType();
        node.setType(declarationType);
        
        identifier.setType(declarationType);
        identifier.setMutable(node.getToken().isLextant(Keyword.VAR));
        addBinding(identifier, declarationType);
    }

    public void visitLeave(AssignmentStatementNode node) {
        if (node.child(0) instanceof ErrorNode) {
            node.setType(PrimitiveType.ERROR);
            return;
        }

        ParseNode target = node.child(0);
        if (!target.isMutable()) {
            logError("Cannot assign to immutable target");
            node.setType(PrimitiveType.ERROR);
            return;
        }
        ParseNode expression = node.child(1);
        Type targetType = target.getType();
        Type expressionType = expression.getType();
        if (!targetType.equals(expressionType)) {
            List<Type> types = new ArrayList<Type>();
            types.add(targetType);
            types.add(expressionType);
            typeCheckError(node, types);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // expressions
    @Override
    public void visitLeave(OperatorNode node) {
        List<Type> childTypes;
        if (node.nChildren() == 1) {
            ParseNode child = node.child(0);
            childTypes = Arrays.asList(child.getType());
        } else {
            assert node.nChildren() == 2;
            ParseNode left = node.child(0);
            ParseNode right = node.child(1);

            childTypes = Arrays.asList(left.getType(), right.getType());
        }

        Lextant operator = operatorFor(node);

        // length array operator
		if (childTypes.size() == 1 && childTypes.get(0) instanceof ArrayType && operator == Keyword.LENGTH) {
            FunctionSignature signature = new FunctionSignature(new LengthArraySCG(), new ArrayType(), PrimitiveType.INTEGER);
            node.setSignature(signature);
            node.setType(signature.resultType());
            return;
		}

        FunctionSignature signature = FunctionSignatures.signature(operator, childTypes);

        if (signature.accepts(childTypes)) {
            node.setSignature(signature);
            node.setType(signature.resultType());
        } else {
            typeCheckError(node, childTypes);
        }
    }

    private Lextant operatorFor(OperatorNode node) {
        LextantToken token = (LextantToken) node.getToken();
        return token.getLextant();
    }

    public void visitLeave(CastNode node) {
        ParseNode expression = node.child(0);
        Type castType = node.getCastType();
        Type expressionType = expression.getType();
        if (castType == PrimitiveType.ERROR || expressionType == PrimitiveType.ERROR) {
            node.setType(PrimitiveType.ERROR);
            return;
        }
        if (expressionType.equals(castType)) {
            node.setType(castType);
            return;
        }

        List<Type> childTypes = Arrays.asList(expressionType, castType);
        FunctionSignature signature = FunctionSignatures.signature(PseudoOperator.CAST, childTypes);

        if(signature.accepts(childTypes)) {
            node.setSignature(signature);
            node.setType(signature.resultType());
            return;
        }

        logError("cannot cast type '" + childTypes.get(0).infoString() + "' to type '" + childTypes.get(1).infoString() + "'");
        node.setType(PrimitiveType.ERROR);
    }
        
    @Override
    public void visitLeave(TargetExpressionNode node) {
        ParseNode target = node.child(0);
        if (target instanceof IdentifierNode) {
            IdentifierNode identifier = (IdentifierNode) target;
            Binding binding = identifier.getBinding();
            if (binding == null) {
                logError("identifier " + identifier.getToken().getLexeme() + " not declared");
                node.setType(PrimitiveType.ERROR);
                return;
            }
            Type type = binding.getType();
            node.setType(type);
        } else {
            node.setType(target.getType());
        }
    }

    @Override
    public void visitLeave(PopulatedArrayNode node) {
        List<Type> childTypes = new ArrayList<Type>();
		node.getChildren().forEach(child -> childTypes.add(child.getType()));

        // Check that all values are of same type
        Type type = childTypes.get(0);
        for (ParseNode child : node.getChildren()) {
            if (!(type.equals(child.getType()))) {
                typeCheckError(node, childTypes);
                return;
            }
        }
        Type nodeType = new ArrayType(type);
        node.setType(nodeType);
    }

    @Override
    public void visitLeave(ArrayIndexNode node) {
        ParseNode array = node.child(0);
        ParseNode index = node.child(1);
        Type arrayType = array.getType();
        Type indexType = index.getType();
        if (arrayType instanceof ArrayType) {
            Type elementType = ((ArrayType) arrayType).getSubType();
            if (indexType == PrimitiveType.INTEGER) {
                node.setType(elementType);
                return;
            }
        }
        typeCheckError(node, Arrays.asList(arrayType, indexType));
    }

    @Override
    public void visitLeave(NewArrayNode node) {
        ParseNode size = node.child(0);
        Type sizeType = size.getType();
        if (sizeType != PrimitiveType.INTEGER) {
            typeCheckError(node, Arrays.asList(sizeType));
        }
    }

    @Override
    public void visitLeave(IfStatementNode node) {
        ParseNode condition = node.child(0);
        Type conditionType = condition.getType();
        if (conditionType != PrimitiveType.BOOLEAN) {
            typeCheckError(node, Arrays.asList(conditionType));
        }
    }

    @Override
    public void visitLeave(WhileStatementNode node) {
        ParseNode condition = node.child(0);
        Type conditionType = condition.getType();
        if (conditionType != PrimitiveType.BOOLEAN) {
            typeCheckError(node, Arrays.asList(conditionType));
        }
    }

    @Override
    public void visit(BreakStatementNode node) {
        // find enclosing loop
        ParseNode loop = node;
        while (loop != null && !(loop instanceof WhileStatementNode || loop instanceof ForStatementNode)) {
            loop = loop.getParent();
        }
        if (loop == null) {
            logError("break statement not inside loop");
            node.setType(PrimitiveType.ERROR);
        }
    }

    @Override
    public void visit(ContinueStatementNode node) {
        // find enclosing loop
        ParseNode loop = node;
        while (loop != null && !(loop instanceof WhileStatementNode || loop instanceof ForStatementNode)) {
            loop = loop.getParent();
        }
        if (loop == null) {
            logError("continue statement not inside loop");
            node.setType(PrimitiveType.ERROR);
        }
    }

    @Override
    public void visitLeave(ReturnStatementNode node) {
        // check void return
        if (node.nChildren() == 0) {
            node.setType(PrimitiveType.VOID);
        } else {
            node.setType(node.child(0).getType());
        }
        // verify the return type matches the function's return type
        // get enclosing function
        ParseNode function = node;
        while (function != null && !(function instanceof FunctionDefinitionNode)) {
            function = function.getParent();
        }
        if (function == null) {
            logError("return statement not inside function");
            node.setType(PrimitiveType.ERROR);
            return;
        }
        FunctionDefinitionNode functionDefinition = (FunctionDefinitionNode) function;
        Type functionType = functionDefinition.getType();
        assert functionType instanceof FunctionSignature;
        Type functionReturnType = ((FunctionSignature) functionType).resultType();
        Type returnType = node.getType();
        if (!functionReturnType.equals(returnType)) {
            typeCheckError(node, Arrays.asList(functionReturnType, returnType));
        }
    }

    @Override
    public void visitEnter(FunctionDefinitionNode node) {
        Scope scope = Scope.createParameterScope();
        node.setScope(scope);
    }

    @Override
    public void visitLeave(FunctionDefinitionNode node) {
        leaveScope(node);
    }

    @Override
    public void visitLeave(FunctionInvocationNode node) {
        IdentifierNode identifier = (IdentifierNode) node.child(0);
        Binding binding = identifier.getBinding();
        if (binding == null) {
            logError("identifier " + identifier.getToken().getLexeme() + " not declared");
            node.setType(PrimitiveType.ERROR);
            return;
        }
        Type type = binding.getType();
        if (!(type instanceof FunctionSignature)) {
            logError("identifier " + identifier.getToken().getLexeme() + " is not a function");
            node.setType(PrimitiveType.ERROR);
            return;
        }
        FunctionSignature signature = (FunctionSignature) type;
        List<Type> childTypes = new ArrayList<Type>();
        ParseNode arguments = node.child(1);
        for (int i = 0; i < arguments.nChildren(); i++) {
            childTypes.add(arguments.child(i).getType());
        }
        if (signature.accepts(childTypes)) {
            node.setType(signature.resultType());
        } else {
            typeCheckError(node, childTypes);
        }
    }

    @Override
    public void visitLeave(ParameterNode node) {
        IdentifierNode identifier = (IdentifierNode) node.child(0);
        identifier.setType(node.getType());
        // identifier.setMutable(true);
        addBinding(identifier, node.getType());
    }

    @Override
    public void visitEnter(ForStatementNode node) {
        enterSubscope(node);
    }

    @Override
    public void visitLeave(ForStatementNode node) {
        leaveScope(node);
    }
    ///////////////////////////////////////////////////////////////////////////
    // simple leaf nodes
    @Override
    public void visit(BooleanConstantNode node) {
        node.setType(PrimitiveType.BOOLEAN);
    }

    @Override
    public void visit(ErrorNode node) {
        node.setType(PrimitiveType.ERROR);
    }

    @Override
    public void visit(IntegerConstantNode node) {
        node.setType(PrimitiveType.INTEGER);
    }

    @Override
    public void visit(FloatConstantNode node) {
        node.setType(PrimitiveType.FLOAT);
    }

    @Override
    public void visit(CharConstantNode node) {
        node.setType(PrimitiveType.CHARACTER);
    }

    @Override
    public void visit(StringConstantNode node) {
        node.setType(PrimitiveType.STRING);
    }

    @Override
    public void visit(NewlineNode node) {}

    @Override
    public void visit(SpaceNode node) {}

    ///////////////////////////////////////////////////////////////////////////
    // IdentifierNodes, with helper methods
    @Override
    public void visit(IdentifierNode node) {
        if (isForIdentifier(node)) {
            ParseNode parent = node.getParent();
            ParseNode from = parent.child(0);
            ParseNode to = parent.child(1);
            node.setType(from.getType());
            addBinding(node, node.getType());
            return;
        }

        if (!isBeingDeclared(node) && !isForIdentifier(node)) {
            Binding binding = node.findVariableBinding();

            node.setType(binding.getType());
            node.setBinding(binding);
            node.setMutable(binding.isMutable());
        }
        // else parent DeclarationNode does the processing.
    }

    private boolean isForIdentifier(IdentifierNode node) {
        ParseNode parent = node.getParent();
        return (parent instanceof ForStatementNode) && (node == parent.child(2));
    }

    private boolean isBeingDeclared(IdentifierNode node) {
        ParseNode parent = node.getParent();
        return (parent instanceof DeclarationNode) && (node == parent.child(0))
            || (parent instanceof FunctionDefinitionNode) && (node == parent.child(0))
            || (parent instanceof ParameterNode) && (node == parent.child(0));
    }

    private void addBinding(IdentifierNode identifierNode, Type type) {
        Scope scope = identifierNode.getLocalScope();
        Binding binding = scope.createBinding(identifierNode, type);
        identifierNode.setBinding(binding);
    }

    ///////////////////////////////////////////////////////////////////////////
    // error logging/printing

    private void typeCheckError(ParseNode node, List<Type> operandTypes) {

        Token token = node.getToken();

        // apply promotion
		if (node instanceof OperatorNode && promoter.promotable((OperatorNode) node)) return;
        if (node instanceof AssignmentStatementNode && promoter.promotable(node)) return;
        if (node instanceof PopulatedArrayNode && promoter.promotable((PopulatedArrayNode) node)) return;
        if (node instanceof NewArrayNode && promoter.promotable((NewArrayNode) node)) return;
        if (node instanceof ArrayIndexNode && promoter.promotable((ArrayIndexNode) node)) return;
        logError("operator " + token.getLexeme() + " not defined for types " + operandTypes + " at "
                + token.getLocation());

        node.setType(PrimitiveType.ERROR);
    }

    private void logError(String message) {
        TanLogger log = TanLogger.getLogger("compiler.semanticAnalyzer");
        log.severe(message);
    }

}
