package tokens;

import inputHandler.LocatedChar;
import inputHandler.Locator;

public class StringToken extends TokenImp {

    String value;

    protected StringToken(Locator locator, String lexeme) {
        super(locator, lexeme);
        value = lexeme.substring(1, lexeme.length() - 1);
    }

    public static Token make(LocatedChar ch, String string) {
        return new StringToken(ch, string);
    }

    @Override
    protected String rawString() {
        return "string, " + getLexeme();
    }

    public String getValue() {
        return value;
    }

}
