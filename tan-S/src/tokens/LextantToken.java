package tokens;

import lexicalAnalyzer.Lextant;
import inputHandler.Locator;

// 所有的关键字，分隔符和运算符都是LextantToken
// 包含了一个Lextant，这个Lextant是一个接口，可以是关键字，分隔符或者运算符
// 以及一个定位信息
// 这个类的作用就是把Lextant和定位信息封装起来
public final class LextantToken extends TokenImp {

    private Lextant lextant;

    private LextantToken(Locator locator, String lexeme, Lextant lextant) {
        super(locator, lexeme);
        this.lextant = lextant;
        // 这里我们把lexeme和lextant分开，其实有利于实现替代字符方案
        // 比如我们可以提供关键字AND，但是在词法分析时，我们可以把它替换成&&
        // 这样就可以在词法分析时就把关键字替换成运算符，而不是在语法分析时再替换
        // 或者类似C语言中的三字符序列，例如：??=, ??(, ??/等等
    }

    public Lextant getLextant() {
        return lextant;
    }

    // Lextant这个接口非常好，就算没有任何实现，也可以用来表示关键字，分隔符和运算符
    // 跨越具体的实现，只关注接口的概念，这是一个很好的编程习惯
    // Enum of Keywords, or Enum of operators, or Enum of separators
    public boolean isLextant(Lextant... lextants) {
        for (Lextant lextant : lextants) {
            if (this.lextant == lextant)
                return true;
        }
        return false;
    }

    protected String rawString() {
        return lextant.toString();
    }


    public static LextantToken make(Locator locator, String lexeme, Lextant lextant) {
        return new LextantToken(locator, lexeme, lextant);
    }
}
