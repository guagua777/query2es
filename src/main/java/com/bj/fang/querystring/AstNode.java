package com.bj.fang.querystring;

public class AstNode {

    private String key;
    private AstNode left;
    private Operator operator;
    private AstNode right;
    private String value;

    //普通token时存储text
    private String text;

    public AstNode() {
    }

    public AstNode(String text) {
        this.text = text;
    }

    public boolean isReference(){
        if ((key == null || "".equals(key)) && left != null){
            return true;
        }
        return false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        if (key == null || "".equals(key)){
            throw new RuntimeException("filter is error");
        }
        this.key = key;
    }

    public AstNode getLeft() {
        return left;
    }

    public void setLeft(AstNode left) {
        this.left = left;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public AstNode getRight() {
        return right;
    }

    public void setRight(AstNode right) {
        this.right = right;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null || "".equals(value)){
            throw new RuntimeException("filter is error");
        }
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
