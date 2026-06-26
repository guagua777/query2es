package com.bj.fang.querystring;

import com.bj.fang.querystring.Transfer.SortAndScroll;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class TransferV2 {

    public SearchSourceBuilder transfer(String filter, SearchType searchType) throws IOException {
        return transfer(filter, -1, searchType);
    }

    public SearchSourceBuilder transfer(String filter, int timeout, SearchType searchType) throws IOException {
        //1.去掉空格
        String f1 = filter.replaceAll(" ", "");
        filter = f1;
        if (filter.length() > 2000) {
            throw new RuntimeException("filter is too long");
        }
        //括号是否匹配
        parenthesisCheck(filter);
        //page和size是否同时出现
//        pageSizeCheck(filter);

        TokenReader tokenReader = new TokenReader(filter);
        List<Token> tokens = tokenReader.getTokens();
        AstNode astNode = transferAst(tokens);
        SearchSourceBuilder searchSourceBuilder = transferES(astNode, timeout, searchType);
        return searchSourceBuilder;
    }

    private void parenthesisCheck(String filter) {
        int length = filter.length();
        int k = 0;
        for (int i = 0; i < length; i++) {
            if (filter.charAt(i) == '(') {
                k++;
            } else if (filter.charAt(i) == ')') {
                k--;
            }
        }
        if (k != 0) {
            throw new RuntimeException("filter is error, parenthes is not match");
        }
    }

    private void pageSizeCheck(String filter) {
        if (filter.contains("page")) {
            if (!filter.contains("size")) {
                throw new RuntimeException("filter is error size is missing");
            }
        }
        if (filter.contains("size")) {
            if (!filter.contains("page")) {
                throw new RuntimeException("filter is error page is missing");
            }
        }

    }

    private AstNode transferAst(List<Token> tokens) {
        Deque<AstNode> constantDeque = new LinkedList<>();
        Deque<Token> operatorDeque = new LinkedList<>();
        for (Token t : tokens) {
            if (t.getTokenType() == Token.TokenType.STRING) {
                constantDeque.push(new AstNode(t.getText()));
            } else {
                if (t.getText().equals("(")) {
                    operatorDeque.push(t);
                } else if (t.getTokenType() == Token.TokenType.OPERATOR) {
                    /**
                     * 1.如果栈为空，则直接入栈
                     * 2.如果栈顶元素为(，则直接入栈
                     * 3.如果栈顶元素优先级<=t的优先级，不停弹出，直到栈顶元素为(或者是栈顶元素优先级>t的优先级
                     * 4.如果栈顶元素>t的优先级，直接入栈
                     */
                    while (true) {
                        if (operatorDeque.isEmpty()) {
                            operatorDeque.push(t);
                            break;
                        } else if (operatorDeque.peek().getText().equals("(")) {
                            operatorDeque.push(t);
                            break;
                        } else if (getPriority(t) <= getPriority(operatorDeque.peek())) {
                            AstNode astNode = new AstNode();
                            Token top = operatorDeque.pop();
                            Operator operator = Operator.get(top.getText());
                            if (operator.isBasicOp()) {
                                astNode.setOperator(operator);
                                astNode.setValue(constantDeque.pop().getText());
                                astNode.setKey(constantDeque.pop().getText());
                            } else {
                                astNode.setOperator(operator);
                                astNode.setRight(constantDeque.pop());
                                astNode.setLeft(constantDeque.pop());
                            }
                            constantDeque.push(astNode);
                        } else {
                            operatorDeque.push(t);
                            break;
                        }
                    }
                } else if (t.getText().equals(")")) {
                    //依次弹出栈中元素，直到(
                    while (!operatorDeque.peek().getText().equals("(")) {
                        AstNode astNode = new AstNode();
                        Token top = operatorDeque.pop();
                        Operator operator = Operator.get(top.getText());
                        if (operator.isBasicOp()) {
                            astNode.setOperator(operator);
                            astNode.setValue(constantDeque.pop().getText());
                            astNode.setKey(constantDeque.pop().getText());
                        } else {
                            astNode.setOperator(operator);
                            astNode.setRight(constantDeque.pop());
                            astNode.setLeft(constantDeque.pop());
                        }
                        constantDeque.push(astNode);
                    }
                    AstNode qutoeNode = constantDeque.peek();
                    qutoeNode.setKey("quote");
                    //弹出(
                    operatorDeque.pop();
                } else {
                    throw new RuntimeException("filter is error");
                }
            }
        }
        //处理特殊情况操作符
        while (!operatorDeque.isEmpty()) {
            Token pop = operatorDeque.pop();
            if (pop.getText().equals("(")) {
                throw new RuntimeException("filter is error");
            } else {
                Operator o = Operator.get(pop.getText());
                AstNode astNode = new AstNode();
                if (o.isBasicOp()) {
                    astNode.setOperator(o);
                    astNode.setValue(constantDeque.pop().getText());
                    astNode.setKey(constantDeque.pop().getText());
                } else {
                    astNode.setOperator(o);
                    astNode.setRight(constantDeque.pop());
                    astNode.setLeft(constantDeque.pop());
                }
                constantDeque.push(astNode);
            }
        }
        if (constantDeque.size() != 1) {
            throw new RuntimeException("filter is error");
        }
        return constantDeque.pop();
    }

    private SearchSourceBuilder transferES(AstNode astNode, int timeout, SearchType searchType) {
        SortAndScroll sortAndScroll = new SortAndScroll();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilderInner = QueryBuilders.boolQuery();
        preOrder(astNode, boolQueryBuilderInner, sortAndScroll);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(boolQueryBuilderInner);
        sourceBuilder.query(boolQueryBuilder);
        if (sortAndScroll.sortField != null && !"".equals(sortAndScroll.sortField)) {
            if (sortAndScroll.sort == null || "".equals(sortAndScroll.sort) || sortAndScroll.sort.equals("desc")) {
                sourceBuilder.sort(sortAndScroll.sortField, SortOrder.DESC);
            } else {
                sourceBuilder.sort(sortAndScroll.sortField, SortOrder.ASC);
            }
        }
        if (sortAndScroll.size > 0) {
            //scroll不支持from设置
            if (searchType == SearchType.QUERY) {
                sourceBuilder.from((sortAndScroll.page - 1) * sortAndScroll.size);
            }
            sourceBuilder.size(sortAndScroll.size);
        }
        if (timeout > 0) {
            sourceBuilder.timeout(new TimeValue(timeout, TimeUnit.SECONDS));
        }
        return sourceBuilder;
    }

    private void preOrder(AstNode astNode, BoolQueryBuilder boolQueryBuilder, SortAndScroll sortAndScroll) {

        if (!astNode.isReference() && !"quote".equals(astNode.getKey())) {
            Operator operator = astNode.getOperator();
            if (operator == Operator.EQUAL) {
                //剔除元素：分页，排序
                if (astNode.getKey().equalsIgnoreCase("page") || astNode.getKey().equalsIgnoreCase("size") || astNode.getKey().equalsIgnoreCase("sort")) {
                    if (astNode.getKey().equalsIgnoreCase("page")) {
                        int i = Integer.parseInt(astNode.getValue());
//                        if (i <= 0){
//                            throw new RuntimeException("filter is error, page is error");
//                        }
                        sortAndScroll.page = i;
                    } else if (astNode.getKey().equalsIgnoreCase("size")) {
                        sortAndScroll.size = Integer.parseInt(astNode.getValue());
                    } else if (astNode.getKey().equalsIgnoreCase("sort")) {
                        if (astNode.getValue().contains("_")) {
                            sortAndScroll.sortField = astNode.getValue().split("_")[0];
                            sortAndScroll.sort = astNode.getValue().split("_")[1].toLowerCase();
                        } else {
                            sortAndScroll.sortField = astNode.getValue();
                        }
                    }
                    return;
                }
                /**
                 * 分为：
                 * 1.value全部为数字，则使用must term
                 * 3.value为数字和|，则使用should term
                 * 2.value除去数字外，包含一个下划线_，则使用rang
                 * 4.value为单独*，则使用存在
                 * 5.value为!*，则使用不存在
                 * 6.其他则使用match
                 */
                if (isNumber(astNode.getValue())) {
                    boolQueryBuilder.must(QueryBuilders.termQuery(astNode.getKey(), astNode.getValue()));
                } else if (isNumberOne_(astNode.getValue())) {
                    firstLastIndexIs_(astNode.getValue());
                    RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).from(astNode.getValue().split("_")[0]).to(astNode.getValue().split("_")[1])
                        .includeLower(true);
                    boolQueryBuilder.must(rangeQueryBuilder);
                } else if (isNumberAndYaxis(astNode.getValue())) {
                    firstLastIndexIsYaxis(astNode.getValue());
                    BoolQueryBuilder boolquery = QueryBuilders.boolQuery();
                    for (String str : astNode.getValue().split("\\|")) {
                        boolquery.should(QueryBuilders.termQuery(astNode.getKey(), str));
                    }
                    boolQueryBuilder.must(boolquery);
                } else if (astNode.getValue().equals("*")) {
                    boolQueryBuilder.must(QueryBuilders.existsQuery(astNode.getKey()));
                } else if (astNode.getValue().equals("!*")) {
                    boolQueryBuilder.mustNot(QueryBuilders.existsQuery(astNode.getKey()));
                } else {
                    boolQueryBuilder.must(QueryBuilders.matchQuery(astNode.getKey(), astNode.getValue()).operator(org.elasticsearch.index.query.Operator.AND));
                }
            } else if (operator == Operator.NOT_EQUAL) {
                if (isNumber(astNode.getValue())) {
                    boolQueryBuilder.mustNot(QueryBuilders.termQuery(astNode.getKey(), astNode.getValue()));
                } else if (isNumberOne_(astNode.getValue())) {
                    firstLastIndexIs_(astNode.getValue());
                    RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).from(astNode.getValue().split("_")[0]).to(astNode.getValue().split("_")[1])
                        .includeLower(true);
                    boolQueryBuilder.mustNot(rangeQueryBuilder);
                } else if (isNumberAndYaxis(astNode.getValue())) {
                    firstLastIndexIsYaxis(astNode.getValue());
                    BoolQueryBuilder boolquery = QueryBuilders.boolQuery();
                    for (String str : astNode.getValue().split("\\|")) {
                    boolQueryBuilder.mustNot(QueryBuilders.termQuery(astNode.getKey(), str));
//                        boolquery.should(QueryBuilders.termQuery(astNode.getKey(), str));
                    }
//                    boolQueryBuilder.mustNot(boolquery);
                } else if (astNode.getValue().equals("*")) {
                    throw new RuntimeException("filter is error");
                } else if (astNode.getValue().equals("!*")) {
                    throw new RuntimeException("filter is error");
                } else {
                    boolQueryBuilder.mustNot(QueryBuilders.matchQuery(astNode.getKey(), astNode.getValue()).operator(org.elasticsearch.index.query.Operator.AND));
                }
            } else if (operator == Operator.GT) {
                if (!isNumber(astNode.getValue())) {
                    throw new RuntimeException("filter is error");
                }
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).gt(astNode.getValue()).includeLower(false);
                boolQueryBuilder.must(rangeQueryBuilder);
            } else if (operator == Operator.GTE) {
                if (!isNumber(astNode.getValue())) {
                    throw new RuntimeException("filter is error");
                }
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).gt(astNode.getValue()).includeLower(true);
                boolQueryBuilder.must(rangeQueryBuilder);
            } else if (operator == Operator.LT) {
                if (!isNumber(astNode.getValue())) {
                    throw new RuntimeException("filter is error");
                }
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).lt(astNode.getValue()).includeUpper(false);
                boolQueryBuilder.must(rangeQueryBuilder);
            } else if (operator == Operator.LTE) {
                if (!isNumber(astNode.getValue())) {
                    throw new RuntimeException("filter is error");
                }
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(astNode.getKey()).lt(astNode.getValue()).includeUpper(true);
                boolQueryBuilder.must(rangeQueryBuilder);
            }
            return;
        }

        // 内嵌括号
        if ("quote".equals(astNode.getKey())) {
            BoolQueryBuilder quoteBuilder = new BoolQueryBuilder();
            BoolQueryBuilder boolQueryBuilderLeft = new BoolQueryBuilder();
            BoolQueryBuilder boolQueryBuilderRight = new BoolQueryBuilder();
            quoteBuilder.should(boolQueryBuilderLeft).should(boolQueryBuilderRight);
            preOrder(astNode.getLeft(), boolQueryBuilderLeft, sortAndScroll);
            preOrder(astNode.getRight(), boolQueryBuilderRight, sortAndScroll);
            boolQueryBuilder.must(quoteBuilder);
        } else {
            preOrder(astNode.getLeft(), boolQueryBuilder, sortAndScroll);
            preOrder(astNode.getRight(), boolQueryBuilder, sortAndScroll);
        }
    }


    private int getPriority(Token token) {
        Operator operator = Operator.get(token.getText());
        return operator.getPriority();
    }

    private boolean isNumber(String str) {
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if ((c >= '0' && c <= '9') || c == '.') {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean isNumberOne_(String str) {
        int count = 0;
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if (count > 1) {
                return false;
                //添加支持小数点
            } else if ((c >= '0' && c <= '9') || c == '.') {
                continue;
            } else if (c == '_') {
                count++;
                continue;
            } else {
                return false;
            }
        }
        if (count == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNumberAndYaxis(String str) {
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if ((c >= '0' && c <= '9') || c == '.') {
                continue;
            } else if (c == '|') {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public void firstLastIndexIsYaxis(String str) {
        if (str.charAt(0) == '|') {
            throw new RuntimeException("filter is error");
        }
        if (str.charAt(str.length() - 1) == '|') {
            throw new RuntimeException("filter is error");
        }
    }

    public void firstLastIndexIs_(String str) {
        if (str.charAt(0) == '_') {
            throw new RuntimeException("filter is error");
        }
        if (str.charAt(str.length() - 1) == '_') {
            throw new RuntimeException("filter is error");
        }
    }


    private String printASTNode(AstNode astNode, int level) {
        if (!astNode.isReference()) {
            return astNode.getKey() + astNode.getOperator().getText() + astNode.getValue();
        } else {
            return getSpace(level + 1) + printASTNode(astNode.getRight(), level + 1) + "\r\n" + getSpace(level) + astNode.getOperator().getText() + "\r\n" + getSpace(level + 1)
                + printASTNode(astNode.getLeft(), level + 1) + "\r\n";
        }
    }

    private String getSpace(int n) {
        if (n == 0) {
            return "";
        } else {
            return "------" + getSpace(n - 1);
        }
    }

    private void printFilter(String filter) throws IOException {
        //1.去掉空格
        String f1 = filter.replaceAll(" ", "");
        filter = f1;
        if (filter.length() > 2000) {
            throw new RuntimeException("filter is too long");
        }
        //括号是否匹配
        parenthesisCheck(filter);
        //page和size是否同时出现
//        pageSizeCheck(filter);

        TokenReader tokenReader = new TokenReader(filter);
        List<Token> tokens = tokenReader.getTokens();
        AstNode astNode = transferAst(tokens);
        String s = printASTNode(astNode, 0);
        System.out.println(s);
    }


    public static void main(String[] args) throws IOException {

//        String str = "asdfasdfa|";
//        if (str.charAt(str.length()-1) == '|'){
//            System.out.println("true");
//        }
//        System.out.println("");

//        String str1 = "(cateId=1|2&isVip>1)||(cateId>=11|12&isVip<2&title<=北京租房)||title.keyword=北京租房||param350=*||params=!*&page=1&size=100&sort=adf";

//        String str = "(cateId=1|2&isVip>1||(cateId>=11|12&isVip<2&title<=北京租房)||title.keyword=北京租房||param350=*||params=!*&page=1&size=100&sort=adf";
//        String str = "(cateId=1|2||source>50)||jiage>=12.02&page=1&size=12&sort=source";
//        String str = "(cateId=|2||source>50)||jiage>=12.02&page=2&size=12&sort=source_ASC";
//        String str = "(cateId=2||source>50)||jiage>=12.02&page=2&size=12&sort=source_ASC&content=adffafe|";
//        String str = "params.zifu=beijing||params.zifu=tianjin&params.shuzi=12";
//        String str = "unityCityId=984&cateId=11|12&state=2&params586=6&(params350=1||params421=1)&page=1&size=500";
//        String str = "a=b&c=d&page=-1&size=20";
        String str = "a=b&c=d&page=-10";
        new TransferV2().printFilter(str);
        SearchSourceBuilder transfer = new TransferV2().transfer(str, 2000, SearchType.QUERY);
        System.out.println(transfer);
    }


}
