package parser;

import java.util.Arrays;
import logging.TanLogger;
import parseTree.*;
import parseTree.nodeTypes.ArrayIndexNode;
import parseTree.nodeTypes.AssignmentStatementNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CastNode;
import parseTree.nodeTypes.CharConstantNode;
import parseTree.nodeTypes.ContinueStatementNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.ExpressionListNode;
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
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.ParameterNode;
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TargetExpressionNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;


public class Parser {
    private Scanner scanner;
    private Token nowReading;
    private Token previouslyRead;

    public static ParseNode parse(Scanner scanner) {
        Parser parser = new Parser(scanner);
        return parser.parse();
    }

    public Parser(Scanner scanner) {
        super();
        this.scanner = scanner;
    }

    public ParseNode parse() {
        readToken();
        return parseProgram();
    }

    ////////////////////////////////////////////////////////////
    // "program" is the start symbol S
    // S -> MAIN block

    private ParseNode parseProgram() {
        if (!startsProgram(nowReading)) {
            return syntaxErrorNode("program");
        }
    
        ParseNode program = new ProgramNode(nowReading);

        while (startsGlobalDefinition(nowReading)) {
            ParseNode globalDefinition = parseGlobalDefinition();
            program.appendChild(globalDefinition);
        }

        expect(Keyword.MAIN);
        ParseNode mainBlock = parseBlockStatement();
        program.appendChild(mainBlock);

        if (!(nowReading instanceof NullToken)) {
            return syntaxErrorNode("end of program");
        }

        return program;
    }

    private boolean startsProgram(Token token) {
        return token.isLextant(Keyword.MAIN) || startsGlobalDefinition(token);
    }

    ////////////////////////////////////////////////////////////
    // globlDefinition -> functionDefinition
    
    private boolean startsGlobalDefinition(Token token) {
        return startsFunctionDefinition(token);
    }

    private ParseNode parseGlobalDefinition() {
        if (!startsGlobalDefinition(nowReading)) {
            return syntaxErrorNode("global definition");
        }
        if (startsFunctionDefinition(nowReading)) {
            return parseFunctionDefinition();
        }
        return syntaxErrorNode("global definition");
    }

    private ParseNode parseFunctionDefinition() {
        if (!startsFunctionDefinition(nowReading)) {
            return syntaxErrorNode("function definition");
        }
        Token funcdefToken = nowReading;
        expect(Keyword.FUNCDEF);
        Type type = parseType();
        ParseNode identifier = parseIdentifier();
        expect(Punctuator.OPEN_PAREN);
        ParseNode parameterList = parseParameterList();
        expect(Punctuator.CLOSE_PAREN);
        ParseNode blockStatement = parseBlockStatement();
        return FunctionDefinitionNode.withChildren(funcdefToken, type, identifier, parameterList, blockStatement);
    }

    private ParseNode parseParameterList() {
        if (!startsParameterList(nowReading)) {
            return syntaxErrorNode("parameter list");
        }
        ParseNode result = new ParameterListNode(nowReading);
        while (!nowReading.isLextant(Punctuator.CLOSE_PAREN)) {
            Token token = nowReading;
            Type type = parseType();
            ParseNode identifier = parseIdentifier();
            if (nowReading.isLextant(Punctuator.COMMA)) {
                readToken();
            }
            result.appendChild(ParameterNode.withChildren(token, type, identifier));
        }
        return result;
    }

    private boolean startsParameterList(Token token) {
        return token.isLextant(Punctuator.CLOSE_PAREN) || startsType(token);
    }

    private boolean startsFunctionDefinition(Token token) {
        return token.isLextant(Keyword.FUNCDEF);
    }

    ///////////////////////////////////////////////////////////
    // block statement


    // block -> { statement* }
    private ParseNode parseBlockStatement() {
        if (!startsBlockStatement(nowReading)) {
            return syntaxErrorNode("block");
        }
        ParseNode block = new BlockStatementNode(nowReading);
        expect(Punctuator.OPEN_BRACE);

        while (startsStatement(nowReading)) {
            ParseNode statement = parseStatement();
            block.appendChild(statement);
        }
        expect(Punctuator.CLOSE_BRACE);
        return block;
    }

    private boolean startsBlockStatement(Token token) {
        return token.isLextant(Punctuator.OPEN_BRACE);
    }


    ///////////////////////////////////////////////////////////
    // statements

    // statement-> declaration | printStmt | assignmentStmt | blockStmt | callStmt
    private ParseNode parseStatement() {
        if (!startsStatement(nowReading)) {
            return syntaxErrorNode("statement");
        }
        if (startsDeclaration(nowReading)) {
            return parseDeclaration();
        }
        if (startsPrintStatement(nowReading)) {
            return parsePrintStatement();
        }
        if (startsAssignmentStatement(nowReading)) {
            return parseAssignmentStatement();
        }
        if (startsBlockStatement(nowReading)) {
            return parseBlockStatement();
        }
        if (startsIfStatement(nowReading)) {
            return parseIfStatement();
        }
        if (startsWhileStatement(nowReading)) {
            return parseWhileStatement();
        }
        if (startsCallStatement(nowReading)) {
            return parseCallStatement();
        }
        if (startsReturnStatement(nowReading)) {
            return parseReturnStatement();
        }
        if (startsBreakStatement(nowReading)) {
            return parseBreakStatement();
        }
        if (startsContinueStatement(nowReading)) {
            return parseContinueStatement();
        }
        if (startsForStatement(nowReading)) {
            return parseForStatement();
        }
        return syntaxErrorNode("statement");
    }

    private ParseNode parseForStatement() {
        if (!startsForStatement(nowReading)) {
            return syntaxErrorNode("for statement");
        }
        Token forToken = nowReading;
        expect(Keyword.FOR);
        expect(Punctuator.OPEN_PAREN);
        ParseNode id = parseIdentifier();
        expect(Keyword.FROM);
        ParseNode from = parseExpression();
        expect(Keyword.TO);
        ParseNode to = parseExpression();
        expect(Punctuator.CLOSE_PAREN);
        ParseNode body = parseStatement();
        return ForStatementNode.withChildren(forToken, from, to, id, body); // put id after from and to.
    }

    private boolean startsForStatement(Token token) {
        return token.isLextant(Keyword.FOR);
    }

    private ParseNode parseContinueStatement() {
        if (!startsContinueStatement(nowReading)) {
            return syntaxErrorNode("continue statement");
        }
        ParseNode continueStatement = new ContinueStatementNode(nowReading);
        expect(Keyword.CONTINUE);
        expect(Punctuator.TERMINATOR);
        return continueStatement;
    }

    private boolean startsContinueStatement(Token token) {
        return token.isLextant(Keyword.CONTINUE);
    }

    private ParseNode parseBreakStatement() {
        if (!startsBreakStatement(nowReading)) {
            return syntaxErrorNode("break statement");
        }
        ParseNode breakStatement = new BreakStatementNode(nowReading);
        expect(Keyword.BREAK);
        expect(Punctuator.TERMINATOR);
        return breakStatement;
    }

    private boolean startsBreakStatement(Token token) {
        return token.isLextant(Keyword.BREAK);
    }

    private ParseNode parseReturnStatement() {
        if (!startsReturnStatement(nowReading)) {
            return syntaxErrorNode("return statement");
        }
        ParseNode returnStatement = new ReturnStatementNode(nowReading);
        expect(Keyword.RETURN);
        if (startsExpression(nowReading)) {
            ParseNode expression = parseExpression();
            returnStatement.appendChild(expression);
        }
        expect(Punctuator.TERMINATOR);
        return returnStatement;
    }

    private boolean startsReturnStatement(Token token) {
        return token.isLextant(Keyword.RETURN);
    }

    private ParseNode parseCallStatement() {
        if (!startsCallStatement(nowReading)) {
            return syntaxErrorNode("call statement");
        }
        ParseNode callStatement = new CallStatementNode(nowReading);
        expect(Keyword.CALL);
        ParseNode identifier = parseIdentifier();
        expect(Punctuator.OPEN_PAREN);
        ParseNode expressionList = parseExpressionList();
        expect(Punctuator.CLOSE_PAREN);
        ParseNode functionInvocation = FunctionInvocationNode.withChildren(identifier.getToken(), identifier,
                expressionList);
        expect(Punctuator.TERMINATOR);
        callStatement.appendChild(functionInvocation);
        return callStatement;
    }

    private ParseNode parseExpressionList() {
        ParseNode expressionList = new ExpressionListNode(nowReading);
        while (startsExpression(nowReading)) {
            expressionList.appendChild(parseExpression());
            if (nowReading.isLextant(Punctuator.COMMA)) {
                readToken();
            }
        }
        return expressionList;
    }

    private boolean startsCallStatement(Token token) {
        return token.isLextant(Keyword.CALL);
    }

    private boolean startsWhileStatement(Token token) {
        return token.isLextant(Keyword.WHILE);
    }

    private ParseNode parseWhileStatement() {
        if (!startsWhileStatement(nowReading)) {
            return syntaxErrorNode("while statement");
        }
        ParseNode whileStatement = new WhileStatementNode(nowReading);
        expect(Keyword.WHILE);
        expect(Punctuator.OPEN_PAREN);
        whileStatement.appendChild(parseExpression());
        expect(Punctuator.CLOSE_PAREN);
        whileStatement.appendChild(parseBlockStatement());
        return whileStatement;
    }

    private boolean startsIfStatement(Token token) {
        return token.isLextant(Keyword.IF);
    }

    private ParseNode parseIfStatement() {
        if (!startsIfStatement(nowReading)) {
            return syntaxErrorNode("if statement");
        }
        ParseNode ifStatement = new IfStatementNode(nowReading);
        expect(Keyword.IF);
        expect(Punctuator.OPEN_PAREN);
        ifStatement.appendChild(parseExpression());
        expect(Punctuator.CLOSE_PAREN);
        ifStatement.appendChild(parseBlockStatement());
        if (nowReading.isLextant(Keyword.ELSE)) {
            expect(Keyword.ELSE);
            ifStatement.appendChild(parseBlockStatement());
        }
        return ifStatement;
    }

    // assignmentStmt -> targetExpression ASSIGN expression TERMINATOR
    private ParseNode parseAssignmentStatement() {
        if (!startsAssignmentStatement(nowReading)) {
            return syntaxErrorNode("assignment statement");
        }
        ParseNode target = parseTargetExpression();
        expect(Punctuator.ASSIGN);
        Token assignToken = previouslyRead;
        ParseNode expression = parseExpression();
        expect(Punctuator.TERMINATOR);
        return AssignmentStatementNode.withChildren(assignToken, target, expression);
    }

    private ParseNode parseTargetExpression() {
        if (!startsTargetExpression(nowReading)) {
            return syntaxErrorNode("target expression");
        }
        ParseNode target = new TargetExpressionNode(nowReading);
        if (startsIdentifier(nowReading)) {
            target.appendChild(parseIdentifier());
            return target;
        }
        if (nowReading.isLextant(Punctuator.OPEN_PAREN)) {
            expect(Punctuator.OPEN_PAREN);
            target.appendChild(parseExpression());
            expect(Punctuator.CLOSE_PAREN);
            return target;
        }
        if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
            target.appendChild(parseExpression());
            return target;
        }
        return syntaxErrorNode("target expression");
    }

    private boolean startsTargetExpression(Token token) {
        return startsIdentifier(token) || token.isLextant(Punctuator.OPEN_PAREN) || token.isLextant(Punctuator.OPEN_BRACKET);
    }

    private boolean startsAssignmentStatement(Token token) {
        return startsTargetExpression(token);
    }

    private boolean startsStatement(Token token) {
        return startsPrintStatement(token)
            || startsDeclaration(token)
            || startsAssignmentStatement(token)
            || startsBlockStatement(token)
            || startsIfStatement(token)
            || startsWhileStatement(token)
            || startsCallStatement(token)
            || startsReturnStatement(token)
            || startsBreakStatement(token)
            || startsContinueStatement(token)
            || startsForStatement(token);
    }

    // printStmt -> PRINT printExpressionList TERMINATOR
    private ParseNode parsePrintStatement() {
        if (!startsPrintStatement(nowReading)) {
            return syntaxErrorNode("print statement");
        }
        ParseNode result = new PrintStatementNode(nowReading);

        readToken();
        result = parsePrintExpressionList(result);

        expect(Punctuator.TERMINATOR);
        return result;
    }

    private boolean startsPrintStatement(Token token) {
        return token.isLextant(Keyword.PRINT);
    }

    // This adds the printExpressions it parses to the children of the given parent
    // printExpressionList -> printSeparator* (expression printSeparator+)* expression? (note that
    // this is nullable)

    private ParseNode parsePrintExpressionList(ParseNode parent) {
        if (!startsPrintExpressionList(nowReading)) {
            return syntaxErrorNode("printExpressionList");
        }

        while (startsPrintSeparator(nowReading)) {
            parsePrintSeparator(parent);
        }
        while (startsExpression(nowReading)) {
            parent.appendChild(parseExpression());
            if (nowReading.isLextant(Punctuator.TERMINATOR)) {
                return parent;
            }
            do {
                parsePrintSeparator(parent);
            } while (startsPrintSeparator(nowReading));
        }
        return parent;
    }

    private boolean startsPrintExpressionList(Token token) {
        return startsExpression(token) || startsPrintSeparator(token)
                || token.isLextant(Punctuator.TERMINATOR);
    }


    // This adds the printSeparator it parses to the children of the given parent
    // printSeparator -> PRINT_SEPARATOR | PRINT_SPACE | PRINT_NEWLINE | PRINT_TAB

    private void parsePrintSeparator(ParseNode parent) {
        if (!startsPrintSeparator(nowReading)) {
            ParseNode child = syntaxErrorNode("print separator");
            parent.appendChild(child);
            return;
        }

        if (nowReading.isLextant(Punctuator.PRINT_NEWLINE)) {
            readToken();
            ParseNode child = new NewlineNode(previouslyRead);
            parent.appendChild(child);
        } else if (nowReading.isLextant(Punctuator.PRINT_SPACE)) {
            readToken();
            ParseNode child = new SpaceNode(previouslyRead);
            parent.appendChild(child);
        } else if (nowReading.isLextant(Punctuator.PRINT_TAB)) {
            readToken();
            ParseNode child = new TabNode(previouslyRead);
            parent.appendChild(child);
        } else if (nowReading.isLextant(Punctuator.PRINT_SEPARATOR)) {
            readToken();
        }
    }

    private boolean startsPrintSeparator(Token token) {
        return token.isLextant(Punctuator.PRINT_SEPARATOR, Punctuator.PRINT_SPACE,
                Punctuator.PRINT_NEWLINE, Punctuator.PRINT_TAB);
    }


    // declaration -> CONST identifier := expression TERMINATOR
    // declaration -> VAR identifier := expression TERMINATOR
    private ParseNode parseDeclaration() {
        if (!startsDeclaration(nowReading)) {
            return syntaxErrorNode("declaration");
        }
        Token declarationToken = nowReading;
        readToken();

        ParseNode identifier = parseIdentifier();
        expect(Punctuator.ASSIGN);
        ParseNode initializer = parseExpression();
        expect(Punctuator.TERMINATOR);

        return DeclarationNode.withChildren(declarationToken, identifier, initializer);
    }

    private boolean startsDeclaration(Token token) {
        return token.isLextant(Keyword.CONST, Keyword.VAR);
    }

    ///////////////////////////////////////////////////////////
    // expressions
    // expr -> logicOrExpression
    // logicOrExpression -> logicAndExpression [OR logicAndExpression]* (left-assoc)
    // logicAndExpression -> comparisonExpression [AND comparisonExpression]* (left-assoc)
    // comparisonExpression -> additiveExpression [(<|>|<=|>=|==|!=) additiveExpression]?
    // additiveExpression -> multiplicativeExpression [(+|-) multiplicativeExpression]* (left-assoc)
    // multiplicativeExpression -> unaryExpression [MULT unaryExpression]* (left-assoc)
    // unaryExpression -> UNARYOP unaryExpression | atomicExpression
    // atomicExpression -> literal ｜ (expression)
    // literal -> intNumber | identifier | booleanConstant | floatNumber | charConstant | stringConstant

    // expr -> comparisonExpression
    private ParseNode parseExpression() {
        if (!startsExpression(nowReading)) {
            return syntaxErrorNode("expression");
        }
        return parseLogicOrExpression();
    }

    private boolean startsExpression(Token token) {
        return startsLogicOrExpression(token);
    }

    private ParseNode parseLogicOrExpression() {
        if (!startsLogicOrExpression(nowReading)) {
            return syntaxErrorNode("logic or expression");
        }

        ParseNode left = parseLogicAndExpression();
        while (nowReading.isLextant(Punctuator.OR)) {
            Token logicOrToken = nowReading;
            readToken();
            ParseNode right = parseLogicAndExpression();
            left = OperatorNode.withChildren(logicOrToken, left, right);
        }
        return left;
    }

    private ParseNode parseLogicAndExpression() {
        if (!startsLogicAndExpression(nowReading)) {
            return syntaxErrorNode("logic and expression");
        }

        ParseNode left = parseComparisonExpression();
        while (nowReading.isLextant(Punctuator.AND)) {
            Token logicAndToken = nowReading;
            readToken();
            ParseNode right = parseComparisonExpression();
            left = OperatorNode.withChildren(logicAndToken, left, right);
        }
        return left;
    }

    private boolean startsLogicAndExpression(Token token) {
        return startsComparisonExpression(token);
    }

    private boolean startsLogicOrExpression(Token token) {
        return startsLogicAndExpression(token);
    }

    // comparisonExpression -> additiveExpression [> additiveExpression]?
    private ParseNode parseComparisonExpression() {
        if (!startsComparisonExpression(nowReading)) {
            return syntaxErrorNode("comparison expression");
        }

        ParseNode left = parseAdditiveExpression();
        while (nowReading.isLextant(
            Punctuator.GREATER,
            Punctuator.LESS,
            Punctuator.GREATER_OR_EQUAL,
            Punctuator.LESS_OR_EQUAL,
            Punctuator.EQUAL,
            Punctuator.NOT_EQUAL)) {
            Token compareToken = nowReading;
            readToken();
            ParseNode right = parseAdditiveExpression();

            left = OperatorNode.withChildren(compareToken, left, right);
        }
        return left;
    }

    private boolean startsComparisonExpression(Token token) {
        return startsAdditiveExpression(token);
    }

    // additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]* (left-assoc)
    private ParseNode parseAdditiveExpression() {
        if (!startsAdditiveExpression(nowReading)) {
            return syntaxErrorNode("additiveExpression");
        }

        ParseNode left = parseMultiplicativeExpression();
        while (nowReading.isLextant(Punctuator.ADD, Punctuator.SUBTRACT)) {
            Token additiveToken = nowReading;
            readToken();
            ParseNode right = parseMultiplicativeExpression();

            left = OperatorNode.withChildren(additiveToken, left, right);
        }
        return left;
    }

    private boolean startsAdditiveExpression(Token token) {
        return startsMultiplicativeExpression(token);
    }

    // multiplicativeExpression -> unaryExpression [MULT unaryExpression]* (left-assoc)
    private ParseNode parseMultiplicativeExpression() {
        if (!startsMultiplicativeExpression(nowReading)) {
            return syntaxErrorNode("multiplicativeExpression");
        }

        ParseNode left = parseUnaryExpression();
        while (nowReading.isLextant(Punctuator.MULTIPLY, Punctuator.DIVIDE)) {
            Token multiplicativeToken = nowReading;
            readToken();
            ParseNode right = parseUnaryExpression();

            left = OperatorNode.withChildren(multiplicativeToken, left, right);
        }
        return left;
    }

    private boolean startsMultiplicativeExpression(Token token) {
        return startsUnaryExpression(token);
    }

    // atomicExpression -> literal | (expression) | PopulatedArrayExpression | NewArrayExpression | ArrayIndexExpression | FunctionInvocationExpression
    private ParseNode parseAtomicExpression() {
        if (!startsAtomicExpression(nowReading)) {
            return syntaxErrorNode("atomic expression");
        }
        if (nowReading.isLextant(Punctuator.OPEN_PAREN)) {
            readToken();
            ParseNode expression = parseExpression();
            expect(Punctuator.CLOSE_PAREN);
            return expression;
        }
        if (nowReading.isLextant(Punctuator.LESS)) {
            Token castToken = nowReading;
            readToken();
            Type type = parseType();
            expect(Punctuator.GREATER);
            expect(Punctuator.OPEN_PAREN);
            ParseNode expression = parseExpression();
            expect(Punctuator.CLOSE_PAREN);
            return CastNode.withChildren(castToken, type, expression);
        }
        if (startsPopulatedArrayOrArrayIndexingExpression(nowReading)) {
            return parsePopulatedArrayOrArrayIndexingExpression();
        }
        if (startsNewArrayExpression(nowReading)) {
            return parseNewArrayExpression();
        }
        ParseNode literal = parseLiteral();
         if (literal instanceof IdentifierNode && nowReading.isLextant(Punctuator.OPEN_PAREN)) {
            expect(Punctuator.OPEN_PAREN);
            ParseNode expressionList = parseExpressionList();
            expect(Punctuator.CLOSE_PAREN);
            return FunctionInvocationNode.withChildren(literal.getToken(), literal, expressionList);
        }
        return literal;
    }

    private ParseNode parseNewArrayExpression() {
        if (!startsNewArrayExpression(nowReading)) {
            return syntaxErrorNode("new array expression");
        }
        Token newToken = nowReading;
        expect(null, Keyword.NEW);
        Type type = parseType();
        if (!(type instanceof ArrayType)) {
            return syntaxErrorNode("new array expression");
        }
        expect(Punctuator.OPEN_PAREN);
        ParseNode expression = parseExpression();
        expect(Punctuator.CLOSE_PAREN);
        return NewArrayNode.withChildren(newToken, type, expression);
    }

    private boolean startsNewArrayExpression(Token token) {
        return token.isLextant(Keyword.NEW);
    }

    // [expression, expression, ...]
    private ParseNode parsePopulatedArrayOrArrayIndexingExpression() {
        if (!startsPopulatedArrayOrArrayIndexingExpression(nowReading)) {
            return syntaxErrorNode("populated array expression");
        }
        Token openBracket = nowReading;
        readToken();
        ParseNode expression = parseExpression();

        // Array Indexing Expression
        // [expression : expression]
        if (nowReading.isLextant(Punctuator.COLON)) {
            readToken();
            ParseNode result = ArrayIndexNode.withChildren(openBracket, expression, parseExpression());
            expect(Punctuator.CLOSE_BRACKET);
            return result;
        }
        // populated array expression
        ParseNode result = new PopulatedArrayNode(openBracket);
        result.appendChild(expression);
        while (nowReading.isLextant(Punctuator.COMMA)) {
            readToken();
            expression = parseExpression();
            result.appendChild(expression);
        }
        expect(Punctuator.CLOSE_BRACKET);
        return result;
    }

    private boolean startsPopulatedArrayOrArrayIndexingExpression(Token token) {
        return token.isLextant(Punctuator.OPEN_BRACKET);
    }

    private Type parseType() {
        if (!startsType(nowReading)) {
            return PrimitiveType.ERROR;
        }
        Token token = nowReading;

        if (token.isLextant(Punctuator.OPEN_BRACKET)) {
            readToken();
            Type subtype = parseType();
            expect(Punctuator.CLOSE_BRACKET);
            return new ArrayType(subtype);
        }

        assert token instanceof LextantToken;
        LextantToken lextantToken = (LextantToken) token;
        Lextant lextant = lextantToken.getLextant();
        
        assert lextant instanceof Keyword;
        Keyword keyword = (Keyword) lextant;

        Type type = PrimitiveType.ERROR;
        switch (keyword) {
        case BOOL:
            type = PrimitiveType.BOOLEAN;
            break;
        case INT:
            type = PrimitiveType.INTEGER;
            break;
        case FLOAT:
            type =  PrimitiveType.FLOAT;
            break;
        case CHAR:
            type = PrimitiveType.CHARACTER;
            break;
        case STRING:
            type = PrimitiveType.STRING;
            break;
        case VOID:
            type = PrimitiveType.VOID;
            break;
        default:
            type = PrimitiveType.ERROR;
        }
        readToken();
        return type;
    }

    private boolean startsType(Token token) {
        return token.isLextant(
            Keyword.BOOL,
            Keyword.INT,
            Keyword.FLOAT,
            Keyword.CHAR,
            Keyword.STRING,
            Keyword.VOID,
            Punctuator.OPEN_BRACKET);
    }

    private boolean startsAtomicExpression(Token token) {
        return startsLiteral(token)
            || token.isLextant(Punctuator.OPEN_PAREN)
            || token.isLextant(Punctuator.LESS)
            || startsPopulatedArrayOrArrayIndexingExpression(token)
            || startsNewArrayExpression(token);
    }

    // unaryExpression -> UNARYOP unaryExpression
    //                  | atomicExpression
    private ParseNode parseUnaryExpression() {
        if (startsAtomicExpression(nowReading)) {
            return parseAtomicExpression();
        }
        if (!startsUnaryExpression(nowReading)) {
            return syntaxErrorNode("unary expression");
        }
        Token operatorToken = nowReading;
        readToken();
        ParseNode child = parseUnaryExpression();
        return OperatorNode.withChildren(operatorToken, child);
    }

    private boolean startsUnaryExpression(Token token) {
        return token.isLextant(Punctuator.SUBTRACT, Punctuator.ADD, Punctuator.NOT, Keyword.LENGTH) || startsAtomicExpression(token);
    }

    // literal -> number | identifier | booleanConstant
    private ParseNode parseLiteral() {
        if (!startsLiteral(nowReading)) {
            return syntaxErrorNode("literal");
        }

        if (startsIntLiteral(nowReading)) {
            return parseIntLiteral();
        }
        if (startsIdentifier(nowReading)) {
            return parseIdentifier();
        }
        if (startsBooleanLiteral(nowReading)) {
            return parseBooleanLiteral();
        }
        if (startsCharLiteral(nowReading)) {
            return parseCharLiteral();
        }
        if (startsFloatLiteral(nowReading)) {
            return parseFloatLiteral();
        }
        if (startsStringLiteral(nowReading)) {
            return parseStringLiteral();
        }

        return syntaxErrorNode("literal");
    }

    private ParseNode parseStringLiteral() {
        readToken();
        return new StringConstantNode(previouslyRead);
    }

    private boolean startsStringLiteral(Token token) {
        return token instanceof StringToken;
    }

    private ParseNode parseFloatLiteral() {
        readToken();
        return new FloatConstantNode(previouslyRead);
    }

    private ParseNode parseCharLiteral() {
        readToken();
        return new CharConstantNode(previouslyRead);
    }

    private boolean startsLiteral(Token token) {
        return startsIntLiteral(token)
            || startsIdentifier(token)
            || startsBooleanLiteral(token)
            || startsCharLiteral(token)
            || startsFloatLiteral(token)
            || startsStringLiteral(token);
    }

    private boolean startsFloatLiteral(Token token) {
        return token instanceof FloatToken;
    }

    private boolean startsCharLiteral(Token token) {
        return token instanceof CharToken;
    }

    // number (literal)
    private ParseNode parseIntLiteral() {
        if (!startsIntLiteral(nowReading)) {
            return syntaxErrorNode("integer constant");
        }
        readToken();
        return new IntegerConstantNode(previouslyRead);
    }

    private boolean startsIntLiteral(Token token) {
        return token instanceof NumberToken;
    }

    // identifier (terminal)
    private ParseNode parseIdentifier() {
        if (!startsIdentifier(nowReading)) {
            return syntaxErrorNode("identifier");
        }
        readToken();
        return new IdentifierNode(previouslyRead);
    }

    private boolean startsIdentifier(Token token) {
        return token instanceof IdentifierToken;
    }

    // boolean literal
    private ParseNode parseBooleanLiteral() {
        if (!startsBooleanLiteral(nowReading)) {
            return syntaxErrorNode("boolean constant");
        }
        readToken();
        return new BooleanConstantNode(previouslyRead);
    }

    private boolean startsBooleanLiteral(Token token) {
        return token.isLextant(Keyword.TRUE, Keyword.FALSE);
    }

    private void readToken() {
        previouslyRead = nowReading;
        nowReading = scanner.next();
    }

    // if the current token is one of the given lextants, read the next token.
    // otherwise, give a syntax error and read next token (to avoid endless looping).
    private void expect(Lextant... lextants) {
        if (!nowReading.isLextant(lextants)) {
            syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
        }
        readToken();
    }

    private ErrorNode syntaxErrorNode(String expectedSymbol) {
        syntaxError(nowReading, "expecting " + expectedSymbol);
        ErrorNode errorNode = new ErrorNode(nowReading);
        readToken();
        return errorNode;
    }

    private void syntaxError(Token token, String errorDescription) {
        String message = "" + token.getLocation() + " " + errorDescription;
        error(message);
    }

    private void error(String message) {
        TanLogger log = TanLogger.getLogger("compiler.Parser");
        log.severe("syntax error: " + message);
    }
}

