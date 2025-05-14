package gridServer;


import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rapidoid.http.Req;
import org.rapidoid.http.ReqRespHandler;
import org.rapidoid.http.Resp;
import org.rapidoid.io.Upload;
import org.rapidoid.setup.On;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Thread.sleep;

public class main {

    /*


        https://www.w3schools.com/howto/howto_js_toggle_like.asp   - toggle by keeping default ...
        https://www.w3schools.com/howto/howto_js_toggle_hide_show.asp

        https://www.w3schools.com/howto/howto_js_treeview.asp

        msg             https://www.w3schools.com/howto/howto_js_callout.asp
                        https://www.w3schools.com/howto/howto_js_snackbar.asp


        https://www.w3schools.com/howto/howto_css_zoom_hover.asp
        https://www.w3schools.com/howto/howto_css_flip_box.asp

        center        https://www.w3schools.com/howto/howto_css_center-vertical.asp


        buttons         https://www.w3schools.com/howto/howto_css_pill_button.asp
                        https://www.w3schools.com/howto/howto_css_notification_button.asp
                        https://www.w3schools.com/howto/howto_css_icon_buttons.asp

        window - look
        https://www.w3schools.com/howto/howto_css_browser_window.asp
        https://www.w3schools.com/howto/howto_css_chat.asp

        icon-bar ...
        https://www.w3schools.com/howto/howto_css_icon_bar.asp
        https://www.w3schools.com/howto/howto_css_navbar_icon.asp

        tabs        https://www.w3schools.com/howto/howto_js_tabs.asp

        drowpdowns...   https://www.w3schools.com/howto/howto_css_dropdown.asp
                        https://www.w3schools.com/howto/tryit.asp?filename=tryhow_js_cascading_dropdown
                        https://www.w3schools.com/howto/howto_css_button_split.asp

        drag        https://www.w3schools.com/howto/howto_js_draggable.asp

        slides...   https://www.w3schools.com/howto/howto_js_quotes_slideshow.asp
                    https://www.w3schools.com/howto/howto_js_quotes_slideshow.asp

        syntax highlight        https://www.w3schools.com/howto/howto_syntax_highlight.asp

        charts      https://www.w3schools.com/howto/howto_google_charts.asp

        symbols ... https://www.w3schools.com/howto/howto_css_arrows.asp
                    https://www.w3schools.com/howto/howto_css_shapes.asp
                    https://www.w3schools.com/howto/howto_css_loading_buttons.asp
                    https://www.w3schools.com/howto/howto_css_download_button.asp
                    https://www.w3schools.com/howto/howto_css_custom_checkbox.asp

        grid        https://www.w3schools.com/howto/howto_css_pagination.asp
                    https://www.w3schools.com/howto/howto_css_next_prev.asp
                    https://www.w3schools.com/howto/howto_css_breadcrumbs.asp

        https://www.w3schools.com/howto/howto_html_favicon.asp

     */

    static Map<String ,String> connectionUrls = new HashMap<>();
    static String connectionCurrent = "";
    static List<DataConnector> dataConnectorList;

    static Queue<String> notifications = new ConcurrentLinkedQueue<>();
    static private int waitForNotification = 0;
    static public boolean DEMO = true;

    public static String getLogin(Map<String, String> params) {
        String login_cur = connectionCurrent; // allow multi-threading
        if (params.containsKey("login")){
            login_cur = params.get("login");
            if (connectionUrls.containsKey(login_cur)) {
                connectionCurrent = login_cur;
            }
        }
        return login_cur;
    }


    public static UrlHelper getConnection(Map<String, String> params) {
        // allow to add further connection filter ...
        return new UrlHelper( connectionUrls.get(getLogin(params))) ;
    }

    public static UrlHelper getConnection(Map<String, String> params, String protocol) {
        // allow to add further connection filter ...
        return new UrlHelper( connectionUrls.get(getLogin(params) + ":" + protocol)) ;
    }

    public static void notify(String msg) {
        main.notifications.add(msg);
    }

    public static void notifyError(String msg) {
        System.out.println("ERROR " + msg);
        //main.notifications.add("{ \"error\": { \"msg\": \"" + msg.replace("\"","\\\"").replaceAll("[\n\r]+","\\\\n") +"\"}}");
        JSONObject jsn = (new JSONObject()).put("error", (new JSONObject()).put("msg", msg));
        String jsn_msg = jsn.toString();
        main.notifications.add( jsn_msg);
    }
    public static void notifyWarn(String msg) {
        System.out.println("WARN  " + msg);
        //main.notifications.add("{ \"error\": { \"msg\": \"" + msg.replace("\"","\\\"").replaceAll("[\n\r]+","\\\\n") +"\"}}");
        JSONObject jsn = (new JSONObject()).put("warn", (new JSONObject()).put("msg", msg));
        String jsn_msg = jsn.toString();
        main.notifications.add( jsn_msg);
    }

    public static void notifyInfo(String msg) {
        System.out.println("INFO  " + msg);
        //main.notifications.add("{ \"info\": { \"msg\": \"" + msg.replace("\"","\\\"").replaceAll("[\n\r]+","\\\\n") +"\"}}"); <<< all in one will create sometimes issues for rapidoid
        JSONObject jsn = (new JSONObject()).put("info", (new JSONObject()).put("msg", msg));
        String jsn_msg = jsn.toString();
        main.notifications.add( jsn_msg);
    }


    public static void createNewDatabase(String fileName) {

        String url = "jdbc:sqlite:./data/" + fileName;
        try {
            Files.createDirectories(Paths.get("./data"));
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");

                String[] initStmts = { ""/*must be part of code to ensure it is part of jar ...*/
                        , "CREATE TABLE `query` (`conn` TEXT,`stmt` TEXT,`cnt` INTEGER,`appr` TEXT,`createdAt` datetime,`updatedAt` datetime);"
                        , "CREATE TABLE `bug` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`conn` TEXT,`stmt` TEXT,`error` TEXT,`cnt` INTEGER,`ctx` TEXT,`status` TEXT,`dev` TEXT,`comment` TEXT,`createdAt` datetime,`updatedAt` datetime);"
                        , "CREATE TABLE `record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT,`before` TEXT,`action` TEXT,`after`   TEXT,`status` TEXT,`cnt` INTEGER,`dev` TEXT,`comment` TEXT,`createdAt` datetime,`updatedAt` datetime);"
                        , ""
                        , "" /* as well as Synonymes */
                        , ""
                        , "LOGINS===select appr, updatedAt from query where appr <> '' and stmt like 'LOGIN_' order by updatedAt desc"
                        , "SESSIONS===select appr, updatedAt, '.' rename from query W stmt like '{\"session\":%' order by updatedAt desc  /*\"valHndl\": { \"appr\": \"LINK\" }*/"
                        , "W===where"
                        , ""
                        , "v_dbTabusageLinked===select * from v_dbTabusage limit 300 /*{\"valHndl\":{\"tab\":\"LINK\"}, \"cdm\": \"none\" }*/"
                        , ""
                        , "" /*  and references .. */
                        , ""
                        , "[productNo,articleNo,identifier,article_av.*]article[]article_av[<]attribute W attribute.identifier='katalog_artikel'"
                        , "[productNo,articleNo,identifier,article_av.*]article[]article_av[<]attribute"
                        , "article[]article_asset_rel"
                        , "article[]article_asset_rel[<]asset"
                        , "article[]article_av[<]attribute"
                        , "article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*]"
                        , "article[]article_detail_tab"
                        , "article[]article_detail_tab[content][textid]locale_text"
                        , "article[]article_detail_tab[content][textid]locale_text[article_detail_tab.tabname][id]type"
                        , "article[]article_price[<]type[productNo,articleNo,identifier,article_price.*]"
                        , "article[]article_rating"
                        , "attribute[<]type"
                        , "attribute[]attribute_value"
                        , "category_node[description][textid]locale_lookuptext"
                        , "erp_import_article[]erp_import_article_av"
                        , "job[]job_history[id][refid]process_log[job.identifier,process_log.*] order by job_history.id desc"
                        , "job[]job_history[job.identifier,job_history.*] order by job_history.id desc"
                        , "type[]attribute"
                        , "type[]attribute[type.identifier,attribute.*] order by attribute.id desc"
                        , "article[]article_av[<]attribute[article_av.freetext][textid]locale_freetext[productno,articleno,identifier,locale,text,article_av.*]"

                };

                Map<String, String> dummyParam = new HashMap<>();
                dummyParam.put("login","");
                Statement stmt = conn.createStatement();
                for (String initStmt : initStmts) {

                    System.out.println(initStmt);
                    if (initStmt.contains("===")) {
                        QrySyntax.saveSyn(initStmt, dummyParam);
                    } else if (initStmt.contains("[")){
                        QrySyntax.saveSyn(initStmt + "===" + initStmt, dummyParam);
                    } else if (!initStmt.isEmpty()){
                        stmt.executeUpdate(initStmt);
                    }

                } // execute all init stmt
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    public static String getData(Req req){
        int size1 = 10;
        int size2 = 50;
        String size = req.params().isEmpty() ? null : req.param("size");
        System.out.println( (new Date()).toString() + " getData ... for " + (null == size ? "<null>": size) );
        if (null != size && !size.isEmpty()){
            // size = size.replace("+"," "); // js searchparam change " " to "+"
            String[] sizes = size.split("\\s*[ x]\\s*");
            if (2 == sizes.length && sizes[0].matches("[0-9]+") && sizes[1].matches("[0-9]+")) {
                size1 = Integer.valueOf(sizes[0]);
                size2 = Integer.valueOf(sizes[1]);
            }
        }
        return getData( size1, size2);
    }


    public static String getData(int size1, int size2){
        int l = (new Date()).hashCode();
        System.out.println( (new Date()).toString() + " getData ... " + l );
        StringBuffer data = new StringBuffer();
        for (int i = 0; i < size2 ; i++) {
            for (int j = 0; j < size1 ; j++) {
                if ( 0 != j ) data.append(";");
                if ( 0 == i || 0 == j) {
                    data.append( i + j );
                } else {
                    data.append(  BigInteger.valueOf(l + i+j).hashCode() );
                }
            }
            data.append("\n");
        }
        System.out.println( (new Date()).toString() + " getData DONE " );
        return data.toString();
    }


    static String data2Result(QryResponse response){
        // data may not contain line-breaks and "
        String data = response.getData();
        if (null == data) {
            data = "";
        } else {
            data = data.replace("\\", "\\\\");
            data = data.replace(System.lineSeparator(), "\n");
        }
        //data = data.replace( "\n","\\n");
        //data = data.replace( "\"","\\\"");

        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("login",  null != response.getLogin() ? response.getLogin() : connectionCurrent);
        if (null != response.getDoc())  json.put("doc", response.getDoc());
        if (null != response.getExecutedStmt())  json.put("stmt", response.getExecutedStmt());
        if (null != response.getErrorMsg())  json.put("errorMsg", response.getErrorMsg());
        if (null != response.getHint())  json.put("hint", response.getHint());
        if (null != response.getDate())  json.put("date", Util.toString(response.getDate()));
        if (null != response.getFirstDate())  json.put("firstDate", Util.toString(response.getFirstDate()));
        if (null != response.getLastAccessDate())  json.put("lastAccessDate", Util.toString(response.getLastAccessDate()));
        if (null != response.getVersion())  json.put("version", response.getVersion());
        if (0 != response.getRowCount())  json.put("rows", response.getRowCount());
        if (0 != response.getExecutionTime())  json.put("executionTime", response.getExecutionTime());
        if (null != response.getTargetTable())  json.put("targetTable", response.getTargetTable());
        // {  "data": "\"domain\",\"topic\",\"issue\",\"date\"\n\"a\",\"b\",\"c\",\"d\"\n\"a\",\"b2\",\"c2\",\"d2\"\n\"a3\",\"b3\",\"c33335\",\"d3\"\n\"a3\",\"b3\",\"c4\",\"d47789\"\n\"dom-4\",\"topic-4\",\"issue-4\",\"date-45\"\n" , "login": "", "date": "2024/03/08 12:55:09", "firstDate": "2024/03/01 08:55:24", "lastAccessDate": "2024/03/08 17:15:49"}
        //System.out.println( "data2Result: " + json.toString() );
        return json.toString();
    }



    public static void main(String[] arg) {

        if (!(new File("./data/query_list.db").exists())){
            System.out.println("-------------------------------");
            System.out.println("-  CREATE DATABASE   ");
            System.out.println("-------------------------------");
            createNewDatabase("query_list.db");
        }

        connectionCurrent = "";
        connectionUrls.put( connectionCurrent, "jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");

        dataConnectorList = new ArrayList<>();
        dataConnectorList.add( new Cache()); // allow quering Cached-Versions as query ...
        dataConnectorList.add( new DataConPropertyChecker()); // very accurate match, 1 keyword
        dataConnectorList.add( new DataConSecretHandler()); // very accurate match, 1 keyword
        dataConnectorList.add( new DataConSSH()); // very accurate match, 1 keyword
        dataConnectorList.add( new DataConCSV());
        dataConnectorList.add( new DataConTxt());
        dataConnectorList.add( new DataConSqlite());
        dataConnectorList.add( new DataConPostgres());
        dataConnectorList.add( new DataConMongoDb());
        dataConnectorList.add( new DataConnectorDemo()); // plain-easy-modify files ...

        // https://replit.com/@preisfrieden/jsGrdi1

        // files from  static/ is loadadble by default ..!
        // http://localhost:8080/index.html

        // http://localhost:8080/size

        System.out.println("start with http://localhost:8080/");
        System.out.println("test  with http://localhost:8080/test");
        System.out.println("-------------------------------------------");
        //On.get("/size").json("helloWorld"); // .json((String msg) -> msg.length());
        //On.get("/size").json("helloWorld2");
        //On.get("/size").plain("helloWorld2");
        //On.get("").(

        //createNewDatabase("test.db");

        /*

            ways ...

            -   {... -> json, data in data-Attribute
            -   "... -> csv mit ";" delimiter
            -   sonst -> csv mit ";" delimiter




         */

        On.post("/upload").html(new ReqRespHandler() {
            @Override
            public Object execute(Req req, Resp resp) throws Exception {
                Map<String, List<Upload>> files = req.files();
                if (null!=files && files.size()>0) {
                    for (Map.Entry<String, List<Upload>> filesEntry : files.entrySet()) {
                        // expect key = "file"
                        List<Upload> uploadedFiles = filesEntry.getValue();
                        for (Upload uploadedFile : uploadedFiles) {
                            String filename = uploadedFile.filename();
                            notifyInfo("received " + filename);
                            byte[] content = uploadedFile.content();
                            File file = new File( "data/" + filename.replaceAll(".*/",""));
                            if (file.exists()) {
                                // idea - uploaded files will be historiesed by CACHE - just request build grid ...
                                notifyWarn( "file replaced: " + file.getName());
                            }
                            try(FileOutputStream fos= new FileOutputStream(file)){
                                fos.write(content);
                            } catch (Exception e) {
                                notifyError(e.toString());
                            }
                        }
                    }
                    notifyInfo("saved upload(s)");
                } else {
                    String fileName = req.posted().get("fileName").toString();
                    String data = req.posted().get("data").toString();
                    File file = new File( "data/" + fileName.replaceAll(".*/",""));
                    if (file.exists()) {
                        // idea - uploaded files will be historiesed by CACHE - just request build grid ...
                        notifyWarn( "file replaced: " + file.getName());
                    }
                    try(FileOutputStream fos= new FileOutputStream(file)){
                        fos.write(data.getBytes());
                    } catch (Exception e) {
                        notifyError(e.toString());
                    }
                    notifyInfo("saved data as " + fileName);
                }
                return "OK";
            }
        });

        On.get("/login").html(new ReqRespHandler() {
            @Override
            public Object execute(Req req, Resp resp) throws Exception {
                Map<String, String> params = req.params();
                SecretHandler secretHandler = null;
                String msg = "";
                String login = params.get("login");
                String urlStr = params.get("url");
                String userPwdStr = params.get("userPwd");

                if ( null != login && !login.isEmpty()) {

                    if (null != urlStr) {
                        UrlHelper urlHelper = new UrlHelper(urlStr);
                        if (null != userPwdStr && !userPwdStr.isEmpty()) {
                            urlHelper.setUserPwd( userPwdStr );
                        }

                        if (!urlHelper.getConnectionId().isEmpty()){
                            System.out.println("enforce Login from URL (" + urlHelper.getConnectionId()  + " -> " + login + ")");
                            login = urlHelper.getConnectionId();
                        } else {
                            urlHelper.setConnectionId(login); // enforce consistency
                        }


                        if (urlHelper.getHost().isEmpty()) {
                            if (login.toUpperCase().endsWith("VM")) {
                                main.notifyInfo( "try generate from login-name (empty host, no matching keypass)");
                                UrlHelper urlHelper2 = getVMUrl(login);
                                main.notifyInfo( "try credentials from login-name (" + urlHelper2.getUrlMaskedPWD() + ")");
                                if (null != urlHelper2){
                                    urlHelper = urlHelper2;
                                }
                            }
                        }

                        if (urlHelper.getHost().isEmpty()) {
                            main.notifyInfo( "try credentials from keypass (empty host) ...");
                            secretHandler = new SecretHandler();
                            secretHandler.setPwdOonce( urlHelper.getUserPwd() );
                            UrlHelper urlHelper2 = null;
                            urlHelper2 = secretHandler.getKdbxCreds4LoginCached(urlHelper.clone());
                            if (null == urlHelper2){
                                urlHelper2 = secretHandler.getKdbxCreds4Login(urlHelper.clone());
                            }
                            if (null != urlHelper2){
                                urlHelper = urlHelper2;
                            }
                        }



                        if (urlHelper.isValid()) {
                            if (urlHelper.getScheme().startsWith("jdbc") && !urlHelper.getPath().startsWith("/")) {
                                // WARNUNG: JDBC URL must contain a / at the end of the host or port: jdbc:postgresql://192.168.56.114:5432
                                msg = "WRN login - JDBC URL must contain a / at the end of the host or port: " + urlHelper.getUrlMaskedPWD();
                                urlHelper.setPath("/");
                            }
                            if (!urlHelper.getScheme().startsWith("jdbc") || urlHelper.getPath().startsWith("/")) {
                                connectionUrls.put( login, urlHelper.getUrl(true));
                                msg = "OK   login - connectionUrl set to " + urlHelper.getUrlMaskedPWD();
                            } else {
                                msg = "FAIL login - JDBC URL must contain a / at the end of the host or port: " + urlHelper.getUrlMaskedPWD();
                            }
                        } else if (urlHelper.isConnectionIdOnly()) {
                            login = urlHelper.getConnectionId();
                            msg = "OK login - connectionUrl ConnectionIdOnly (" + login + ")";
                        } else {
                            msg = "FAIL login - connectionUrl is INVALID (" + urlStr + ")";
                        }
                    } else {
                        msg = "FAIL login - connectionUrl set MISS (" + urlStr + ")";
                    }


                    connectionCurrent = login;
                    if (connectionUrls.containsKey(connectionCurrent) && !msg.startsWith("FAIL ")) { // show error when trying to update with invalid URL
                        UrlHelper url = new UrlHelper(connectionUrls.get(connectionCurrent));
                        msg = "OK   login - set to " + connectionCurrent + " as " + url.getUrlMaskedPWD() ;

                        // at least on connector should use it ...
                        Set<DataConnector> matchingDataConnectors = new HashSet<>();
                        msg = "FAIL login - no matching Connector or Invalid Credentials!";
                        for (DataConnector dataConnector : dataConnectorList) {
                            System.out.println( dataConnector.getClass().getName() + " ... ");
                            if (dataConnector.isMatchingUrlType( url )) {
                                if (dataConnector.checkConnection(params)){
                                    matchingDataConnectors.add(dataConnector);
                                    msg = "OK   login - " + url.getUrlMaskedPWD() + " valid for "
                                            + matchingDataConnectors.size() + " connector(s) (" + matchingDataConnectors.toArray()[0].getClass().getName() + ")";
                                }
                            }
                        }
                        System.out.println( "DONE ");

                    } else if (msg.isEmpty() || msg.startsWith("OK ")) {
                        msg = "FAIL login - is UNKNOWN (" + connectionCurrent + ")";
                    }
                }

                if (null != secretHandler) {
                    msg = msg.replaceFirst(" - " , " - by " + secretHandler.keypassPathName + " - ");
                }
                main.notifications.add("{ \"" +  ( msg.startsWith("FAIL") ? "error" : "info" ) +  "\": { \"msg\": \"" + msg + "\"}}");
                System.out.println(msg);
                resp.headers().put("Access-Control-Allow-Origin", "*");
                resp.code( 200);
                resp.body( msg.getBytes() );
                return resp;
            }
        });


        On.get("/size").plain(
                new ReqRespHandler() {
                    @Override
                    public Object execute(Req req,Resp resp) throws Exception {
                        //return "helloWorld3";
                        //Resp resp = new Resp;
                        // https://github.com/rapidoid/rapidoid/issues/98
                        resp.headers().put("Access-Control-Allow-Origin", "*");
                        return getData(req);
                    }
                }

        );

        On.get("/query").plain(
                new ReqRespHandler() {
                    @Override
                    public Object execute(Req req,Resp resp) throws Exception {
                        //return "helloWorld3";
                        //Resp resp = new Resp;
                        // https://github.com/rapidoid/rapidoid/issues/98
                        System.out.println("\n----  r e q u e s t     . . .              ----\n");
                        resp.headers().put("Access-Control-Allow-Origin", "*");
                        QryResponse data = null;
                        Map<String, String> params = req.params();
                        String result = null;
                        try {
                            if (params.containsKey("qry")) {

                                String qry = params.get("qry");
                                params.put("qry_orig", qry);
                                //qry = URLDecoder.decode(qry);
                                params.put(qry, qry.trim());

                                notifyInfo("BE received request " + params.toString());
                                String login = getLogin(params);// TRICKY set currentConnection
                                // always offer login once ...
                                //    adding VM-Connection automatically will ensure that login is done once ...
                                if (connectionUrls.isEmpty()){
                                    main.notifyError("HINT: login once"); // trigger notification ... trigger login
                                }
                                //
                                if (!connectionUrls.containsKey(login) && login.endsWith("_VM")) {
                                    // allow to start without database just using cached data ...
                                    connectionUrls.put(login + ":mongodb", "jdbc:mongodb://ipim-supply:secret@192.168.56.114:27017/ipim-supply");
                                    connectionUrls.put(login, getVMUrl(login).getUrl(true));
                                }
                                if (!connectionUrls.containsKey(login)) {
                                    SecretHandler secretHandler = new SecretHandler();
                                    UrlHelper urlHelper2 = new UrlHelper("jdbc:postgresql://");
                                    urlHelper2.setConnectionId(login);
                                    urlHelper2 = secretHandler.getKdbxCreds4LoginCached(urlHelper2);
                                    if (null != urlHelper2) {
                                        connectionUrls.put(login, urlHelper2.getUrl(true));
                                    }
                                }

                                //
                                if (connectionUrls.containsKey(login) || DEMO) {
                                    // notifications.add("{ \"error\": { \"msg\": \"error " + req.params().toString()+"\"}}");
                                    // notifications.add("{ \"info\": { \"msg\": \"execute2 " + req.params().toString()+"\"}}");
                                    QrySyntax.saveSyn(login + "===LOGIN_", params); // allow to List logins ...
                                    if (!connectionUrls.containsKey(login)) {
                                        main.notifyWarn("'" + login + "' unknown (HINT: login once 0)");
                                    }
                                    data = DataMerge.get(req);
                                    if ((null == data || null == data.getData() || null != data.getErrorMsg()) && params.containsKey("qrySug")) {
                                        String qrySug = req.params().get("qrySug");
                                        if (!qrySug.equals(qrySug)) {
                                            notifyWarn("replaced query by suggestion (" + qrySug + " <<new // old>>>" + qrySug + ")");  // UseCase: just run query with unresolved suggestion
                                            req.params().put("qryOrig", qrySug); // allow Caching on entered QRY -- simulate Synonyme-Replacement without persisting the Synonym // KLUDGE --check for collision with qry_orig
                                            req.params().put("qrySug", qrySug); //
                                            data = DataMerge.get(req);
                                        }
                                    }
                                    if (null == data) {
                                        data = new QryResponse(params, getData(5, 5)); // getData( req);
                                    } else {

                                    }
                                    if (null != data && null != data.getData()) {
                                        data.setData(data.getData().replace("\t", "\\t")); // grid.js::setDataJson : Uncaught (in promise) SyntaxError: JSON.parse: bad character in string literal at line 1 column 724 of the JSON data
                                    }
                                } else {
                                    main.notifyError("'" + login + "' unknown (HINT: login once 1)");
                                    data = new QryResponse();
                                    data.setErrorMsg("'" + login + "' unknown (HINT: login once 2)");
                                    data.setLogin("");
                                }
                                result = data2Result(data);
                                //
                                new DataConnectorDemo().save(req, data);
                                //
                                System.out.println("\n----  r e q u e s t     f i n i s h e d    ----\n");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            result = "r e q u e s t     f a i l   (exception)";
                            req.response().result(result).code(500);
                            System.out.println("\n----  r e q u e s t     f a i l   (exception)  ----\n");
                        }
                        if (null == result) {
                            result = "r e q u e s t     f a i l   (bad formatted)";
                            req.response().result(result).code(500);
                            System.out.println("\n----  r e q u e s t     f a i l   (bad formatted)  ----\n");
                        }
                        return result;
                    }
                }
        );



        On.get("/info").plain(
                new ReqRespHandler() {
                    @Override
                    public Object execute(Req req,Resp resp) throws Exception {
                        //return "helloWorld3";
                        //Resp resp = new Resp;
                        // https://github.com/rapidoid/rapidoid/issues/98
                        resp.headers().put("Access-Control-Allow-Origin", "*");
                        //connector.save( req.body() );
                        //return getData(req);
                        //return connector.load();
                        return DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now());
                    }
                }

        );

        On.get("/notification").plain(
                new ReqRespHandler() {
                    @Override
                    public Object execute(Req req,Resp resp) throws Exception {
                        resp.headers().put("Access-Control-Allow-Origin", "*");
                        // wait ??

                        String msg = "";
                        int loop = 0;
                        if (waitForNotification < 20) { // allow multiple sessions ...

                            waitForNotification++;
                            System.out.println("waitForNotification " + waitForNotification);
                            // prevent blocking by multiple requests  on  empty notifications
                            // release latest after 5 minutes - like heartbeat
                            while ( ( null == msg || msg.isEmpty() ) && loop < 4*60*5 ) { // as msg could be empty too
                                if (notifications.isEmpty()) {
                                    loop++;
                                    sleep(250);
                                } else {
                                    try {
                                        msg = notifications.poll();
                                    } catch (Exception e) {
                                        // due to multi-threading
                                        // Semaphore -- on optimistic locking
                                        // java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
                                    }
                                }
                            }
                            waitForNotification--;

                        } else {
                            System.out.println("waitForNotification " + waitForNotification + " - too many requests");
                        }

                        return null == msg ? "" : msg;
                    }
                }

        );


        On.get("/rec").plain(
                new ReqRespHandler() {
                    @Override
                    public Object execute(Req req,Resp resp) throws Exception {
                        DataConSqlite db = new DataConSqlite();
                        Map<String, String> record = new HashMap<>();
                        for (String key : "before, action, after, comment".split(",\\s*")) {
                            if (req.params().containsKey(key)){
                                record.put( key, req.params().get(key));
                            }
                        }
                        record.put("cnt++","");
                        db.merge("record", record);
                        resp.headers().put("Access-Control-Allow-Origin", "*");
                        return "OK";
                    }
                }

        );

        On.get("/test").html(new ReqRespHandler() {
            @Override
            public Object execute(Req req, Resp resp) throws Exception {
                // resp.filename("index.html");
                resp.headers().put("Access-Control-Allow-Origin", "*");
                resp.code( 200);
                byte[] bytes = Files.readAllBytes(Path.of("static/index.html"));
                resp.body( bytes );
                return resp;
            }
        });

        On.get("/suggest").html(new ReqRespHandler() {
            /*
                   called by context-menu on
                    -  codeMirrorAnker -> suggest queries ...

                    result is list of
                        queries, icon, description (? nested queries ... )

             */
            @Override
            public Object execute(Req req, Resp resp) throws Exception {
                JSONArray json = new JSONArray();
                String suggestions = " article[]article_price[<]type[productNo,articleNo,identifier,article_price.*] // article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*] ";
                suggestions += " // erp_import_article[]erp_import_article_av";
                suggestions += " // article[]article_asset_rel[<]asset // article[]article_detail_tab[content][textid]locale_text ";
                suggestions += " // attribute[]attribute_value // type[]attribute // type // users // datafeed // article ";
                suggestions += " // attribute[description][textid]locale_lookuptext // category_node[description][textid]locale_lookuptext ";
                suggestions += " // article[]article_av[<]attribute[productno,articleno,identifier,article_av.*]";
                suggestions += " // job[]job_history[job.identifier,job_history.*] order by job_history.id desc // progress_tracker order by id desc // job[]job_history[id][refid]process_log[job.identifier,process_log.*] order by job_history.id desc ";
                suggestions += " // job[]job_history[job.identifier,state,,job_history.starttime] W witherror=1 or endtime is null O max(job_history.starttime) desc // JOB_ERROR ";
                suggestions += " // dummy.csv // .csv[Artikel][articleNo]article // .csv[<]article ";
                suggestions += " // article[]node_article_rel[node][id]category_node[productno,node_article_rel.status,category_node.*] ";
                suggestions += " // article[]node_article_rel[node][id]category_node[productno,node_article_rel.status,node_article_rel.lastmodified,category_node.*] order by node_article_rel.lastmodified desc ";
                suggestions += " // article[]article_rating[productNo,articleNo,article_rating.*] O id desc ";
                suggestions += " // ipim-supply.supplier // ipim-supply.supplier.JSON // db.tabUsage // dbTab // ipim-supply.tabUsage // ERP_STATE ";
                suggestions += " // worklist[description][textid]locale_freetext[worklist.type][id]type ";
                suggestions += " // attribute_mapping //  category_node[id][node]node_av[<]attribute // category_node[description][textid]locale_lookuptext[category_node.id][node]node_av[<]attribute // ";
                suggestions += " // article[]article_av[attributevalue][id]attribute_value[<]attribute[productNo,articleNo,attribute.identifier,attribute_value.identifier,article_av.*] W attribute.identifier ~ 'mig2_Farb' ";
                suggestions += " // article[]article_av[<]attribute[article_av.freetext][textid]locale_freetext[productNo,articleNo,identifier,text,article_av.*] W identifier = 'variantShortDescription' ";
                suggestions += " // ~/status.sh -a // tail -n 100 ~/core.log // ~/core.log ";
                suggestions += " // url.check data/2024_iPIM_QRY_short.kdbx ";
                // check query_list for additional ...
                suggestions = QrySyntax.extractSuggestion(suggestions);
                for (String s : suggestions.split(" *// *")) {
                    json.put(new JSONObject().put("qry",s)); // { "qry": "article" }
                }
                // should match controller.js
                json.put(new JSONObject().put("descr", "UPDATE").put("action","controller(contextMenuCircular.event_grid,'UPDATE:');") );
                String qry =  req.params().isEmpty() ? null : req.param("qry");
                if ( null == qry || !qry.toLowerCase().contains(" limit 100") ) {
                    json.put(new JSONObject().put("descr", "LIMIT 100").put("action","controller(contextMenuCircular.event_grid,'LIMIT:100:');").put("icon", "fa fa-expand") );
                } else {
                    json.put(new JSONObject().put("descr", "LIMIT 500").put("action","controller(contextMenuCircular.event_grid,'LIMIT:500:');").put("icon", "fa fa-expand") );
                }
                String jsonStr = new JSONObject().put("suggestions", json).toString();
                //
                return jsonStr;
            }
        });

        // transform path to param
        On.req((Req x) -> x.response().redirect("/?qry="
                + x.path().replaceFirst("/","")  ));




        /* for testing */
        On.get("/showVerb").json((Req req) -> req.verb());
        On.get("/showPath").json((Req req) -> req.path());
        On.get("/showUri").json((Req req) -> req.uri());
        On.get("/showData").json((Req req) -> req.data());

    }



    @NotNull
    private static UrlHelper getVMUrl(String login) {
        UrlHelper urlHelper2 = new UrlHelper(login);
        // jdbc:postgresql://pagen@192.168.56.114:5432/pagen
        urlHelper2.setHost("192.168.56.114");
        String userName = (login.replaceAll("_.*","").toLowerCase() + "    " ).substring(0,3).trim();
        userName = userName.replaceAll("3p", "pagen");
        userName = userName.replaceAll("fd", "fod");
        urlHelper2.setUserName( userName);
        urlHelper2.setUserPwd( userName + userName);
        urlHelper2.setPort(5432);
        urlHelper2.setPath("/"+ userName);
        urlHelper2.setScheme("jdbc:postgresql");
        return urlHelper2;
    }


}