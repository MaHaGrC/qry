package gridServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataConSSHTest {

    private static DataConSSH dataConSSH;
    private static Map<String, String> params = new java.util.HashMap<>();

    @BeforeAll
    static void setUp() {
        dataConSSH = new DataConSSH();
    }

    void expandTest(String query, String expected, String msg) {
        params.clear();
        params.put("qry", query);
        dataConSSH.expand(query, params);
        assertTrue(dataConSSH.matches(query, params), "matches: " + msg);
        assertEquals(expected, dataConSSH.expand(query, params), msg);
    }

    @Test
    void expand() {
        expandTest( "ls", "ls", "base cmd" );
        expandTest( "ls /tmp", "ls /tmp", "base cmd rel" );
        expandTest( "cd /tmp; ls", "cd /tmp; ls", "cd rel" );
    }

    @Test
    void expandFolder() {
        //
        //expandTest( "/tmp", "ls /tmp", "folder" );
        expandTest( "/tmp", "ls -d /tmp/*    /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/", "folder" );
        expandTest( "~", "ls -d ~/*    /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/", "folder" );
        //expandTest( "", "ls -d ~/*    /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/", "folder" );
        expandTest( "~ #/*{\"limit\":\"100\"}*/", "ls -d ~/* #   /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/", "folder with bash hint" );
        expandTest( "~ /*{\"limit\":\"100\"}*/", "ls -d ~/*    /*{\"valHndl\":{\"cmd\":\"LINK\"}}*/", "folder with hint" );
    }

    @Test
    void expandFile() {
        //
        expandTest( "~/db.log", "cat ~/db.log", "file with base" );
        expandTest( "~/db.sh", "~/db.sh", "file to execute" );
    }

    //@Test
    void test_todo() {
        expandTest( "db.log", "cat db.log", "file" );
    }



    @Test
    void untab() {
        assertEquals("abc", DataConSSH.untab("a\tb\tc", 8, ""));
        //                     123456781234567812345678
        assertEquals("a       b", DataConSSH.untab("a\tb", 8, " "));
        assertEquals("a.      b", DataConSSH.untab("a.\tb", 8, " "));
        assertEquals("a..     b", DataConSSH.untab("a..\tb", 8, " "));
        assertEquals("a       b       c", DataConSSH.untab("a\tb\tc", 8, " "));
        //
        assertEquals("ab\na       b", DataConSSH.untab("ab\na\tb", 8, " "));
        assertEquals("ab\na       b\n", DataConSSH.untab("ab\na\tb\n", 8, " "));
        //
        assertEquals("a\u001B[34m       b", DataConSSH.untab("a\u001B[34m\tb", 8, " "),"skipp color codes");
    }


    @Test
    void expandWhere() {
        //
        expandTest( "~/db.sh", "~/db.sh", "file to execute" );
        // TODO expandTest( "~/db.log W content", "cat ~/db.log", "file with base" );
    }


}