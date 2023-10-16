package tokens;

import inputHandler.Locator;

public class CharToken extends TokenImp {
    protected char value;

    // for any escape sequence, lexeme is the sequence itself
    protected CharToken(Locator locator, String lexeme) {
        super(locator, lexeme);
    }

    protected void setValue(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public static CharToken make(Locator locator, String lexeme) {
        CharToken result = new CharToken(locator, lexeme);
        if (lexeme.charAt(0) == '\'') {
            result.setValue(lexeme.charAt(1));
        } else {
            assert lexeme.charAt(0) == '%' : "Bad lexeme in CharToken";
            result.setValue((char)Integer.parseInt(lexeme.substring(1), 8));
        }
        return result;
    }

    @Override
    protected String rawString() {
        return "char, " + getLexeme();
    }
}
