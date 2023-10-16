package lexicalAnalyzer;

import inputHandler.TextLocation;
import tokens.LextantToken;
import tokens.Token;

public enum PseudoOperator implements Lextant {
    CAST("cast");

    private String lexeme;
    private Token prototype;

    private PseudoOperator(String lexeme) {
        this.lexeme = lexeme;
        this.prototype = LextantToken.make(TextLocation.nullInstance(), lexeme, this);
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    @Override
    public Token prototype() {
        return this.prototype;
    }
    
}
