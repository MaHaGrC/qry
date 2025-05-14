package gridServer;

import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QryHintHelperTest {


    Map<String,String> params =  new HashMap<>();

    void checkAndExtractHint( String qry, String hints, String hint, String qry_, String msg  ) {
        params.clear();
        params.put("qry", qry);
        params.put("hint", hints);
        QryHintHelper.checkAndExtractHint( params, hint);
        assertEquals( qry_, params.get("qry"), msg);
    }
    void checkAndExtractHint( String qry, String qry_, String msg  ) {
        checkAndExtractHint(qry,"", "UPDATE", qry_, msg);
    }

    @Test
    void checkAndExtractHint() {
        checkAndExtractHint("article", "article", "simple");
        checkAndExtractHint("UPDATE article", "article", "front");
        checkAndExtractHint(" UPDATE article", "article", "front+");
        checkAndExtractHint(" UPDATE article", "article", "front++");
        checkAndExtractHint("article UPDATE", "article", "back");
        checkAndExtractHint("article UPDATE ", "article", "back+");
        checkAndExtractHint("article  UPDATE", "article", "+back");
        checkAndExtractHint("article  UPDATE ", "article", "+back+");
        checkAndExtractHint("article  UPDATE CACHE", "article CACHE", "+backM+");
        checkAndExtractHint("CACHE  UPDATE article", "CACHE article", "+Mid+");
    }



}