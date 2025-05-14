package gridServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataConPropertyCheckerTest {

    List<String> data = new ArrayList<>();
    List<String> dataOut = new ArrayList<>();
    Map<String, String> props = new HashMap<>();

    @BeforeEach
    void setUp() {
        data = new ArrayList<>();
        props = new HashMap<>();
    }

    @AfterEach
    void tearDown() {
    }

    List<String> process(Map<String, String> props, List<String> data) {
        return DataConPropertyChecker.process( props, data);
    }

    private void process(String s, String key, String expected, String msg) {
        System.out.println("test: \"" + msg + "\" ...");
        props.clear();
        data = List.of(s.split("\n"));
        dataOut = process( props, data );
        String val = props.get(key);
        System.out.println( "      ==> " + props.toString());
        assertEquals( expected, val, msg);
    }

    @Test
    void processDirect() {
        process("x=1", "x","1", "direct set");
        process("x = 1", "x","1", "direct with spaces");
        process(" x = 1", "x","1", "direct with indent");
        process("   x = 1", "x","1", "direct with indent");
        process("   x = 1 ", "x","1", "direct skipp trailing space");
    }

    @Test
    void processOverwrite() {
        process("x=1\nx=2", "x","2", "re-set");
    }

    @Test
    void processifndef() {
        process(" ifndef x \n  x=2", "x","2", "ifndef by indent");
        process("x=1 \n ifndef x \n  x=2", "x","1", "ifndef negative");
        process("x=1 \n ifndef x \n x=2", "x","2", "ifndef reset by indent");
    }

    @Test
    void processifdef() {
        process(" ifdef x \n  x=2", "x",null, "ifdef ");
        process(" ifdef x \n x=2", "x","2", "ifndef reset by indent");
        process("x=1 \n ifdef x \n  x=2", "x","2", "ifdef positive");
        process(" ifdef x \n  x=2 \n else \n  x=3 ", "x","3", "ifdef else by indent");
        process(" ifdef x \n  x=2 \n endif \n  x=3 ", "x","3", "ifdef end by indent");
    }


    @Test
    void processSingleOption() {
        process(" x < \n  >> 1 \n  > 2", "x", "1", "singleOption with default");
        process(" x < \n  > 1 \n  > 2", "x", null, "singleOption w/o default");
        process(" x < \n y = 2", "x", null, "singleOption w/o options");
        process(" x < \n  >> 1 \n  >> 2", "x", "2", "singleOption 2 default - faulty state!");
    }

    @Test
    void processMultiOption() {
        process(" x ,< \n  >> 1 \n  > 2 \n  >> 3", "x", "1,3", "multiOption with 2 x default");
        process(" x ,< \n  >> 1 \n  > 2", "x", "1", "multiOption with default");
        process(" x ,< \n  > 1 \n  > 2", "x", null, "multiOption w/o default");
        process(" x ,< \n y = 2", "x", null, "multiOption w/o options");
        process(" x |< \n  >> 1 \n  > 2 \n  >> 3", "x", "1|3", "multiOption with alternative delimiter");
    }

    @Test
    void processComment() {
        process("# comment \n # x ,< \n # x = 1 \n # ifndef x  \n ", "x", null, "just comment row");
        process("# comment \n x=1 # comment", "x","1", "comment direct set");
        process("# comment \n x < # comment \n  >> 1 # comment \n  > 2 # comment ", "x", "1", "comment singleOption with default");
        process("# comment \n ifndef x # comment \n  x=2 # comment ", "x","2", "comment ifndef by indent");
        process("# comment \n ifdef x  # comment \n  x=2 # comment ", "x",null, "comment ifdef ");
        process("# comment \n x ,<  # comment \n  >> 1  # comment \n  > 2  # comment \n  >> 3 # comment ", "x", "1,3", "comment multiOption with 2 x default");
    }

    @Test
    void processSubst() {
        process("x=1 \n y=%{x}", "y", "1", "subst value by prop");
        process("x=1 \n y.%{x}=2", "y.1", "2", "subst key_part by prop");
    }

    @Test
    void processMultiply() {
        process("x.{y|z}=1", "x.y", "1", "multiply key");
        process("x.{y|z}=1", "x.z", "1", "multiply key2");
        process("x.{y|z}.{a|b}=1", "x.z.b", "1", "multiply key2x2");
        assertEquals("x.{y|z}.{a|b}=1 \n   x.y.a = 1 \n   x.y.b = 1 \n   x.z.a = 1 \n   x.z.b = 1", String.join(" \n ", dataOut));
    }


}