package gridServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataConPostgresTest {

    private DataConPostgres dataConPostgres;
    private HashMap<String, String> params;
    private Map<String,String> readMock = new HashMap<>() {};

    class DataConPostgresMocked extends DataConPostgres{
        @Override
        public QryResponse read(String query, Map<String, String> params, boolean saveSyn) {
            return new QryResponse(readMock.get(query), "mocked");
        }
    }

    @BeforeEach
    void setUp() {
        dataConPostgres = new DataConPostgresMocked();
        params = new HashMap<>();
    }

    void matches(String query, String msg) {
        assertTrue( dataConPostgres.matches(query, params), "matches: " + msg);
    }

    @Test
    void matches() {
        matches("SELECT * FROM table","sql simple");
        matches("SELECT * \n\tFROM table","sql multiline");
        matches("SELECT rm.zeitstempel, rm.messwert_adapt\n" +
                "FROM wpa_skn_o1_schema.rohr_messwerte AS rm\n" +
                "JOIN wpa_skn_o1_schema.rohr AS r ON rm.rohr_id = r.rohr_id\n" +
                "JOIN wpa_skn_o1_schema.bauteil AS bt ON r.bauteil_id = bt.bauteil_id\n" +
                "JOIN wpa_skn_o1_schema.baugruppe AS bg ON bt.baugruppe_id = bg.baugruppe_id\n" +
                "JOIN wpa_skn_o1_schema.betrachtungssystemgruppe_zu_baugruppe AS bsg_zu_bg ON bg.baugruppe_id = bsg_zu_bg.baugruppe_id\n" +
                "JOIN wpa_skn_o1_schema.betrachtungssystemgruppe_zu_fw_hast AS bsg_zu_hast ON bsg_zu_bg.betrachtungssystemgruppe_id = bsg_zu_hast.betrachtungssystemgruppe_id\n" +
                "WHERE\n" +
                "\tbsg_zu_hast.fw_hast_id = 7765\n" +
                "\tAND rm.rohr_code_titel = 'v_id'\n" +
                "ORDER BY rm.zeitstempel", "complex");

        readMock.put("select from aTable limit 1", "some data");
        matches("aTable","sql qry");

        readMock.put("select from aSchema.aTable limit 1", "some data");
        matches("aSchema.aTable ","sql qry with schema");
    }

    void notMatches(String query, String msg) {
        assertFalse( dataConPostgres.matches(query, params), "notMatches: " + msg);
    }

    @Test
    void notMaches(){
        notMatches("unknownTable","unknown table");
    }



}