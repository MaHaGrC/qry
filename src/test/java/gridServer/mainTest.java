package gridServer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class mainTest {

    Map<String, String> params;

    @Test
    void data2Result() {
        // {  "data": "\"domain\",\"topic\",\"issue\",\"date\"\n\"a\",\"b\",\"c\",\"d\"\n" , "login": "", "date": "2024/03/08 12:55:09", "firstDate": "2024/03/01 08:55:24", "lastAccessDate": "2024/03/08 17:15:49"}

        String csv = "\"domain\",\"topic\",\"issue\",\"date\"\n" +
                "\"a\",\"b\",\"c\",\"d\"\n";

        //assertEquals( "{\"data\":\"\\\"domain\\\",\\\"topic\\\",\\\"issue\\\",\\\"date\\\"\\n\\\"a\\\",\\\"b\\\",\\\"c\\\",\\\"d\\\"\\n\" }", main.data2Result_old(new QryResponse(csv)) );
        assertEquals( "{\"data\":\"\\\"domain\\\",\\\"topic\\\",\\\"issue\\\",\\\"date\\\"\\n\\\"a\\\",\\\"b\\\",\\\"c\\\",\\\"d\\\"\\n\",\"login\":\"\"}", main.data2Result(new QryResponse(params, csv)) );


        // {  "data": "\"domain\",\"topic\",\"issue\",\"date\"\n\"a\",\"b\",\"c\",\"d\"\n\"a\",\"b2\",\"c2\",\"d2\"\n\"a3\",\"b3\",\"c33335\",\"d3\"\n\"a3\",\"b3\",\"c4\",\"d47789\"\n\"dom-4\",\"topic-4\",\"issue-4\",\"date-45\"\n" , "login": "", "date": "2024/03/08 12:55:09", "firstDate": "2024/03/01 08:55:24", "lastAccessDate": "2024/03/08 17:15:49"}
        csv = "\"domain\",\"topic\",\"issue\",\"date\"\n" +
                "\"a\",\"b\",\"c\",\"d\"\n" +
                "\"a\",\"b2\",\"c2\",\"d2\"\n" +
                "\"a3\",\"b3\",\"c33335\",\"d3\"\n" +
                "\"a3\",\"b3\",\"c4\",\"d47789\"\n" +
                "\"dom-4\",\"topic-4\",\"issue-4\",\"date-45\"\n";

        assertEquals( "{\"data\":\"\\\"domain\\\",\\\"topic\\\",\\\"issue\\\",\\\"date\\\"\\n\\\"a\\\",\\\"b\\\",\\\"c\\\",\\\"d\\\"\\n\\\"a\\\",\\\"b2\\\",\\\"c2\\\",\\\"d2\\\"\\n\\\"a3\\\",\\\"b3\\\",\\\"c33335\\\",\\\"d3\\\"\\n\\\"a3\\\",\\\"b3\\\",\\\"c4\\\",\\\"d47789\\\"\\n\\\"dom-4\\\",\\\"topic-4\\\",\\\"issue-4\\\",\\\"date-45\\\"\\n\",\"login\":\"\"}"
                    ,  main.data2Result(new QryResponse(params, csv)));

    }
}