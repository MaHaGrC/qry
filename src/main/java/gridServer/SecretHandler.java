package gridServer;

import com.bea.xml.stream.util.SymbolTable;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.dom.DomDatabaseWrapper;
import org.linguafranca.pwdb.kdbx.dom.DomEntryWrapper;
import org.linguafranca.pwdb.kdbx.dom.DomGroupWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class SecretHandler {

    // test with:   "url.check data/2024_iPIM_QRY.kdbx"

    protected final String keypassShortPathName = "data/2024_iPIM_QRY_short.kdbx";
    protected final String keypassPathName =  "data/2024_iPIM_QRY.kdbx";
    private List<String[]> csvData;
    private static Map<String,UrlHelper> loginUrlCache = new HashMap<>();
    private static String pwd;

    public boolean credentialsAvailable(){
        return null != pwd;
    }

    class SecrectCheck implements Callable<SecrectCheck> {
        DomEntryWrapper entry = null;
        String msgContext = "";
        UrlHelper url = null;
        String msg = "";
        boolean successful = true;
        boolean modified = false;

        public SecrectCheck(UrlHelper url, DomEntryWrapper entry) {
            this.url = url;
            this.msgContext = String.format("    %-45s \t %-80s \t>",
                    null == entry ? ""
                        : entry.getParent().getPath()
                            + (entry.getTitle().matches(".*(.novomind.com|nmop.*)") ? entry.getUsername() : entry.getTitle())
                    , url.getUrlMaskedPWD());
            ;
            this.entry = entry;
            //System.out.println(msgContext);
            //System.out.print(msgContext + msg);
        }

        public SecrectCheck(UrlHelper url, DomEntryWrapper entry, String host) {
            modified = true;
            this.url = url.clone().setHost(host); // deep clone
            this.msgContext = String.format("    %-45s \t %-80s \t ", "", this.url.getUrlMaskedPWD());
            this.entry = entry;
        }

        public String toString() {
            return msgContext + msg;
        }

        String add(String msgPart) {
            //if (msgPart.startsWith(" OK ") || msgPart.startsWith(" WARN ") || msgPart.startsWith("OK ") || msgPart.startsWith("WARN "))  {
                msg = msgPart + (msg.isEmpty() ? "" : " // " ) + msg; // have final result first if OK or WARN
            //} else {
            //    msg += msgPart;
            //}
            //System.out.println(msgContext + msg);
            //System.out.print(msgPart);
            return msg;
        }

        String logTry(String msgPart) {
            add(" " + msgPart);
            return msgPart;
        }

        boolean logFail(String msgFail) {
            // mask password in url
            msgFail = msgFail.replaceFirst(":://[^:]+:[^@]+@", ":://<user>:<pwd>@");
            add(" FAIL \"" + msgFail + "\"");
            successful = false;
            return successful;
        }

        public void logFail(Exception e) {
            Notifier.print(e);
            logFail(e.getMessage());
        }

        public void logFail(Exception e, String msgFail) {
            Notifier.print(e);
            logFail(msgFail);
        }

        boolean finished() {
            return successful;
        }

        @Override
        public SecrectCheck call() throws Exception {
            (new SecretHandlerHelper()).verifyCredentials(this);
            return this;
        }

    }

    private List<SecrectCheck> kdbxRead(DomGroupWrapper group, List<String> filter) {
        return kdbxRead(group, filter, new HashMap<String, String>());
    }

    private List<SecrectCheck> kdbxRead(DomGroupWrapper group, List<String> filter, Map<String, String> constants) {
        List<SecrectCheck> urls = new ArrayList<>();
        if (null == filter) {
            System.out.println(group.getPath() + "   " + group.getName());
        }
        for (DomEntryWrapper entry : group.getEntries()) {
            //System.out.println(entry.getPath() + "        " + entry.getTitle() + " " + entry.getUrl() );
            String id = (group.getName() + '_' + entry.getTitle()).replace(" ", "_");
            String urlFilled = entry.getUrl().replace("{USERNAME}", URLEncoder.encode(entry.getUsername())).replace("{Title}", entry.getTitle());
            // save constants like "host" to be used in url later
            if (entry.getTitle().equals(entry.getTitle().toLowerCase()) && !entry.getTitle().contains(" ")) { // recognize constants lowercase title
                constants.put(entry.getTitle(), urlFilled);
                constants.put(entry.getTitle() + "::Notes", entry.getNotes());
            }
            String notes = "";
            if (urlFilled.contains("{") && urlFilled.contains("}")) {
                for (String key : constants.keySet()) {
                    String urlFilledBefore = urlFilled + ""; // clone
                    urlFilled = urlFilled.replace("{ref:a@t:" + key + "}", constants.get(key));
                    if (!urlFilledBefore.equals(urlFilled)) {
                        // allow to inject alternative host-names / IPs from global-def to all entries
                        notes = notes + System.lineSeparator() + constants.get(key + "::Notes");
                    }
                }
            }
            if (null == filter) {
                System.out.print(entry.getPath() + "        " + entry.getTitle() + " " + urlFilled + " (" + id + ") ");
            } else if (null == filter || filter.isEmpty() || filter.contains("*") || (1 == filter.size() && filter.contains(""))
                    || filter.contains(group.getName())
                    || filter.contains(entry.getTitle())
                    || filter.contains(entry.getUsername() + "@" + entry.getTitle())
                    || filter.contains(entry.getUsername() + "@" + group.getName())
                    || filter.contains(group.getPath())
                    || filter.contains(id)
                    || filter.contains(null != group.getParent() ? group.getParent().getName() : "*")
                    || filter.contains(null != group.getParent() && null != group.getParent().getParent() ? group.getParent().getParent().getName() : "*")
                    || filter.contains(null != group.getParent() && null != group.getParent().getParent() && null != group.getParent().getParent().getParent() ? group.getParent().getParent().getParent().getName() : "*")
            ) {
                //System.out.print(entry.getPath() + "        " + entry.getTitle() + " " + urlFilled + " (" + id + ") ");
                //String msg = "    " + entry.getPath() + "        " + entry.getUsername() + " ";
                // UrlHelper urlHelper = new UrlHelper(urlFilled.replace("{PASSWORD}", entry.getPassword()));
                UrlHelper urlHelper = new UrlHelper(urlFilled);
                urlHelper.setUserName(entry.getUsername());
                urlHelper.setUserPwd(entry.getPassword());

                urls.add(new SecrectCheck(urlHelper, entry));

                notes = notes + System.lineSeparator() + entry.getNotes();
                if (!urlHelper.getHost().isEmpty() && null != notes ) {
                    List<String> hostAlias = new LinkedList<String>(Arrays.asList(notes.split("\\s+|\\r|\\n")));
                    hostAlias.removeIf(n -> !n.matches("[^@]*(\\.novomind\\.com|nmop\\.de|[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)")); // exclude email like imail-test@integ-ioniq.novomind.com
                    hostAlias.remove(urlHelper.getHost()); // already checked ...

                    // hostAlias.addAll(hostAlias2);
                    Collections.sort(hostAlias);
                    // hostAlias.set(0, urlHelper.getHost());
                    for (String host : hostAlias) {
                        urls.add(new SecrectCheck(urlHelper, entry, host));
                    }
                } // no host to secret

            }
        }
        for (DomGroupWrapper childGroup : group.getGroups()) {
            urls.addAll(kdbxRead(childGroup, filter, constants));
        }
        return urls;
    }


    public List<String[]> getKdbxCreds(String trgtFileName, List<String> filter, String keypassPathName) {
        keypassPathName = null == keypassPathName || keypassPathName.isEmpty() ? this.keypassPathName
                            :  ( new File(keypassPathName)).exists() ? keypassPathName : "data/" + keypassPathName;
        csvData = new ArrayList<>();
        if (null == pwd) {
            main.notifyError("please Login first");
            csvData.add( new String[]{"Status"} );
            csvData.add( new String[]{"please Login first (credentials for keypass missing)"} );
        } else {
            KdbxCreds creds = new KdbxCreds(pwd.getBytes());

            /*
                    jdbc:postgresql://{USERNAME}:{PASSWORD}@{Title}:5432/ipim
                    sftp://{USERNAME}:{PASSWORD}@{Title}:7387
                    https://{USERNAME}:{PASSWORD}@{Title}/REST
                    https://{Title}/iPIM/rest/api/clients/


             */

            ExecutorService executorService = Executors.newFixedThreadPool(30);
            List<Future<SecrectCheck>> checksFutures = new ArrayList<>();
            ArrayList<SecrectCheck> checks = new ArrayList<>();

            //InputStream inputStream = main.class.getClassLoader().getResourceAsStream("data/2023_iPIM_QRY.kdbx");
            try {
                main.notifyInfo(keypassPathName + " opening ...");
                InputStream inputStream = new FileInputStream(new File(keypassPathName));
                DomDatabaseWrapper db = DomDatabaseWrapper.load(creds, inputStream);
                DomGroupWrapper dbRootGroup = db.getRootGroup();
                main.notifyInfo(keypassPathName + " extracting urls ...");
                for (SecrectCheck secrectCheck : kdbxRead(dbRootGroup, filter)) {
                    checksFutures.add(executorService.submit(secrectCheck));
                }
                main.notifyInfo(keypassPathName + " checking ... (" + checksFutures.size() + " urls)");
                //
                csvData.add("path,url,prio,result".split(","));
                for (Future<SecrectCheck> checkFuture : checksFutures) {
                    SecrectCheck check = checkFuture.get();
                    checks.add(check);
                    check.msg = check.msg.replace("\"", "'"); // KLUDGE ease for JS.GRID
                    if (!check.msg.startsWith(" " + SecretHandlerHelper.HIDE) ){
                        csvData.add(new String[]{check.entry.getPath(), check.url.getUrl(), check.modified ? "" : ">", check.msg});
                    }
                    System.out.println("      " + check.msgContext + check.msg);
                }
                if (null != trgtFileName && !trgtFileName.isEmpty()) {
                    trgtFileName = trgtFileName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    System.out.println("      " + trgtFileName + " save check ...");
                    DataConCSV.insertCSV(trgtFileName, csvData);
                    DataConCSV.insertCSV(trgtFileName + "_" + ((new SimpleDateFormat("yyyyMMdd_HHmmss")).format(new Date())), csvData);
                }
                main.notifyInfo(keypassPathName + " checked");

                executorService.shutdown();

            } catch (InterruptedException | ExecutionException | IOException e) {
                main.notifyError(keypassPathName + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        return csvData;
    }

    public void setPwdOonce(String pwd) {
        if (null == this.pwd || this.pwd.isEmpty()) {
            this.pwd = pwd;  // this.pwd will be reseted if invalid ...
        } else {
            System.out.println("      " + "Kdbx-PWD already set - ignore setPwdOnce");
        }
    }

    public UrlHelper getKdbxCreds4Login(UrlHelper urlFilter) {
        String key = urlFilter.getUrlConnectionId();
        UrlHelper url = loginUrlCache.get(key);
        if (null == url) {

            url = getKdbxCreds4LoginInternal( urlFilter, new File(keypassShortPathName).exists() ? keypassShortPathName :  keypassPathName  );
            loginUrlCache.put( key, url);
        }
        return url;
    }

    private UrlHelper getKdbxCreds4LoginInternal(UrlHelper urlFilter, String keypassPathName) {
        UrlHelper urlHelper = null;
        if (null == pwd || pwd.isEmpty()) {
            System.out.println("      " + "getKdbxCreds - please set pwd first ...");
        } else {

            KdbxCreds creds = new KdbxCreds(pwd.getBytes());

                /*
                        jdbc:postgresql://{USERNAME}:{PASSWORD}@{Title}:5432/ipim
                        sftp://{USERNAME}:{PASSWORD}@{Title}:7387
                        https://{USERNAME}:{PASSWORD}@{Title}/REST
                        https://{Title}/iPIM/rest/api/clients/


                 */

            ExecutorService executorService = Executors.newFixedThreadPool(30);
            List<Future<SecrectCheck>> checksFutures = new ArrayList<>();
            ArrayList<SecrectCheck> checks = new ArrayList<>();

            //InputStream inputStream = main.class.getClassLoader().getResourceAsStream("data/2023_iPIM_QRY.kdbx");
            try {
                System.out.println("      " + keypassPathName + " opening ...");
                InputStream inputStream = new FileInputStream(new File(keypassPathName));
                DomDatabaseWrapper db = DomDatabaseWrapper.load(creds, inputStream);
                DomGroupWrapper group = db.getRootGroup();

                System.out.println("      " + keypassPathName + " extracting urls ...");
                scanKdbxCreds4Login(group, keypassPathName);
                urlHelper = getKdbxCreds4Login(group, urlFilter, keypassPathName);
                System.out.println("      " + keypassPathName + " checked");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return urlHelper;
    }

    @Deprecated
    public UrlHelper getKdbxCreds4Login(DomGroupWrapper group, UrlHelper urlFilter, String keypassPathName) {
        // String loginPrefix = urlFilter.getConnectionId().replaceFirst("[ _-].*", "").toLowerCase(Locale.ROOT);
        urlFilter.setScheme("jdbc");
        urlFilter.setUserPwd(""); // kdbx-password should not be part of search pattern
        String keyPattern = "";
        keyPattern = urlFilter.getConnectionId().toLowerCase(Locale.ROOT).replaceAll("[_ ]", ".*");
        System.out.println("      " + keyPattern);
        keyPattern = (keyPattern + ".*" + urlFilter.getUrl()).replaceAll("(integ|int)",".*").replaceAll("([:@/])", ".*$1");
        System.out.println("      " + keyPattern);
        keyPattern = ".*" + keyPattern + ".*";
        System.out.println("      " + keyPattern);
        UrlHelper url = null;
        for (DomEntryWrapper entry : group.getEntries()) {
            //System.out.println(entry.getPath() + "        " + entry.getTitle() + " " + entry.getUrl() );
            String id = (group.getPath() + '_' + entry.getTitle()).replace(" ", "_").toLowerCase(Locale.ROOT);
            String urlFilled = entry.getUrl().replace("{USERNAME}", entry.getUsername()).replace("{Title}", entry.getTitle());
            System.out.println("      " + entry.getPath() + "        " + entry.getTitle() + " " + urlFilled + " (" + id + ") ");
            if ( (id + ":" + urlFilled).matches( keyPattern) ) {
                System.out.println("      " + entry.getPath() + "        " + entry.getTitle() + " " + urlFilled + " (" + id + ") .. matched");
                //String msg = "    " + entry.getPath() + "        " + entry.getUsername() + " ";
                // UrlHelper url = new UrlHelper(urlFilled.replace("{PASSWORD}", entry.getPassword()));
                url = new UrlHelper(urlFilled);
                url.setUserName(entry.getUsername());
                url.setUserPwd(entry.getPassword());

                System.out.println("DONE  " + keypassPathName + " checked");
                break;
            }
        }
        if (null == url) {
            for (DomGroupWrapper childGroup : group.getGroups()) {
                if (null == url && childGroup.getName().toLowerCase().contains( urlFilter.getConnectionId().toLowerCase(Locale.ROOT).substring(0,2))) {
                    url = getKdbxCreds4Login(childGroup, urlFilter, keypassPathName);
                }
            }
        }
        return url;
    }

    /*

        using KeyPass-Cache ...

     */

    static HashMap<String, UrlHelper> logins = new HashMap<String, UrlHelper>();

    public void scanKdbxCreds4Login(DomGroupWrapper group, String keypassPathName) {
        // String loginPrefix = urlFilter.getConnectionId().replaceFirst("[ _-].*", "").toLowerCase(Locale.ROOT);
        UrlHelper url = null;
        for (DomEntryWrapper entry : group.getEntries()) {
            //System.out.println(entry.getPath() + "        " + entry.getTitle() + " " + entry.getUrl() );
            String id = (group.getPath() + '_' + entry.getTitle()).replace(" ", "_").toLowerCase(Locale.ROOT);
            String urlFilled = entry.getUrl().replace("{USERNAME}", entry.getUsername()).replace("{Title}", entry.getTitle());
            System.out.println("      " + entry.getPath() + "        " + entry.getTitle() + " " + urlFilled + " (" + id + ") ");
            url = new UrlHelper(urlFilled);
            url.setUserName(entry.getUsername());
            url.setUserPwd(entry.getPassword());
            String key = id + ":" + urlFilled;
            if (logins.containsKey(key)) {
                System.out.println("SKIPP " + key+ " (duplicate key)");
            } else {
                logins.put(key, url);
            }
        }
        for (DomGroupWrapper childGroup : group.getGroups()) {
            main.notify("CHECK  " + childGroup.getPath() + "   " + childGroup.getName());
            scanKdbxCreds4Login(childGroup, keypassPathName);
        }
        System.out.println("      " + keypassPathName + " checked");
    }

    public UrlHelper getKdbxCreds4LoginCached(UrlHelper urlFilter) {
        // String loginPrefix = urlFilter.getConnectionId().replaceFirst("[ _-].*", "").toLowerCase(Locale.ROOT);
        urlFilter.setScheme("jdbc");
        urlFilter.setUserPwd(""); // kdbx-password should not be part of search pattern
        String keyPattern = "";
        keyPattern = urlFilter.getConnectionId().toLowerCase(Locale.ROOT).replaceAll("[_ ]", ".*");
        System.out.println("      " + keyPattern);
        keyPattern = (keyPattern + ".*" + urlFilter.getUrl()).replaceAll("(integ|int)",".*").replaceAll("([:@/])", ".*$1");
        System.out.println("      " + keyPattern);
        keyPattern = ".*" + keyPattern + ".*";
        System.out.println("      " + keyPattern);
        UrlHelper url = null;
        // sort keySet
        List<String> keyList = new ArrayList<>(logins.keySet());
        Collections.sort(keyList);
        for (String key : keyList) {
            if (key.matches(keyPattern)) {
                url = logins.get(key);
                System.out.println("      " + key + " .. matched");
                try {
                    SecrectCheck call = new SecrectCheck(url, null).call();
                    if (!call.successful) {
                        main.notifyError(key + " failed");
                        url = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    main.notifyError( key + "  " + e.getMessage());
                    url = null;
                }
                if (null != url) {
                    main.notifyWarn(key + " successfull");
                    break;
                }
            }
        }
        return url;
    }


}

