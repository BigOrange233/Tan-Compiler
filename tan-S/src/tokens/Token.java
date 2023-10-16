package tokens;

import lexicalAnalyzer.Lextant;
import inputHandler.Locator;

public interface Token extends Locator {
    public String getLexeme();

    public String fullString();

    // 这个是Token接口最核心的方法，用来判断Token的类型
    public boolean isLextant(Lextant... lextants);
}
