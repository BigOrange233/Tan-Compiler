package semanticAnalyzer.types;

public class ArrayType implements Type {

    private Type subtype = PrimitiveType.NO_TYPE;

    public ArrayType() {
    }

    public ArrayType(Type subtype) {
        this.subtype = subtype;
    }

    public Type getSubType() {
        return subtype;
    }

    @Override
    public int getSize() {
        return REFERENCE_SIZE;
    }

    @Override
    public String infoString() {
        return "[" + subtype.infoString() + "]";
    }

    @Override
    public boolean isReference() {
        return true;
    }

    @Override
    public String toString() {
        return infoString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType) {
            ArrayType other = (ArrayType) obj;
            return this.subtype.equals(other.subtype);
        }
        return false;
    }

}
