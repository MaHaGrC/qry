package gridServer;

import org.rapidoid.http.Req;

import java.util.Map;

public class QryHintHelper {

    public static boolean checkAndExtractHint(Req req, String hint){
        return checkAndExtractHint( req.params(), hint);
    }

    public static boolean checkAndExtractHint(Map<String, String> params, String hint) {
        String query = params.get("qry");
        if (query.contains("%20")) {
            main.notifyWarn("check encoding: " + query);
            query = query.replace("%20"+hint, " "+hint).replace(hint +"%20", hint+" "); // TODO check how cant that happen?
        }
        boolean hintFound = query.contains(" " + hint + " ") || query.endsWith(" " + hint )  ;
        String hints = params.get("hints");
        params.put("hints", null == hints ? hint : hints + ", " + hint);
        query = query.replaceFirst("(?s)^\\s*"+hint+"\\s*|\\s+"+hint+"\\s*$|\\s+"+hint+"(\\s)\\s*","$1");
        params.put("qry", query); // ganz am Anfang, ganz am Ende
        return hintFound;
    }

    public static void checkAndExtractHints(Map<String, String> params) {
        String query = params.get("qry");
        String hints = params.get("hints");
        if (null == query) {
            query = "";
        }
        if (null == hints) {
            hints = "";
        }
        String hints_new = query.replaceAll("(?s).*(\\/\\*\\{.*)|.*", "$1");
        if (!hints_new.isEmpty()) {
            String query_wo_hints = query.replace(hints_new, ""); // remove spaces !!!
            query_wo_hints = query_wo_hints.trim();
            hints_new = hints_new.replaceFirst("\\s+", "");
            params.put("hints", hints + hints_new);
            params.put("qry_wo_hints", query_wo_hints);
        } else {
            params.put("qry_wo_hints", query);
        }
    }

}
