package gridServer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataConMongoDbTest {

    List<Map<String, String>> flatJsons = new ArrayList<>();
    DataConMongoDbJsonHelper helper = new DataConMongoDbJsonHelper();

    public String simplifyMongoDb(String data){
        data = helper.simplifyMongoDBJso(data);
        data = data
                .replaceAll(":\\{", ": \\{")
                .replaceAll(":\"", ": \"")
                .replaceAll(",\"", ", \"")
                .replaceAll("\":(\\w)", "\": $1")
        ; // just to match test-format
        /*
        while (data.startsWith("{")) {
            String data_ = simplifyMongoDb_( data );
            if (data_.equals(data)) {
                break;
            }
            data = data_;
        }
        */
        System.out.println(data);
        Map<String, String> s = helper.flattenJson(new Gson().fromJson(data, JsonElement.class));
        System.out.println(s.toString());
        flatJsons.add(s);
        return data;
        //
        // DataConMongoDb dataConMongoDb = new DataConMongoDb();
        // return dataConMongoDb.simplify(data);
    }
    public String simplifyMongoDb_(String data) {
        data = data.replaceFirst(": \\{(\"[0-9a-f]{24})\": (\"?\\w*\"?)\\}", ": $2");
        data = data.replaceFirst(": \\{\"lookupValue\": (.*?),\\s*\"_class\": \"value-lookup\"}", ": $1");
        data = data.replaceFirst(": \\{\"\\$oid\": (.*?)}", ": $1");
        data = data.replaceFirst("\\{\"_id\": \"([0-9a-f]{24})\"(\\s*.*),\\s*\"_class\": (\"\\w*\")\\}", "{$3: {\"_id\": \"$1\"$2}}");
        return data;
    }
    @Test
    void simplifyMongoDBJso() {
        // JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        // MongoDB-Special-Case
        // String s = "{\"_id\": {\"$oid\": \"6513d6d3b6536f24b0e45e21\"}, \"enabledForClient\": {\"6513d6d0b6536f24b0e45de4\": true}, \"attributesForClient\": {\"6513d6d0b6536f24b0e45de4\": {\"partnerImportSource\": {\"lookupValue\": \"iPORT\", \"_class\": \"value-lookup\"}}}, \"externalId\": \"\", \"pimReference\": \"10000201\", \"identifier\": \"10000201\", \"description\": {\"de_DE\": \"ospLieferant\"}, \"_class\": \"supplier\"}";


        assertEquals("", "");
        assertEquals("{\"description\": {\"de_DE\": \"ospLieferant\"}}"     , simplifyMongoDb("{\"description\": {\"de_DE\": \"ospLieferant\"}}"));
        assertEquals("{\"enabledForClient\": true}"                         , simplifyMongoDb( "{\"enabledForClient\": {\"6513d6d0b6536f24b0e45de4\": true}}"));
        assertEquals("{\"partnerImportSource\": \"iPORT\"}"                 , simplifyMongoDb("{\"partnerImportSource\": {\"lookupValue\": \"iPORT\", \"_class\": \"value-lookup\"}}"));
        assertEquals("{\"_id\": \"6513d6d3b6536f24b0e45e21\"}"              , simplifyMongoDb("{\"_id\": {\"$oid\": \"6513d6d3b6536f24b0e45e21\"}}"));
        assertEquals("{\"supplier\": {\"_id\": \"6513d6d3b6536f24b0e45e21\"}}"  , simplifyMongoDb("{\"_id\": {\"$oid\": \"6513d6d3b6536f24b0e45e21\"}, \"_class\": \"supplier\"}"));
        // best case for REGEXP ....
        /*
        assertEquals("{\"supplier\": {\"_id\": \"6513d6d3b6536f24b0e45e21\", \"enabledForClient\": true, \"attributesForClient\": {\"6513d6d0b6536f24b0e45de4\": {\"partnerImportSource\": \"iPORT\"}}, \"externalId\": \"\", \"pimReference\": \"10000201\", \"identifier\": \"10000201\", \"description\": {\"de_DE\": \"ospLieferant\"}}}"
                ,  simplifyMongoDb( "{\"_id\": {\"$oid\": \"6513d6d3b6536f24b0e45e21\"}, \"enabledForClient\": {\"6513d6d0b6536f24b0e45de4\": true}, \"attributesForClient\": {\"6513d6d0b6536f24b0e45de4\": {\"partnerImportSource\": {\"lookupValue\": \"iPORT\", \"_class\": \"value-lookup\"}}}, \"externalId\": \"\", \"pimReference\": \"10000201\", \"identifier\": \"10000201\", \"description\": {\"de_DE\": \"ospLieferant\"}, \"_class\": \"supplier\"}"));
         */
        // way-better-case for JSON ...
        assertEquals("{\"supplier\": {\"_id\": \"6513d6d3b6536f24b0e45e21\", \"enabledForClient\": true, \"attributesForClient\": {\"partnerImportSource\": \"iPORT\"}, \"externalId\": \"\", \"pimReference\": \"10000201\", \"identifier\": \"10000201\", \"description\": {\"de_DE\": \"ospLieferant\"}}}"
                ,  simplifyMongoDb( "{\"_id\": {\"$oid\": \"6513d6d3b6536f24b0e45e21\"}, \"enabledForClient\": {\"6513d6d0b6536f24b0e45de4\": true}, \"attributesForClient\": {\"6513d6d0b6536f24b0e45de4\": {\"partnerImportSource\": {\"lookupValue\": \"iPORT\", \"_class\": \"value-lookup\"}}}, \"externalId\": \"\", \"pimReference\": \"10000201\", \"identifier\": \"10000201\", \"description\": {\"de_DE\": \"ospLieferant\"}, \"_class\": \"supplier\"}"));

        System.out.println("flatJsons: " );
        System.out.println( helper.mergeFlatJson(flatJsons) );

    }



    public String simplify_(String data) {
        data = data.replaceFirst("=Document\\{\\{([0-9a-f]{24})=(\\w*)\\}\\}", "=$2");
        data = data.replaceFirst("=Document\\{\\{([0-9a-f]{24}\\.)([\\w=]*)\\}\\}", "=[$2]");
        data = data.replaceFirst("=Document\\{\\{(\\w+)=(\\w*)\\}\\}", ".$1=$2");
        data = data.replaceFirst("=Document\\{\\{lookupValue=(.*),\\s*_class=value-lookup\\}\\}", "=$1");
        data = data.replaceFirst("Document\\{\\{_id=([0-9a-f]*),\\s*(.*),\\s*_class=(\\w*)\\}\\}", "$3.$1{$2}");
        return data;
    }

    public String simplify(String data){
        while (data.contains("Document{{")) {
            String data_ = simplify_( data );
            if (data_.equals(data)) {
                break;
            }
            data = data_;
        }
        return data;
        //
        // DataConMongoDb dataConMongoDb = new DataConMongoDb();
        // return dataConMongoDb.simplify(data);
    }

    @Test
    public void simplify(){
        assertEquals( "", simplify(""), "msg");
        assertEquals( "supplier.66276b148faa5344609d96e8{identifier=10180201}", simplify("Document{{_id=66276b148faa5344609d96e8, identifier=10180201, _class=supplier}}"), "msg");
        assertEquals( "description.de_DE=partner_supplier1", simplify("description=Document{{de_DE=partner_supplier1}}"), "msg");
        assertEquals( "enabledForClient=true", simplify("enabledForClient=Document{{6513d6d0b6536f24b0e45de4=true}}"), "msg");
        assertEquals( "partnerImportSource=iPORT", simplify("partnerImportSource=Document{{lookupValue=iPORT, _class=value-lookup}}"), "msg");
        assertEquals( "attributesForClient=[partnerImportSource=iPORT]", simplify("attributesForClient=Document{{6513d6d0b6536f24b0e45de4.partnerImportSource=iPORT}}"), "msg");
        assertEquals( "supplier.66276b148faa5344609d96e8{enabledForClient=true, attributesForClient=[partnerImportSource=iPORT], externalId=partner_supplier1, pimReference=10180201, identifier=10180201, description.de_DE=partner_supplier1}"
                    , simplify("Document{{_id=66276b148faa5344609d96e8, enabledForClient=Document{{6513d6d0b6536f24b0e45de4=true}}, attributesForClient=Document{{6513d6d0b6536f24b0e45de4=Document{{partnerImportSource=Document{{lookupValue=iPORT, _class=value-lookup}}}}}}, externalId=partner_supplier1, pimReference=10180201, identifier=10180201, description=Document{{de_DE=partner_supplier1}}, _class=supplier}}"));
    }


}