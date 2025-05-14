package gridServer;

import org.rapidoid.http.Req;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DataConnectorDemo implements DataConnector {

    private static String simplify(String string){
        string =string.replaceFirst("(?s)\\s*\\/\\*\\{.*", "");
        string =string.replaceAll("[^a-zA-Z0-9]+", "_");
        if (string.length() > 200) string = string.substring(0, 200)+"__" + Integer.toUnsignedString(string.hashCode());
        return string;
    }
    private static File getBaseFile(String name) {
        return new File("data/demo/" + name);
    }

    protected static String getHash(String data){
        String hash = Integer.toUnsignedString(null == data ? 0 : data.hashCode()); // "" -> "0"
        return hash.length() < 4 ? "0000" + hash : hash;
    }

    private static List<File> getFiles(Map<String, String> params, QryResponse data) {
        ArrayList<File> files = new ArrayList<>();
        String query = params.containsKey("qry") ? params.get("qry") : "";
        String login = main.getLogin(params);
        if ( null != data ){
            // allow multiple versions...
            String hash = getHash(data.getData());
            files.add( getBaseFile( simplify( query) + "_" + hash ) );
            files.add( getBaseFile( simplify( login + "::" + query ) + "_" + hash));
        }
        // needed for query
        files.add( getBaseFile( simplify( query)) );
        files.add( getBaseFile( simplify( login + "::" + query )));
        return files;
    }

    @Override
    public boolean matches(String query, Map<String, String> params) {
        File file = null;
        for (File file_ : getFiles(params, null)) {
            if (file_.getAbsolutePath().length() < 200 && file_.exists()) file = file_;
        }
        return null != file;
    }

    @Override
    public QryResponse run(String qry, Map<String, String> params, QryResponse qryResponse) {
        File file = null;
        for (File file_ : getFiles(params, null)) {
            if (file_.getAbsolutePath().length() < 200 && file_.exists()) file = file_;
        }
        return null == file ? null : readFile( file, qry);
    }

    @Override
    public boolean checkConnection(Map<String, String> params) {
        boolean found = false;
        for (File file : getFiles(params, null)) {
            found = found || (file.getAbsolutePath().length() < 200 && file.exists());
        }
        return main.DEMO && main.connectionCurrent.isEmpty() && found;
    }

    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return true;
    }



    public void save(Req req, QryResponse data) {
        if (null != data.getData() && !QryResponse.LOADING.equals(data.getData())) {
            saveToFile( getFiles(req.params(), data), data.getData());
        }
    }

    private static void saveToFile(List<File> files, String data) {
        // TODO merge with CACHE
        for (File file_fos : files) {

            if (!file_fos.getParentFile().exists()){
                file_fos.getParentFile().mkdirs();
            }
            if (!file_fos.exists()) {
                System.out.println( "DEMO: " + file_fos.getName() +  "\t\t write ... ");
                try {
                    try(FileOutputStream fileOutputStream = new FileOutputStream(file_fos)){
                        if (null != data) fileOutputStream.write( data.getBytes(StandardCharsets.UTF_8) );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println( "DEMO: " + file_fos.getName() +  "\t\t saved");
            } else { // result already known ...
                file_fos.setLastModified( new Date().getTime());
                System.out.println( "DEMO: " + file_fos.getName() +  "\t\t touched (unchanged result)");
            }

        }
    }


    private static QryResponse readFile(File file, String query) {
        // TODO merge with CACHE
        System.out.println( "DEMO: " +file.getName() +  " read ...");
        QryResponse qryResponse = null;
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            qryResponse = new QryResponse("", query);
            qryResponse.setFirstDate( fileAttributes.creationTime() );
            qryResponse.setDate( fileAttributes.lastModifiedTime() );
            qryResponse.setLastAccessDate( fileAttributes.lastAccessTime() ); // will be updated on reading ...
            //
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            for (int length; (length = fileInputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            qryResponse.setData(result.toString("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        main.notifyWarn( "DEMO: " +file.getName() +  " re-loaded");
        return qryResponse;
    }


}
