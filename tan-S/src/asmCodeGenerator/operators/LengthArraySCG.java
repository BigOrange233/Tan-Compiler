package asmCodeGenerator.operators;

import java.util.List;
import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.records.ArrayRecord;
import parseTree.ParseNode;

public class LengthArraySCG implements SimpleCodeGenerator {

    @Override
    public ASMCodeFragment generate(ParseNode node, List<ASMCodeFragment> args) {
        ASMCodeFragment code = new ASMCodeFragment(ASMCodeFragment.CodeType.GENERATES_VALUE);
        assert args.size() == 1;
        code.append(args.get(0)); // [... arrayAddress]
        Macros.readIOffset(code, ArrayRecord.ARRAY_LENGTH_OFFSET); // [... arrayLength]
        return code;
    }
    
}
