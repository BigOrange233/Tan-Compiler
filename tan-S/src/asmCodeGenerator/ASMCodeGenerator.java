package asmCodeGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.operators.SimpleCodeGenerator;
import asmCodeGenerator.records.ArrayRecord;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
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
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TargetExpressionNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
    ParseNode root;
    private ASMCodeFragment functions = new ASMCodeFragment(GENERATES_VOID);

    public static ASMCodeFragment generate(ParseNode syntaxTree) {
        ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
        return codeGenerator.makeASM();
    }

    public ASMCodeGenerator(ParseNode root) {
        super();
        this.root = root;
    }

    public ASMCodeFragment makeASM() {
        ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
        code.append(MemoryManager.codeForInitialization());
        code.append(stackFrameASM());
        code.append(RunTime.getEnvironment());
        code.append(globalVariableBlockASM());
        code.append(programASM());
        code.append(MemoryManager.codeForAfterApplication());

        return code;
    }

    private ASMCodeFragment stackFrameASM() {
        ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
        Macros.declareI(code, RunTime.FRAME_POINTER);
        Macros.declareI(code, RunTime.STACK_POINTER);
        code.add(Memtop);
        code.add(Duplicate);
        Macros.storeITo(code, RunTime.FRAME_POINTER);
        Macros.storeITo(code, RunTime.STACK_POINTER);
        return code;
    }

    private ASMCodeFragment globalVariableBlockASM() {
        assert root.hasScope();
        Scope scope = root.getScope();
        int globalBlockSize = scope.getAllocatedSize();

        ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
        code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
        code.add(DataZ, globalBlockSize);
        return code;
    }

    private ASMCodeFragment programASM() {
        ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

        code.add(Label, RunTime.MAIN_PROGRAM_LABEL);
        code.append(programCode());
        code.add(Halt);
        code.append(functions);
        return code;
    }

    private ASMCodeFragment programCode() {
        CodeVisitor visitor = new CodeVisitor();
        root.accept(visitor);
        return visitor.removeRootCode(root);
    }


    protected class CodeVisitor extends ParseNodeVisitor.Default {
        private Map<ParseNode, ASMCodeFragment> codeMap;
        ASMCodeFragment code;

        public CodeVisitor() {
            codeMap = new HashMap<ParseNode, ASMCodeFragment>();
        }


        ////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
        private void newAddressCode(ParseNode node) {
            code = new ASMCodeFragment(GENERATES_ADDRESS);
            codeMap.put(node, code);
        }

        private void newValueCode(ParseNode node) {
            code = new ASMCodeFragment(GENERATES_VALUE);
            codeMap.put(node, code);
        }

        private void newVoidCode(ParseNode node) {
            code = new ASMCodeFragment(GENERATES_VOID);
            codeMap.put(node, code);
        }

        ////////////////////////////////////////////////////////////////////
        // Get code from the map.
        private ASMCodeFragment getAndRemoveCode(ParseNode node) {
            ASMCodeFragment result = codeMap.get(node);
            codeMap.remove(node);
            return result;
        }

        public ASMCodeFragment removeRootCode(ParseNode tree) {
            return getAndRemoveCode(tree);
        }

        ASMCodeFragment removeValueCode(ParseNode node) {
            ASMCodeFragment frag = getAndRemoveCode(node);
            makeFragmentValueCode(frag, node);
            return frag;
        }

        private ASMCodeFragment removeAddressCode(ParseNode node) {
            ASMCodeFragment frag = getAndRemoveCode(node);
            assert frag.isAddress();
            return frag;
        }

        ASMCodeFragment removeVoidCode(ParseNode node) {
            ASMCodeFragment frag = getAndRemoveCode(node);
            assert frag.isVoid();
            return frag;
        }

        ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
        private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
            assert !code.isVoid();

            if (code.isAddress()) {
                turnAddressIntoValue(code, node);
            }
        }

        private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
            if (node.getType() == PrimitiveType.INTEGER) {
                code.add(LoadI);
            } else if (node.getType() == PrimitiveType.FLOAT) {
                code.add(LoadF);
            } else if (node.getType() == PrimitiveType.BOOLEAN) {
                code.add(LoadC);
            } else if (node.getType() == PrimitiveType.CHARACTER) {
                code.add(LoadC);
            } else if (node.getType() == PrimitiveType.STRING) {
                code.add(LoadI);
            } else if (node.getType() instanceof ArrayType) {
				code.add(LoadI);
			} else {
                assert false : "node " + node;
            }
            code.markAsValue();
        }

        ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave
        public void visitLeave(ParseNode node) {
            assert false : "node " + node + " not handled in ASMCodeGenerator";
        }



        ///////////////////////////////////////////////////////////////////////////
        // constructs larger than statements
        public void visitLeave(ProgramNode node) {
            newVoidCode(node);
            for (ParseNode child : node.getChildren()) {
                ASMCodeFragment childCode = removeVoidCode(child);
                code.append(childCode);
            }
        }

        @Override
        public void visitEnter(FunctionDefinitionNode node) {
            Labeller labeller = new Labeller("function");
            String startLabel = labeller.newLabel("start");
            String epilogueLabel = labeller.newLabel("epilogue");
            node.setStartLabel(startLabel);
            node.setEpilogueLabel(epilogueLabel);
        }

        @Override
        public void visitLeave(FunctionDefinitionNode node) {
            newVoidCode(node);

            // part 1: generate the function code
            ASMCodeFragment functionCode = new ASMCodeFragment(GENERATES_VOID);
            functionCode.add(Label, node.getStartLabel());
            // [... ra]
            // prologue

            // dynamic link: mem[sp - 4] <= fp
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 4);
            functionCode.add(Subtract);
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(StoreI);

            // mem[sp - 8] <= return address
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            // [... ra sp-8]
            functionCode.add(Exchange);
            functionCode.add(StoreI);

            // fp <= sp
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            Macros.storeITo(functionCode, RunTime.FRAME_POINTER);

            // reserve space for dynamic link and return address, sp <= sp - 8
            Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            // reserve space for local variables, sp <= sp - scopeSize
            BlockStatementNode block = (BlockStatementNode) node.child(2);
            functionCode.add(PushI, block.getScope().getAllocatedSize());
            functionCode.add(Subtract);
            Macros.storeITo(functionCode, RunTime.STACK_POINTER);

            functionCode.append(removeVoidCode(node.child(2))); // [... rv?]
            
            // trap for return missing
            FunctionSignature signature = (FunctionSignature) node.getType();
            if (signature.resultType() != PrimitiveType.VOID) {
                functionCode.add(Jump, RunTime.RETURN_MISSING_RUNTIME_ERROR);
            }

            // epilogue
            functionCode.add(Label, node.getEpilogueLabel());

            // restore ra, push to asm stack the value mem[fp - 8]
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(PushI, 8);
            functionCode.add(Subtract);
            functionCode.add(LoadI); // [... rv? ra]

            // restore sp <- fp
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            Macros.storeITo(functionCode, RunTime.STACK_POINTER);

            // restore fp
            Macros.loadIFrom(functionCode, RunTime.FRAME_POINTER);
            functionCode.add(PushI, 4);
            functionCode.add(Subtract);
            functionCode.add(LoadI); // [... ra fp']
            Macros.storeITo(functionCode, RunTime.FRAME_POINTER); // [... rv? ra]

            if (signature.resultType() != PrimitiveType.VOID) {
                functionCode.add(Exchange); // [... ra rv]
                // put the value on the call stack
                // sp <= sp - 4
                Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
                Type type = signature.resultType();
                functionCode.add(PushI, type.getSize());
                functionCode.add(Subtract);
                Macros.storeITo(functionCode, RunTime.STACK_POINTER);
                // mem[sp] <= rv
                Macros.loadIFrom(functionCode, RunTime.STACK_POINTER);
                functionCode.add(Exchange);
                functionCode.add(opcodeForStore(type));
            }
            functionCode.add(Return);
            functions.append(functionCode);
            // part 2: set the function pointer
            newVoidCode(node);
            code.append(removeAddressCode(node.child(0)));
            code.add(PushD, node.getStartLabel());
            code.add(StoreI);
        }

        @Override
        public void visitLeave(FunctionInvocationNode node) {
            newValueCode(node);
            // push arguments to call stack
            ExpressionListNode arguments = (ExpressionListNode) node.child(1);
            int sizeOfArguments = 0;
            // push arguments from right to left
            for (int i = arguments.nChildren() - 1; i >= 0; i--) {
                Type argumentType = arguments.child(i).getType();
                int argumentSize = argumentType.getSize();
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                code.add(PushI, argumentSize); // [... sp size]
                code.add(Subtract); // [... sp - size]
                Macros.storeITo(code, RunTime.STACK_POINTER); // sp <= sp - size, [...]
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                code.append(removeValueCode(arguments.child(i))); // [... sp value]
                code.add(opcodeForStore(argumentType)); // [...], mem[sp] <= value
                sizeOfArguments += argumentSize;
            }

            // call function
            IdentifierNode function = (IdentifierNode) node.child(0);
            code.append(removeAddressCode(function));
            code.add(LoadI);
            code.add(CallV);

            // get return value from mem[sp] and put it on accumulator stack
            FunctionSignature signature = (FunctionSignature) function.getType();
            Type returnType = signature.resultType();
            if (returnType != PrimitiveType.VOID) {
                // [... rv] <- mem[sp]
                Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... sp]
                code.add(opcodeForload(returnType)); // [... rv]
            } else {
                code.add(PushI, 0); // [... 0]
            }

            // clean up the stack, pull back the space for arguments and return value
            Macros.loadIFrom(code, RunTime.STACK_POINTER); // [... rv sp]
            code.add(PushI, returnType.getSize() + sizeOfArguments); // [... rv sp size]
            code.add(Add); // [... rv sp + size]
            Macros.storeITo(code, RunTime.STACK_POINTER); // sp <= sp + size, [... rv]
        }

        public ASMOpcode opcodeForload(Type returnType) {
            if (returnType == PrimitiveType.BOOLEAN) {
                return LoadC;
            }
            if (returnType == PrimitiveType.INTEGER) {
                return LoadI;
            }
            if (returnType == PrimitiveType.FLOAT) {
                return LoadF;
            }
            if (returnType == PrimitiveType.CHARACTER) {
                return LoadC;
            }
            if (returnType == PrimitiveType.STRING) {
                return LoadI;
            }
            if (returnType instanceof ArrayType) {
                return LoadI;
            }

            assert false : "unimplemented type in loadForType";
            return null;
        }

        @Override
        public void visitLeave(CallStatementNode node) {
            newVoidCode(node);
            FunctionInvocationNode functionInvocation = (FunctionInvocationNode) node.child(0);
            code.append(removeValueCode(functionInvocation));
            code.add(Pop);
        }

        public void visitLeave(BlockStatementNode node) {
            newVoidCode(node);
            for (ParseNode child : node.getChildren()) {
                ASMCodeFragment childCode = removeVoidCode(child);
                code.append(childCode);
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // statements and declarations

        public void visitLeave(PrintStatementNode node) {
            newVoidCode(node);
            new PrintStatementGenerator(code, this).generate(node);
        }

        public void visitLeave(ReturnStatementNode node) {
            newVoidCode(node);
            if (node.nChildren() > 0) {
                code.append(removeValueCode(node.child(0)));
            }
            FunctionDefinitionNode function = (FunctionDefinitionNode) node.getFunctionDefinitionNode();
            String epilogueLabel = function.getEpilogueLabel();
            code.add(Jump, epilogueLabel);
        }

        public void visit(NewlineNode node) {
            newVoidCode(node);
            code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
            code.add(Printf);
        }

        public void visit(SpaceNode node) {
            newVoidCode(node);
            code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
            code.add(Printf);
        }

        public void visit(TabNode node) {
            newVoidCode(node);
            code.add(PushD, RunTime.TAB_PRINT_FORMAT);
            code.add(Printf);
        }


        public void visitLeave(DeclarationNode node) {
            newVoidCode(node);
            ASMCodeFragment lvalue = removeAddressCode(node.child(0));
            ASMCodeFragment rvalue = removeValueCode(node.child(1));

            code.append(lvalue);
            code.append(rvalue);

            Type type = node.getType();
            code.add(opcodeForStore(type));
        }

        private ASMOpcode opcodeForStore(Type type) {
            if (type == PrimitiveType.INTEGER) {
                return StoreI;
            }
            if (type == PrimitiveType.FLOAT) {
                return StoreF;
            }
            if (type == PrimitiveType.BOOLEAN) {
                return StoreC;
            }
            if (type == PrimitiveType.CHARACTER) {
                return StoreC;
            }
            if (type == PrimitiveType.STRING) {
                return StoreI;
            }
            if(type instanceof ArrayType) {
                return StoreI;
            }
            assert false : "Type " + type + " unimplemented in opcodeForStore()";
            return null;
        }

        public void visitLeave(AssignmentStatementNode node) {
            newVoidCode(node);
            ASMCodeFragment lvalue = removeAddressCode(node.child(0));
            ASMCodeFragment rvalue = removeValueCode(node.child(1));

            code.append(lvalue);
            code.append(rvalue);

            Type type = node.child(0).getType();
            code.add(opcodeForStore(type));
        }

        public void visitLeave(IfStatementNode node) {
            newVoidCode(node);

            Labeller labeller = new Labeller("if-statement");
            String elseLabel = labeller.newLabel("else");
            String endLabel = labeller.newLabel("end");

            ASMCodeFragment condition = removeValueCode(node.child(0));
            ASMCodeFragment ifBody = removeVoidCode(node.child(1));
            code.append(condition);
            code.add(JumpFalse, elseLabel);
            code.append(ifBody);
            code.add(Jump, endLabel);
            code.add(Label, elseLabel);
            if (node.nChildren() > 2) {
                ASMCodeFragment elseBody = removeVoidCode(node.child(2));
                code.append(elseBody);
            }
            code.add(Label, endLabel);
        }

        @Override
        public void visitEnter(WhileStatementNode node) {
            Labeller labeller = new Labeller("while-statement");
            String startLabel = labeller.newLabel("start");
            String endLabel = labeller.newLabel("end");
            node.setStartLabel(startLabel);
            node.setEndLabel(endLabel);
        }

        @Override
        public void visitLeave(WhileStatementNode node) {
            ASMCodeFragment condition = removeValueCode(node.child(0));
            ASMCodeFragment whileBody = removeVoidCode(node.child(1));


            newVoidCode(node);
            code.add(Label, node.getStartLabel());
            code.append(condition);
            code.add(JumpFalse, node.getEndLabel());
            code.append(whileBody);
            code.add(Jump, node.getStartLabel());
            code.add(Label, node.getEndLabel());
        }

        ///////////////////////////////////////////////////////////////////////////
        // expressions
        public void visitLeave(OperatorNode node) {
            Lextant operator = node.getOperator();

            if ((operator == Punctuator.SUBTRACT || operator == Punctuator.ADD || operator == Punctuator.NOT || operator == Keyword.LENGTH) && node.nChildren() == 1) {
                visitUnaryOperatorNode(node);
            } else if (
                operator == Punctuator.GREATER
                || operator == Punctuator.LESS
                || operator == Punctuator.GREATER_OR_EQUAL
                || operator == Punctuator.LESS_OR_EQUAL
                || operator == Punctuator.EQUAL
                || operator == Punctuator.NOT_EQUAL
            ) {
                visitComparisonOperatorNode(node, operator);
            } else if (operator == Punctuator.AND || operator == Punctuator.OR) {
                visitBooleanOperatorNode(node, operator);
            } else {
                visitNormalBinaryOperatorNode(node);
            }
        }

        private void visitBooleanOperatorNode(OperatorNode node, Lextant operator) {
            ASMCodeFragment arg1 = removeValueCode(node.child(0));
            ASMCodeFragment arg2 = removeValueCode(node.child(1));

            Labeller labeller = new Labeller("boolean-operator");
            String startLabel = labeller.newLabel("arg1");
            String arg2Label = labeller.newLabel("arg2");
            String trueLabel = labeller.newLabel("true");
            String falseLabel = labeller.newLabel("false");
            String joinLabel = labeller.newLabel("join");

            newValueCode(node);
            code.add(Label, startLabel);
            code.append(arg1);

            // short circuit
            if (operator == Punctuator.AND) {
                code.add(JumpFalse, falseLabel);
            } else if (operator == Punctuator.OR) {
                code.add(JumpTrue, trueLabel);
            } else {
                assert false : "unimplemented boolean operator";
            }
            
            code.add(Label, arg2Label);
            code.append(arg2);
            code.add(Jump, joinLabel);

            code.add(Label, trueLabel);
            code.add(PushI, 1);
            code.add(Jump, joinLabel);

            code.add(Label, falseLabel);
            code.add(PushI, 0);
            code.add(Jump, joinLabel);

            code.add(Label, joinLabel);
        }

        private static class BranchInfo {
            public ASMOpcode branchOpcode;
            public boolean jumpTrue;
        }

        private void visitComparisonOperatorNode(OperatorNode node, Lextant operator) {

            ASMCodeFragment arg1 = removeValueCode(node.child(0));
            ASMCodeFragment arg2 = removeValueCode(node.child(1));

            Labeller labeller = new Labeller("compare");

            String startLabel = labeller.newLabel("arg1");
            String arg2Label = labeller.newLabel("arg2");
            String subLabel = labeller.newLabel("sub");
            String trueLabel = labeller.newLabel("true");
            String falseLabel = labeller.newLabel("false");
            String joinLabel = labeller.newLabel("join");

            newValueCode(node);
            code.add(Label, startLabel);
            code.append(arg1);
            code.add(Label, arg2Label);
            code.append(arg2);
            code.add(Label, subLabel);
            code.add(opcodeForOperator(node));

            Type type = node.child(1).getType();
            BranchInfo branchInfo = opcodeForBranch(operator, type);
            code.add(branchInfo.branchOpcode, branchInfo.jumpTrue ? trueLabel : falseLabel);
            code.add(Jump, branchInfo.jumpTrue ? falseLabel : trueLabel);

            code.add(Label, trueLabel);
            code.add(PushI, 1);
            code.add(Jump, joinLabel);
            code.add(Label, falseLabel);
            code.add(PushI, 0);
            code.add(Jump, joinLabel);
            code.add(Label, joinLabel);
        }

        private BranchInfo opcodeForBranch(Lextant operator, Type type) {
            BranchInfo branchInfo = new BranchInfo();
            branchInfo.jumpTrue = (operator == Punctuator.GREATER || operator == Punctuator.LESS || operator == Punctuator.EQUAL);
            if (operator == Punctuator.GREATER) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFPos : JumpPos;
            }
            else if (operator == Punctuator.LESS) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFNeg : JumpNeg;
            }
            else if (operator == Punctuator.GREATER_OR_EQUAL) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFNeg : JumpNeg;
            }
            else if (operator == Punctuator.LESS_OR_EQUAL) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFPos : JumpPos;
            }
            else if (operator == Punctuator.EQUAL) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFZero : JumpFalse;
            }
            else if (operator == Punctuator.NOT_EQUAL) {
                branchInfo.branchOpcode = type == PrimitiveType.FLOAT ? JumpFZero : JumpFalse;
            }
            else {
                assert false : "unimplemented operator in opcodeForBranch()";
            }
            return branchInfo;
        }


        private void visitUnaryOperatorNode(OperatorNode node) {
            newValueCode(node);
            ASMCodeFragment arg1 = removeValueCode(node.child(0));
            List<ASMCodeFragment> args = Arrays.asList(arg1);
            opcodeForOperator(node, args);
        }

        private void visitNormalBinaryOperatorNode(OperatorNode node) {
            newValueCode(node);
            ASMCodeFragment arg1 = removeValueCode(node.child(0));
            ASMCodeFragment arg2 = removeValueCode(node.child(1));
            List<ASMCodeFragment> args = Arrays.asList(arg1, arg2);
            opcodeForOperator(node, args);
        }

        private void opcodeForOperator(OperatorNode node, List<ASMCodeFragment> args) {
            FunctionSignature signature = node.getSignature();
            Object variant = signature.getVariant();
            if (variant instanceof ASMOpcode) {
                args.forEach(arg -> code.append(arg));
                // handle division by zero
                if (args.size() == 2 && node.getOperator() == Punctuator.DIVIDE) {
                    Type divisorType = node.child(1).getType();
                    assert divisorType == PrimitiveType.INTEGER || divisorType == PrimitiveType.FLOAT;
                    Labeller labeller = new Labeller("divide-zero-check");
                    String startLabel = labeller.newLabel("start");
                    String zeroLabel = labeller.newLabel("zero");
                    String joinLabel = labeller.newLabel("join");
                    code.add(Label, startLabel);
                    code.add(Duplicate);
                    code.add(divisorType == PrimitiveType.FLOAT ? JumpFZero : JumpFalse, zeroLabel);
                    code.add(Jump, joinLabel);
                    code.add(Label, zeroLabel);
                    code.add(Jump, divisorType == PrimitiveType.FLOAT ? RunTime.FLOAT_DIVIDE_BY_ZERO_RUNTIME_ERROR
                            : RunTime.INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
                    code.add(Label, joinLabel);
                }
                code.add((ASMOpcode) variant);
                return;
            } else if (variant instanceof SimpleCodeGenerator) {
                SimpleCodeGenerator generator = (SimpleCodeGenerator) variant;
                code.append(generator.generate(node, args));
                return;
            }
            assert false : "No opcode for this variant of " + signature;
        }

        private ASMOpcode opcodeForOperator(OperatorNode node) {
            FunctionSignature signature = node.getSignature();
            return opcodeForOperator(signature);
        }

        private ASMOpcode opcodeForOperator(FunctionSignature signature) {
            Object variant = signature.getVariant();
            if (variant instanceof ASMOpcode) {
                return (ASMOpcode) variant;
            }
            assert false : "No opcode for this variant of " + signature;
            return null;
        }

        @Override
        public void visitLeave(CastNode node) {
            newValueCode(node);
            code.append(removeValueCode(node.child(0)));
            
            Object variant = node.getSignature().getVariant();
            
            if (variant instanceof SimpleCodeGenerator) {
                SimpleCodeGenerator scg = (SimpleCodeGenerator) variant;
                code.append(scg.generate(node, null));
            }
            
            if (variant instanceof ASMOpcode) {
                ASMOpcode opcode = (ASMOpcode) variant;
                code.add(opcode);
            }
        }

        @Override
        public void visitLeave(TargetExpressionNode node) {
            newAddressCode(node);
            ASMCodeFragment target = removeAddressCode(node.child(0));
            code.append(target);
        }

        @Override
        public void visitLeave(PopulatedArrayNode node) {
            // 我们这时知道数组的大小(即子节点个数), 数组元素的类型(subtype), 以及数组元素的大小
            // 这里使用堆分配过程(procedure)给当前数组分配空间(Record)
            // 生成的代码将返回数组记录(array record)的地址在栈顶
            // [...] -> [... arrayRecordAddress]
            newValueCode(node); // 这里的value指的是数据记录的地址
            
            // get size of the element
            Type nodeType = node.getType();
            assert nodeType instanceof ArrayType;
            ArrayType arrayType = (ArrayType) nodeType;
            Type subType = arrayType.getSubType();
            int elementSize = subType.getSize();

            // get number of elements
            int numberOfElements = node.nChildren();

            // get array record size
            // 数组的大小编译时就确定了且不会修改, 所以直接在这里计算出来。而不用生成代码来运行时计算
            int arrayDataSize = elementSize * numberOfElements;
            int arrayRecordSize = ArrayRecord.ARRAY_HEADER_SIZE + arrayDataSize;

            Labeller labeller = new Labeller("create-populated-array");
            code.add(Label, labeller.newLabel("start"));

            // allocate array record on heap
            // [... arrayRecordSize] -> [... arrayRecordAddress]
            code.add(PushI, arrayRecordSize);
            code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);

            // INIT ARRAY RECORD
            // [... arrayRecordAddress] -> [... arrayRecordAddress]

            // set type identifier
            Macros.writeIOffset(code, ArrayRecord.RECORD_TYPE_ID_OFFSET, ArrayRecord.RECORD_TYPE_ID);
            // set Status
            int status_flags = 0;
            if (subType.isReference()) {
                status_flags |= 1 << ArrayRecord.ARRAY_STATUS_FLAGS_SUBTYPE_IS_REFERENCE;
            }
            Macros.writeIOffset(code, ArrayRecord.RECORD_STATUS_FLAGS_OFFSET, status_flags);
            // set Subtype size
            Macros.writeIOffset(code, ArrayRecord.ARRAY_SUBTYPE_SIZE_OFFSET, elementSize);
            // set Length
            Macros.writeIOffset(code, ArrayRecord.ARRAY_LENGTH_OFFSET, numberOfElements);

            // set Data
            for (int i = 0; i < node.nChildren(); i++) {
                code.add(Duplicate); // [... arrayRecordAddress arrayRecordAddress]
                code.add(PushI, ArrayRecord.ARRAY_ELEMENT_OFFSET + i * elementSize); // [... arrayRecordAddress arrayRecordAddress offset]
                code.add(Add); // [... arrayRecordAddress arrayRecordAddress + offset]
                code.append(removeValueCode(node.child(i))); // [... arrayRecordAddress arrayRecordAddress + offset value]
                code.add(opcodeForStore(subType)); // [... arrayRecordAddress]
            }
            // [ ... arrayRecordAddress ]
        }

        @Override
        public void visitLeave(NewArrayNode node) {
            // goal: [...] -> [... arrayRecordAddress]

            newValueCode(node);
            
            // get size of the element
            Type nodeType = node.getType();
            assert nodeType instanceof ArrayType;
            ArrayType arrayType = (ArrayType) nodeType;
            Type subType = arrayType.getSubType();
            int elementSize = subType.getSize();

            Labeller labeller = new Labeller("create-new-array");
            code.add(Label, labeller.newLabel("start"));
            String numberOfElementsLabel = labeller.newLabel("number-of-elements");
            Macros.declareI(code, numberOfElementsLabel);

            // get number of elements
            code.append(removeValueCode(node.child(0))); // [... length]
            code.add(Duplicate); // [... length length]
            Macros.storeITo(code, numberOfElementsLabel); // [... length]

            // check if size is negative
            code.add(Duplicate); // [... length length]
            code.add(JumpNeg, RunTime.ARRAY_SIZE_NEGATIVE_RUNTIME_ERROR); // [... length]

            // get array record size
            code.add(PushI, elementSize); // [... length elementSize]
            code.add(Multiply); // [... length * elementSize]
            code.add(PushI, ArrayRecord.ARRAY_HEADER_SIZE); // [... length * elementSize headerSize]
            code.add(Add); // [... length * elementSize + headerSize]

            // allocate array record on heap
            // [... arrayRecordSize] -> [... arrayRecordAddress]
            code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);

            // INIT ARRAY RECORD
            // [... arrayRecordAddress] -> [... arrayRecordAddress]

            // set type identifier
            Macros.writeIOffset(code, ArrayRecord.RECORD_TYPE_ID_OFFSET, ArrayRecord.RECORD_TYPE_ID);
            // set Status
            int status_flags = 0;
            if (subType.isReference()) {
                status_flags |= 1 << ArrayRecord.ARRAY_STATUS_FLAGS_SUBTYPE_IS_REFERENCE;
            }
            Macros.writeIOffset(code, ArrayRecord.RECORD_STATUS_FLAGS_OFFSET, status_flags);
            // set Subtype size
            Macros.writeIOffset(code, ArrayRecord.ARRAY_SUBTYPE_SIZE_OFFSET, elementSize);
            // set Length
            code.add(Duplicate); // [... arrayRecordAddress arrayRecordAddress]
            Macros.loadIFrom(code, numberOfElementsLabel); // [... arrayRecordAddress arrayRecordAddress length]
            code.add(Exchange); // [... arrayRecordAddress length arrayRecordAddress]
            Macros.writeIOffset(code, ArrayRecord.ARRAY_LENGTH_OFFSET); // [... arrayRecordAddress]

            // set Data
            // allocator returns all zeroed memory, so no need to set data to zero
            // otherwise we need to generate code to set data to zero

            // [ ... arrayRecordAddress ]
        }

        @Override
        public void visitLeave(ArrayIndexNode node) {
            // goal: [...] -> [... elementAddress]
            newAddressCode(node); // it generates lvalue

            Labeller labeller = new Labeller("array-indexing");
            code.add(Label, labeller.newLabel("start"));
            
            // [...]
            code.append(removeValueCode(node.child(0)));
            // [... arrayRecordAddress]

            // get size of the element
            Type arrayNodeType = node.child(0).getType();
            assert arrayNodeType instanceof ArrayType;
            ArrayType arrayType = (ArrayType) arrayNodeType;
            Type subType = arrayType.getSubType();
            int elementSize = subType.getSize();
            
            // get number of elements
            String numberOfElementsLabel = labeller.newLabel("number-of-elements");
            Macros.declareI(code, numberOfElementsLabel);
            code.add(Duplicate); // [... arrayRecordAddress arrayRecordAddress]
            Macros.readIOffset(code, ArrayRecord.ARRAY_LENGTH_OFFSET); // [... arrayRecordAddress length]
            Macros.storeITo(code, numberOfElementsLabel); // [... arrayRecordAddress]

            // get index
            code.append(removeValueCode(node.child(1))); // [... arrayRecordAddress index]

            // check if index is negative
            code.add(Duplicate); // [... arrayRecordAddress index index]
            code.add(JumpNeg, RunTime.ARRAY_INDEX_NEGATIVE_RUNTIME_ERROR); // [... arrayRecordAddress index]

            // check if index is out of bounds
            code.add(Duplicate); // [... arrayRecordAddress index index]
            Macros.loadIFrom(code, numberOfElementsLabel); // [... arrayRecordAddress index index length]
            code.add(PushI, 1); // [... arrayRecordAddress index index length 1]
            code.add(Subtract); // [... arrayRecordAddress index index length - 1]
            code.add(Subtract); // [... arrayRecordAddress index index - length + 1]
            code.add(JumpPos, RunTime.ARRAY_INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR); // [... arrayRecordAddress index]

            code.add(PushI, elementSize); // [... arrayRecordAddress index elementSize]
            code.add(Multiply); // [... arrayRecordAddress index * elementSize]
            code.add(PushI, ArrayRecord.ARRAY_ELEMENT_OFFSET); // [... arrayRecordAddress index * elementSize offsetBase]
            code.add(Add); // [... arrayRecordAddress index * elementSize + offsetBase]
            code.add(Add); // [... arrayRecordAddress + offset]
        }

        @Override
        public void visitEnter(ForStatementNode node) {
            newVoidCode(node);
            Labeller labeller = new Labeller("for-statement");
            node.setIdAddrLabel(labeller.newLabel("id-addr"));
            node.setToValueLabel(labeller.newLabel("to-value"));
            node.setStartLabel(labeller.newLabel("start"));
            node.setEndLabel(labeller.newLabel("end"));
            node.setContinueLabel(labeller.newLabel("continue"));
        }

        @Override
        public void visitLeave(ForStatementNode node) {
            newVoidCode(node);

            // not using RunTime labels since for loop can be nested
            // so for each one of them we need a new set of labels
            Macros.declareI(code, node.getIdAddrLabel());
            Macros.declareI(code, node.getToValueLabel());

            // generate code for initialisation
            code.append(removeAddressCode(node.child(2))); // [... id_address]
            Macros.storeITo(code, node.getIdAddrLabel()); // [...]

            code.append(removeValueCode(node.child(0))); // [... from_value]
            Macros.loadIFrom(code, node.getIdAddrLabel()); // [... from_value id_address]
            code.add(Exchange); // [... id_address from_value]
            code.add(StoreI); // [...] id = from_value
            code.append(removeValueCode(node.child(1))); // [... to_value]
            Macros.storeITo(code, node.getToValueLabel()); // [...]

            code.add(Label, node.getStartLabel());

            // generate code for condition
            Macros.loadIFrom(code, node.getToValueLabel()); // [... to_value]
            Macros.loadIFrom(code, node.getIdAddrLabel()); // [... to_value id_address]
            code.add(LoadI); // [... to_value id_value]
            code.add(Subtract); // [... to_value - id_value]
            code.add(JumpNeg, node.getEndLabel()); // [...]
            
            // generate code for body
            code.append(removeVoidCode(node.child(3))); // [...]

            code.add(Label, node.getContinueLabel());

            // generate code for increment
            Macros.loadIFrom(code, node.getIdAddrLabel()); // [... id_address]
            code.add(Duplicate); // [... id_address id_address]
            code.add(LoadI); // [... id_address id_value]
            code.add(PushI, 1); // [... id_address id_value 1]
            code.add(Add); // [... id_address id_value + 1]
            code.add(StoreI); // [...]
            code.add(Jump, node.getStartLabel()); // [...]

            code.add(Label, node.getEndLabel());
        }
        ///////////////////////////////////////////////////////////////////////////
        // leaf nodes (ErrorNode not necessary)
        public void visit(BooleanConstantNode node) {
            newValueCode(node);
            code.add(PushI, node.getValue() ? 1 : 0);
        }

        public void visit(IdentifierNode node) {
            newAddressCode(node);
            Binding binding = node.getBinding();

            binding.generateAddress(code);
        }

        public void visit(IntegerConstantNode node) {
            newValueCode(node);

            code.add(PushI, node.getValue());
        }

        public void visit(FloatConstantNode node) {
            newValueCode(node);

            code.add(PushF, node.getValue());
        }

        public void visit(CharConstantNode node) {
            newValueCode(node);

            code.add(PushI, node.getValue());
        }

        public void visit(StringConstantNode node) {
            newValueCode(node);
            Labeller labeller = new Labeller("stringliteral");
            String label = labeller.newLabel(node.getValue());
            code.add(DLabel, label);
            code.add(DataI, 3); // record type
            code.add(DataI, 9); // record status
            code.add(DataI, node.getValue().length()); // record length
            code.add(DataS, node.getValue());
            code.add(PushD, label);
        }

        @Override
        public void visit(BreakStatementNode node) {
            newVoidCode(node);
            ParseNode parent = node.getParent();
            while (!(parent instanceof WhileStatementNode || parent instanceof ForStatementNode)) {
                parent = parent.getParent();
            }
            if (parent instanceof ForStatementNode) {
                ForStatementNode forNode = (ForStatementNode) parent;
                if (node.getToken().isLextant(Keyword.BREAK)) {
                    code.add(Jump, forNode.getEndLabel());
                }
                return;
            }
            WhileStatementNode whileNode = (WhileStatementNode) parent;
            if (node.getToken().isLextant(Keyword.BREAK)) {
                code.add(Jump, whileNode.getEndLabel());
            }
        }

        @Override
        public void visit(ContinueStatementNode node) {
            newVoidCode(node);
            ParseNode parent = node.getParent();
            while (!(parent instanceof WhileStatementNode || parent instanceof ForStatementNode)) {
                parent = parent.getParent();
            }
            if (parent instanceof ForStatementNode) {
                ForStatementNode forNode = (ForStatementNode) parent;
                if (node.getToken().isLextant(Keyword.CONTINUE)) {
                    code.add(Jump, forNode.getContinueLabel());
                }
                return;
            }
            WhileStatementNode whileNode = (WhileStatementNode) parent;
            if (node.getToken().isLextant(Keyword.CONTINUE)) {
                code.add(Jump, whileNode.getStartLabel());
            }
        }
    }
}
