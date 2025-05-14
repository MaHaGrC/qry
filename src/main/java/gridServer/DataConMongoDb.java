package gridServer;

import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.sql.*;
import java.util.*;


public class DataConMongoDb implements DataConnector{

    private static boolean driverForced = false;
    private static String protocolPrefix = "mongodb://";
    private static String mongoPrefix = "db";
    private static String mongoPrefix_ = "db.";
    private static String mongoPrefixRegExp_ = "db\\.";
    private static String  mongoPrefixSupply = "ipim-supply";
    private static String  mongoPrefixSupply_ = "ipim-supply.";

    DataConMongoDbJsonHelper helper = new DataConMongoDbJsonHelper();

    public DataConMongoDb() {
    }

    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.startsWith(protocolPrefix) || query.matches("(?i)(" + mongoPrefixRegExp_ + ").+") || checkForTableName(query,params);
    }


    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return url.getScheme().contains("mongodb");
    }


    public boolean checkConnection(Map<String, String> params){
        String ping = read(mongoPrefix_ +"foos.find({})", params).getData();
        if (null == ping) ping = "";
        System.out.println("DataConMongoDb.ping: " + (ping.isEmpty() ? "FAIL" : "valid"));
        return !ping.isEmpty();
    }




    public boolean checkForTableName(String query, Map<String, String> params){
        // assume 1 (non-keyword word) is table-name
        // TODO
        return query.startsWith(protocolPrefix) || query.startsWith(mongoPrefix_) || query.startsWith( mongoPrefixSupply_ ) || query.contains(".find()"); // tricky empty Result-Set contains  "empty-header + NewLine + empty-Row"
    }

    public String escapeData(String data){
        // ensure Delimiter and Line-Brakes (Row-Delimiter) are unique
        // pure \t -> Uncaught (in promise) SyntaxError: JSON.parse: bad character in string literal at line 1 column 651 of the JSON data
        data = data.replace("\t","\\t").replace(System.lineSeparator(),"\\n").replaceAll("(\\\\|;)","\\\\$1");
        return data;
    }


    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

        String query_ = query.replaceFirst("^"+ protocolPrefix,"");
        String id = params.get("id");
        if (null != id && id.matches("[0-9]+,[0-9]+") && params.containsKey("val")) {
            // UPDATE
            String[] i = id.split(",");
            update( query_, params.get("val"), Integer.valueOf(i[0]), Integer.valueOf(i[1]), params);
        }

        QryResponse data = read( query_, params );
        return data;
    }


    public QryResponse read(String query, Map<String, String> params) {
        String sql = "";
        QryResponse data = new QryResponse(params);
        UrlHelper url = main.getConnection( params, "mongodb");
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        url.setDefaultPort(27017); // necessary?

        // jdbc:mongodb://ipim-supply:secret@192.168.56.114:27017/ipim-supply
        // TODO https://www.mongodb.com/docs/atlas/data-federation/query/sql/drivers/jdbc/connect/
        // https://www.baeldung.com/java-mongodb
        String query_ = query.replaceFirst("^"+mongoPrefixRegExp_,"");
        query_ = query.replaceFirst("\\.find\\(\\)","");
        //

        MongoClient mongoClient = MongoClients.create(url.getUrl(true).replace("jdbc:",""));

        int rows = 0;
        if (query_.contains("tabUsage")) {

            // ipim-supply.tabUsage
            // db.tabUsage

            String prefix = query_.replaceFirst( ".tabUsage.*","");
            data.append( "database;collection;count" + System.lineSeparator());
            List<String> data_ = new ArrayList<>();
            for (String dbName : mongoClient.listDatabaseNames()) {
                if ( mongoPrefix.equals(prefix) || prefix.isEmpty() || dbName.contains(prefix)) {
                    data_.add( dbName + ";;" + System.lineSeparator());
                    MongoDatabase database = mongoClient.getDatabase(dbName);
                    for (String collectionName : database.listCollectionNames()) {
                        long l = database.getCollection(collectionName).countDocuments();
                        data_.add( dbName + ";" + (mongoPrefixSupply.equals(dbName) ? "" : mongoPrefix_) +  dbName + "." + collectionName + ";" + l /* + ";"+ System.lineSeparator()    ease sorting .... */ );
                        rows++;
                    }
                }
            }

            // TODO stupid but "GPT-3 is not available"
            // Convert the data to a list of strings
            Collections.sort(data_, new Comparator<String>() {
                @Override
                public int compare(String line1, String line2) {
                    String lastElement1 = line1.substring(line1.lastIndexOf(";") + 1);
                    String lastElement2 = line2.substring(line2.lastIndexOf(";") + 1);
                    int e1 = NumberUtils.toInt(lastElement1, -1);
                    int e2 = NumberUtils.toInt(lastElement2, -1);
                    return - (e1 - e2);
                }
            });
            for (String line : data_) {
                data.append(line + ";" + System.lineSeparator());
            }

        } else {
            /*
            mongoClient.listDatabaseNames().forEach(System.out::println);
            MongoDatabase database = mongoClient.getDatabase("ipim-supply");
            database.getCollection("supplier").find().forEach(System.out::println);
            */
            //
            // ipim-supply.supplier
            // db.ipim-supply.supplier.find()
            data.setExecutedStmt( query_ );
            //
            int limit = NumberUtils.toInt( query.replaceFirst(".* limit\\s*([0-9]*).*|.*","$1") , 25);
            String[] split = query_.replaceFirst("^" + mongoPrefixRegExp_,"").replaceFirst(".JSON[A-Z_]*","").split("\\.", 2);
            String dbName = split[0];
            String collection = split[1];
            MongoDatabase database = mongoClient.getDatabase(dbName);
            FindIterable<Document> supplier = database.getCollection(collection).find();
            //
            System.out.println("DataConMongoDb.read:  dbName: " + dbName + "  collection: " + collection + "   query: " +   query_);
            boolean showAsJSON = query.contains("JSON");
            if (showAsJSON) {
                // show as Json
                data.append( collection + ";" + System.lineSeparator());
                JsonMode jsonMode = query.contains("JSON_SHELL") ? JsonMode.SHELL : query.contains("JSON_EXTENDED") ? JsonMode.EXTENDED : JsonMode.RELAXED;
                JsonWriterSettings settings = JsonWriterSettings.builder().outputMode( jsonMode).build();
                for (Document doc : supplier) {
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()));
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()));
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.SHELL).build()));
                    String jsonStr = doc.toJson(settings);
                    if (!query.contains("JSON_") || query.contains("JSON_SIMPLIFY")) { // JSON_SIMPLIFY is default ...
                        jsonStr = helper.simplifyMongoDBJso2JsonObject(jsonStr).toString();
                    }
                    data.append( jsonStr + ";" + System.lineSeparator() );
                    rows++;
                    if (rows >= limit) {
                        break;
                    }
                }
            } else {
                // show as GRID .... (default)
                JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
                List<Map<String, String>> flatJsons = new ArrayList<>();
                for (Document doc : supplier) {
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()));
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()));
                    //System.out.println(doc.toJson(JsonWriterSettings.builder().outputMode(JsonMode.SHELL).build()));
                    String jsonStr = doc.toJson(settings);
                    JsonObject jsonObject = helper.simplifyMongoDBJso2JsonObject(jsonStr);
                    Map<String, String> stringStringMap = helper.flattenJson(jsonObject);
                    flatJsons.add(stringStringMap);
                    rows++;
                    if (rows >= limit) {
                        break;
                    }
                }
                data.setData( helper.mergeFlatJson(flatJsons) );
                System.out.println( data.getData() );
            }
        }
        main.notifyInfo( rows + " row(s) received" );
        data.setRowCount( rows );

        return data;
    }

    protected String simplify(String values) {
        String values_ = values.replaceAll("Document\\{\\{","").replaceAll("\\}\\}","").replaceAll(", ",";");
        return values_;
    }


    public String update(String qry, String val, int row, int col, Map<String, String> params) {
        String data = null;
        //String urlFull = "jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb";
        UrlHelper url = main.getConnection(params, "mongodb");
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        url.setDefaultPort(5432); // necessary?

        //
        // https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/web/util/UriComponentsBuilder.java
        //
        //String url = "jdbc:postgresql://{host}[:{port}]/[{database}]";
        //url = url.replace("{host}", "192.168.56.114").replace("{port}", "5432").replace("{database}", "hb").replaceAll("[\\[\\]]","");
        String sql = "";

        try (Connection conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() ) ) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();

                Statement stmt = conn.createStatement();

                /* get column-name ... for col */
                ResultSet rs = stmt.executeQuery( expand(qry, null));
                String columnId = rs.getMetaData().getColumnName( 1 );
                String columnName = rs.getMetaData().getColumnName( col+1 );

                sql = "update " + qry + " set " + columnName + " = '" + val + "' where " + columnId + " ='" + row + "'";
                main.notifyInfo(sql);
                stmt.executeUpdate( sql);
                conn.close();

                QrySyntax.saveQry( qry , params); // save only successfully queries
            }

        } catch (SQLException e) {
            System.out.println(sql);
            System.out.println(e.getMessage());
            main.notifyError( e.getMessage() );
        }
        return data;
    }



    String  expand(String query, Map<String, String> params) {
        if (!query.matches("(?is)select .*")) {
            query = QrySyntax.expand( query);
            query = "select * from " + query;
        }
        if (!query.toLowerCase(Locale.ROOT).contains(" limit ")) {
            query = query + " limit 25";
        }
        return query;
    }


}
