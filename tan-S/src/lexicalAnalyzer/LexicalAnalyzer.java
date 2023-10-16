package lexicalAnalyzer;


import logging.TanLogger;

import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import tokens.CharToken;
import tokens.FloatToken;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.NumberToken;
import tokens.StringToken;
import tokens.Token;

import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class LexicalAnalyzer extends ScannerImp {
    public static LexicalAnalyzer make(String filename) {
        InputHandler handler = InputHandler.fromFilename(filename);
        PushbackCharStream charStream = PushbackCharStream.make(handler);
        return new LexicalAnalyzer(charStream);
    }

    public LexicalAnalyzer(PushbackCharStream input) {
        super(input);
    }


    //////////////////////////////////////////////////////////////////////////////
    // Token-finding main dispatch

    @Override
    protected Token findNextToken() {
        LocatedChar ch = nextNonWhitespaceChar();
        if (ch.isDigit()) {
            try {
                return scanNumber(ch);
            } catch (NumberFormatException e) {
                lexicalError(ch, "integer too large for an int.");
                return findNextToken();
            } catch (IllegalArgumentException e) {
                lexicalError(ch, e.getMessage());
                return findNextToken();
            }
        } else if (ch.isIdentifierLeadingChar()) {
            return scanIdentifier(ch);
        } else if (isPunctuatorStart(ch)) {
            return PunctuatorScanner.scan(ch, input);
        } else if (ch.isChar('#')) {
            skipComment();
            return findNextToken();
        } else if (ch.isChar('\'')) {
            StringBuffer sb = new StringBuffer();
            sb.append(ch.getCharacter());
            LocatedChar lc = input.next();
            char c = lc.getCharacter();
            if (c < 32 || c > 126) {
                lexicalError(lc);
                input.pushback(lc);
                return NullToken.make(lc);
            }
            sb.append(c);
            LocatedChar closeQuote = input.next();
            if (!closeQuote.isChar('\'')) {
                lexicalError(closeQuote);
                input.pushback(closeQuote);
                input.pushback(lc);
                return NullToken.make(closeQuote);
            }
            sb.append(closeQuote.getCharacter());
            return CharToken.make(ch, sb.toString());
        } else if (ch.isChar('%')) {
            StringBuffer sb = new StringBuffer();
            sb.append(ch.getCharacter());
            for (int i = 0; i < 3; i++) {
                LocatedChar lc = input.next();
                char c = lc.getCharacter();
                if (c < '0' || c > '7') {
                    lexicalError(lc);
                    return NullToken.make(lc);
                }
                sb.append(c);
            }
            int value = Integer.parseInt(sb.substring(1).toString(), 8);
            if (value > 127) {
                lexicalError(ch, "Octal escape sequence out of range");
                return NullToken.make(ch);
            }
            return CharToken.make(ch, sb.toString());
        } else if (ch.isChar('"')) {
            StringBuffer sb = new StringBuffer();
            sb.append(ch.getCharacter());
            LocatedChar lc = input.next();
            while (!lc.isChar('"') && !lc.isChar('\n')) {
                sb.append(lc.getCharacter());
                lc = input.next();
            }
            if (lc.isChar('\n')) {
                lexicalError(lc);
                return NullToken.make(lc);
            }
            sb.append(lc.getCharacter());
            return StringToken.make(ch, sb.toString());
        } else if (isEndOfInput(ch)) {
            return NullToken.make(ch);
        } else {
            lexicalError(ch);
            return findNextToken();
        }
    }

    private void skipComment() {
        LocatedChar ch = input.next();
        while (!isEndOfInput(ch) && !ch.isChar('#') && !ch.isChar('\n')) {
            ch = input.next();
        }
    }

    private LocatedChar nextNonWhitespaceChar() {
        LocatedChar ch = input.next();
        while (ch.isWhitespace()) {
            ch = input.next();
        }
        return ch;
    }


    //////////////////////////////////////////////////////////////////////////////
    // Integer lexical analysis

    private Token scanNumber(LocatedChar firstChar) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(firstChar.getCharacter());
        appendSubsequentDigits(buffer);
        LocatedChar lc = input.next();
        if (!lc.isChar('.')) {
            input.pushback(lc);
            return NumberToken.make(firstChar, buffer.toString());
        }
        buffer.append(lc.getCharacter());
        lc = input.peek();
        // // number after decimal point is not optional
        if (!lc.isDigit()) {
            lexicalError(lc);
            return NullToken.make(lc);
        }
        appendSubsequentDigits(buffer);
        lc = input.next();
        if (!lc.isChar('e') && !lc.isChar('E')) {
            input.pushback(lc);
            return FloatToken.make(firstChar, buffer.toString());
        }
        buffer.append(lc.getCharacter());
        lc = input.next();
        if (!lc.isChar('+') && !lc.isChar('-')) {
            input.pushback(lc);
            lexicalError(lc);
            return NullToken.make(lc);
        }
        buffer.append(lc.getCharacter()); // +/-
        lc = input.peek();
        if (!lc.isDigit()) {
            lexicalError(lc);
            return NullToken.make(lc);
        }
        appendSubsequentDigits(buffer);
        return FloatToken.make(firstChar, buffer.toString());
    }

    private void appendSubsequentDigits(StringBuffer buffer) {
        LocatedChar c = input.next();
        while (c.isDigit()) {
            buffer.append(c.getCharacter());
            c = input.next();
        }
        input.pushback(c);
    }


    //////////////////////////////////////////////////////////////////////////////
    // Identifier and keyword lexical analysis

    private Token scanIdentifier(LocatedChar firstChar) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(firstChar.getCharacter());
        appendSubsequentIdentifierChars(buffer);

        String lexeme = buffer.toString();
        if (Keyword.isAKeyword(lexeme)) {
            return LextantToken.make(firstChar, lexeme, Keyword.forLexeme(lexeme));
        } else {
            return IdentifierToken.make(firstChar, lexeme);
        }
    }

    private void appendSubsequentIdentifierChars(StringBuffer buffer) {
        LocatedChar c = input.next();
        while (c.isIdentifierTailingChar()) {
            buffer.append(c.getCharacter());
            c = input.next();
        }
        input.pushback(c);
    }


    //////////////////////////////////////////////////////////////////////////////
    // Punctuator lexical analysis
    // old method left in to show a simple scanning method.
    // current method is the algorithm object PunctuatorScanner.java

    @SuppressWarnings("unused")
    private Token oldScanPunctuator(LocatedChar ch) {

        switch (ch.getCharacter()) {
            case '*':
                return LextantToken.make(ch, "*", Punctuator.MULTIPLY);
            case '+':
                return LextantToken.make(ch, "+", Punctuator.ADD);
            case '>':
                return LextantToken.make(ch, ">", Punctuator.GREATER);
            case ':':
                if (ch.getCharacter() == '=') {
                    return LextantToken.make(ch, ":=", Punctuator.ASSIGN);
                } else {
                    lexicalError(ch);
                    return (NullToken.make(ch));
                }
            case ',':
                return LextantToken.make(ch, ",", Punctuator.PRINT_SEPARATOR);
            case ';':
                return LextantToken.make(ch, ";", Punctuator.TERMINATOR);
            default:
                lexicalError(ch);
                return (NullToken.make(ch));
        }
    }



    //////////////////////////////////////////////////////////////////////////////
    // Character-classification routines specific to tan scanning.

    private boolean isPunctuatorStart(LocatedChar lc) {
        char c = lc.getCharacter();
        return isPunctuatorStartingCharacter(c);
    }

    private boolean isEndOfInput(LocatedChar lc) {
        return lc == LocatedCharStream.FLAG_END_OF_INPUT;
    }

    //////////////////////////////////////////////////////////////////////////////
    // Error-reporting

    private void lexicalError(LocatedChar ch) {
        TanLogger log = TanLogger.getLogger("compiler.lexicalAnalyzer");
        log.severe("Lexical error: invalid character " + ch);
    }

    private void lexicalError(LocatedChar ch, String message) {
        TanLogger log = TanLogger.getLogger("compiler.lexicalAnalyzer");
        log.severe(message + " at " + ch);
    }

}
