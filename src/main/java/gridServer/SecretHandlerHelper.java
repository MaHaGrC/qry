package gridServer;

import com.jcraft.jsch.*;
import org.linguafranca.pwdb.kdbx.dom.DomEntryWrapper;

import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SecretHandlerHelper {

    public static final String HIDE = "HIDE";
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }
    };

    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session)  {
             System.out.println("SecretHandlerHelper: verify " + hostname + " - " + session.getPeerHost() + " force true");
             return true;
        }
    };

    class SFtpWrapper implements AutoCloseable {
        Session session;
        ChannelSftp channel;

        public SFtpWrapper(String userName, String userPwd, String host, int port) throws IOException {

            try {
                session = (new JSch()).getSession(userName, host, port);
                // session.setPassword(URLEncoder.encode(userPwd));
                session.setPassword(userPwd);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(2000);
                session.connect();

                channel = (ChannelSftp) session.openChannel("sftp");
                if (null != channel ) {
                    channel.connect();
                } else {
                    close();
                    throw new IOException("SFTP-Channel-Fail '" + userName + "' for '" + host + "'");
                }
            } catch (JSchException ex) {
                close();
                throw new IOException("SFTP-Fail '" + userName + "' for '" + host + "'", ex);
            }
        }

        @Override
        public void close()
        {
            try {
                if( channel != null ) {
                    channel.disconnect();
                    channel = null;
                }
            } finally {
                if( session != null ) {
                    session.disconnect();
                    session = null;
                }
            }
        }

    }

    public SFtpWrapper createSFtpWrapper(String userName, String userPwd, String host, int port)  {
        try {
            return new SFtpWrapper(userName, userPwd, host, port);
        } catch (IOException e) {
            Notifier.print(e);
            return null;

        }
    }

    boolean verifyCapabilities(UrlHelper urlHelper, SecretHandler.SecrectCheck check){
        try {
            if (urlHelper.getUrl().toLowerCase().startsWith("sftp://osp") && urlHelper.getPath().isEmpty()){
                SFtpWrapper sFtp = new SFtpWrapper(urlHelper.getUserName(), urlHelper.getUserPwd(), urlHelper.getHost(), urlHelper.getPort());

                try {
                    sFtp.channel.ls( check.logTry("/in/remote_control/commands"));
                    check.logTry(" readMCPLog");
                    InputStream inputStream = sFtp.channel.get("/in/remote_control/commands/log/mcp.log");
                    check.logTry(" writeMCP");
                    OutputStream put = sFtp.channel.put("/in/remote_control/commands/sftp.capabilityTest.WriteMCP");
                    (new OutputStreamWriter( put )).write("sftp.capabilityTest.WriteMCP");
                    check.logTry(" removeMCP");
                    sFtp.channel.rm("/in/remote_control/commands/sftp.capabilityTest.WriteMCP");
                } catch (IOException | SftpException e) {
                    check.logFail(e);
                }

                try{
                    sFtp.channel.ls(check.logTry("ipim_data/erp"));
                    List<ChannelSftp.LsEntry> ls = sFtp.channel.ls(check.logTry("ipim_data/erp/done"));
                    //todo get file from done and read
                    if (ls.size()>2) {
                        String fileName = "ipim_data/erp/done/" + ls.get(2).getFilename();
                        check.logTry(" read (" + fileName + ")");
                        InputStream inputStream = sFtp.channel.get(fileName);
                    } else if (ls.size()>0) {
                        check.logTry(" empty"); // contains . ..
                    }
                } catch (SftpException e) {
                    check.logFail(e);
                }

                sFtp.close();
            }
        } catch (IOException  e) {
            check.logFail(e);
        }
        return check.successful;
    }


    //boolean verifyCredentials(DomEntryWrapper entry, String urlString, String msg ) {
    boolean verifyCredentials(SecretHandler.SecrectCheck check) {
        String urlString = check.url.getUrl(true);
        //System.out.println(urlString);
        DomEntryWrapper entry = check.entry;
        SSLContext sc = null;
        int retries = 1; // 3;
        String lastError = "";
        while (null != entry && retries-- > 0){
            System.out.println("verifyCredentials: " + UrlHelper.maskPWD(urlString) + " - " + retries);
            try {
                UrlHelper urlHelper = new UrlHelper(urlString);
                if( urlHelper.getHost().isEmpty()) {
                    check.logTry("Skipp - no host");
                } else if (urlString.startsWith("::://")) {
                    check.logTry(HIDE + " (no protocol)");
                } else if (null != entry.getNotes() && entry.getNotes().contains("url.check-SKIPP")) {
                    // get whole line from Notes starting with "url.check-SKIPP"
                    String line = Arrays.stream(entry.getNotes().split("\n")).filter( s -> s.contains("url.check-SKIPP")).findFirst().orElse("url.check-SKIPP");
                    check.logTry("Skipp - " + line);
                } else if ("ping".equals(urlHelper.getScheme())){
                    boolean reachable = InetAddress.getByName(urlHelper.getHost()).isReachable(1000);
                    check.logTry( (reachable ? "FAIL - Ping for known host" : "OK - Ping" ));
                } else if( urlString.startsWith("http") && !urlString.contains("/rest/")) {
                    HttpURLConnection con = (HttpURLConnection) new URL(urlHelper.getEncodedUrl()).openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "text/html;charset=utf-8");
                    con.setConnectTimeout(1000);
                    if (null != sc){
                        ((HttpsURLConnection) con).setHostnameVerifier(allHostsValid);
                        ((HttpsURLConnection) con).setSSLSocketFactory(sc.getSocketFactory());
                    }
                    String s = con.getInputStream().toString();
                    check.logTry("OK - HTTP " + s.length() + " read");
                    // System.out.print("OK-HTTP " + s.length() + " read"+ '\r' + "OK");
                } else if (urlString.startsWith("sftp")) {
                    //SFtpWrapper( entry.getUsername(), entry.getPassword(), entry.getTitle(), 7387 );
                    (new SFtpWrapper( urlHelper.getUserName(), urlHelper.getUserPwd(), urlHelper.getHost(), urlHelper.getPort() )).close();
                    check.logTry("OK - SFTP");
                    verifyCapabilities( urlHelper, check);
                } else if (urlString.contains("postgres")) {
                    //SFtpWrapper( entry.getUsername(), entry.getPassword(), entry.getTitle(), 7387 );
                    if (urlHelper.getPath().isEmpty()) {
                        check.logTry("FAIL - append Path \"/ipim\" - ");
                        urlHelper.setPath("/ipim");
                    }
                    DriverManager.setLoginTimeout(1000);
                    (new DataConPostgres()).checkConnection(urlHelper);
                    check.logTry("OK - Postgres");
                } else if (urlString.contains("rest")) {
                    if (null == urlHelper.getScheme() || urlHelper.getScheme().isEmpty()) {
                        check.logTry("FAIL - Rest - try \"http\" - ");
                        urlHelper.setScheme("http");
                        urlHelper.setUrl(urlHelper.getUrl()); //
                        urlString = urlHelper.getEncodedUrl(); // encode password
                    }
                    URL url = new URL(urlString);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-Type", "text/html;charset=utf-8");
                    con.setRequestProperty("iPIM-User", entry.getUsername());
                    con.setRequestProperty("iPIM-Pass", entry.getPassword());
                    con.setRequestProperty("iPIM-ClientId", "1");
                    con.setConnectTimeout(1000);
                    if (null != sc){
                        ((HttpsURLConnection) con).setHostnameVerifier(allHostsValid);
                        ((HttpsURLConnection) con).setSSLSocketFactory(sc.getSocketFactory());
                    }
                    String s = con.getInputStream().toString();
                    check.logTry("OK - iPIM-REST " + s.length() + " read");

                } else {
                    check.logTry("?? - unknown protocol");
                }
                check.successful = true;
                System.out.println("verifyCredentials: " + UrlHelper.maskPWD(urlString) + " - " + retries + " - " + "OK");
                retries = 0 ;
            } catch (IOException | SQLException e) {
                System.out.println("verifyCredentials: " + UrlHelper.maskPWD(urlString) + " - " + retries + " - " + e.getMessage());
                String errMsg = e.getMessage();
                errMsg = errMsg.replace("Server returned HTTP response code: ", "").replaceAll("for URL: .*","");
                if (e.getMessage().equals(lastError)) {
                    check.logFail("");
                } else if (errMsg.contains("Server redirected too many  times")) {
                    check.logTry( "WARN - redirected too many times");
                } else if (errMsg.startsWith("401 ")) {
                    check.logTry( "WARN - " + errMsg);
                } else if (errMsg.matches("No subject alternative .* found.?") || errMsg.contains("PKIX path building failed:")) {
                    // e.g. PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
                    check.logFail( e, errMsg );
                    check.logTry( "disable SSL");
                    try {
                        sc = SSLContext.getInstance("SSL");
                        sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    } catch (NoSuchAlgorithmException | KeyManagementException e2) {
                        e2.printStackTrace();
                    }
                    retries = 1;
                } else {
                    check.logFail(e, errMsg);
                }
                if (urlString.contains("://") && !urlString.contains("@") && !entry.getUsername().isEmpty() && !entry.getPassword().isEmpty()) {
                    urlString = urlString.replace("://", "://" + entry.getUsername() + ":" + entry.getPassword() + "@");
                    check.logTry(" / inject Credentials ... ");
                }
                lastError = e.getMessage();
                if (!e.getMessage().contains(" 401 ")) { // 403 Forbidden
                   // retries = 0;
                }
                // 401 - missing credentials
                //e.printStackTrace();
            }
        }
        return check.finished();
    }
}
