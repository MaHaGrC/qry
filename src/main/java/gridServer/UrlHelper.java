package gridServer;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlHelper {

    private String url = "";
    private String urlConnectionId = "";

    private String connectionId = "";
    private String scheme = "";
    private String userName = "";
    private String userPwd = "";
    private String host = "";
    private int port;
    private String path = "";
    //URL url;

    int portDefault;


    //
    // java.net.URL  will not work with jdbc ...
    // "jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb"
    final String CONNECTION_ID_PATTERN = "[^:]+";
    final String SCHEME_PATTERN = ".*?";
    //final String USER_INFO_PATTERN = "([^@#/:]*)(?::([^@#/]*))?";
    final String USER_INFO_PATTERN = "([^@#/:]*)(?::([^@]+))?"; // PWD may contain all but @
    final String HOST_PATTERN = "[^@#/:]+";
    final String PORT_PATTERN = "[0-9]+";
    final String PATH_PATTERN = "[^?:]+"; // need : here to keep port ...
    Pattern pattern = Pattern.compile("(?:(" + CONNECTION_ID_PATTERN + ")::)?(" + SCHEME_PATTERN + ")://(?:" + USER_INFO_PATTERN +"@)?(" + HOST_PATTERN + ")?(?::(" + PORT_PATTERN +  ")?)?(" + PATH_PATTERN + ")?");

    static String notNull(String val) {return null == val ? "" : val;}

    UrlHelper(String spec){
        if (null != spec && spec.length() > 0) {
            this.url = spec;
            this.urlConnectionId = spec; // in case it pattern wont match ...
            Matcher matcher = pattern.matcher(spec);
            if (matcher.matches()) { // allow ://ipim_dev@:   to login via keypass
                setConnectionId(notNull(matcher.group(1)));
                setScheme(notNull(matcher.group(2)));
                setUserName(notNull(matcher.group(3)));
                setUserPwd(notNull(matcher.group(4)));
                setHost(notNull(matcher.group(5)));
                if (null != matcher.group(6)) {
                    setPort(Integer.valueOf(matcher.group(6)));
                }
                setPath(notNull(matcher.group(7)));

                setUrl(getUrl(true)); // remove Login etc ...
            }


            if (null != spec && spec.matches(CONNECTION_ID_PATTERN + "(::://@:)?")) {
                setConnectionId(spec.replaceFirst("(::://@:)?$", ""));
            }
        }

    }

    public static String mask(String urlString) {
        //UrlHelper urlHelper = new UrlHelper(urlString);
        //return urlHelper.getUrlMaskedPWD();
        return urlString.replaceFirst("://[^@#/:]*(:.*)?@", "://<USER>:<PWD>@");
    }

    public static String maskPWD(String urlString) {
        //UrlHelper urlHelper = new UrlHelper(urlString);
        //return urlHelper.getUrlMaskedPWD();
        return urlString.replaceFirst("://([^@#/:]*)(:.*)?@", "://$1:<PWD>@");
    }

    public boolean isValid(){
        return null != host && !host.isEmpty();
    }

    public boolean isConnectionIdOnly(){
        return null != connectionId && connectionId.matches(CONNECTION_ID_PATTERN + "(::://@:)?");
    }


    public String getStringWoUser() {
        return url.replaceFirst("//.*@","//");
    }

    public UrlHelper setDefaultPort(int port) {
        portDefault = port;
        url = getUrl(true);
        return this;
    }


    private String prefix(String prefix, String val) {
        return null == val || val.isEmpty() ? "" : prefix + val;
    }

    private String suffix(String val, String suffix) {
        return null == val || val.isEmpty() ? "" : val + suffix;
    }

    public String getUrlMaskedPWD() {
        return suffix(scheme , "://") + suffix( userName + prefix( ":", (null != userPwd && !userPwd.isEmpty() ? "<PWD>" : null) ) , "@") + host + prefix( ":" ,getPortAsString()) + path;
    }

    public String getUrl(boolean inclPWD, boolean encode) {
        return suffix(scheme , "://") + suffix(  (true || encode ? URLEncoder.encode(userName) : userName ) + prefix( ":", (inclPWD && null != userPwd && !userPwd.isEmpty() ? encode ? URLEncoder.encode(userPwd) : userPwd : null) ) , "@") + host + prefix( ":" , getPortAsString()) + path;
    }

    public String getUrl() {
        return getUrl(false);
    }

    public String getUrl(boolean inclPWD) {
        return getUrl(inclPWD, false);
    }

    public String getEncodedUrl() {
        return getUrl(true, true);
    }


    public UrlHelper setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getScheme() {
        return scheme;
    }

    public UrlHelper setScheme(String scheme) {
        this.scheme = scheme;
        url = getUrl(true);
        return this;
    }


    public String getHost() {
        return host;
    }

    public UrlHelper setHost(String host) {
        this.host = host;
        url = getUrl(true);
        return this;
    }

    public String getPortAsString() {
        return 0 == port ? "" : String.valueOf(port);
    }

    public int getPort() {
        return port;
    }

    public UrlHelper setPort(int port) {
        this.port = port;
        url = getUrl(true);
        return this;
    }

    public String getPath() {
        return path;
    }

    public UrlHelper setPath(String path) {
        this.path = path;
        url = getUrl(true);
        return this;
    }


    public String getConnectionId() {
        return connectionId;
    }

    public UrlHelper setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        this.url = getUrl(true);
        this.urlConnectionId = connectionId + ( null == scheme || scheme.isEmpty() ? "::://" : "::" ) + url;
        return this;
    }


    public String getUserName() {
        return userName;
    }

    public UrlHelper setUserName(String userName) {
        this.userName = userName;
        url = getUrl(true);
        return this;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public UrlHelper setUserPwd(String userPwd) {
        this.userPwd = userPwd;
        url = getUrl(true);
        return this;
    }

    public String toString() {
        return getUrl();
    }


    public String getUrlConnectionId() {
        return getUrlConnectionId( false);
    }


    public String getUrlConnectionId(boolean inclPwd) {
        // return inclPwd ? urlConnectionId : connectionId + ( null == scheme || scheme.isEmpty() ? "::://" : "::" ) + getUrl(inclPwd) ;
        return inclPwd && !(urlConnectionId.isEmpty() || urlConnectionId.equals("::://"))
                    ? urlConnectionId // shortcut if available
                    : ( !connectionId.isEmpty() ? connectionId + ( null == scheme || scheme.isEmpty() ? "::://" : "::" ) : "" ) // prefix ConnectionId if avail
                        + getUrl(inclPwd) ;
    }

    public UrlHelper setUrlConnectionId(String urlConnectionId) {
        this.urlConnectionId = urlConnectionId;
        url = getUrl(true);
        return this;
    }

    public UrlHelper clone(){
        return (new UrlHelper(getUrlConnectionId(true)));
    }

}
