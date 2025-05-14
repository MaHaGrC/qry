package gridServer;

import gridServer.QryParser2.BlockTest;
import gridServer.QryParser2.QryLexer;
import gridServer.QryParser2.QryToSqlTranslator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrySyntaxTest {

    Map<String, String> params;

    QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();


    void applyTest(String exp, String is, String msg) {
        qryToSqlTranslator = new QryToSqlTranslator();
        String translate = qryToSqlTranslator.translate(is);
        if (null != qryToSqlTranslator.block){
            System.out.println( BlockTest.showBlock(qryToSqlTranslator.block) );
            System.out.println(""  );
        } else {
            System.out.println(  QryLexer.instance.tokenize(is) );
        }
        if (translate.startsWith("select") && !exp.toLowerCase().startsWith("select")) {
            System.out.println("translated: " + translate);
            System.out.println("");
            translate = translate.substring(translate.indexOf("from ")+5);
        }
        if (null == msg || msg.isEmpty()) {
            assertEquals(exp, translate);
        } else {
            assertEquals(exp, translate, msg);
        }
    }

    void applyTest(String exp, String is){
        applyTest(exp, is, "");
    }



    @Test
    void translateOK() {
        applyTest("", "", "");
        applyTest( "a join b on a.x = b.y", "a[x][y]b" );
        applyTest( "a join b on a.x1 = b.y1 and a.x2 = b.y2", "a[x1,x2][y1,y2]b" );
        applyTest( "a join b on a.id = b.a", "a[]b" );
        applyTest( "a join b on a.id = b.a join c on b.id = c.b", "a[]b[]c" );
    }

    @Test
    void translateGlobalColumn() {
        applyTest( "a join b on x = b.y", "a[..x][y]b" );
        applyTest( "a join b on a.x1 = b.y1 and x2 = b.y2", "a[x1,..x2][y1,y2]b" );
    }

    @Test
    void translateAutoAlias() {
        applyTest( "a join a a2 on a.x = a2.y", "a[x][y]a" );
        applyTest( "a join a a2 on a.id = a2.a join a a3 on a2.id = a3.a", "a[]a[]a" );
        applyTest( "a join b on a.b = b.y", "a[b][y]b" );
    }

    @Test
    void translateManualAlias() {
        applyTest( "a join a a2 on a.x = a2.y", "a[x][y]a__a2" );
        applyTest( "a join a a2 on a.id = a2.a join a a3 on a2.id = a3.a", "a[]a__a2[]a__a3" );
    }


    @Test
    void translateSpace() {
        applyTest( "a join b on a.id = b.a", " a[]b" );
    }


    @Test
    void translateExpCol() {
        applyTest( "a join b on a.id = b.a where 1=1", "a[]b where 1=1" );
    }

    @Test
    void translate() {
        applyTest( "article", "article" );
        applyTest( "article where 1=1", "article where 1=1", "handle unparseable content" );
        applyTest( "article where 1=1 and 2=2", "article where 1=1 and 2=2", "handle unparseable content longer then debug-print length" );
    }



    @Test
    void translateRightToLeft() {
        applyTest( "a join b on a.b = b.id", "a[<]b" );
        applyTest( "a join b on a.id = b.a join c on b.c = c.id", "a[]b[<]c" );
        applyTest("article join article_price on article.id = article_price.article join type on article_price.type = type.id", "article[]article_price[<]type");
    }


    @Test
    void sqlJoinExplicit() {
        applyTest( "a join b on a.id = b.a join c on a.x = c.y", "a[]b[a.x][y]c" );
    }

    @Test
    void sqlFilter() {
        applyTest( "select productNo from a", "[productNo]a" );
        applyTest( "select productNo,articleNo from a", "[productNo,articleNo]a" );
        applyTest( "select productNo,articleNo from a join b on a.id = b.a", "[productNo,articleNo]a[]b" );
    }

    @Test
    void sqlFilterSuffix() { /* ease auto-complete by appending to already named tables ... */
        applyTest( "select productNo from a", "a[productNo]" );
        applyTest( "select productNo,articleNo from a", "a[productNo,articleNo]" );
        applyTest( "select productNo,articleNo from a join b on a.id = b.a", "a[]b[productNo,articleNo]" );
    }


    @Test
    void sql() {
        applyTest( "select productNo,a.* from a", "[productNo,a.*]a", "allow * to be part of word (suffix)" );
    }

    @Test
    void sqlGroup() { /* ease auto-complete by appending to already named tables ... */
        applyTest( "select productNo, count(*) cnt from a group by productNo", "a[productNo,,]" );
        applyTest( "select productNo,articleNo, count(*) cnt from a group by productNo,articleNo", "a[productNo,articleNo,,]" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a group by productNo", "a[productNo,,articleNo]" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a group by productNo", "a[productNo,,articleNo]" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a join b on a.id = b.a group by productNo", "a[]b[productNo,,articleNo]" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a join b on a.id = b.a where productNo='1' group by productNo", "a[]b[productNo,,articleNo] W productNo='1'" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a join b on a.id = b.a where productNo='1' group by productNo limit 25", "a[]b[productNo,,articleNo] W productNo='1' limit 25" );
        applyTest( "select productNo, min(articleNo), max(articleNo), count(*) cnt from a join b on a.id = b.a where productNo='1' group by productNo having count(*)>1 limit 25", "a[]b[productNo,,articleNo] W productNo='1' having count(*)>1 limit 25" );
    }


    @Test
    void sqlNestedFunction() {

        applyTest( "select id from attribute_value", "attribute_value[id]" );
        applyTest( "select bo2json(id) from attribute_value", "attribute_value[bo2json(id)]" );
        applyTest( "select bo2json(Greatest(id)) from attribute_value", "attribute_value[bo2json(Greatest(id))]" );
    }

    @Test
    void test2FIX() {
        applyTest( "select bo2json(Greatest(id)) from attribute_value", "attribute_value[bo2json(Greatest(id))]" );
    }


    void mergeHints(String expected, String a, String b, String msg) {
        assertEquals( expected, QrySyntax.mergeHints( a, b), msg);
        applyTest( "select * from v_dbTabusage limit 100 /*{ \"colsGrouped\": 0, \"pos\": { \"top\": \"50\", \"left\": \"50\"} , \"valHndl\": { \"tab\": \"LINK\" }, \"cdm\": \"none\" } */"
                , "select * from v_dbTabusage limit 100 /*{ \"colsGrouped\": 0, \"pos\": { \"top\": \"50\", \"left\": \"50\"} , \"valHndl\": { \"tab\": \"LINK\" }, \"cdm\": \"none\" } */" );
    }


    @Test
    void mergeHints() {
        mergeHints("","","" , "empty");
        mergeHints("/*{\"cdm\":\"none\"}*/","/*{ cdm: \"none\"}*/","" , "reformat Hints");
        mergeHints("/*{\"cdm\":\"none\"}*/","/*{\"cdm\":\"none\"}*/","" , "unchanged w/o b");
        mergeHints("/*{\"cdm\":\"none\"}*/","/*{\"cdm\":\"none\"}*/","/*{\"cdm\":\"none\"}*/" , "unchanged same");
        mergeHints("/*{\"valHndl\":{\"tab\":\"LINK\"},\"cdm\":\"none\"}*/","/*{\"cdm\":\"none\"}*/","/*{\"valHndl\":{\"tab\":\"LINK\"},\"cdm\":\"none\"}*/" , "take additional hint from below");
    }

    @Test
    void mergeHintsFix() {
        mergeHints("/*{\"valHndl\":{\"tab\":\"LINK\"},\"cdm\":\"none\"}*/","/*{\"cdm\":\"none\"}*/","/*{\"valHndl\":{\"tab\":\"LINK\"},\"cdm\":\"none\"}*/" , "take additional hint from below");
    }

    @Test
    void mergeHintsFix2() {
        String x="app[module,identifier,url] order by 1,2 /*{\"pos\":{\"top\":\"50\",\"left\":\"600\",\"width\":\"300\",\"height\":\"800\"},\"colOrderIdx\":[0,1],\"colsGrouped\":1,\"valHndl\":{\"identifier\":\"HREF_2\"},\"limit\":500}*/";
        applyTest(x, x);
    }

    @Test
    void mergeHintsFix3() {
        String x = "select productno,articleno,identifier,article_av.* from article join article_av on article.id = article_av.article join attribute on article_av.attribute = attribute.id limit 7";
        applyTest(x, x);
    }

    @Test
    void mergeHintsFix4() {
        String x = "~/status.sh -a";
        applyTest(x, x);
    }

    @Test
    void joinTabToCol() {
        String x = "~/status.sh -a";
        applyTest( "select * from erp_import_article join erp_import_article_av on erp_import_article.id = erp_import_article_av.erpimportarticle where key like '%igest%'"
                ,"erp_import_article[]erp_import_article_av W key like '%igest%'");
    }



}