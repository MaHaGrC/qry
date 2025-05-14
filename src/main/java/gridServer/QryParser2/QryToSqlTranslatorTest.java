package gridServer.QryParser2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gridServer.QryParser2.QryLexer.TokenDefinition.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class QryToSqlTranslatorTest {



    @Test
    void initTest() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        qryToSqlTranslator.translate("a[]b+[]c");
    }

    @Test
    void initTestOrder() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        qryToSqlTranslator.translate("a[]b+[]c O d");
    }

    @Test
    void initTestMultiCol() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        assertEquals("select * from a join f on a.b = f.d and a.c = f.e", qryToSqlTranslator.translate("a[b,c][d,e]f"));
    }

    @Test
    void initTestGroup() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        assertEquals( "select d from a join b on a.id = b.a left join c on b.id = c.b order by e", qryToSqlTranslator.translate("a[]b+[]c[d] O e"));
    }

    @Test
    void complex() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        assertEquals("select productNo,articleNo,identifier,article_av.* from article join article_av on article.id = article_av.article join attribute on article_av.attribute = attribute.id",qryToSqlTranslator.translate("article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*]"));

    }

    @Test
    void complexAfterSyn() {
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertNotNull(qryToSqlTranslator);

        // syn already allows to map QRY to SQL --> keep it
        assertEquals("select productNo,articleNo,identifier,article_av.* from article join article_av on article.id = article_av.article join attribute on article_av.attribute = attribute.id limit 25",qryToSqlTranslator.translate("select productNo,articleNo,identifier,article_av.* from article join article_av on article.id = article_av.article join attribute on article_av.attribute = attribute.id limit 25"));

    }

    @Test
    void joinUsingTest(){
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertEquals("select * from qry_1 join article on qry_1.articleNo = article.articleNo",qryToSqlTranslator.translate("qry_1[articleNo]article"));

    }

    @Test
    void joinPostgresTest(){
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertEquals("select productNo,articleNo,identifier,article_av.* from article join article_av on article.id = article_av.article join attribute on article_av.attribute = attribute.id where identifier ~ 'CMYK' order by article_av.lastmodified desc"
                ,qryToSqlTranslator.translate("article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*] W identifier ~ 'CMYK' O article_av.lastmodified desc"));

    }

    @Test
    void joinPostgresWoJoinTest(){
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertEquals("select * from attribute_mapping where target ~ 'COL'",qryToSqlTranslator.translate("attribute_mapping W target ~ 'COL'"));

    }

    @Test
    void joinWOTest(){
        QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
        assertEquals("select attribute_value.identifier, count(*) cnt from attribute join attribute_value on attribute.id = attribute_value.attribute where attribute.identifier like 'mig2_Farbe' group by attribute_value.identifier O 1 desc"
                ,qryToSqlTranslator.translate("attribute[]attribute_value[attribute_value.identifier,,] W attribute.identifier like 'mig2_Farbe' O 1 desc"));
        assertEquals("select attribute_value.identifier, count(*) cnt from attribute join attribute_value on attribute.id = attribute_value.attribute where attribute.identifier ~ 'mig2_Farbe' group by attribute_value.identifier O 1 desc"
                ,qryToSqlTranslator.translate("attribute[]attribute_value[attribute_value.identifier,,] W attribute.identifier ~ 'mig2_Farbe' O 1 desc"));

    }


}
