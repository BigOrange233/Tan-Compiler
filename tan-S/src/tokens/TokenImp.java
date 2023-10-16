package tokens;

import lexicalAnalyzer.Lextant;
import inputHandler.Locator;
import inputHandler.TextLocation;

public abstract class TokenImp implements Token {
    private TextLocation location;
    private String lexeme;

    protected TokenImp(Locator locator, String lexeme) {
        super();
        this.location = locator.getLocation();
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return lexeme;
    }

    @Override
    public TextLocation getLocation() {
        return location;
    }

    /**
     * A string (not surrounded by parentheses) representing the subclass information.
     * 
     * @return subclass information string
     */
    abstract protected String rawString();

    // 模版方法
    public String toString() {
        return "(" + rawString() + ")";
    }

    /**
     * convert to a string containing all information about the token.
     * 
     * @return string with all token info.
     */
    // 没有任何地方真的用到，我们其实可以从Token接口中删除这个方法
    public String fullString() {
        String locationString = location == null ? "(no text location)" : location.toString();
        return "(" + rawString() + ", " + locationString + ", " + lexeme + ")";
    }

    // 提供一个默认实现，可以在子类中覆盖这个方法，来判断Token的类型
    // 对于关键字，分隔符和运算符，我们可以直接使用LextantToken的实现
    // 对于标识符和字面量，我们直接使用这里的默认实现就可以了，避免重复代码是TokenImpl的目的
    @Override
    public boolean isLextant(Lextant... lextants) {
        return false;
    }

}
