package gridServer;

import java.util.Map;

public interface DataConnector {

    boolean matches(String query, Map<String, String> params);

    QryResponse run(String qry, Map<String, String> params, QryResponse qryResponse);

    public boolean checkConnection(Map<String, String> params);

    public boolean isMatchingUrlType( UrlHelper url);


}
