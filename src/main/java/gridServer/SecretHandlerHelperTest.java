package gridServer;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.util.UriEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecretHandlerHelperTest {

    @Test
    void getSecret() {
        // create SFtpWrapper
        SecretHandlerHelper helper = new SecretHandlerHelper();
        System.out.println("createSFtpWrapper 1");
        helper.createSFtpWrapper("ipimexport", "<:@", "fd20.ipim.priv.nmop.de", 7387).close();

        // sftp://ipimexport:uywq0UgQizm_nmW:eIBB@fd20.ipim.priv.nmop.de:7387
        // sftp://ipimexport:uywq0UgQizm_nmW%3AeIBB@fd20.ipim.priv.nmop.de:7387
    }

    @Test
    void getEncode() {
        // create SFtpWrapper
        SecretHandlerHelper helper = new SecretHandlerHelper();
        System.out.println("createSFtpWrapper 1" + UriEncoder.encode("<:@"));


        // sftp://ipimexport:uywq0UgQizm_nmW:eIBB@fd20.ipim.priv.nmop.de:7387
        // sftp://ipimexport:uywq0UgQizm_nmW%3AeIBB@fd20.ipim.priv.nmop.de:7387
    }

}