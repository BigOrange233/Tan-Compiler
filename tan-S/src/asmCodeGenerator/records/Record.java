package asmCodeGenerator.records;

public interface Record {
    public enum RecordTypeIdentifier {
        STRING(3),
        ARRAY(5);

        private int intValue;
        RecordTypeIdentifier(int intValue) {
            this.intValue = intValue;
        }
        public int getIntValue() {
            return intValue;
        }
    }
    public static final int RECORD_TYPE_ID_OFFSET = 0;
    public static final int RECORD_STATUS_FLAGS_OFFSET = 4;
}
