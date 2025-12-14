package main.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static main.java.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
       keywords = new HashMap<>();
       keywords.put("and", AND);
       keywords.put("class", CLASS);
       keywords.put("else", ELSE);
       keywords.put("false", FALSE);
       keywords.put("for", FOR);
       keywords.put("fun", FUN);
       keywords.put("if", IF);
       keywords.put("nil", NIL);
       keywords.put("or", OR);
       keywords.put("print", PRINT);
       keywords.put("return", RETURN);
       keywords.put("super", SUPER);
       keywords.put("this", THIS);
       keywords.put("true", TRUE);
       keywords.put("var", VAR);
       keywords.put("while", WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd()) {
           start = current;
           scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }


    private void scanToken() {
        char c = next();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) next();
                } else {
                    addToken(SLASH);
                }
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

            case '"': checkString(); break;

            default:
                if (isDigit(c)) {
                    checkNumber();
                } else if (isAlpha(c)) {
                   checkIdentifier();
                } else {
                    Jvlox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private char next() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

     private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
     }

     private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
     }

     private void checkString() {
       while (peek() != '"' && !isAtEnd()) {
          if (peek() == '\n') line++;
          next();
       }

       if (isAtEnd()) {
           Jvlox.error(line, "Unterminated String");
       }

       next();

       String value = source.substring(start + 1, current - 1);
       addToken(STRING, value);
     }

     private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
     }

     private void checkNumber() {
        while (isDigit(peek())) next();

        if (peek() == '.' && isDigit(peekNext())) {
            do next();
            while (isDigit(peek()));
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
     }

     private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
     }

     private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
     }

     private void checkIdentifier() {
        while (isAlphaNumeric(peek())) next();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
     }
}