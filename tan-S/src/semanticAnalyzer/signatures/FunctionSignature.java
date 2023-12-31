package semanticAnalyzer.signatures;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

// immutable
public class FunctionSignature implements Type {
    private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
    private Type resultType;
    private Type[] paramTypes;
    Object whichVariant;


    ///////////////////////////////////////////////////////////////
    // construction

    public FunctionSignature(Object whichVariant, Type... types) {
        assert (types.length >= 1);
        storeParamTypes(types);
        resultType = types[types.length - 1];
        this.whichVariant = whichVariant;
    }

    public FunctionSignature(Object whichVariant, List<Type> parameterTypes, Type returnType) {
        this.paramTypes = parameterTypes.toArray(new Type[parameterTypes.size()]);
        this.resultType = returnType;
        this.whichVariant = whichVariant;
    }

    private void storeParamTypes(Type[] types) {
        paramTypes = new Type[types.length - 1];
        for (int i = 0; i < types.length - 1; i++) {
            paramTypes[i] = types[i];
        }
    }


    ///////////////////////////////////////////////////////////////
    // accessors

    public Object getVariant() {
        return whichVariant;
    }

    public Type resultType() {
        return resultType;
    }

    public boolean isNull() {
        return false;
    }


    ///////////////////////////////////////////////////////////////
    // main query

    public boolean accepts(List<Type> types) {
        if (types.size() != paramTypes.length) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            if (!assignableTo(paramTypes[i], types.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean assignableTo(Type variableType, Type valueType) {
        if (valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
            return true;
        }
        return variableType.equals(valueType);
    }

    // Null object pattern
    private static FunctionSignature neverMatchedSignature =
            new FunctionSignature(1, PrimitiveType.ERROR) {
                public boolean accepts(List<Type> types) {
                    return false;
                }

                public boolean isNull() {
                    return true;
                }
            };

    public static FunctionSignature nullInstance() {
        return neverMatchedSignature;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String infoString() {
        String parameters = Arrays.asList(paramTypes).stream().map(Type::infoString).collect(Collectors.joining(", "));
        return "function (" + parameters + ") -> " + resultType.infoString();
    }

}
