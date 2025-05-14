package gridServer;

import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataConPostgres implements DataConnector{

    // jdbc:postgresql://{host}[:{port}]/[{database}]

    private static boolean driverForced = false;

    Notifier notifier = new Notifier(this.getClass().getName());

    Map<String, Boolean> check = new HashMap<>();

    public DataConPostgres() {
        if (!driverForced) {
            try {
                /*
                    needed for executable jar - fixes  java.sql.SQLException: No suitable driver found for jdbc:postgresql:
                 */
                Class.forName("org.postgresql.Driver").newInstance();
                driverForced = true;
            } catch (InstantiationException e2) {
                throw new RuntimeException(e2);
            } catch (IllegalAccessException e2) {
                throw new RuntimeException(e2);
            } catch (ClassNotFoundException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches("(?i)(s |select ).+") || query.equals("dbTab") || query.startsWith("dbTab ") || checkForTableName(query,params);
    }


    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return url.getScheme().contains("postgresql");
    }


    public boolean checkConnection(Map<String, String> params){
        String ping = read("select 1", params).getData();
        if (null == ping) ping = "";
        System.out.println("DataConPostgres.ping: " + (ping.isEmpty() ? "FAIL" : "valid"));
        return !ping.isEmpty();
    }

    void checkConnection(UrlHelper url) throws SQLException {
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        Connection conn = null;
        conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() );

    }


    Connection getConnection(UrlHelper url){
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        Connection conn = null;
        try  {
            conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() );
        } catch (SQLException e) {
            main.notifyError(e.getMessage());
            System.out.println(e.getMessage());
        }
        return conn;
    }



    public boolean checkForTableName(String query, Map<String, String> params){
        // assume 1 (non-keyword word) is table-name
        String firstWordResult = "";
        String firstWord = query.replaceFirst("(?s)^.*?\\sfrom\\s+",""); // skipp SQL-select
        firstWord = firstWord.replaceFirst("(?s)[^a-zA-Z0-9_\"].*$",""); // allow \" for table-names with spaces or camelCase

        // if database changes - clear check-Map
        //  as table-names are different for different databases
        UrlHelper url = main.getConnection( params);
        String connectionIdentifier = url.getStringWoUser();
        String key = connectionIdentifier + ":::" + firstWord;

        if  (check.containsKey( key)) {
            firstWordResult = check.get(key) ? firstWord : "";
        } else {
            if (" bomerge tab2json json2tab bo2json  bo2jsons ".contains(" "+firstWord+" ") ) {
                // TODO check for plsql-functions like boMerge
                firstWordResult="Function";
            } else if (query.startsWith(firstWord+"(")) {
                // function ... have to call it the long way...
                firstWord = query.replaceFirst(" .*","");
                firstWordResult = read("select from " + firstWord + " limit 1", params, false).getData();
                if (null == firstWordResult) firstWordResult = "";
                main.notifyInfo("DataConPostgres.checkForTableName: firstWord '" + firstWord + "' (function) " + (firstWordResult.isEmpty() ? "UNKNOWN" : "known"));
            } else if (!firstWord.isEmpty()) {
                firstWordResult = read("select from " + firstWord + " limit 1", params, false).getData();
                if (null == firstWordResult) firstWordResult = "";
                if (firstWordResult.isEmpty()) {
                    firstWordResult = read("select from \"" + firstWord + "\" limit 1", params, false).getData();
                    if (null == firstWordResult) firstWordResult = "";
                    if (firstWordResult.isEmpty()) {
                        main.notifyInfo("DataConPostgres.checkForTableName: firstWord '" + firstWord + "' " + (firstWordResult.isEmpty() ? "UNKNOWN" : "known"));
                    } else {
                        main.notifyWarn("DataConPostgres.checkForTableName: firstWord \"" + firstWord + "\" must be quoted with \" !! ");
                    }
                } else {
                    main.notifyInfo("DataConPostgres.checkForTableName: firstWord '" + firstWord + "' " + (firstWordResult.isEmpty() ? "UNKNOWN" : "known"));
                }
            }
            check.put(key, !firstWordResult.isEmpty());
        }
        return !firstWordResult.isEmpty(); // tricky empty Result-Set contains  "empty-header + NewLine + empty-Row"
    }

    public String escapeData(String data){
        // ensure Delimiter and Line-Brakes (Row-Delimiter) are unique
        // pure \t -> Uncaught (in promise) SyntaxError: JSON.parse: bad character in string literal at line 1 column 651 of the JSON data
        data = data.replace("\t","\\t").replaceAll("\\r?\\n","\\n").replaceAll("(;)","\\$1");
        if (data.contains("\"")||data.contains("\\")) {
            // escape \n to \\n - allow multiline to be saved in one row - eases the csv-reading in grid.js
            data = "\""+data.replace("\\","\\\\").replace("\"","\\\"")+"\"";
        }
        return data;
    }


    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

        query = QrySyntax.translateToSql(params, qryResponse, query);

        String id = params.get("id");
        if (null != id && id.matches("[0-9]+,[0-9]+") && params.containsKey("val")) {
            // UPDATE
            String[] i = id.split(",");
            update( query, params.get("val"), Integer.valueOf(i[0]), Integer.valueOf(i[1]), params);
        }

        QryResponse data = read( query, params );
        return data;
    }



    public String update(String qry, String val, int row, int col, Map<String, String> params) {
        String data = null;
        //String urlFull = "jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb";
        UrlHelper url = main.getConnection(params);
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


    public QryResponse read(String query, Map<String, String> params) {
        if (query.equals("dbTab") || query.startsWith("dbTab ") /*ignore changed-limit-clause / UPDATE ...*/) {
            // exclude views as they are usually expensive
            query = "select  /**DYNAMIC_SQL limit -1 **/ 'select tab, creationdate, lastmodified, cnt, ins_, upd_ from ( ' stmt " +
                    "union all " +
                    "SELECT 'select null tab, null::timestamptz creationdate , null::timestamptz lastmodified, 0 cnt, 0 ins_, 0 upd_ where 1=0' " +
                    "union all " +
                    "SELECT concat(' union all select ''',t.table_name,''' tab, ', coalesce(col,'null::timestamptz creationdate , null::timestamptz lastmodified, count(*) cnt, null::bigint ins_, null::bigint upd_')  , ' from \"',t.table_name,'\"') qry  \n" +
                    "FROM information_schema.tables t\n" +
                    "  left join (\n" +
                    "   SELECT  table_name, 'min(creationdate), max(lastmodified), count(*) cnt, count(case when creationdate > current_date - 1  then 1 end ) ins_, count(case when lastmodified > current_date - 1  then 1 end ) upd_' col  \n" +
                    "     FROM information_schema.columns\n" +
                    "     WHERE table_schema = 'public' and column_name = 'creationdate'\n" +
                    "     group by table_name\n" +
                    "  ) t2 on t2.table_name=t.table_name\n" +
                    "WHERE table_schema = 'public' and table_type='BASE TABLE' " +
                    "union all " +
                    "SELECT concat(' union all select ''',table_name,''' tab, null::timestamptz creationdate , null::timestamptz lastmodified, count(*) cnt, null::bigint ins_, null::bigint upd_ ') qry " +
                    " FROM information_schema.tables " +
                    " WHERE table_schema = 'public' and table_type='VIEW' " +
                    "union all select ') x order by (to_char(lastmodified, ''YYYY-MM-DD HH24:MI:SS''::text)) DESC NULLS LAST, 1 limit 9999 /*{\"valHndl\":{\"tab\":\"LINK\"}}*/' ";
        }
        QryResponse qryResponse = read(query, params, false);
        if (query.contains("DYNAMIC_SQL")) {
            query = qryResponse.getData();
            if (null != query) {
                query = query.replaceFirst("^.*?\r?\n","").replaceAll(";(\r?\n)","$1"); // KLUDGE - remove header + initial union
                query = query.replaceAll("(?m)^\"|\"$","").replaceAll("\\\\\"","\""); // KLUDGE - unescape
            }
            qryResponse = read(query, params, false);
        }
        return qryResponse;

    }

    public QryResponse read(String query, Map<String, String> params, boolean saveSyn) {
        String sql = "";
        QryResponse data = new QryResponse(params);
        UrlHelper url = main.getConnection( params);
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        url.setDefaultPort(5432); // necessary?


        try (Connection conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() ) ) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                //System.out.println("The driver name is " + meta.getDriverName());
                //System.out.println("A new database has been created.");

                Statement stmt = conn.createStatement();;
                sql = expand( query, params);
                main.notifyInfo("DataConPostgres: " + sql);
                data.setExecutedStmt(sql);
                //stmt.executeUpdate(sql)
                ResultSet rs = null;
                try{
                    main.notifyInfo("DataConPostgres: .... execute ");
                    try {
                        rs = stmt.executeQuery(sql);
                    } catch (SQLException e) {
                        String sql_heal = selfHeal(sql,e);
                        if (null != sql_heal) {
                            main.notifyWarn("DataConPostgres: ... try selfHeal ");
                            rs = stmt.executeQuery(sql_heal);
                        } else {
                            throw e;
                        }
                    }
                    main.notifyInfo("DataConPostgres: ... DONE  with OK");
                } finally {
                    main.notifyInfo("DataConPostgres: ... DONE finally");
                }
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount ; i++) {
                    data.append( rs.getMetaData().getColumnName(i)+ ";" );
                }
                data.append(System.lineSeparator());
                int rows = 0;
                while(rs.next()) {
                    for (int i = 1; i <= colCount ; i++) {
                        data.append ((null == rs.getString(i) ? "" : escapeData(rs.getString(i)) ) + ";");
                    }
                    data.append(System.lineSeparator());
                    rows += 1;
                }
                main.notifyInfo( rows + " row(s) received" );
                conn.close();
                if (null != params && saveSyn) { // skipp internal check ...
                    QrySyntax.saveQry( sql, params.get("qry"), params ); // save only successfully queries
                }

            }

        } catch (Exception e) {
            data.setErrorMsg( e.getMessage());
            notifier.error(e.getMessage());
            System.out.println("ERROR caught: ");
            notifier.error(e);

        }
        return data;
    }

    private String selfHeal(String sql, SQLException e) {
        String sql_heal = null;
        if (e.getMessage().contains(" ERROR: syntax error at or near \"]\"")){
            sql_heal = sql.replaceFirst("(?s)\\s*\\]\\s*","] where "); // inject where
        }
        return sql_heal;
    }

    String  expand(String query, Map<String, String> params) {
        if (!query.matches("(?is)select .*")) {
            query = QrySyntax.expand( query);
            query = "select * from " + query;
        }
        if (query.toLowerCase(Locale.ROOT).contains("\"limit\":")) { // extract hint ...
            query = query.replaceFirst(" limit +[0-9,]*","") + query.replaceAll(".*\\b(limit)\"?:\\s*\"?([0-9,]+)\"?.*|.*"," $1 $2"); // select * from attribute  /*{ limit: "1,100" }*/     ==>     select * from attribute  /*{ limit: "1,100" }*/ limit 1,100
        } else  if ( query.toLowerCase(Locale.ROOT).contains(" v_article") && query.toLowerCase(Locale.ROOT).contains(" where ")) {
            main.notifyInfo("DataConPostgres.expand: skipp limit for v_article to prevent postgres slow down"); // limit will drastically slow down the query
            // see    v_article W product = '3984p'
            //  vs    select * from v_article where product = '3984p' limit 25
        } else  if (!query.toLowerCase(Locale.ROOT).contains(" limit ")) {
            query = query + " limit 25"; /* match connect.js::handleProp*/
        }
        return query;
    }


}
