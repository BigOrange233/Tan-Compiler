package asmCodeGenerator.records;

public interface ArrayRecord extends Record {
    public static final int ARRAY_SUBTYPE_SIZE_OFFSET = 8;
    public static final int ARRAY_LENGTH_OFFSET = 12;
    public static final int ARRAY_ELEMENT_OFFSET = 16;
    public static final int ARRAY_HEADER_SIZE = ARRAY_ELEMENT_OFFSET;
    public static final int RECORD_TYPE_ID = RecordTypeIdentifier.ARRAY.getIntValue();

    // status flags
    public static final int ARRAY_STATUS_FLAGS_IMMUTABILITY = 0;
    public static final int ARRAY_STATUS_FLAGS_SUBTYPE_IS_REFERENCE = 1;
    public static final int ARRAY_STATUS_FLAGS_IS_DELETED = 2;
    public static final int ARRAY_STATUS_FLAGS_PERMANENT = 3;
}
