package gridServer;


import org.jetbrains.annotations.NotNull;
import org.rapidoid.http.Req;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static gridServer.main.getData;
import static gridServer.main.notifyWarn;

public class Cache implements DataConnector {

    static Set<String> currentlyProcessing = new HashSet<>();
    private static boolean ALLOW_GLOBAL_DATA = true;

    protected static String getHash(String data){
        String hash = Integer.toUnsignedString(data.hashCode()); // "" -> "0"
        return hash.length() < 4 ? "0000" + hash : hash;
    }

    @NotNull
    private static File getBaseFile(String hash) {
        return new File("data/cache/" + hash.substring(0, 2) + "/" + hash.substring(2, 4) + "/" + hash);
    }

    public static QryResponse get(Req req) {
        try {
            return getInternal(req);
        } catch (Exception e) {
            currentlyProcessing.remove(""); //TODO ...
            throw e;
        }
    }

    public static QryResponse getInternal(Req req) {
        QryResponse qryResponse = new QryResponse(req);
        String data =  null;
        Map<String, String> params = req.params();
        boolean UPDATE = QryHintHelper.checkAndExtractHint( req, "UPDATE") || null != params.get("id") ; // UPDATE might be REFRESH or value-Update
        String query = handleSynonymes(req, qryResponse);
        String queryOrig = params.containsKey("qryOrig") ? params.get("qryOrig") : query; // use for Caching fast-try-out-suggenstions
        String hashWoEnv = getHash( queryOrig);
        String login = main.getLogin(req.params());
        String hash = getHash( login + "::" + queryOrig);  // ??? handle ENV-independent

        try {
            if (query.startsWith("{")) {

                main.notifyInfo("Cache.qryResponse Synonyme for Session set / read - return Session");
                data = query;
                qryResponse.setData(data);

            } else if (currentlyProcessing.contains(hash) || currentlyProcessing.contains(hashWoEnv)) {

                main.notifyInfo("Cache.qryResponse currently loading");
                qryResponse = new QryResponse(QryResponse.LOADING, query);
                qryResponse.setHint("loading"); // TODO user dedicated status ...

            } else {

                File fileEnv = getBaseFile(hash);
                File fileWoEnv = getBaseFile(hashWoEnv);
                File file = fileEnv.exists() ? fileEnv : fileWoEnv; // use EnvIndependent Version if available (depends on Data-Source, locale files and SQL)

                if (UPDATE || !file.exists()) {

                    currentlyProcessing.add(hash);
                    long start = System.currentTimeMillis();
                    qryResponse = createQryResponse(query, params, qryResponse);
                    // measure time for query
                    if (null == qryResponse) {
                        qryResponse = new QryResponse();
                    }
                    qryResponse.setExecutionTime( System.currentTimeMillis() - start );


                    if (null == qryResponse || null == qryResponse.getData() || null != qryResponse.getErrorMsg()) {

                        System.out.println("file " + file.getName() + " skipped (no DataConnector found)");
                        if (null == qryResponse) {
                            qryResponse = new QryResponse();
                        }
                        if (null == qryResponse.getErrorMsg() && null == qryResponse.getHint()) {
                            qryResponse.setHint("no DataConnector found (" + (null == main.connectionCurrent || main.connectionCurrent.isEmpty() ? "login" : "check credentials") + ")");
                        }

                    } else if (qryResponse.getData().isEmpty() && null != qryResponse.getErrorMsg() && !qryResponse.getErrorMsg().isEmpty()) {
                        System.out.println("Cache.qryResponse SKIPP " + file.getName() + " (" + (qryResponse.isEnvIndependent() ? "global" : "" + login + "") + " query failed with Error)");
                        if (null == qryResponse.getErrorMsg()) {
                            qryResponse.setErrorMsg("Cache.qryResponse SKIPP " + file.getName() + " (" + (qryResponse.isEnvIndependent() ? "global" : "" + login + "") + " query failed with Error)");
                        }
                    } else {

                        System.out.println("Execution time in nanoseconds: " + qryResponse.getExecutionTime() + "ms");
                        saveToFile(fileWoEnv, qryResponse);
                        System.out.println("Cache.qryResponse saveToFile_v0_pureData " + fileWoEnv.getName() + " (global)");
                        if (!qryResponse.isEnvIndependent()) {
                            System.out.println("Cache.qryResponse saveToFile_v0_pureData " + fileEnv.getName() + " (" + login + ")");
                            saveToFile(fileEnv, qryResponse);
                        }

                    }
                    currentlyProcessing.remove(hash);

                } else { // file exists ?
                    if (ALLOW_GLOBAL_DATA || fileEnv.exists()) {
                        System.out.println("Cache.qryResponse readFile " + file.getName() + (fileEnv.exists() ? login : " (global)"));
                        qryResponse = readFile(file, query);
                        if (!fileEnv.exists()) { // global data -> mark and keep current login (ignore connection from file)
                            notifyWarn("used global data " + file.getName() + " (global)!");
                            qryResponse.setDate(FileTime.fromMillis(1));
                            qryResponse.setLogin( null);
                        }
                        System.out.println( "Cache.qryResponse readFile " + qryResponse.getExecutionTime() + "ms executionTime saved!");
                    } else {
                        System.out.println("Cache.qryResponse readFile - won't use global data " + file.getName() + " (global)");
                    }
                }

            } // currentlyProcessing
        } catch (Exception e) {
            System.out.println(e.getMessage());
            currentlyProcessing.remove(hash);
            throw e;
        }
        qryResponse = null != qryResponse ? qryResponse : new QryResponse(data, query); /* TODO store whole Reponse in Cache, including statements ... / need executed Statement as it may contain needed Hints as SESSIONS */
        System.out.println("Cache.qryResponse created ");
        return qryResponse;
    }

    private static String handleSynonymes(Req req , QryResponse qryResponse) {
        return QrySyntax.handleSynonymes(req.params(), qryResponse);
    }



    protected static QryResponse createQryResponse(String query, Map<String, String> params, QryResponse qryResponse) {
        System.out.println( "createQryResponse create ...");
        List<QryResponse> qryResponsesWError = new ArrayList<>();
        // prepare query-translation
        QrySyntax.parseToQryStmt(params, qryResponse, query);
        System.out.println( "createQryResponse parsed: \"" + query + "\"");
        // try to find a matching DataConnector
        for (DataConnector con : main.dataConnectorList){

            System.out.println(con.getClass().getName() + " try ...");
            if (con.matches(query, params)) {
                System.out.println(con.getClass().getName() + " run ...");
                qryResponse = con.run(query, params, qryResponse);
                if (null != qryResponse.getData()) {
                    if (null != qryResponse.getErrorMsg()) {
                        qryResponsesWError.add(qryResponse);
                        System.out.println(con.getClass().getName() + " " + qryResponse.getData().length() + " chars with error found -> keep trying other connectors");
                    } else {
                        System.out.println(con.getClass().getName() + " " + qryResponse.getData().length() + " chars found");
                        break;
                    }
                } else {
                    System.out.println(con.getClass().getName() + " no data found");
                }
            } else {
                System.out.println(con.getClass().getName() + " does not match");
            }
        }
        return (null == qryResponse || null == qryResponse.getData()) && !qryResponsesWError.isEmpty() ? qryResponsesWError.get(0) : qryResponse ;
    }


    private static void saveToFile(File file, QryResponse qryResponse) {
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        File fos_version = new File(file.getAbsolutePath() + "_" + getHash( qryResponse.getData() )); // ignore executionTime etc for versioning
        if (!fos_version.exists()) {
            qryResponse.setQryStmtBlock( null ); // KLUDGE do not store statements in cache // not serializable
            System.out.println( fos_version.getName() +  " write ...");
            try {
                try(FileOutputStream fileOutputStream = new FileOutputStream(file)){
                    new ObjectOutputStream(fileOutputStream).writeObject( qryResponse );
                }
                // History ... data might change ....
                try(FileOutputStream fileOutputStream = new FileOutputStream(fos_version)) {
                    new ObjectOutputStream(fileOutputStream).writeObject( qryResponse );
                };
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println( fos_version.getName() +  " saved");
        } else { // result already known ...
            fos_version.setLastModified( new Date().getTime());
            System.out.println( fos_version.getName() +  " touched (unchanged result)");
        }
    }

    private static QryResponse readFile(File file, String query) {
        System.out.println( file.getName() +  " read ...");
        QryResponse qryResponse = null;
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime lastAccessTime = fileAttributes.lastAccessTime();// will be updated on reading ... !!
            qryResponse = new QryResponse("", query);
            //
            try (FileInputStream fileInputStream = new FileInputStream(file)){
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                qryResponse = (QryResponse) objectInputStream.readObject();
            } catch (Exception e) {

                System.out.println( file.getName() +  " read failed (" + e.getMessage() + ") -> try to read as v0_pureData");

                try (FileInputStream fileInputStream = new FileInputStream(file)){
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    for (int length; (length = fileInputStream.read(buffer)) != -1; ) {
                        result.write(buffer, 0, length);
                    }
                    qryResponse.setData(result.toString("UTF-8"));
                }

            }

            qryResponse.setFirstDate( fileAttributes.creationTime() );
            qryResponse.setDate( fileAttributes.lastModifiedTime() );
            qryResponse.setLastAccessDate( lastAccessTime ); // will be updated on reading ... !!
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println( file.getName() +  ( null == qryResponse || null == qryResponse.getData() ? " read failed" : " read " + qryResponse.getData().length() + " chars") );
        return qryResponse;
    }


    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches("(?i)CACHE(:[0-9_]+)? .*");
    }

    @Override
    public QryResponse run(String qry, Map<String, String> params, QryResponse qryResponseDummy) {
        QryResponse qryResponse = null;
        if (qry.startsWith("CACHE ")){
            qryResponse = listSnapshots( qry, params);
        } else if (qry.startsWith("CACHE:")) {
            qryResponse = getSnapshot( qry, params);
        } else {
            qryResponse = new QryResponse();
            qryResponse.setErrorMsg( "unknown stmt \"" + qry + "\""  );
            main.notifyError("unknown stmt \"" + qry + "\"" );
        }
        return qryResponse;
    }

    @Override
    public boolean checkConnection(Map<String, String> params) {
        return true;
    }

    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return true;
    }


    public static QryResponse listSnapshots(String qry, Map<String, String> params) {
        QryResponse resp = new QryResponse();
        String query = qry; // handleSynonymes(params);
        query = query.replaceFirst("^CACHE ","");
        String loginCur = main.getLogin(params);
        // allow all current logins to be seen ...
        Set<String> loginSet = new HashSet<>();
        loginSet.addAll(main.connectionUrls.keySet());
        //
        if (main.DEMO) {
            // allow all existing logins to be seen ...
            DataConSqlite sqlite = new DataConSqlite();
            //LOGIN===select appr, updatedAt from query where appr <> '' and stmt like 'LOGIN_' order by updatedAt desc
            QryResponse sqliteLogins = sqlite.read("select appr, updatedAt from query where appr <> '' and stmt like 'LOGIN_' order by updatedAt desc", params);
            String data = sqliteLogins.getData();
            List<String> rows = List.of(data.split("\\n"));
            for (String row : rows.subList(1, rows.size())) { // skipp headline
                String[] vals = row.split(";");
                if (vals.length > 0) loginSet.add( vals[0] );
            }
        }
        // move current login to front
        List<String> logins = new ArrayList<>(loginSet);
        logins.add(0, loginCur);
        logins.add(""); // dummy for hash w/o env ...

        StringBuilder str = new StringBuilder("creation;modified;hash\n");
        HashSet<String> checkedFiles = new HashSet<>();
        for (String login : logins) {
            String hash = getHash( "" == login ? query : login + "::" + query);  // ??? handle ENV-independent
            File basefile = getBaseFile(hash);
            if (!checkedFiles.contains(basefile.getName())) { // ensure uniq sorted entries
                checkedFiles.add(basefile.getName());
                File[] versionFiles = basefile.getParentFile().listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith(hash + "_");
                    }
                });
                if (null != versionFiles) {
                    for (File versionFile : versionFiles) {
                        try {
                            BasicFileAttributes fileAttributes = Files.readAttributes(versionFile.toPath(), BasicFileAttributes.class);
                            str.append(Util.toString(fileAttributes.creationTime()) + ";" + Util.toString(fileAttributes.lastModifiedTime()) + ";" + versionFile.getName() + ";" + login);
                            System.out.println("Cache::listSnapshots: " + versionFile + ": " + Util.toString(fileAttributes.creationTime()) + " " + Util.toString(fileAttributes.lastModifiedTime()) + " " + versionFile.getName() + ";" + login);
                        } catch (IOException e) {
                            main.notifyError("Cache::listSnapshots: " + versionFile + ": " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                        str.append("\n");
                    }
                }
            } // checkedFiles
        }
        resp.setData( str.toString() );
        return resp;
    }


    private QryResponse getSnapshot(String hash_qry, Map<String, String> params) {
        QryResponse qryResponse = null;
        String[] split = hash_qry.split("[: ]+", 3);
        if (split.length > 2){
            String hash = split[1];
            String qry = split[2];
            File file = getBaseFile(hash);
            qryResponse = readFile(file, qry);
            qryResponse.setVersion( hash );
        }
        return qryResponse;
    }



}
