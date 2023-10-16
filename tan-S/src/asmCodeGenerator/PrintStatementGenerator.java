package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.TabNode;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.records.ArrayRecord;
import asmCodeGenerator.runtime.RunTime;

public class PrintStatementGenerator {
    ASMCodeFragment code;
    ASMCodeGenerator.CodeVisitor visitor;


    public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor) {
        super();
        this.code = code;
        this.visitor = visitor;
    }

    public void generate(PrintStatementNode node) {
        for (ParseNode child : node.getChildren()) {
            if (child instanceof NewlineNode || child instanceof SpaceNode
                    || child instanceof TabNode) {
                ASMCodeFragment childCode = visitor.removeVoidCode(child);
                code.append(childCode);
            } else {
                appendPrintCode(child);
            }
        }
    }

    private void appendPrintCode(ParseNode node) {
        Type type = node.getType();
        // [...]
        code.append(visitor.removeValueCode(node));
        // [... value]
        appendPrintCode(type);
        // [...]
	}

    private void appendPrintCode(Type type) {
        if (type instanceof ArrayType) {
            Type subType = ((ArrayType) type).getSubType();
            appendPrintArrayCode(subType);
            return;
        }

        String format = printFormat(type);
        convertToStringIfBoolean(type);
        getValueIfString(type);
        code.add(PushD, format);
        code.add(Printf);
    }

    private void appendPrintArrayCode(Type subType) {
        
        // [... arrayRecordAddr]

        // print [
        code.add(PushD, RunTime.OPEN_BRACKET_STRING);
        code.add(Printf);
        
        Labeller labeller = new Labeller("print-array");
        String numberOfElementsLabel = labeller.newLabel("number-of-elements");
        String elementSizeInBytesLabel = labeller.newLabel("element-size-in-bytes");
        String iteratorLabel = labeller.newLabel("iterator");
        String elementsBaseAddressLabel = labeller.newLabel("elements-base-address");
        String continueLabel = labeller.newLabel("continue");
        String statusLabel = labeller.newLabel("status");
        String printElementLabel = labeller.newLabel("print_element");
        String printNullRefLabel = labeller.newLabel("print_null_ref");
        // 这里我们每次打印一个数组，都分配了一组变量!
        // 其他可行的办法还有利用accumulator来分配这些变量，然后使用procedure来进行打印。
        // 不过需要注意accumulator空间比较稀缺。
        // 不能使用Runtime来分配一套变量提供给procedure使用，因为Runtime的空间是全局的，而打印数组的逻辑是允许嵌套的。
        // 又或者可以使用heap来分配一套变量。
        Macros.declareI(code, numberOfElementsLabel);
        Macros.declareI(code, elementSizeInBytesLabel);
        Macros.declareI(code, iteratorLabel);
        Macros.declareI(code, elementsBaseAddressLabel);
        Macros.declareI(code, statusLabel);

        code.add(Duplicate); // [... arrayRecordAddr arrayRecordAddr]
        code.add(PushI, ArrayRecord.ARRAY_HEADER_SIZE);
        code.add(Add); // [... arrayRecordAddr elementBaseAddr]
        Macros.storeITo(code, elementsBaseAddressLabel); // [... arrayRecordAddr]

        code.add(Duplicate); // [... arrayRecordAddr arrayRecordAddr]
        Macros.readIOffset(code, ArrayRecord.ARRAY_LENGTH_OFFSET); // [... arrayRecordAddr arrayLength]
        Macros.storeITo(code, numberOfElementsLabel); // [... arrayRecordAddr]

        code.add(Duplicate); // [... arrayRecordAddr arrayRecordAddr]
        Macros.readIOffset(code, ArrayRecord.ARRAY_SUBTYPE_SIZE_OFFSET); // [... arrayRecordAddr elementSizeInBytes]
        Macros.storeITo(code, elementSizeInBytesLabel); // [... arrayRecordAddr]

        // status
        code.add(Duplicate); // [... arrayRecordAddr arrayRecordAddr]
        Macros.readIOffset(code, ArrayRecord.RECORD_STATUS_FLAGS_OFFSET); // [... arrayRecordAddr status]
        Macros.storeITo(code, statusLabel); // [... arrayRecordAddr]

        // i = 0
        code.add(PushI, 0);
        Macros.storeITo(code, iteratorLabel);

        code.add(Label, labeller.newLabel("loop")); // [... arrayRecordAddr]
        // if (i == array_length) goto end
        Macros.loadIFrom(code, iteratorLabel);
        Macros.loadIFrom(code, numberOfElementsLabel);
        code.add(Subtract);
        code.add(JumpFalse, labeller.newLabel("end"));

        // [... array_addr &array[i]]
        Macros.loadIFrom(code, elementsBaseAddressLabel);
        Macros.loadIFrom(code, iteratorLabel);
        Macros.loadIFrom(code, elementSizeInBytesLabel);
        code.add(Multiply);
        code.add(Add);

        // dereference [... array_addr array[i]]
        ASMOpcode load_instruction = LoadI;
        if (subType instanceof ArrayType) {
            load_instruction = LoadI;
        } else if (subType == PrimitiveType.FLOAT) {
            load_instruction = LoadF;
        } else if (subType == PrimitiveType.INTEGER) {
            load_instruction = LoadI;
        } else if (subType == PrimitiveType.CHARACTER) {
            load_instruction = LoadC;
        } else if (subType == PrimitiveType.BOOLEAN) {
            load_instruction = LoadC;
        } else if (subType == PrimitiveType.STRING) {
            load_instruction = LoadI;
        }
        code.add(load_instruction);

        // if subtype is ref
        // [... array_addr array[i]]
        Macros.loadIFrom(code, statusLabel); // [... array_addr array[i] status]
        code.add(PushI, 1 << ArrayRecord.ARRAY_STATUS_FLAGS_SUBTYPE_IS_REFERENCE); // [... array_addr array[i] status 2]
        code.add(BTAnd); // [... array_addr array[i] status & 2]
        code.add(JumpFalse, printElementLabel); // [... array_addr array[i]]
        // if ref is NULL
        code.add(Duplicate); // [... array_addr array[i] array[i]]
        code.add(JumpFalse, printNullRefLabel); // [... array_addr array[i]]

        code.add(Label, printElementLabel);
        appendPrintCode(subType); // [... array_addr array[i]]; THIS IS THE RECURSIVE CALL
        code.add(Jump, continueLabel);

        // print null reference
        // [... array_addr 0]
        code.add(Label, printNullRefLabel);
        code.add(PushD, RunTime.NULL_REF_STRING); // [... array_addr array[i] "(nil)"]
        code.add(Printf); // [... array_addr array[i]]
        code.add(Pop); // [... array_addr]
        code.add(Jump, continueLabel);

        // i++
        code.add(Label, continueLabel);
        Macros.incrementInteger(code, iteratorLabel);

        // if (i == array_length) goto skip_print_comma
        Macros.loadIFrom(code, iteratorLabel);
        Macros.loadIFrom(code, numberOfElementsLabel);
        code.add(Subtract);
        code.add(JumpFalse, labeller.newLabel("skip-print-comma"));

        // print ', '
        code.add(PushD, RunTime.COMMA_SPACE_STRING);
        code.add(Printf);

        code.add(Label, labeller.newLabel("skip-print-comma"));

        // goto loop
        code.add(Jump, labeller.newLabel("loop"));

        // end
        code.add(Label, labeller.newLabel("end"));

        // [... array_addr]

        code.add(Pop);
        // [...]

        code.add(PushD, RunTime.CLOSE_BRACKET_STRING);
        code.add(Printf);
    }

    private void getValueIfString(Type type) {
        if (type != PrimitiveType.STRING) {
            return;
        }
        code.add(PushI, 12); // skip record type, status, and length
        code.add(Add);
    }

    private void convertToStringIfBoolean(Type type) {
        if (type != PrimitiveType.BOOLEAN) {
            return;
        }

        Labeller labeller = new Labeller("print-boolean");
        String trueLabel = labeller.newLabel("true");
        String endLabel = labeller.newLabel("join");

        code.add(JumpTrue, trueLabel);
        code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
        code.add(Jump, endLabel);
        code.add(Label, trueLabel);
        code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
        code.add(Label, endLabel);
    }


    private static String printFormat(Type type) {
        assert type instanceof PrimitiveType;

        switch ((PrimitiveType) type) {
            case INTEGER:
                return RunTime.INTEGER_PRINT_FORMAT;
            case BOOLEAN:
                return RunTime.BOOLEAN_PRINT_FORMAT;
            case CHARACTER:
                return RunTime.CHARACTER_PRINT_FORMAT;
            case FLOAT:
                return RunTime.FLOAT_PRINT_FORMAT;
            case STRING:
                return RunTime.STRING_PRINT_FORMAT;
            default:
                assert false : "Type " + type
                        + " unimplemented in PrintStatementGenerator.printFormat()";
                return "";
        }
    }
}
