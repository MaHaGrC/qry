package gridServer;

import com.google.common.html.HtmlEscapers;
import com.jcraft.jsch.*;
import org.rapidoid.commons.Str;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataConSSH implements DataConnector{


    private static Session conn;
    private  static UrlHelper urlLast;

    private static final String cmds = "ls|pwd|cd|cat|echo|touch|rm|mv|cp|mkdir|chmod|chown|chgrp|ln|find|grep|sed|awk|sort|uniq|wc|head|tail|cut|tr|diff|patch|tar|zip|unzip|gzip|gunzip|bzip2|bunzip2|7z|rar|unrar|lsof|ps|kill|top|df|du|free|mount|umount|ifconfig|ping|traceroute|netstat|ss|telnet|ssh|scp|sftp|rsync|curl|wget|nc|nmap|whois|dig|host|nslookup|route|iptables|firewall-cmd|semanage|setenforce|getenforce|systemctl|journalctl|crontab|at|systemd|service|chkconfig|init|runlevel|reboot|shutdown|poweroff|halt|passwd|useradd|userdel|usermod|groupadd|groupdel|groupmod|chage|chpasswd|chsh|su|sudo|visudo|passwd|id|who|w|last|finger|uptime|date|cal|time|timedatectl|hwclock|ntpdate|ntpd|systemd-timesyncd|systemd-timedated";

    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches("^(/|~|~/|./).*|^("+cmds+")\\b.*") ; //  just start with "./"-Prefix to use ssh    || query.matches("^[^\\s]*\\.(log|txt|csv|tsv|json|xml|html|htm|js|css|java|py|sh|bat)(\\s.*)?$");
    }


    // jdbc:postgresql://{host}[:{port}]/[{database}]

    private static boolean driverForced = false;

    Notifier notifier = new Notifier(this.getClass().getName());

    Map<String, Boolean> check = new HashMap<>();

    public DataConSSH() {
        if (!driverForced) {
        }
    }

    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return url.getScheme().contains("ssh");
    }


    public boolean checkConnection(Map<String, String> params){
        String ping = read("pwd", params).getData();
        if (null == ping) ping = "";
        System.out.println("DataConSSH.ping: " + (ping.isEmpty() ? "FAIL" : "valid"));
        return !ping.isEmpty();
    }

    void checkConnection(UrlHelper url) throws SQLException {
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        Connection conn = null;
        conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() );

    }


    Session getConnection(UrlHelper url){
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");

        if (null == conn || !url.equals(urlLast)) {
            urlLast = url;
            if (null != conn) {
                conn.disconnect();
            }
            try  {
                JSch jsch = new JSch();
                //conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() );
                //conn = jsch.getSession(url.getUserName(), url.getHost(), url.getPort());
                //conn.setPassword(url.getUserPwd());
                conn = jsch.getSession("osp", "192.168.56.114" , 22);
                conn.setPassword("osp");
                conn.setConfig("StrictHostKeyChecking", "no");
                conn.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                conn.connect();
                System.out.println("JSch Connected");
            } catch (JSchException e) {
                main.notifyError(e.getMessage());
                System.out.println(e.getMessage());
            }
        }
        return conn;
    }



    public String escapeData(String data){
        // ensure Delimiter and Line-Brakes (Row-Delimiter) are unique
        // pure \t -> Uncaught (in promise) SyntaxError: JSON.parse: bad character in string literal at line 1 column 651 of the JSON data
        data = data.replace("\t","\\t").replace(System.lineSeparator(),"\\n").replaceAll("(\\\\|;)","\\\\$1");
        if (data.contains("\"")||data.contains("\\")) {
            data = "\""+data.replace("\"","\\\"")+"\"";
        }
        return data;
    }


    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

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

        return data;
    }


    public QryResponse read(String query, Map<String, String> params) {
        QryResponse qryResponse = read(query, params, false);
        return qryResponse;

    }

    public QryResponse read(String query, Map<String, String> params, boolean saveSyn) {
        String sql = "";
        StringBuffer stringBuffer = new StringBuffer();
        QryResponse data = new QryResponse(params);
        UrlHelper url = main.getConnection( params);
        url.setDefaultPort(22); // necessary?
        //UrlHelper url = new UrlHelper("jdbc:postgresql://hb:hbhb@192.168.56.114:5432/hb");
        // hints already extracted
        String limitStr = (params.get("qry")+" "+params.get("hints")).replaceFirst("(?s).*[ \"{]?limit\"?:?\\s*\"?(\\d+).*|.*", "$1");
        int lineLimit = 25;
        try {
            lineLimit = (int)Long.parseLong(limitStr);
        } catch (NumberFormatException e) {
            main.notifyWarn("limit not a number: " + limitStr);
        }
        try {
            Channel channel = getConnection(url).openChannel("exec");
            channel.setInputStream(null);
            InputStream in=channel.getInputStream();
            ((ChannelExec)channel).setErrStream(System.err);
            //
            String cmd = expand(query, params);
            main.notifyInfo("DataConSSH w HINT: " + cmd);
            data.setExecutedStmt(cmd);
            // hide comment ... ???
            cmd = cmd.replaceAll("\\s*#?\\s*/\\*\\{\\s*\".*",""); // remove "expanded" hints, but leave hints in QryResponse to inject HINT for GUI
            main.notifyInfo("DataConSSH       : " + cmd);
            ((ChannelExec)channel).setCommand(cmd);
            channel.connect();
            //
            byte[] tmp=new byte[1024];
            boolean run = true;
            int countLines = 0;
            while(run){
                while(in.available()>0 && run){
                    int i=in.read(tmp, 0, 1024);
                    if(i<0) break;
                    String s = new String(tmp, 0, i);
                    int pos = 0;
                    while ((pos = s.indexOf("\n", pos) + 1) != 0) {
                        countLines++;
                        if (countLines > lineLimit) {
                            run = false;
                            s=s.substring(0, pos);
                        }
                    }
                    s = untab(s, 8 , " "); // convert tabs to spaces
                    stringBuffer.append(s);
                    System.out.print(s);
                }
                if(channel.isClosed() || !run){
                    System.out.println("exit-status: "+channel.getExitStatus());
                    break;
                }
                try{Thread.sleep(1000);
                }catch(Exception ee)
                {}
            }
            main.notifyInfo( stringBuffer.length() + " char(s) received" );
            channel.disconnect();
            System.out.println("DONE");
        } catch (JSchException | IOException e) {
            data.setErrorMsg( e.getMessage());
            notifier.error(e.getMessage());
            System.out.println("ERROR caught: ");
            e.printStackTrace();
        }

        // replace pattern Http://... with <a href="http://...">http://...</a>
        // use Pattern.compile("(?i)http://[^ ]*") and loop through the matches

        String[] lines = stringBuffer.toString().split("\r\n|\r|\n");
        if (logFileToHtml(data, lines, query)) {
            // do nothing
        } else if (lsLongToHtml(data, lines, query)) {
            //
        } else {
            shellColoredToHtml(data, lines);
        }

        return data;
    }

    private static boolean logFileToHtml(QryResponse data, String[] lines, String query) {
        String row1 = lines.length>0 ? lines[0] : "";
        // 2024-09-02 10:47:25,786 INFO  [com.novomind.ipim.core.util.scheduler.jobs.HealthCheckJob] (DefaultQuartzScheduler_Worker-8) HealthCheck - HealthCheckJob / Quartz: Done
        // lines log-line row1 to columns
        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (\\w+)\\s+\\[(.*)\\] \\((.*)\\) (.*)$");

        Matcher matcher = pattern.matcher(row1);
        String msg = null;
        if (matcher.matches() && !query.contains(" RAW") && (null == data.getHint() || !data.getHint().contains("RAW"))) {

            main.notifyInfo("Log-File detected");
            data.appendRow(new String[]{"date", "level", "logger", "thread", "msg"});
            String date = "";
            String level = "";
            String logger = "";
            String thread = "";
            for (String s : lines) {
                matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    date = matcher.group(1);
                    level = matcher.group(2);
                    logger = matcher.group(3);
                    thread = matcher.group(4);
                    msg = matcher.group(5);
                } else {
                    msg = s;
                }
                data.appendRow(new String[]{date, level, logger, thread, msg});
            }
        }
        return msg != null;
    }

    private static boolean lsLongToHtml(QryResponse data, String[] lines, String query) {
        String row1 = lines.length>0 ? lines[0] : "";
        String row2 = lines.length>1 ? lines[1] : "";
        // total 51300
        // -rwxrwxrwx.  1 osp  osp       108 May  8  2023 tail_errors.sh
        // -rwxrwxrwx.  1 osp  osp       108 May  8  2023 checkspace.sh -> /home/osp/checkspace.sh
        // separate link from target

        // split ls lines
        String msg = null;
        if (row1.matches("^total \\d+$")) {
            Pattern pattern = Pattern.compile("^([ld-])([r-][w-][x-]){3}\\.\\s+(\\d+)\\s+(\\w+)\\s+(\\w+)\\s+(\\d+)\\s+(\\w{3}\\s+\\d{1,2}\\s+\\d{4})\\s+((.*) -> (.*)|.*)$");
            Matcher matcher = pattern.matcher(row2);
            if (matcher.matches() && !query.contains(" RAW") && (null == data.getHint() || !data.getHint().contains("RAW"))) {

                main.notifyInfo("ls-long detected");
                data.appendRow(new String[]{"type", "rights", "links", "owner", "group", "size", "date", "name", "target"});
                msg = "";
                String fileType = "";
                String rights = "";
                String links = "";
                String owner = "";
                String group = "";
                String size = "";
                String date = "";
                String name = "";
                String name_src = "";
                String name_trg = "";
                String link = "";
                String name_linked = "";
                for (String s : lines) {
                    matcher = pattern.matcher(s);
                    if (matcher.matches()) {
                        fileType = matcher.group(1);
                        rights = matcher.group(2);
                        links = matcher.group(3);
                        owner = matcher.group(4);
                        group = matcher.group(5);
                        size = matcher.group(6);
                        date = matcher.group(7);
                        name_src = matcher.group(9);
                        name = null != name_src ? name_src : matcher.group(8);
                        name_trg = null == matcher.group(10) ? "" : matcher.group(10);
                        link = name.startsWith("~") || name.startsWith("/") || name.startsWith(". ") ? name : "~/" + name;
                        // markdown [text](url)
                        name_linked = "["+name+"]("+link+")"; // match grid markdown syntax: [text](url)
                    } else {
                        name_linked = s;
                    }
                    data.appendRow(new String[]{fileType, rights, links, owner, group, size, date,  name_linked, name_trg});
                }
                data.setHint("/*{\"valHndl\":{\"name\":\"LINK\"}}*/");
            }
        }
        return msg != null;
    }


    private static void shellColoredToHtml(QryResponse data, String[] split) {
        data.appendRow(new String[] {"cmd"});
        for (String string : split) {

            // ensure data alignement ... ????
            // string = string.replaceAll("\\t", "    ");

            string = HtmlEscapers.htmlEscaper().escape(string);
            string = string.replaceAll(" ", "&nbsp;"); // do not modify HTML-Elements!!

            // java convert bash color to html color
            //string = string.replaceAll("\u001B\\[0;31m", "<span style=\"color: red\">");
            string = string.replaceAll("\u001B\\[31m([^\u001B]*)", "<span style=\"color: red\">$1</span>");
            string = string.replaceAll("\u001B\\[32m([^\u001B]*)", "<span style=\"color: green\">$1</span>");
            string = string.replaceAll("\u001B\\[33m([^\u001B]*)", "<span style=\"color: yellow\">$1</span>");
            string = string.replaceAll("\u001B\\[34m([^\u001B]*)", "<span style=\"color: blue\">$1</span>");
            string = string.replaceAll("\u001B\\[35m([^\u001B]*)", "<span style=\"color: purple\">$1</span>");
            string = string.replaceAll("\u001B\\[36m([^\u001B]*)", "<span style=\"color: cyan\">$1</span>");
            string = string.replaceAll("\u001B\\[37m([^\u001B]*)", "<span style=\"color: white\">$1</span>");
            string = string.replaceAll("\u001B\\[91m([^\u001B]*)", "<span style=\"color: red\">$1</span>");
            string = string.replaceAll("\u001B\\[93m([^\u001B]*)", "<span style=\"color: orange\">$1</span>"); // instead of yellow - to see it better
            //java remove color from bash output
            string = string.replaceAll("\u001B\\[[;\\d]*m", "");    // remove color KLUDGE -> convert to HTML-color?

            data.appendRow( new String[] {string} ); // does the escaping
        }
    }


    // check https://stackoverflow.com/questions/68588981/replace-t-tab-in-string-with-number-of-spaces-tab-contains-in-java
    // but not suitable for multi-char substition
    public static String untab(String s, int tabSize, String substitute) {
        StringBuilder sb = new StringBuilder();
        if (!s.contains("\t")) {
            return s;
        } else if (tabSize <= 0) {
            return s.replace("\t", "");
        } else {
            String[] lines = s.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String[] split = line.split("\t");
                for (int ii = 0; ii < split.length-1; ii++) {
                    String part = split[ii];
                    sb.append(part);
                    int l = part.contains("\u001B") ? part.replaceAll("\u001B\\[[;\\d]*m", "").length() : part.length();
                    sb.append(substitute.repeat(tabSize - (l % tabSize)));
                }
                sb.append(split[split.length-1]);
                if (i < lines.length -1 || s.endsWith("\n")) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    String  expand(String query, Map<String, String> params) {
        if (query.matches("^[~.]?/[^. ]*$") || query.equals("~")|| query.startsWith("~ ") /*allow hints ...*/
                        ) {
            // ensure complete filename with path to link
            QryHintHelper.checkAndExtractHints(params);
            //
            String query_wo_hints = params.get("qry_wo_hints");
            if (null != query_wo_hints && !query_wo_hints.isEmpty()){
                query = query_wo_hints;
            }
            // extract comment from bash query, separate by "#"
            String query_cmd_comment = "";
            String[] parts = query.split("#", 2);
            if (parts.length > 1) {
                query = parts[0].trim();
                query_cmd_comment = "#" + parts[1];
            }
            // unable to work around hints here ... as we inject hints ...
            query = "ls -d " + query +"/* " + query_cmd_comment  + "   /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/"; //     ls /tmp/* {"valHndl":{"cmd":"LINK"}}
        }
        if (query.contains(".sh" ))  {
            // keep it and assume execution
        } else if (query.contains(".") && !query.contains(" ") ) {
            query = "cat " + query;
        }
        return query;
    }



}
