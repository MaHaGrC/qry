package gridServer;

import org.junit.jupiter.api.Test;

class SecretHandlerTest {
    String keypassPwd = "tst";
    String keypassFilePath = "data/secrets_qry_tst.kdbx";

    @Test
    void importSecret(){
        SecretHandler secretHandler = new SecretHandler();
        UrlHelper urlHelper = new UrlHelper("");
        secretHandler.setPwdOonce(keypassPwd);
        UrlHelper urlHelperCredentials = secretHandler.getKdbxCreds4LoginInternal(urlHelper, keypassFilePath);
    }

    @Test
    void initOrUpdateSecret(){
        SecretHandler secretHandler = new SecretHandler();
        secretHandler.createKeyStorage( keypassFilePath, keypassPwd);
    }

    @Test
    void rereadSecret(){
        SecretHandler secretHandler = new SecretHandler();
        secretHandler.setPwdOonce(keypassPwd);
        UrlHelper urlHelperCredentials = secretHandler.getKdbxCreds4LoginInternal(new UrlHelper("coolDB::http://sample_user3:sample_password3@example.com"), keypassFilePath);
    }


}