package gridServer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gridServer.QryParser2.Block;
import gridServer.QryParser2.QryParser;
import gridServer.QryParser2.QryToSqlTranslator;

import java.net.URLDecoder;
import java.sql.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class QrySyntax {

    static String url = "jdbc:sqlite:./data/query_list.db";
    static Connection conn = null;
    static DataConSqlite queryList = null;

    static Notifier notifier = new Notifier( QrySyntax.class.getName());
    private static boolean allowPartialTranslation = true;

    static String expand(String qry){
        //saveQry( qry);
        // article[]article_av --> article[id][article]article_av

        return qry;
    }

    static void saveQry(String qry, Map<String, String> params){
        saveQry(qry, null, params);
    }

    static void saveQry(String qry, String appr, Map<String, String> params){
        try {
            if (null == conn){
                    conn = DriverManager.getConnection(url);
            }
            //
            if (qry.startsWith("select from ") && qry.endsWith(" limit 1")) {
                throw new RuntimeException("illegal Synonym to save (" + qry + ")");
            }

            //Statement stmt = conn.createStatement();
            // String sql = "update query set cnt = cnt + 1, updatedAt=date('now') where conn=\"CONNECTION\" and stmt=\"QRY\"".replace("CONNECTION", main.getConnection().getConnectionId()).replace("QRY", qry);
            String sql = "update query set cnt = cnt + 1, updatedAt=time('now') where  Coalesce(conn,'-')=Coalesce(?,'-') and stmt=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, main.getLogin(params));
            stmt.setString(2, qry);
            int rowcount = stmt.executeUpdate();
            if (0 == rowcount) {
                if (null != appr) {
                    //sql = "insert into query(conn,stmt,appr,cnt,createdAt,updatedAt) values(\"CONNECTION\",\"QRY\",\"APPR\",1,date('now'),date('now'))".replace("CONNECTION", main.getConnection().getConnectionId()).replace("APPR",appr).replace("QRY", qry);
                    sql = "insert into query(conn,stmt,cnt,appr,createdAt,updatedAt) values(?, ?,0, ?,time('now'),time('now'))";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, main.getLogin(params));
                    stmt.setString(2, qry);
                    stmt.setString(3, appr);
                    stmt.execute( );
                    main.notifyWarn("QrySyntax: " + stmt + " >>" + appr + "===" +  qry + "<<");
                } else {
                    //sql = "insert into query(conn,stmt,cnt,createdAt,updatedAt) values(\"CONNECTION\",\"QRY\",1,date('now'),date('now'))".replace("CONNECTION", main.getConnection().getConnectionId()).replace("QRY", qry);
                    sql = "insert into query(conn,stmt,cnt,createdAt,updatedAt) values(?, ?,0, time('now'),time('now'))";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, main.getLogin(params));
                    stmt.setString(2, qry);
                    stmt.execute( );
                    main.notifyWarn("QrySyntax: " + stmt + " >>>" +  qry + "<<<");
                }
                //stmt.execute( sql);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }


    static String saveSyn(String qry, Map<String, String> params){
        if (qry.contains("===")){
            String[] syn_parts = qry.split("===");
            try {
                if (null == conn){
                    conn = DriverManager.getConnection(url);
                }
                String sql = "update query set stmt=?, updatedAt=time('now'), cnt = cnt + 1 where  Coalesce(conn,'-')=Coalesce(?,'-') and appr=?"; // ensure uniq appr
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, syn_parts[1]);
                stmt.setString(2, main.getLogin(params));
                stmt.setString(3, syn_parts[0]);
                int rowcount = stmt.executeUpdate( );
                if (0 == rowcount) {
                    sql = "insert into query(conn,stmt,cnt,appr,createdAt,updatedAt) values(?, ?,0, ?,time('now'),time('now'))";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, main.getLogin(params));
                    stmt.setString(2, syn_parts[1]);
                    stmt.setString(3, syn_parts[0]);
                    stmt.execute( );
                    if (!"LOGIN_".contains(syn_parts[1])) {
                        main.notifyInfo("Synonym " + syn_parts[0]+ " inserted");
                    }
                } else {
                    if (!"LOGIN_".contains(syn_parts[1])) {
                        main.notifyInfo("Synonym " + syn_parts[0] + " updated");
                    }
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                main.notifyError( throwables.getMessage());
            }
            qry = syn_parts[1];
        }
        return qry;
    }

    public static String applySyn(String qryWHint, Map<String, String> params){

        String splitQryHint = "(?s)(?=\\s+\\/\\*\\{\\s*.*\\s*\\}\\*\\/\\s*)";
        String[] qry_parts=  qryWHint.contains("/*{") ? qryWHint.split(splitQryHint) : null;  ; // including HINTS... Hint should be kept!
        String qry = null == qry_parts ? qryWHint : qry_parts[0];
        String qry_hint = null == qry_parts || qry_parts.length < 2 ? null : qry_parts[1];

        if (true){
            //String splitQryHint = "(?s)\\s+\\/\\*\\{\\s*|\\s*\\}\\*\\/\\s*";
            try {
                if (null == conn){
                    conn = DriverManager.getConnection(url);
                }
                Statement connStmt = conn.createStatement();
                String sql = "select appr, stmt from query where appr is not null and coalesce(stmt,'-') not in ('LOGIN_', 'select 1 limit 25') order by length(appr) desc, cnt desc, appr desc";
                ResultSet rs = connStmt.executeQuery(sql);
                while(rs.next()) {
                    String apprWHint = rs.getString(1);
                    String stmtWHint = rs.getString(2);
                    if (apprWHint.isEmpty()) {
                    } else if (qry.startsWith("{\"session\":")) {
                    } else if (apprWHint.equals(stmtWHint)) {
                    } else {
                        String[] appr_parts=  apprWHint.contains("/*{") ? apprWHint.split(splitQryHint) : null;  ; // including HINTS... Hint should be kept!
                        String appr = null == appr_parts ? apprWHint : appr_parts[0];
                        String appr_hint = null == appr_parts || appr_parts.length < 2 ? null : appr_parts[1];
                        String[] stmt_parts=  stmtWHint.contains("/*{") ? stmtWHint.split(splitQryHint) : null;  ; // including HINTS... Hint should be kept!
                        String stmt = null == stmt_parts ? stmtWHint : stmt_parts[0];
                        String stmt_hint = null == stmt_parts || stmt_parts.length < 2 ? null : stmt_parts[1];
                        if (stmt.contains(appr)) { // no self-recursion  like       attribute -> select * from attribute limit 1
                        } else if (qry.contains(appr)) {
                            String qry0 = qry;
                            // TODO
                            //     - uppercase Word as Syn -> replace match ALL or separated word (surrounded by space)
                            //     - or replacement is requested by "..."-Suffix
                            //     - else .. must match whole stmt (prevent "select * from article limit 7[]select * from article_av join attribute on article_av.attribute = attribute.id limit 7")
                            if (appr.equals(appr.toUpperCase(Locale.ROOT))) {
                                qry = qry.replaceAll("\\b" + Pattern.quote(appr) + "\\b", stmt);
                            } else if (appr.endsWith("...")) {
                                qry = qry.replaceAll("\\b" + Pattern.quote(appr), stmt);
                            } else {
                                qry = qry.replaceAll("^" + Pattern.quote(appr) + "$", stmt);
                            }
                            if (!qry0.equals(qry)) {
                                qry_hint = mergeHints( qry_hint, stmt_hint);
                                //
                                //  /*{"cdm": "none"}*/                              >>> aus qry           << hier soll HINT in HTML nicht sichtbar sein ...
                                //  /*{"valHndl":{"tab":"LINK"}, "cdm": "none" }*/   >>> aus synonym ...   << hier soll valHndl versteckt sein in HTML-Query
                                //
                                // force most relevant properties to executed statement
                                qry_hint = qry_hint.trim(); // as we change qry anyway - we might fix spaces here ...
                                System.out.println("QrySyntax.applySyn \"" + appr + "\" -> " + qry);
                            } else {
                                System.out.println("QrySyntax.applySyn SKIPP \"" + appr + "\" not applied ");
                            }
                        } else {
                            //System.out.println("QrySyntax.applySyn SKIPP \""+appr+"\" not matched ");
                        }
                    } // appr - to check


                } //
                rs.close();
                connStmt.close();


            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return null == qry_hint || qry_hint.isEmpty() ? qry : qry + " " + qry_hint;
    }


    static String mergeHints(String hint, String hint2) {
        JsonObject hintJson = null;
        JsonObject hintJson2 = null;
        if (null != hint) {
            hint = hint.replaceAll("(?s)^\\s*/\\*(.*)\\*/\\s*$","$1");
            JsonElement hintElem = (new JsonParser()).parse(hint);
            hintJson = null == hintElem ||  !hintElem.isJsonObject() ? null : hintElem.getAsJsonObject();
        }
        if (null != hint2) {
            hint2 = hint2.replaceAll("(?s)^\\s*/\\*(.*)\\*/\\s*$","$1");
            JsonElement hintElem2 = (new JsonParser()).parse(hint2);
            hintJson2 = null == hintElem2 ||  !hintElem2.isJsonObject() ? null : hintElem2.getAsJsonObject();
        }
        // overwrite values in 2 by values from 1
        if (null != hintJson && null != hintJson2) {
            for (Map.Entry<String, JsonElement> entry : hintJson.entrySet()) {
                hintJson2.add( entry.getKey(), entry.getValue());
            }
        }
        String hintMerged = null != hintJson2 ? hintJson2.toString() : null != hintJson ? hintJson.toString() : null;

        return null == hintMerged || hintMerged.isEmpty() ? "" : "/*" + hintMerged + "*/";
    }

    static String handleSynonymes(Map<String, String> params, QryResponse qryResponse) {
        String query = params.get("qry");
        /*
          progress_tracker W name not like 'IPIM_WEB%' order by id desc
            --> java.lang.IllegalArgumentException: URLDecoder: Illegal hex characters in escape (%) pattern - Error at index 0 in: "' "
            --> HINT: progress_tracker W not name ~ 'IPIM_WEB_' order by id desc
         */
        try {
            query = URLDecoder.decode(query);
        } catch (IllegalArgumentException e) {
            notifier.warning("QrySyntax.handleSynonymes --> SKIPP URLDecoding: " + e.getMessage());
        }
        query = saveSyn(query, params);
        System.out.println("QrySyntax.extractSyn --> \"" + query + "\"");
        query = applySyn(query, params);
        System.out.println("QrySyntax.applySyn --> \"" + query + "\"");
        return query;
    }

    static Block parseToQryStmt(Map<String, String> params, QryResponse qryResponse, String query) {
        QryParser parser = new QryParser();
        Block block = parser.parse(query.trim());
        if (null != block) {
            List<Block> trgTabBlocks = block.getBlocks("trgTab");
            if (null != trgTabBlocks && !trgTabBlocks.isEmpty()) {
                qryResponse.setTargetTable(trgTabBlocks.get(0).getValue()); // set target table
            }
        }
        System.out.println("QrySyntax.parse --> \"" + (null == block ? "" : block.toString()) + "\"");
        qryResponse.setQryStmtBlock(block);
        return block;
    }


    static String translateToSql(Map<String, String> params, QryResponse qryResponse, String query) {
        Block block = qryResponse.getQryStmtBlock();
        String query_translated = translate(block, params);
        if (null == query_translated) {
            query_translated = query;
            System.out.println("QrySyntax.translate --> SKIPP (not translated) \"" + query + "\"");
        } else if (!block.tokensFree.isEmpty()) {
            if (allowPartialTranslation) {
                System.out.println("QrySyntax.translate --> WARN (only partial translated) \"" + query_translated + "\"");
            } else {
                query_translated = query;
                System.out.println("QrySyntax.translate --> SKIPP (only partial translated) \"" + query + "\"");
            }
        } else {
            System.out.println("QrySyntax.translate --> \"" + query_translated + "\"");
        }
        return query_translated;
    }


    /* for testing */
    static String translate(String stmt, Map<String, String> params){
        QryParser parser = new QryParser();
        Block block = parser.parse(stmt);
        return translate(block, params);
    }

    static String translate(Block block, Map<String, String> params){
        String str = null;
        if ( null != block){
            // substitute join ...
            //String pattern = "([a-zA-Z0-9_]+)" + "\\[([^\\])\\]" + "(\\[([^\\])\\])?" + "([a-zA-Z0-9_]+)";
            QryToSqlTranslator qryToSqlTranslator = new QryToSqlTranslator();
            str = qryToSqlTranslator.translate(block, true);


        /*
        List<QryLexer.Token> tokenize = (new QryLexer()).tokenize(qry);
        QryParser.Block block = (new QryParser()).parse( tokenize ); // consume tokens to Blocks ..
        System.out.println(block.toString());
        String str = block.toJoin("");
        // add non parsed content ...
        for (QryLexer.Token token : tokenize) {
            str += token.value;
        }
        // TODO self healing - should work with tokens here ...
        if (str.matches("(?i).* group by .* (W|where) .*")){
            // jsut try educated guess
            str=str.replaceFirst("(?i)( group by .*?)( (W|where) .*?)( (limit|having|//\\*).*$|$)","$2$1$4");
        }
        */

            //
            //

        }

        return str;
    }


    static String extractSuggestion(String suggestions){
        try {
            if (null == conn){
                conn = DriverManager.getConnection(url);
            }
            Statement connStmt = conn.createStatement();
            // TODO improve ranking - häufige die aktuell verwendet werden zuerst ...
            String sql = "select appr, stmt from query where coalesce(stmt,'-') not in ('LOGIN_', 'select 1 limit 25')  order by cnt desc, appr desc limit 20";
            ResultSet rs = connStmt.executeQuery(sql);
            while(rs.next()) {
                String appr = rs.getString(1);
                String stmt = rs.getString(2);
                appr = null != appr && !appr.isEmpty() ? appr : stmt;
                if (!suggestions.contains(" " + appr + " ")){
                    suggestions += " // " +  (null != appr && !appr.isEmpty() ? appr : stmt);
                }
            }
            rs.close();
            connStmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return suggestions;
    }


}
