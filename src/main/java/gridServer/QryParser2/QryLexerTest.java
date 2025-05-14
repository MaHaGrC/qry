package gridServer.QryParser2;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

class QryLexerTest {

    QryLexer qryLexer = new QryLexer();
    List<QryLexer.Token> token = null;

    String tokenize(String text) {
        token = qryLexer.tokenize(text);
        String toString = token.toString();
        System.out.println( toString);
        return toString;
    }

    @Test
    void tokenize() {
        qryLexer.tokenize("");
        token = qryLexer.tokenize("word");
        System.out.println(token.toString());
    }

    @Test
    void parseMulti() {
        assertEquals( "[[0..4] WORD 'word', [4] SPACE ' ', [5] WORD 'w']", tokenize("word w") );
    }


    @Test
    void parseMultiQuote() {
        // assertEquals( "[[0..4] WORD 'word', [4..5] QUOTE ''', [5..6] WORD 'w', [6..7] QUOTE ''']", parse("word'w'"));
        assertEquals("[[0..4] WORD 'word', [4..7] WORD_QUOTE1 ''w'']", tokenize("word'w'"));
    }

    @Test
    void parseBlock() {
        assertEquals( "[[0..4] WORD 'word', [4] OPERATION '+', [5] BLOCK_START '[', [6] WORD 'w', [7] BLOCK_END ']']", tokenize("word+[w]") );
    }

    @Test
    void parseDelim() {
        assertEquals( "[[0..2] WORD 'a9', [2] DELIMITER ',', [3..5] WORD 'b9']", tokenize("a9,b9"));
    }

    @Test
    void parseHint() {
        assertEquals( "[[0..2] WORD 'a9', [2] SPACE ' ', [3..18] HINT '/*HINT do so */', [18] SPACE ' ', [19..21] WORD 'la']", tokenize("a9 /*HINT do so */ la"));
    }

    @Test
    void parseLongSQL() {
        assertEquals( "[[0..2] WORD 'a9', [2] SPACE ' ', [3..9] WORD 'having', [9] SPACE ' ', [10..15] " +
                        "WORD 'count', [15] BRACE_START '(', [16] ASTERIKS '*', [17] BRACE_END ')', [18] COMPERATOR '>', [19] NUMBER '1', [20] SPACE ' ', [21..36] " +
                        "HINT '/*HINT do so */', [36] SPACE ' ', [37..39] WORD 'la']"
                , tokenize("a9 having count(*)>1 /*HINT do so */ la"));
    }



    @Test
    void parseWordWithASTERIKS() {

        assertEquals( "[[0..3] WORD 'a.*']"
                , tokenize("a.*"));

        assertEquals( "[[0] BLOCK_START '[', [1..10] WORD 'productNo', [10] DELIMITER ',', [11..14] WORD 'a.*', [14] BLOCK_END ']', [15] WORD 'a']"
                , tokenize("[productNo,a.*]a"));
    }


}