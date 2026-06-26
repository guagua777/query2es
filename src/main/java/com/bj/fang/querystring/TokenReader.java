package com.bj.fang.querystring;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

public class TokenReader extends StringReader {


    /**
     * Creates a new string reader.
     *
     * @param s String providing the character stream.
     */
    public TokenReader(String s) {
        super(s);
    }

    public List<Token> getTokens() throws IOException {
        List<Token> tokens = new LinkedList<>();
        int ci = -1;
        while ((ci = read()) != -1){
            this.reset();
            if (isKey()){
                Token key = getKey();
                tokens.add(key);
            } else if(isComparer()){
                Token comparer = getComparer();
                tokens.add(comparer);
                Token value = getValue();
                checkValue(value.getText());
                tokens.add(value);
            } else if (isOperatorAndOr()){
                Token spliter = getOperatorAndOr();
                tokens.add(spliter);
            } else if (isParenthesis()){
                char c = (char) read();
                mark(0);
                Token token = new Token();
                token.setText(c+"");
                token.setTokenType(Token.TokenType.PARENTHESIS);
                tokens.add(token);
            } else {
                throw new RuntimeException("filter is error");
            }
        }
        return tokens;
    }
    public List<Token> getTokensWithOutCheckValue() throws IOException {
        List<Token> tokens = new LinkedList<>();
        int ci = -1;
        while ((ci = read()) != -1){
            this.reset();
            if (isKey()){
                Token key = getKey();
                tokens.add(key);
            } else if(isComparer()){
                Token comparer = getComparer();
                tokens.add(comparer);
                Token value = getValue();
                tokens.add(value);
            } else if (isOperatorAndOr()){
                Token spliter = getOperatorAndOr();
                tokens.add(spliter);
            } else if (isParenthesis()){
                char c = (char) read();
                mark(0);
                Token token = new Token();
                token.setText(c+"");
                token.setTokenType(Token.TokenType.PARENTHESIS);
                tokens.add(token);
            } else {
                throw new RuntimeException("filter is error");
            }
        }
        return tokens;
    }

    public boolean isKey() throws IOException {
        int read = this.read();
        this.reset();
        if (read == -1){
            throw new RuntimeException("filter is end");
        } else {
            char c = (char) read;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'){
                return true;
            } else {
                return false;
            }
        }
    }

    public Token getKey() throws IOException {
        int ci = -1;
        StringBuilder sb = new StringBuilder("");
        while ((ci = read()) != -1){
            char c = (char) ci;
            if (isKeyCharator(c)){
                sb.append(c);
                this.mark(0);
            } else {
                reset();
                Token token = new Token();
                token.setTokenType(Token.TokenType.STRING);
                token.setText(sb.toString());
                return token;
            }
        }
        return null;
    }

    public boolean isKeyCharator(char c) throws IOException {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9') || c == '.'){
            return true;
        } else {
            return false;
        }
    }

    public boolean isComparer() throws IOException {
        int read = read();
        this.reset();
        if (read == -1){
            throw new RuntimeException("filter is error");
        } else {
            char c = (char)read;
            if (c == '=' || c == '!' || c == '>' || c == '<'){
                return true;
            } else {
                return false;
            }
        }
    }


    public Token getComparer() throws IOException {
        int read = read();
        mark(0);
        if (read == -1){
            throw new RuntimeException("filter is error");
        } else {
            char c = (char) read;
            if (c == '='){
                Token token = new Token();
                token.setText("=");
                token.setTokenType(Token.TokenType.OPERATOR);
                return token;
            } else if (c == '!'){
                c = (char) read();
                if (c != '='){
                    throw new RuntimeException("filter is error");
                } else {
                    Token token = new Token();
                    token.setText("!=");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    mark(0);
                    return token;
                }
            } else if (c == '>'){
                c = (char) read();
                if (c != '='){
                    reset();
                    Token token = new Token();
                    token.setText(">");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    return token;
                } else {
                    Token token = new Token();
                    token.setText(">=");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    mark(0);
                    return token;
                }
            } else if (c == '<'){
                c = (char) read();
                if (c != '='){
                    reset();
                    Token token = new Token();
                    token.setText("<");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    return token;
                } else {
                    Token token = new Token();
                    token.setText("<=");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    mark(0);
                    return token;
                }
            }
        }
        return null;
    }


    public Token getValue() throws IOException {
        int ci = read();
        reset();
        if (ci == -1){
            throw new RuntimeException("filter is error");
        }
        Token token = new Token();
        StringBuilder sb = new StringBuilder("");
        while ((ci = read()) != -1){
            char c = (char) ci;
            if (c == '|' || c == '&' || c == '(' || c == ')'){
                if (c == '|'){
                    char n = (char) read();
                    if (n == '|'){
                        reset();
                        token.setTokenType(Token.TokenType.STRING);
                        token.setText(sb.toString());
                        return token;
                    } else {
                        sb.append(c).append(n);
                        this.mark(0);
                    }
                } else {
                    reset();
                    token.setTokenType(Token.TokenType.STRING);
                    token.setText(sb.toString());
                    return token;
                }
            } else {
                sb.append(c);
                this.mark(0);
            }
        }
        token.setTokenType(Token.TokenType.STRING);
        token.setText(sb.toString());
        return token;
    }

    public boolean isOperatorAndOr() throws IOException {
        int read = read();
        this.reset();;
        if (read == -1){
            throw new RuntimeException("filter is error");
        } else {
            char c = (char) read;
            if (c == '&' || c == '|'){
                return true;
            } else {
                return false;
            }
        }
    }


    public Token getOperatorAndOr() throws IOException {
        int read = read();
        mark(0);
        if (read == -1){
            throw new RuntimeException("filter is error");
        } else {
            char c = (char) read;
            if (c == '&'){
                Token token = new Token();
                token.setText("&");
                token.setTokenType(Token.TokenType.OPERATOR);
                return token;
            } else if (c == '|'){
                c = (char)read();
                if (c != '|'){
                    throw new RuntimeException("filter is error");
                } else {
                    mark(0);
                    Token token = new Token();
                    token.setText("||");
                    token.setTokenType(Token.TokenType.OPERATOR);
                    return token;
                }
            }
        }
        return null;
    }



    public boolean isParenthesis() throws IOException {
        int read = read();
        reset();
        if (read == -1){
            throw new RuntimeException("filter is error");
        } else {
            char c = (char) read;
            if (c == '(' || c == ')'){
                return true;
            } else {
                return false;
            }
        }
    }

    public void checkValue(String value){
        //value为空
        if (value == null || "".equals(value)){
            throw new RuntimeException("filter is error");
        }
        //如果包含_，则剩余字符全部为数字
        if (value.contains("_")){
            //如果为排序字段，则为正常
            if (value.toLowerCase().contains("desc") || value.toLowerCase().contains("asc")){
                return;
            }
            if (!Transfer.isNumberOne_(value)){
                throw new RuntimeException("filter is error");
            }
        }
        //如果包含竖杠|，则必须为数字
        if (value.contains("|")){
            if (!Transfer.isNumberAndYaxis(value)){
                throw new RuntimeException("filter is error");
            }
        }
        //


    }




    public Token getStringValue(){
        return null;
    }

    public Token getNumber(){
        return null;
    }









    public static void main(String[] args) throws IOException {
//        TokenReader t = new TokenReader("cateId=12|11&state=2&(params586=6||params756=6)&title=_我知道_");
//        TokenReader t = new TokenReader("(cateId=1|2&isVip=1)||(cateId=11|12&isVip=2&title=北京租房)||title.keyword=北京租房||param350=*||params=!*");
        TokenReader t = new TokenReader("(cateId=1|2&isVip>1)||(cateId>=11|12&isVip<2&title<=北京租房)||title.keyword=北京租房||param350=*||params=!*");
        List<Token> tokens = t.getTokens();
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens){
            sb.append(token.getText()).append("   ");
        }
        System.out.println(sb.toString());
    }



}
