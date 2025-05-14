package gridServer;

import org.json.JSONArray;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataConSqlite implements DataConnector{

    String fileName = "query_list.db";

    // SESSIONS===select appr, updatedAt from query W stmt like '{"session":%' order by updatedAt desc  /*"valHndl": { "appr": "LINK" }*/
    // LOGINS==select appr, updatedAt from query where appr <> '' and stmt like 'LOGIN_' order by updatedAt desc

    /*

        CREATE TABLE `query` (`conn` TEXT,`stmt` TEXT,`cnt` INTEGER,`appr` TEXT,`createdAt` datetime,`updatedAt` datetime);
        CREATE TABLE `bug` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`conn` TEXT,`stmt` TEXT,`cnt` INTEGER,`ctx` TEXT,`status` TEXT,`dev` TEXT,`comment` TEXT,`createdAt` datetime,`updatedAt` datetime);
        CREATE TABLE `record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`before` TEXT,`action` TEXT,`after`   TEXT,`status` TEXT,`cnt` INTEGER,`dev` TEXT,`comment` TEXT,`createdAt` datetime,`updatedAt` datetime);

     */


    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches(".*(synonyme|t1|query).*|session|login|bug|record");
    }

    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

        String id = params.get("id");
        if (null != id && id.matches("[0-9]+,[0-9]+") && params.containsKey("val")) {
            // UPDATE
            String[] i = id.split(",");
            update( query, params.get("val"), Integer.valueOf(i[0]), Integer.valueOf(i[1]), params);
        }

        QryResponse data = read( query, params ).setEnvIndependent();
        return data;
    }

    @Override
    public boolean checkConnection(Map<String, String> params){
        String ping = read("select 1", null).getData();
        if (null == ping) ping = "";
        System.out.println("DataConSqlite.ping: " + (ping.isEmpty() ? "FAIL" : "valid"));
        return ping.isEmpty();
    }


    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return false;
    }


    public String update(String qry, String val, int row, int col, Map<String, String> params) {
        String data = null;
        String url = "jdbc:sqlite:./data/" + fileName;
        String sql = "";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();

                Statement stmt = conn.createStatement();

                /* get column-name ... for col */
                ResultSet rs = stmt.executeQuery( expand(qry, null));
                String columnId = rs.getMetaData().getColumnName( 1 );
                String columnName = rs.getMetaData().getColumnName( col+1 );

                if ( params.containsKey("qry")  && "SESSIONS".equals(params.get("qry").trim()) && qry.startsWith("select appr, updatedAt")) { // Assume Update Session !!!!!
                    /* KLUDGE TODO .....  */
                    JSONArray rowData = new JSONArray(params.get("rowData"));
                    qry = "query";
                    columnName = "appr"; // allow to use UpdatedAt as Input ....
                    columnId = "appr";
                    String rowId = rowData.getString(0);
                    val = val.replace("<br>","").replaceAll("[^a-zA-Z0-9_.-]","_");
                    if (val.isEmpty() || "DELETE".equals(val) ) {
                        sql = "delete from " + qry + " where " + columnId + " =\"" + rowId + "\"";
                    } else {
                        if (!val.startsWith("SES_")) val="SES_" + val; // ensure not to messup synonymes
                        sql = "update " + qry + " set " + columnName + " = \"" + val + "\" where " + columnId + " =\"" + rowId + "\"";
                    }
                    if  (".".equals(val)) {
                        sql = ""; // ignore
                    } else {
                        stmt.executeUpdate( sql);
                    }
                } else {
                    sql = "update " + qry + " set " + columnName + " = \"" + val + "\" where " + columnId + " =\"" + row + "\"";
                    stmt.executeUpdate( sql);
                }
                main.notifyInfo(sql);
                conn.close();
            }

        } catch (SQLException e) {
            System.out.println(sql);
            System.out.println(e.getMessage());
            main.notifyError(e.getMessage());
        }
        return data;
    }





    public String insert(String qry, Map<String,String> values, Map<String, String> params) {
        String data = null;
        String url = "jdbc:sqlite:./data/" + fileName;
        String sql = "";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {

                /* get column-name ... for col */
                String qry_expand = expand(qry, null);
                if ("bug".equals(qry_expand)) {
                    int rowcount = 0;
                    if (values.containsKey("id")) {
                        sql = "update " + qry_expand + " set conn=?, stmt=?, error=?, cnt=?, ctx=?,  status=?,  dev=?,  comment=?, updateAt=time('now') where id=? ";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, values.get("conn"));
                        stmt.setString(2, values.get("stmt"));
                        stmt.setString(3, values.get("error"));
                        stmt.setString(4, values.get("cnt"));
                        stmt.setString(5, values.get("ctx"));
                        stmt.setString(6, values.get("status"));
                        stmt.setString(7, values.get("dev"));
                        stmt.setString(8, values.get("comment"));
                        stmt.setString(9, values.get("id"));
                        rowcount = stmt.executeUpdate( );
                    }
                    if (0== rowcount){
                        sql = "insert into " + qry_expand + "(conn, stmt, error, cnt, ctx,  status,  dev,  comment, updateAt) value (?,?,?,?,?,?,?,?,time('now'))";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, values.get("conn"));
                        stmt.setString(2, values.get("stmt"));
                        stmt.setString(3, values.get("error"));
                        stmt.setString(4, values.get("cnt"));
                        stmt.setString(5, values.get("ctx"));
                        stmt.setString(6, values.get("status"));
                        stmt.setString(7, values.get("dev"));
                        stmt.setString(8, values.get("comment"));
                        rowcount = stmt.executeUpdate( );
                    }

                }

                main.notifyInfo(sql);
            }

        } catch (SQLException e) {
            System.out.println(sql);
            System.out.println(e.getMessage());
            main.notifyError(e.getMessage());
        }
        return data;
    }



    public String merge(String tab, Map<String,String> values /*, Set<String> condValues*/) {
        String data = null;
        String url = "jdbc:sqlite:./data/" + fileName;
        String sql = "";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                /* get column-name ... for col */
                //String qry_expand = expand(tab, null);
                int rowcount = 0;
                    // sql = "update " + qry_expand + " set conn=?, stmt=?, error=?, cnt=?, ctx=?,  status=?,  dev=?,  comment=?, updateAt=time('now') where id=? ";
                List<String> stmtCols = new ArrayList<>();
                List<String> stmtCondCols = new ArrayList<>();
                boolean cnt = false;
                for (String col : values.keySet()) {
                    if ("cnt++".equals(col)) {
                        cnt = true;
                    } else {
                        List<String> cols = col.equals("id") ? stmtCondCols : stmtCols;
                        cols.add(col);
                    }
                }
                //
                if (values.containsKey("id")) {
                    // sql = "update " + qry_expand + " set conn=?, stmt=?, error=?, cnt=?, ctx=?,  status=?,  dev=?,  comment=?, updateAt=time('now') where id=? ";
                    sql = "update " + tab + " updateAt=time('now'), " + (cnt ? "cnt=cnt+1, " : "") + String.join("=?, ", stmtCols) + "=? where " + String.join("=? AND ", stmtCondCols) + "=?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    int i = 1;
                    for (String col : stmtCols) {
                        stmt.setString(i++, values.get(col));
                    }
                    for (String col : stmtCondCols) {
                        stmt.setString(i++, values.get(col));
                    }
                    rowcount = stmt.executeUpdate( );
                }

                if (0 == rowcount) {
                    // sql = "insert into " + qry_expand + "(conn, stmt, error, cnt, ctx,  status,  dev,  comment, updateAt) values (?,?,?,?,?,?,?,?,time('now'))";
                    sql = "insert into " + tab + "( "+ ( cnt ? "cnt, " : "") + String.join(", ", stmtCols) + ", updatedAt, createdAt) values (" + ( cnt ? "0, " : "") + String.join(",", stmtCols).replaceAll("[^,]+", "?") + ",time('now'),time('now'))";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    int i = 1;
                    for (String col : stmtCols) {
                        stmt.setString(i++, values.get(col));
                    }
                    rowcount = stmt.executeUpdate();
                }

                main.notifyInfo(sql + " (" + sql.replaceAll("\\s.*","") + " " + rowcount + " rows)");
            }

        } catch (SQLException e) {
            System.out.println(sql);
            System.out.println(e.getMessage());
            main.notifyError(e.getMessage());
        }
        return data;
    }



    public QryResponse read(String query, Map<String, String> params) {
        QryResponse data = new QryResponse();
        String url = "jdbc:sqlite:./data/" + fileName;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String sql="";
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                //System.out.println("The driver name is " + meta.getDriverName());
                //System.out.println("A new database has been created.");

                Statement stmt = conn.createStatement();;
                sql = expand( query, params);
                data.setExecutedStmt( sql );
                //stmt.executeUpdate(sql)
                ResultSet rs = stmt.executeQuery(sql);
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount ; i++) {
                    data.append(rs.getMetaData().getColumnName(i)+ ";");
                }
                data.append(System.lineSeparator());
                int rows = 0;
                while(rs.next()) {
                    for (int i = 1; i <= colCount ; i++) {
                        data.append(rs.getString(i)+ ";");
                    }
                    data.append(System.lineSeparator());
                    rows += 1;
                }
                main.notifyInfo( rows + " row(s) received" );
                conn.close();
            }

        } catch (SQLException e) {
            data.setErrorMsg( e.getMessage());
            main.notifyError(e.getMessage() + "(" + sql + ")");
        }
        return data;
    }

    String  expand(String query, Map<String, String> params) {
        return query.toLowerCase().startsWith("select ") ? query : "select * from " + query;
    }


}
