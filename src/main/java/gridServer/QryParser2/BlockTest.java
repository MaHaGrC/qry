package gridServer.QryParser2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gridServer.QryParser2.QryLexer.TokenDefinition.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockTest {


    private Block block = null;
    private Block blockResult = null;

    @Test
    void parseCatchAll() {

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[]d");

        Block block = Block.createBlock("unparseable => '.*'");
        block = block.parse(tokens);
        assertEquals("unparseable", block.toString());
        assertEquals("unparseable: {tokens: [WORD, BLOCK_START, BLOCK_END, WORD]}", block.toString(false));
        assertEquals("unparseable: {tokens: [[0] WORD 'a', [1] BLOCK_START '[', [2] BLOCK_END ']', [3] WORD 'd']}", block.toString(true));

    }

    @Test
    void parseTableName() {

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[]d");
        Block.registerDefaultBlocks();
        Block block = Block.getBlock("WORD", "TableName");
        block = block.parse(tokens);
        assertEquals("TableName", block.toString());
        assertEquals("TableName: {tokens: [WORD]}", block.toString(false));
        assertEquals("TableName: {tokens: [[0] WORD 'a']}", block.toString(true));
    }


    @Test
    void parseQry() {

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[]d");
        //Block.createBlock("tab => WORD");
        Block.registerDefaultBlocks();
        Block block = Block.createBlock("" +
                "unparseable => '.*'\n" +
                "Qry => WORD:tab? unparseable");
        block = block.clone().parse(tokens);
        assertEquals("Qry: {tab, unparseable}", block.toString());
        assertEquals("Qry: {tab: {tokens: [WORD]}, unparseable: {tokens: [BLOCK_START, BLOCK_END, WORD]}}", block.toString(false));
        assertEquals("Qry: {tab: {tokens: [[0] WORD 'a']}, unparseable: {tokens: [[1] BLOCK_START '[', [2] BLOCK_END ']', [3] WORD 'd']}}", block.toString(true));

        tokens = lexer.tokenize("[]d");
        block = block.clone().parse(tokens);
        assertEquals("Qry: {unparseable}", block.toString());
        assertEquals("Qry: {unparseable: {tokens: [[0] BLOCK_START '[', [1] BLOCK_END ']', [2] WORD 'd']}}", block.toString(true));

    }




    //@Test
    void parseDyn(){

        //TODO try to make it obsolete
        // Block.registerBlock(new BlockQry());

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[]d");
        Block block = Block.getBlock("Qry");
        block = block.parse(tokens);
        assertEquals("Qry: {tab, unparseable: {}}", block.toString());

        // register
        assertNull(Block.getBlock("alias"));
        Block.registerBlock( new BlockToken( "alias", WORD, "as" ) );
        tokens = lexer.tokenize("as ");
        // get
        block = Block.getBlock("alias");
        assertNotNull(block);
        block = block.parse(tokens);
        assertEquals("alias", block.toString());


        // combine


    }


    /*



        stmt -> src_tab |  src_tab | unparseable
        stmt_qry -> stmt_join | trgTab
        stmt_join -> srcTab join_Tab
        join_Tab -> join stmt_qry

        trgTab -> joinTab | trgTab





     */




    Block testDynBlock(String stmt, String[] blockNameAndDefs, String expectedDef, String expectedParsed){
        List<QryLexer.Token> tokens = QryLexer.instance.tokenize(stmt);
        block = null;
        for (String blockNameAndDef : blockNameAndDefs) {
            block = Block.createBlock(blockNameAndDef); // might overwrite existing definitions
        }
        if (null != expectedDef) {
            assertEquals(expectedDef, block.toString(true));
        }
        blockResult = block.clone().parse(tokens); // ensure to keep original block clean
        if (null != expectedParsed) {
            if (null == blockResult) {
                System.out.println("------------------------- ASSERT-HELPER -------------------------");
                System.out.println(block.toString(true, true, true, true));
                System.out.println(showBlock(block));
            }
            assertNotNull(blockResult, "stmt does ot match block definitions");
            if (!expectedParsed.equals("*")) {
                assertEquals(expectedParsed, blockResult.toString(true));
            }
        } else {
            if (null != blockResult) {
                System.out.println("------------------------- ASSERT-HELPER -------------------------");
                System.out.println(blockResult.toString(true, true, true, true));
                System.out.println(showBlock(blockResult));
            }
            //assertNull(blockResult, "stmt does not match block definitions - as expected");
        }
        return blockResult;
    }


    @Test
    void createBlockTest_toFIX() {

        Block.registerDefaultBlocks();
        testDynBlock(" aWord"
                , new String[]{"A => SPACE WORD:src"}, "A"
                , "A: {SPACE: {tokens: [[0] SPACE ' ']}, src: {tokens: [[1..6] WORD 'aWord']}}");
    }


    @Test
    void createBlockTest(){


        testDynBlock("aWord"
                , new String[]{"WORD"}, "WORD"
                , "WORD: {tokens: [[0..5] WORD 'aWord']}");

        Block.registerDefaultBlocks();

        testDynBlock(" aWord"
                , new String[]{"A => SPACE WORD:src"}, "A"
                , "A: {SPACE: {tokens: [[0] SPACE ' ']}, src: {tokens: [[1..6] WORD 'aWord']}}");

        testDynBlock("aWord"
                , new String[]{"A => SPACE WORD:src"}, "A"
                , null);






    }

    static String showBlock(Block block, List<String> blockIdxLines, List<String> blockLines) {
        //
        if (null == block) {
            return "";
        }
        for (Block block1 : block.blocks) {
            String s = showBlock(block1, blockIdxLines, blockLines);
            //System.out.println("-----");
            //System.out.println(s);
        }
        //
        if (null == block.tokenDef || !block.name.equals(block.tokenDef.name())){ // skipp basic token-dummys ... as they listed as Token already
            if (null != block.getFirstToken()) {

                int blockStart = block.getFirstToken().start;
                int blockEnd = block.getLastToken().end;
                String name = block.name;
                // create gap, but occupy space for hierarchy
                int len = blockEnd - blockStart - 2;
                if (len - name.length() > 2) {
                    int l = (len - name.length()) ;
                    name = ".".repeat( l - l/2) + name + ".".repeat(l/2);
                }
                // skipp basic token-dummys
                insertText(blockLines, name, blockStart + (blockEnd - blockStart) / 2);
                //
                insertText(blockIdxLines, 0, "~".repeat(blockEnd - blockStart), blockStart,1);

            }
        }
        return String.join("\n", blockLines);
    }


    static String showBlock(Block block, List<String> stmtLines, List<String> tokenIdxLines, List<String> tokenLines, List<String> blockLines){
        /*
            String:  " aTable [ aCol ]  aTable2 "
            Token:     ---1-- 2 -3-- 4  ---5---
            Block:

            Block:  {tab: {tokens: [[1..7] WORD 'aTable'], [9..14] WORD 'aCol'], [16..23] WORD 'aTable2']}

         */
        if (null == block) {
            return "";
        }
        System.out.println(block.toString());
        if (!block.tokens.isEmpty()) {

            int len = block.tokens.get( block.tokens.size() - 1).end;
            String def = " ".repeat(len);
            String stmtLine =  stmtLines.isEmpty() ? def : stmtLines.get(0);
            int idx = 0;
            int first = block.tokens.get(0).tokenDefinition.equals(SPACE) ? 0 : 1;
            for (QryLexer.Token token : block.tokens) {
                idx++;
                // replace content of stmtLine with token value
                stmtLine = stmtLine.substring(0, token.start) + token.value + stmtLine.substring(token.end);
                if (first == idx % 2) {
                    // replace middle of sPos-String with idx modulo 10
                    String sTokenIdx = "-".repeat(token.value.length()/2) + (idx % 10) + "-".repeat(token.value.length() - 1 - token.value.length()/2);
                    insertText(tokenIdxLines, 0, sTokenIdx, token.start,1);
                }
                insertText(tokenLines, token.tokenDefinition.name(), (token.start + token.end) / 2);
                System.out.println( String.format("%3d %3d %15s %s", token.start, token.end, token.tokenDefinition.name(), token.value));
            }

            //
            stmtLines.clear(); // replace with new content
            stmtLines.add(stmtLine); // need to have list to return a string ...
        }
        //

        //
        return block.toString();
    }

    static String showBlock(Block block, List<String> lines){
        List<String> tokenLines = new ArrayList<>();
        List<String> tokenIdxLines = new ArrayList<>();
        List<String> blockIdxLines = new ArrayList<>();
        List<String> blockLines = new ArrayList<>();
        List<String> stmtLine = new ArrayList<>();
        showBlock(block, stmtLine, tokenIdxLines, tokenLines, blockLines);
        showBlock(block, blockIdxLines, blockLines);
        lines.clear();
        lines.addAll(stmtLine);
        lines.addAll(tokenIdxLines);
        lines.addAll(tokenLines);
        lines.addAll(blockIdxLines);
        lines.addAll(blockLines);
        // align to longest line
        int lenMax = lines.stream().mapToInt(String::length).max().orElse(0);
        // extend lines to lenMax with spaces
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, lines.get(i) + " ".repeat(lenMax - lines.get(i).length()));
        }
        return String.join("\n", lines);
    }

    static boolean insertText( List<String> lines, int idx , String text, int posM, int orientation){
        // insert text into line at start, replacing - only if spaces are replaced
        int posS = posM - (text.length() / 2)*( 1 - orientation); // start position - to -1 left, 0 center, 1 right
        // allow orientation = 0 = middle to slightly shift right for first word
        // "WORD...." instead of "`-WORD..." as it should start at Position 0
        if (posS < 0 && posS + text.length() > 0){
            posS = 0;
        }
        if (posS >= 0) {
            int posE = posS + text.length();
            // extract chars between posS-posE from line
            String line = idx < lines.size() ? lines.get(idx) : "";
            // ensure gap between words ...
            boolean gap = !text.equals("|");
            int posS_ = gap && 0 < posS ? posS - 1 : posS;
            int posE_ = gap && posE < line.length() - 1 ? posE + 1 : posE;
            String sub = posE_ < line.length() ? line.substring(posS_, posE_) : posS_ < line.length() ? line.substring(posS_) : "";
            if (sub.trim().isEmpty() && (posS >= 0 )) {
                for (int i = lines.size(); i <= idx; i++) {
                    lines.add("");
                }
                if (line.length() < posE) {
                    line = line + " ".repeat(posE - line.length());
                }
                lines.set(idx, line.substring(0, posS) + text + line.substring(posE));
                return true;
            }
        }
        return false;
    }

    /*
      ADD test for insertText( List<String> lines, int idx , String text, int posM, int orientation)
     */
    @Test
    void insertTextTest(){
        List<String> lines = new ArrayList<>();
        assertEquals(true, insertText( lines , 0, "a", 0, 0));
        assertEquals( "a", String.join("\n", lines) );

        assertEquals(true, insertText( lines , 0, "b", 2, 0)); // other position, behind end
        assertEquals( "a b", String.join("\n", lines) );

        assertEquals(false, insertText( lines , 0, "c", 2, 0)); // same position - keep
        assertEquals( "a b", String.join("\n", lines) );


    }



    static boolean insertText( List<String> lines, String text, int pos) {
        // search for line with space for text, starting from pos

        boolean inserted = false;
        for (int i = 0; i < lines.size(); i++) {
            /*
                ? insert text better to left (safe space) or right (readability)
             */
            if (/*insertText(lines, i, text + "-´", pos, -1)
                    ||*/ insertText(lines, i, text, pos, 0)
                    || ( !text.startsWith(".") && ( /* "."-indicates it should show position - so try to stay position-correct */
                               insertText(lines, i, text + "-´", pos, -1)
                            || insertText(lines, i, "`-" + text, pos, 1)
                        )
                    )) {
                inserted = true;
                break;
            } else if (!text.startsWith(".")) {
                insertText(lines, i, "|", pos, 0);
            }
        }
        if (!inserted) {
            if (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) {
                // break recursion
                throw new RuntimeException("no space for text found");
            }
            lines.add("");
            insertText(lines, text, pos); // retry with empty line
        }
        return true;
    }


    @Test
    void insertText2Test(){
        List<String> lines = new ArrayList<>();

        assertEquals(true,insertText( lines , "a", 0));
        assertEquals( "a", String.join("\n", lines) );

        assertEquals(true,insertText( lines , "b", 2));
        assertEquals( "a b", String.join("\n", lines) );

        assertEquals(true,insertText( lines , "b", 2));
        assertEquals( "a b\n  b", String.join("\n", lines) );


        lines.clear();
        assertEquals(true, insertText( lines , "a", 0 ));
        assertEquals(true, insertText( lines , "b", 1 ), "no gap");
        assertEquals( "a|\n b", String.join("\n", lines) );

    }


    public static String showBlock(Block block){
        List<String> lines = new ArrayList<>();
        showBlock(block, lines);
        return String.join("\n", lines);
    }

    String[] testShowBlock(String stmt, String[] blockNameAndDefs, String[] expectedShowBlockLines) {
        return testShowBlock(stmt, blockNameAndDefs, expectedShowBlockLines, false);
    }

    String[] testShowBlock(String stmt, String[] blockNameAndDefs, String[] expectedShowBlockLines, boolean  show){
        Block block = testDynBlock(stmt , blockNameAndDefs , null,  (null != expectedShowBlockLines && expectedShowBlockLines.length > 0 ? "*" : null));
        this.block = block;
        if (null != expectedShowBlockLines && expectedShowBlockLines.length > 0) {
            assertNotNull(block, "stmt does not match block definitions");
        }
        String showBlock = showBlock(block);
        if (show) {
            System.out.println(showBlock);
        }
        String[] showBlockLines = showBlock.split("\n");

        if (null != expectedShowBlockLines) {
            for (String expectedShowBlock : expectedShowBlockLines) {

                if (null == expectedShowBlock) {
                    // individual check
                } else if (expectedShowBlock.contains("\n")) {
                    assertEquals(expectedShowBlock, showBlock);
                } else if (expectedShowBlock.trim().startsWith("-") || expectedShowBlock.trim().startsWith("1") || expectedShowBlock.trim().startsWith("2")) {
                    // is a posLine - it might start with token-idx number too (it may start with 2 if leading space exists)
                    assertEquals(expectedShowBlock.trim(), showBlockLines[1].trim());
                } else if (!showBlock.contains(expectedShowBlock)){
                    assertEquals(expectedShowBlock, showBlock, "expectedShowBlock not found in showBlock"); // tricky - enforce both strings to be compared to ease analysis
                } else {
                    assertTrue(showBlock.contains(expectedShowBlock), expectedShowBlock + " not found in " + showBlock);
                }

            }
        }
        return showBlockLines;
    }



    void assertShowBlock(String stmt, String[] vals){
        String[] split = stmt.split("\n");
        // iterate over lines
        for (int i = 0; i < split.length || i < vals.length; i++) {
            assertEquals( i < split.length ? split[i] : "", i < vals.length ? vals[i] : "");
        }
    }

    @Test
    void testShowBlockBase() {
        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock("a"
                , new String[]{"WORD"}
                , new String[]{"1"});

        testShowBlock(" a"
                , new String[]{"A => SPACE WORD"}
                , new String[]{" 2"});

        testShowBlock("aTable"
                , new String[]{"WORD"}
                , new String[]{"---1--"});


        testShowBlock(" aTable aCol aTable2 "
                , new String[]{"A => SPACE WORD SPACE WORD SPACE WORD"}
                , new String[]{" ---2-- --4- ---6---   "});


    }

    String[] testShowBlock_OUT(String stmt, String[] blockNameAndDefs, String[] expectedShowBlock){
        String[] strings = testShowBlock(stmt, blockNameAndDefs, expectedShowBlock, true);
        return strings;
    }


    @Test
    void testShowBlock(){
        Block.registerDefaultBlocks();

        String[] vals = testShowBlock("aTable[aCol]aTable2"
                , new String[]{"A => WORD:tab BLOCK_START WORD BLOCK_END WORD"}
                , new String[]{"---1-- --3- ---5---"});
        System.out.println(String.join("\n", vals));
        assertShowBlock(
                "aTable[aCol]aTable2\n" +
                "---1-- --3- ---5---\n" +
                " WORD `-BLOCK_START\n" +
                "       WORD| WORD  \n" +
                "       BLOCK_END   \n" +
                "~~~~~~             \n" +
                "  tab              \n" +
                " ........A........ "
                , vals);

        assertShowBlock(
                "aTable[aCol]aTable2\n" +
                        "---1-- --3- ---5---\n" +
                        " WORD `-BLOCK_START\n" +
                        "       WORD| WORD  \n" +
                        "       BLOCK_END   \n" +
                        "~~~~~~             \n" +
                        "  tab              \n" +
                        " ........A........ "
                , testShowBlock("aTable[aCol]aTable2"
                        , new String[]{
                                "A => WORD:tab BLOCK_START WORD BLOCK_END WORD"
                                ,
                            }
                        , null));




    }




    @Test
    void testBlockQuantifier() {
        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT(" aTable aCol", new String[]{"A => SPACE WORD", "B => A A:A2"}, new String[]{" ---2-- --4-  ", " ..A..  A2", " .....B...."});

        testShowBlock_OUT(" aTable aCol", new String[]{"A => SPACE WORD", "B => A+"}, new String[]{" ---2-- --4-  ", " ..A..   A", " .....B...."});
        testShowBlock_OUT(" aTable aCol", new String[]{"A => SPACE WORD", "B => A*"}, new String[]{" ---2-- --4-  ", " ..A..   A", " .....B...."});
        testShowBlock_OUT(" aTable aCol", new String[]{"A => SPACE WORD", "B => A A?"}, new String[]{" ---2-- --4-  ", " ..A..   A", " .....B...."});

    }


    @Test
    void testBlockOr() {
        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT("aTable", new String[]{"A => SPACE | WORD"}, new String[]{"---1--", "WORD", " ..A."});
        testShowBlock_OUT("aTable", new String[]{"A => WORD | SPACE"}, new String[]{"---1--", "WORD", " ..A."}); // independent of order

        testShowBlock_OUT(" aTable", new String[]{"A => WORD | SPACE WORD"}, new String[]{" ---2--", "WORD", " ..A.", "A_1_"}); // nested block

    }


    @Test
    void testBlockQry() {
        Block.registerDefaultBlocks(); // get Default Blocks
        // first extract HINT (typically from END) --> will ease the extraction of unparsable content
        // then extract Stmt
        testShowBlock_OUT("/*+HINT: aTable */", new String[]{"Hint => HINT"}, new String[]{"---------1--------", "HINT", "Hint"});

        testShowBlock_OUT("word /*+HINT: aTable */"
                    , new String[]{
                        "Hint => HINT"
                        ,"A => WORD SPACE"
                        , "stmt => Hint A"  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                                            //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                }
                    , new String[]{"--1- ---------3--------"
                      , "word /*+HINT: aTable */\n" +
                        "--1- ---------3--------\n" +
                        "WORD|       HINT       \n" +
                        "  SPACE                \n" +
                        "     ~~~~~~~~~~~~~~~~~~\n" +
                        "  A   ......Hint...... \n" +
                        " .........stmt........ "});


    }

    String testShowBlock_OUT(String def, String inputAndExpectedStr){
        System.out.println();
        System.out.println( "S T A R T :      \""  + (null == inputAndExpectedStr ? "" : inputAndExpectedStr.split("\\n")[0]) + "\"");
        System.out.println();
        String[] defs = def.split("\n");
        List<String> inputAndExpected = new ArrayList<>(Arrays.asList(inputAndExpectedStr.split("\n")));
        String input = inputAndExpected.remove(0);
        String[] inputAndExpected_ = inputAndExpected.toArray(new String[0]);
        String[] x = testShowBlock_OUT(input, defs, inputAndExpected_);
        return x.length > 0 ? x[x.length-1] : "";
    }

    @Test
    void testBlockQryComplex() {
        Block.registerDefaultBlocks(); // get Default Blocks
        // first extract HINT (typically from END) --> will ease the extraction of unparsable content
        // then extract Stmt

        testShowBlock_OUT(
                "Hint => HINT \n" +
                        "StmtQry => WORD:tabSrc BLOCK_START BLOCK_END WORD:tabTrg \n" +
                        "Stmt => Hint StmtQry SPACE* "  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t1[]t2[]t3 /*+HINT: aTable */\n" +
                        "tabSrc      ......Hint...... \n" +
                        "  tabTrg                     \n" +
                        "StmtQry                      \n" +
                        " ............Stmt...........");


        testShowBlock_OUT(
                "Hint => HINT \n" +
                        "JoinQry => WORD:tabSrc BLOCK_START BLOCK_END \n" +
                        "StmtQry => JoinQry* WORD:tabTrg \n" +
                        "Stmt => Hint StmtQry SPACE* "  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t1[]t2[]t3 /*+HINT: aTable */\n" +
                        "-1 3  5 -7 ---------9--------\n" +
                        "WORD `-WORD       HINT       \n" +
                        "BLOCK_START                  \n" +
                        "BLOCK_END||                  \n" +
                        " BLOCK_START                 \n" +
                        "   BLOCK_END                 \n" +
                        "       WORD                  \n" +
                        "        SPACE                \n" +
                        "~~  ~~  ~~ ~~~~~~~~~~~~~~~~~~\n" +
                        "tabSrc|  |  ......Hint...... \n" +
                        "JoinQry  `-tabTrg            \n" +
                        "  tabSrc                     \n" +
                        "   JoinQry                   \n" +
                        "  StmtQry                    \n" +
                        " ............Stmt........... ");

        assertEquals("Stmt: {Hint: {HINT}, StmtQry: {JoinQry: {tabSrc, BLOCK_START, BLOCK_END}, JoinQry: {tabSrc, BLOCK_START, BLOCK_END}, tabTrg}, SPACE}", block.toString());
        assertEquals("Stmt: {Hint, StmtQry: {JoinQry: {tabSrc}, JoinQry: {tabSrc}, tabTrg}}", block.toStringSimplified());
        assertEquals("[ Hint: { \"/*+HINT: aTable */\"}, StmtQry: { \"t1[]t2[]t3\", [ JoinQry: { \"t1[]\", tabSrc: { \"t1\"}}, JoinQry: { \"t2[]\", tabSrc: { \"t2\"}}, tabTrg: { \"t3\"} ]} ]", block.toStringNestedToken(block.blocks, false));
        assertEquals("t3", block.getBlocks("tabTrg").get(0).getValue());
        assertEquals("t3", block.getValue("tabTrg"));
        assertEquals("t1[]", block.getBlocks("JoinQry").get(0).getValue());
        assertEquals("t1", block.getBlocks("JoinQry").get(0).getBlocks("tabSrc").get(0).getValue());
    }

    @Test
    void testBlockQry_OR() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT(
                        "OR => WORD | DELIMITER  \n" +
                        "Stmt => OR*"  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t,a\n" +
                        "1 3 \n" );
        assertEquals("Stmt: {OR, OR, OR}", block.toString(true,true,true, false));
        assertEquals( "[ OR: { \"t\"}, OR: { \",\"}, OR: { \"a\"} ]", block.toStringNestedToken( block.blocks , false ) );


    }


    @Test
    void testBlockQry_CONST() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT(
                "del => ','\n" +
                "Stmt => WORD del WORD"  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t,a\n" +
                        "1 3 \n" );
        assertEquals("Stmt: {WORD: {tokens: [[0] WORD 't']}, del: {tokens: [[1] DELIMITER ',']}, WORD: {tokens: [[2] WORD 'a']}}", block.toString(true,true,true, true));
        assertEquals( "del: { \",\"}", block.toStringNestedToken( block.blocks , false ) );

        /*
        testShowBlock_OUT(
                "Stmt => WORD ',' WORD"  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t,a\n" +
                        "1 3 \n" );
        assertEquals("Stmt: {WORD: {tokens: [[0] WORD 't']}, del: {tokens: [[1] DELIMITER ',']}, WORD: {tokens: [[2] WORD 'a']}}", block.toString(true,true,true, true));
        assertEquals( "del: { \",\"}", block.toStringNestedToken( block.blocks , false ) );

        */

    }

    @Test
    void testBlockQryComplex_02() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT(
                "Hint => HINT \n" +
                        "JoinCols => WORD:tabSrc BLOCK_START WORD:colsSrc BLOCK_END BLOCK_START WORD:colsTrg BLOCK_END  \n" +
                        "JoinNat => WORD:tabSrc BLOCK_START BLOCK_END \n" +
                        "JoinQry => JoinCols | JoinNat \n" +
                        "StmtQry => JoinQry* WORD:tabTrg \n" +
                        "Stmt => Hint StmtQry SPACE* "  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t1[]t2[c2][c3]t3 /*+HINT: aTable */\n" +
                        "-1 3  5  7 -9 -1 ---------3--------\n" );
        assertEquals("Stmt: {Hint, StmtQry: {JoinQry: {JoinNat: {tabSrc: {tokens: [[0..2] WORD 't1']}}}, JoinQry: {JoinCols: {tabSrc: {tokens: [[4..6] WORD 't2']}, colsSrc: {tokens: [[7..9] WORD 'c2']}, colsTrg: {tokens: [[11..13] WORD 'c3']}}}, tabTrg: {tokens: [[14..16] WORD 't3']}}}", block.toString(true,true,true, false));
        assertEquals( "[ Hint: { \"/*+HINT: aTable */\"}, StmtQry: { \"t1[]t2[c2][c3]t3\", [ JoinQry: { \"t1[]\", JoinNat: { \"t1[]\", tabSrc: { \"t1\"}}}, JoinQry: { \"t2[c2][c3]\", JoinCols: { \"t2[c2][c3]\", [ tabSrc: { \"t2\"}, colsSrc: { \"c2\"}, colsTrg: { \"c3\"} ]}}, tabTrg: { \"t3\"} ]} ]", block.toStringNestedToken( block.blocks , false ) );


    }

    @Test
    void testBlockQryComplex_03() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT(
                "Hint => HINT \n" +
                        "JoinCols => WORD:tabSrc BLOCK_START WORD:colsSrc BLOCK_END BLOCK_START WORD:colsTrg BLOCK_END  \n" +
                        "JoinNat => WORD:tabSrc BLOCK_START BLOCK_END \n" +
                        "JoinQry => JoinCols | JoinNat \n" +
                        "StmtQry => JoinQry* WORD:tabTrg \n" +
                        "Stmt => Hint StmtQry SPACE* "  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t1[]t2[c2][c3]t3 /*+HINT: aTable */\n" +
                        "-1 3  5  7 -9 -1 ---------3--------\n" +
                        "WORD `-WORD `-WORD      HINT       \n" +
                        "BLOCK_START  `-BLOCK_END           \n" +
                        "BLOCK_END||  WORD                  \n" +
                        " BLOCK_START  SPACE                \n" +
                        "      WORD|                        \n" +
                        "     BLOCK_END                     \n" +
                        "     BLOCK_START                   \n" +
                        "~~  ~~ ~~  ~~ ~~ ~~~~~~~~~~~~~~~~~~\n" +
                        "tabSrc  `-colsSrc ......Hint...... \n" +
                        "JoinNat  colsTrg                   \n" +
                        "JoinQry  `-JoinCols                \n" +
                        "  tabSrc `-JoinQry                 \n" +
                        "            tabTrg                 \n" +
                        " ....StmtQry...                    \n" +
                        " ...............Stmt.............."
        );
        assertEquals("Stmt: {Hint, StmtQry: {JoinQry: {JoinNat: {tabSrc: {tokens: [[0..2] WORD 't1']}}}, JoinQry: {JoinCols: {tabSrc: {tokens: [[4..6] WORD 't2']}, colsSrc: {tokens: [[7..9] WORD 'c2']}, colsTrg: {tokens: [[11..13] WORD 'c3']}}}, tabTrg: {tokens: [[14..16] WORD 't3']}}}", block.toString(true,true,true, false));
        assertEquals( "[ Hint: { \"/*+HINT: aTable */\"}, StmtQry: { \"t1[]t2[c2][c3]t3\", [ JoinQry: { \"t1[]\", JoinNat: { \"t1[]\", tabSrc: { \"t1\"}}}, JoinQry: { \"t2[c2][c3]\", JoinCols: { \"t2[c2][c3]\", [ tabSrc: { \"t2\"}, colsSrc: { \"c2\"}, colsTrg: { \"c3\"} ]}}, tabTrg: { \"t3\"} ]} ]", block.toStringNestedToken( block.blocks , false ) );

    }


    @Test
    void test_Regexp() {
        Block.blockMap.clear();
        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT("" +
                        "WhereClause => '(.*?)(?= ORDER BY | order by | O | GROUP BY | group by | G | LIMIT | limit | l | /\\*|$)' \n" +  // TODO backtracking, non-greedy - good thing - Hint-tokens already claimed
                        "WhereKeyword => 'WHERE' | 'where' | 'W' \n" +
                        "Where => SPACE+ WhereKeyword SPACE+  WhereClause \n" +
                        "Stmt => WORD:tab Where SPACE* WORD*"  // TRICKY - order of execution not position!! ... - first extract HINT (typically from END) --> will ease the extraction of unparsable content
                //              `---- basically but does not match for required()-Test, this enforces an order left-to-right (see requireSomeWhereIndicator)
                , "" +
                        "t1 where 1=1 and 2=2 O 1\n" +
                        "-1 --3-- 5 7 -9- 1 3 5 7   \n" +
                        "WORD `-WORD|| `-WORD||||   \n" +
                        "SPACE SPACE|| SPACE|||||   \n" +
                        "      NUMBER| NUMBER||||   \n" +
                        "       ASSIGN  ASSIGN|||   \n" +
                        "        NUMBER  NUMBER||   \n" +
                        "          SPACE   SPACE|   \n" +
                        "                   WORD|   \n" +
                        "                    SPACE  \n" +
                        "                    NUMBER \n" +
                        "~~ ~~~~~ ~~~~~~~~~~~       \n" +
                        "tab  `-WhereKeyword_2_     \n" +
                        "WhereKeyword  `-WhereClause\n" +
                        "   ......Where.....        \n" +
                        " ........Stmt........      "
        );

    }

    @Test
    void test_Word2_Word() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT("" +
                        "WORD2 => WORD\n" +
                        "Stmt => WORD:tab BLOCK_START WORD2 BLOCK_END WORD"
                , "" +
                        "t1[a]\n" +
                        "-1 3\n" +
                        "WORD|      \n" +
                        "BLOCK_START\n" +
                        " WORD      \n" +
                        "BLOCK_END  \n" +
                        "~~ ~       \n" +
                        "tab|       \n" +
                        " WORD2     \n" +
                        "Stmt     "
        );

    }

    @Test
    void test_CMDS() {

        Block.registerDefaultBlocks(); // get Default Blocks

        assertEquals("CMDS", testShowBlock_OUT("CMDS", "a(aaaa)").trim());

        assertEquals("", testShowBlock_OUT("CMDS", ",").trim(), "FAIL - none - cmd stop at ,");

        assertEquals("", testShowBlock_OUT("CMDS", "a(a").trim(), "FAIL - incomplete");

        assertEquals("CMDS", testShowBlock_OUT("CMDS", "a(a),").trim(), "FAIL - incomplete - cmd stop at ,");
        assertEquals(1, blockResult.tokensFree.size(), "cmd will leave free-,");

        assertEquals("STMT", testShowBlock_OUT("STMT => CMDS DELIMITER", "a(a),").trim(), "cmd will not claim ,");
        assertEquals("a(a)", blockResult.getBlocks("CMDS").get(0).getValue(), "fully - cmd will claim only a(a)");

        assertEquals("CMDS", testShowBlock_OUT("CMDS", "a(a'{')").trim(), "high prio quote will not stop at incomplete lower prio quote");
        assertEquals("a(a'{')", blockResult.getBlocks("CMDS").get(0).getValue(), "fully - cmd will claim only a(a)");
        assertEquals(0, blockResult.tokensFree.size(), "full cmd claimed");

        assertEquals("STMT", testShowBlock_OUT("STMT => CMDS BLOCK_END", "a]").trim(), "cmd will not claim ]");
        assertEquals("a", blockResult.getBlocks("CMDS").get(0).getValue(), "fully - cmd will claim only a");
        assertEquals(0, blockResult.tokensFree.size(), "fully matched");

        assertEquals("STMT", testShowBlock_OUT("STMT => CMDS BLOCK_END", "a(a)]").trim(), "cmd will not claim ]");
        assertEquals("a(a)", blockResult.getBlocks("CMDS").get(0).getValue(), "fully - cmd will claim only a(a)");
        assertEquals(0, blockResult.tokensFree.size(), "fully - cmd will claim ]");

        assertEquals("CMDS", testShowBlock_OUT("CMDS", "a,").trim(), "FAIL - one - cmd stop at ,");
        assertEquals(true, blockResult.name.equals("CMDS"), "cmd will do and leave ,");
        assertEquals(1, blockResult.tokensClaimed.size(), "cmd will take 1 leave ,");

        assertEquals("", testShowBlock_OUT("STMT => CMDS DELIMITER", ",").trim(), "must fail as CMDS must claim at least 1 token");

    }




    @Test
    void test_Word2_Function_ASTERIKS__FIX_tokenizerASTERIKS() {

        Block.registerDefaultBlocks(); // get Default Blocks

        testShowBlock_OUT("" +
                        "WORD2 => WORD\n" +
                        "Stmt => WORD:tab BLOCK_START CMDS BLOCK_END WORD"
                , "" +
                        "t1[count(*)]\n" +
                        "-1 --3-- 5 7\n" +
                        "WORD `-WORD|    \n" +
                        "BLOCK_START|    \n" +
                        "   BRACE_START  \n" +
                        "     ASTERIKS   \n" +
                        "      BRACE_END \n" +
                        "       BLOCK_END\n" +
                        "~~ ~~~~~~~~     \n" +
                        "tab  CMDS       \n" +
                        " ...Stmt..."
        );

    }


}