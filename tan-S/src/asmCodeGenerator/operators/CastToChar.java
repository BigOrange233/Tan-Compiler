package asmCodeGenerator.operators;

import java.util.List;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import parseTree.ParseNode;

public class CastToChar implements SimpleCodeGenerator {

    @Override
    public ASMCodeFragment generate(ParseNode node, List<ASMCodeFragment> args) {
        ASMCodeFragment code = new ASMCodeFragment(ASMCodeFragment.CodeType.GENERATES_VOID);
        code.add(ASMOpcode.PushI, 0x7f);
        code.add(ASMOpcode.BTAnd);
        return code;
    }

}
