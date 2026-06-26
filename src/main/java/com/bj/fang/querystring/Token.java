package com.bj.fang.querystring;

public class Token {

    public enum TokenType {
        STRING,
        NUMBER,
        OPERATOR,
        PARENTHESIS;
    }

    private String text;
    private TokenType tokenType;

    public Token() {
    }

    public Token(String text, TokenType tokenType) {
        this.text = text;
        this.tokenType = tokenType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }
}
