package com.bj.fang.querystring;

public enum Operator {
    AND("&", false, 10),
    OR("||", false,10),

    EQUAL("=", true, 20),
    NOT_EQUAL("!=", true, 20),
    GT(">", true, 20),
    GTE(">=", true, 20),
    LT("<", true, 20),
    LTE("<=", true, 20)
    ;


    private String text;
    private boolean basicOp;
    private int priority;



    Operator(String text, boolean basicOp, int priority) {
        this.text = text;
        this.basicOp = basicOp;
        this.priority = priority;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isBasicOp() {
        return basicOp;
    }

    public void setBasicOp(boolean basicOp) {
        this.basicOp = basicOp;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public static Operator get(String text){
        for (Operator o : Operator.values()){
            if (o.text.equals(text)){
                return o;
            }
        }
        return null;
    }

}
