package gridServer;

import gridServer.QryParser2.Block;
import gridServer.QryParser2.BlockTest;
import org.rapidoid.http.Req;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Map;

public class QryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String LOADING = "Status" + System.lineSeparator() + "currently loading ...";

    String data = "";
    String hint = null ;
    String doc = null ;
    String executedStmt = null;

    String login = null;

    Date lastAccessDate = null;
    Date firstDate = null;
    Date date = null;



    String version = null;
    private int rows = 0;
    private long executionTime = 0;
    private String targetTable = null;
    private Block qryStmtBlock = null;

    private String errorMsg;


    public QryResponse() {
        this.data = null;
    }

    public QryResponse(Req req) {
        data = null; // force to null - allow ERROR vs EMPTY-Result
        login = main.getLogin(req.params());
    }

    public QryResponse(Map<String, String> params) {
        data = null; // force to null - allow ERROR vs EMPTY-Result
        login = main.getLogin(params);
    }

    public QryResponse(String data, String stmt) {
        this.data = data;
        this.executedStmt = stmt;
    }

    public QryResponse(Map<String, String> params, String data) {
        this.data = data;
        login = main.getLogin(params);
    }



    @Deprecated
    public void append(String s) {
        if (null == data) {
            data = "";
        }
        data += s;
    }

    public void appendRow(String[] cols) {
        String row = "";
        for (String s : cols) {
            row +=  (null == s ? "" : s.replaceAll("([\\;\"])", "\\\\$1").replace(System.lineSeparator(),"\\n")) + ";";
        }
        appendRow(row);
    }

    public void appendRow(String row) {
        if (null == data) {
            data = "";
        }
        data += row + System.lineSeparator();
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getExecutedStmt() {
        return executedStmt;
    }

    public void setExecutedStmt(String executedStmt) {
        this.executedStmt = executedStmt;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDate(FileTime fileTime) {
        this.date = new Date(fileTime.toMillis());
    }

    public Date getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(Date firstDate) {
        this.firstDate = firstDate;
    }

    public void setFirstDate(FileTime fileTime) {
        this.firstDate = new Date(fileTime.toMillis());
    }


    public Date getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(Date lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    public void setLastAccessDate(FileTime fileTime) {
        this.lastAccessDate = new Date(fileTime.toMillis());
    }


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public QryResponse setEnvIndependent(){
        login = null;
        return this;
    }

    public QryResponse setEnvIndependent(boolean independent){
        if (independent) {
            login = null;
        };
        return this;
    }
    public boolean isEnvIndependent(){
        return null == login;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRowCount(int rows) {
        this.rows = rows;
    }

    public void setExecutionTime(long l) {
        this.executionTime = l;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setTargetTable(String value) {
        this.targetTable = value;
    }

    public double getRowCount() {
        return rows;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setQryStmtBlock(Block block) {
        this.qryStmtBlock = block;
    }

    public Block getQryStmtBlock() {
        return qryStmtBlock;
    }


    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}
