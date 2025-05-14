package gridServer;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.rapidoid.http.Req;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataMerge {

    static Notifier notifier = new Notifier(DataMerge.class.getName());

    public static QryResponse get(Req req) {
        QryResponse qryResponse = Cache.get(req);
        // Hyphothese
        //   Postgres-Tabellen involviert -> wenn Tabelle nicht gefunden, dann meint es andere Datenquelle, kann diese abgefragt werden kommt eine "Tabelle" raus, dann kann diese Tabelle in Postgress eingespielt werden und normal verwendet werden
        //   keine Postgress-Tabellen -> wenn Daten für alle Quellen gefunden werden, dann lassen diese sich in Postgres-DB einspielen und als Tabelle abgefragen...
        if (null == qryResponse.getData()) {
            main.notifyWarn("DataMerge: try to merge data ... ");
            qryResponse = mergeDataSources(req);
            if (null == qryResponse || null == qryResponse.getData() || qryResponse.getData().isEmpty()){
                main.notifyError("DataMerge: data merge FAILED");
            } else {
                main.notifyWarn("DataMerge: data merge SUCCESSFUL");
            }
        }
        return qryResponse;
    }

    public static QryResponse mergeDataSources( Req req ) {
        // TOOD mus evaluate errors during execution -> refactor notification via QryResponse/Session
        QryResponse csvSrcQryResponse = null;
        Map<String, String> params = req.params();
        String qry = params.get("qry");
        if (null != qry && !qry.isEmpty()) {
            List<String> qry_ = List.of(qry.replaceAll("\\s.*", "").replace("][", "").replaceAll("\\[[^\\]]+\\]", "[]").split("\\[\\]")); // TODO use QrySyntax here ... -> or keep Syntax at QryResponse-Object
            // TODO imp -  can currently only handle csv in postgres ...
            List<String> qryCsv = qry_.stream().filter(src -> src.endsWith(".csv")).collect(Collectors.toList());
            if (qry_.size()>1 && !qryCsv.isEmpty()) {
                int i = 0;
                for (String csvSrc : qryCsv) {
                    i = i +1;
                    csvSrcQryResponse = Cache.createQryResponse(csvSrc, params,  new QryResponse());
                    if (null == csvSrcQryResponse || null==csvSrcQryResponse.getData() || csvSrcQryResponse.getData().isEmpty()) {
                        main.notifyWarn("DataMerge: no data to source for merge(" + csvSrc + ")");
                        csvSrcQryResponse = null;
                        break;
                    }
                    // upload
                    String csvSrcTab = csvSrc.replaceFirst("\\..*", "");
                    String qryTab = "qry_" + i ; // allow mutiple csv in one query // ensure not to overwrite something !!!
                    uploadFileToTable(qryTab, params, csvSrcQryResponse.getData());
                    // replace csv with table in query
                    // TODO should introduce ALIAS
                    qry = qry.replace( csvSrc + "[]", qryTab + "[id][" + csvSrcTab + "]" ); // a.csv[]b.csv --> qry_1[id][a]qry_2
                    qry = qry.replace( csvSrc, qryTab );
                }
                if ( null != csvSrcQryResponse) {
                    if (!params.containsKey("qry_orig")) {
                        params.put("qry_orig",params.get("qry"));
                    }
                    req.params().put("qry",qry +" UPDATE "); // enforce UPDATE - is it was removed from cache at first
                    csvSrcQryResponse = Cache.get(req );
                }
            } // csv[]

        } // qry
        return csvSrcQryResponse;
    }

    private static void uploadFileToTable(String tab, Map<String, String> params, String data) {
        UrlHelper url = main.getConnection(params);
        url.setDefaultPort(5432); // necessary?

        try (Connection conn = DriverManager.getConnection(url.getStringWoUser(), url.getUserName(), url.getUserPwd() ) ) {
            // need to create table ...
            Statement s = conn.createStatement();
            if (tab.startsWith("qry_")) {
                // create matching table ...
                s.execute("drop table if exists " + tab);
                List<String> header = List.of(data.replaceFirst("(?s)[\\r\\n].*", ";").replaceFirst(";$",";DUMMY").split(";"));
                // Postgres needs matching of types ...  to match article.id it musst integer be served
                // TODO keep data as data not as string to ease analyses here ...
                // String colDef = String.join(" VARCHAR(1024), ",header) + " VARCHAR(1024)";
                String colDef = " " + String.join(" _X_, ",header) + " _X_";
                colDef = colDef.replaceAll("( article )_X_|(id)_X_", "$1$2 bigint"); // need to behave like iPIM-Postgress-Definition and there is article an id referring to article.id
                colDef = colDef.replace("_X_", "VARCHAR(1024)");
                s.execute("create table " + tab + " ( " +  colDef + " )");
            }
            // do the copy
            CopyManager copyManager = new CopyManager((BaseConnection) conn);
            //
            // --> if first line ends with ";"
            //
            //      ERROR FEHLER: zusätzliche Daten nach letzter erwarteter Spalte
            //          Wobei: COPY qry_1, Zeile 1: »00ebab6c-812b-46df-87fa-a504e7190ca4;«
            //
            //
            //      Caused by: org.postgresql.util.PSQLException: FEHLER: doppelter Schl�sselwert verletzt Unique-Constraint �pg_type_typname_nsp_index�
            //          Detail: Schl�ssel �(typname, typnamespace)=(qry_1, 2200)� existiert bereits.
            //
            copyManager.copyIn("COPY " + tab + " FROM STDIN (DELIMITER ';',FORMAT csv, ENCODING 'UTF8')", new ByteArrayInputStream(data.replaceFirst(".*[\\r\\n]+","").getBytes())); // TODO improve .. / remove header
        } catch (SQLException e) {
            notifier.error(e,"Check File-Encoding, col-count header vs content, all header cols named");
            main.notifyError( e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            main.notifyError( e.getMessage());
            throw new RuntimeException(e);
        }

    }
/* TODO

org.postgresql.util.PSQLException: FEHLER: doppelter Schl�sselwert verletzt Unique-Constraint �pg_type_typname_nsp_index�
  Detail: Schl�ssel �(typname, typnamespace)=(qry_1, 2200)� existiert bereits.
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2676)
	at gridServer.DataMerge.uploadFileToTable(DataMerge.java:96)
	at gridServer.DataMerge.mergeDataSources(DataMerge.java:60)
	at gridServer.DataMerge.get(DataMerge.java:28)
	at gridServer.main$4.execute(main.java:569)
	at gridServer.main$4.execute(main.java:524)
java.lang.RuntimeException: org.postgresql.util.PSQLException: FEHLER: doppelter Schl�sselwert verletzt Unique-Constraint �pg_type_typname_nsp_index�
  Detail: Schl�ssel �(typname, typnamespace)=(qry_1, 2200)� existiert bereits.
	at gridServer.DataMerge.uploadFileToTable(DataMerge.java:110)
	at gridServer.DataMerge.mergeDataSources(DataMerge.java:60)
	at gridServer.DataMerge.get(DataMerge.java:28)
	at gridServer.main$4.execute(main.java:569)
	at gridServer.main$4.execute(main.java:524)
	at org.rapidoid.http.handler.optimized.DelegatingParamsAwareReqRespHandler.handleReq(DelegatingParamsAwareReqRespHandler.java:49)
	at org.rapidoid.http.handler.AbstractHttpHandlerDecorator.handleReqAndPostProcess(AbstractHttpHandlerDecorator.java:48)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator$3.invokeNext(HttpManagedHandlerDecorator.java:161)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator$3.invoke(HttpManagedHandlerDecorator.java:121)
	at org.rapidoid.http.handler.HttpAuthWrapper.wrap(HttpAuthWrapper.java:70)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator.wrap(HttpManagedHandlerDecorator.java:185)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator.handleWithWrappers(HttpManagedHandlerDecorator.java:100)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator.access$200(HttpManagedHandlerDecorator.java:39)
	at org.rapidoid.http.handler.HttpManagedHandlerDecorator$2.run(HttpManagedHandlerDecorator.java:83)
	at org.rapidoid.job.PredefinedContextJobWrapper.run(PredefinedContextJobWrapper.java:56)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1589)
Caused by: org.postgresql.util.PSQLException: FEHLER: doppelter Schl�sselwert verletzt Unique-Constraint �pg_type_typname_nsp_index�
  Detail: Schl�ssel �(typname, typnamespace)=(qry_1, 2200)� existiert bereits.
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2676)
	at gridServer.DataMerge.uploadFileToTable(DataMerge.java:96)
	at gridServer.DataMerge.mergeDataSources(DataMerge.java:60)
	at gridServer.DataMerge.get(DataMerge.java:28)
	at gridServer.main$4.execute(main.java:569)
	at gridServer.main$4.execute(main.java:524)




 */

}
