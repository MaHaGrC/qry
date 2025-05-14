package gridServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DataConSecretHandler implements DataConnector{

    String fileName = "url.check";

    @Override
    public boolean matches(String query, Map<String, String> params) {
        return query.matches("url.check") || query.startsWith("url.check");
    }

    @Override
    public QryResponse run(String query, Map<String, String> params, QryResponse qryResponse) {

        String result = "";
        SecretHandler secretHandler = new SecretHandler();
        // Filter by
        //  - Name
        //  - Title
        //  - UserName@Title
        //  - UserName@group.Name
        //  - Name_Title  == id
        //  - part of path - group.Name
        //  - full path - /group.Path/  << starts/ends with /
        // List<String> filter = Arrays.asList("ipimimport@3pg20new.ipim.nmop.de", "ipim_dev@3P Integ", "ipim-web@3P Integ", "osp@3p-integ-ipim.novomind.com");
        String filter = "";
        /*
        filter += "3P 3Pagen;";
        filter += "FD FocusDiscount;";
        filter += "IO ioniq;";
        filter += "_GLOBAL;";
         */
        //filter = "iAGENT INTEG UI";
        List<String> filters = Arrays.asList(filter.split(" *; *"));
        Collections.sort(filters);
        List<String[]> data = secretHandler.getKdbxCreds(query, filters, query.replaceFirst("url.check\\s*", "").replaceAll("\\\\\\*.*","").replaceAll("\\s+$",""));
        for (String[] row : data) {
            if (data.get(0).length>3){
                // skipp coloring on header ...
                String color = row[3].contains("Skipp") ? "grey"
                                : row[3].contains("WARN") ? "orange"
                                : row[3].contains("FAIL '4") ? "orange"
                                : row[3].contains("FAIL") ? row[3].contains("OK") ? "orange" : "red"
                                : row[3].contains("OK") ? "green"
                                : "orange" ;
                if (!row[2].isEmpty()) { // 1st connector color always
                    row[0] = "<div style='color: " + color + "'>" + row[0] + "</div>";
                    row[1] = "<a href='" + row[1] + "' style='color: " + color + "'>" + row[1] + "</a>";
                    row[2] = "<div style='color: " + color + "'>" + row[2] + "</div>";
                    row[3] = "<div style='color: " + color + "'>" + row[3] + "</div>";
                } else {  // fallback only url
                    row[0] = "<div style='color: " + "grey" + "'>" + row[0] + "</div>";
                    color = color.replace("red", "grey"); // assume fallbacks fail ,-(
                    row[1] = "<a href='" + row[1] + "' style='color: " + color + "'>" + row[1] + "</a>";
                    row[2] = "<div style='color: " + color + "'>" + row[2] + "</div>";
                    row[3] = "<div style='color: " + color + "'>" + row[3] + "</div>";
                }
            } else {
                // used for LOGIN-Error Msg
                // TODO new QryResponse().setErrorMsg();
            }
            result += "\"" + String.join("\",\"",row) + "\"" + System.lineSeparator();
        }
        qryResponse.setData(result);
        qryResponse.setEnvIndependent();
        //QryResponse qryResponse = new QryResponse(params, result).setEnvIndependent();
        if (!secretHandler.credentialsAvailable()){
            qryResponse.setErrorMsg("please Login first (credentials for keypass missing)");
            qryResponse.setHint("please Login first (credentials for keypass missing)");
        }
        return qryResponse;
    }

    @Override
    public boolean checkConnection(Map<String, String> params){
        return true;
    }


    @Override
    public boolean isMatchingUrlType(UrlHelper url) {
        return false;
    }


}
