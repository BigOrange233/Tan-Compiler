package lexicalAnalyzer;

import tokens.Token;

// 词法分析获得的Token的类型有
//  关键字
//  标识符
//  字面量
//      整型字面量
//      浮点型字面量
//      字符串字面量
//      布尔型字面量
//  分隔符
//  运算符
// 其中关键字、分隔符、运算符这些Token拥有固定的字符串，没有必要每次出现时都为这些词素分配内存
// 对每一个具体的关键字，分隔符，运算符，我们只需要保存唯一的实例，并且这个概念并不是Token
// 因为Token需要保存文件，行号，列号等定位信息。于是我们用Lextant来代表这类信息。
// 我们也可以用一个enum来实现这个概念，但是存在一个问题，就是单一的enum无法分别管理关键字，运算符和分隔符。
// 所以我们用一个接口来实现这个概念，然后用多个enum来实现这个接口。
// 这个接口应该具有哪些方法呢，这取决于我们如何使用它。

// Lextant这个接口非常好，就算没有任何实现，也可以用来表示关键字，分隔符和运算符
// 跨越具体的实现，只关注接口的概念，这是一个很好的编程习惯
// Enum of Keywords, or Enum of operators, or Enum of separators

// interface Token has method:
// public boolean isLextant(Lextant... lextants);

public interface Lextant {
    public String getLexeme();

    public Token prototype();
}
