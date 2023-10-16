package tokens;

import inputHandler.Locator;

public class FloatToken extends TokenImp {
    protected double value;

    protected FloatToken(Locator locator, String lexeme) {
        super(locator, lexeme);
    }

    protected void setValue(double value) {
        this.value = value;
        if (value == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("FloatToken: value too large.");
        }
    }

    public double getValue() {
        return value;
    }

    public static FloatToken make(Locator locator, String lexeme) {
        FloatToken result = new FloatToken(locator, lexeme);
        result.setValue(Double.parseDouble(lexeme));
        return result;
    }

    @Override
    protected String rawString() {
        return "float, " + value;
    }
}
