package com.bj.fang.querystring;

import java.io.IOException;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class TransferTest {

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
//    String str = "a=b&c=d&page=-10";
//    String str = "cateId=11|12&state=2&page=1&params837!=0|1|2|3|4|5|6|7&unityCityId=2015|4|241|669|414|5|2054|497|93|463";
    String str = "cateId=11|12&state=2&page=1&params837!=0|1|2|3|4|5|6|7&(params350=1||params421=1)&unityCityId=2015|4|241|669|414|5|2054|497|93|463";
    SearchSourceBuilder transfer = new TransferV2().transfer(str, 2000, SearchType.QUERY);
    System.out.println(transfer);
  }


}