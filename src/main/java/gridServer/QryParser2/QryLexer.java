package gridServer.QryParser2;

import gridServer.main;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QryLexer {

    private static final boolean DEBUG_OUTPUT = false;
    public static QryLexer instance = new QryLexer();

    public enum TokenDefinition {
        // WORD_X("[a-zA-Z0-9_.-]*[a-zA-Z][a-zA-Z0-9_.-]*((?<=\\.)\\*)"),  // allow "*" to get "article_av.*" as word
        WORD("[a-zA-Z0-9_.-]*[a-zA-Z][a-zA-Z0-9_.-]*((?<=\\.)\\*)?"),  // allow "*" to get "article_av.*" as word
        NUMBER("[0-9][0-9,.]*"),
        SPACE("[ \t\n\r]+"),
        HINT("(/\\*\\+?HINT[: ].*?\\*/|/\\*\\{.*?\\}\\s*\\*/|#HINT[: ].*$)"),
        DOC("(/\\*\\+?DOC[: ].*?\\*/|#DOC[: ].*$)"),
        COMMENT("(/\\*.*?\\*/|#.*$)"),
        DELIMITER(","),
        OPERATION("[+-]"),
        ASSIGN("="),
        //BLOCK_ELEM("[\\[\\]]"),
        BLOCK_REV("\\[<\\]"),
        BLOCK_START("\\["),
        BLOCK_END("\\]"),
        BRACE_START("\\("),
        BRACE_END("\\)"),
        WORD_QUOTE1("'.*?'"),
        WORD_QUOTE2("\".*?\""),
        QUOTE("[\"']"),
        ASTERIKS("\\*"),
        COMPERATOR("=|>|<|>=|<=|<>|!="),
        LOGIC("AND|OR|NOT"),
        OPERATOR_TUPEL ("IN|LIKE|BETWEEN|IS|NULL|EXISTS"),
        OPERATOR_EXTENDED ("~"), // Postgres matcher
        LEFT_TEXT("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        ;

        public final String patternString;
        public final Pattern pattern;
        public Matcher matcher = null;
        public String value;
        public int start;
        public int end;

        TokenDefinition(String patternString) {
            this.patternString = patternString;
            this.pattern = Pattern.compile(patternString);
            start = -1;
        }

    }

    public class Token{

        public TokenDefinition tokenDefinition;
        int start;
        int end;
        public String value;

        Token( TokenDefinition tokenDefinition, String text) {
            this.tokenDefinition = tokenDefinition;
            start = tokenDefinition.start;
            end = tokenDefinition.end;
            try{
                value = text.substring( start, end);
            } catch (Exception e) {
                main.notifyError(e.getMessage());
                throw(e);
            }

        }

        public String toString( boolean showPos, boolean showType, boolean showValue ) {
            //return "[" + this.start + ".." + this.end + "] " + tokenDefinition.name() + " '" + this.value + "'";
            return (showPos ? "[" + this.start +  (1 == this.end - this.start ? "": ".." + this.end ) + "] " : "")
                        +  (showType ? tokenDefinition.name() : "")
                        +  (showValue ? " '" + this.value + "'" : "");
        }

        public String toString(  ) {
            return toString(true, true, true);
        }

    }

    public List<Token> tokenize(String text) {
        int c = 0 ;
        int l = text.length();
        TokenDefinition token = TokenDefinition.WORD; // init
        token.end = 0;
        List<Token> tokens = new ArrayList<>();
        for( TokenDefinition t : TokenDefinition.values()) {
            t.matcher = t.pattern.matcher( text );
            t.start = -1; // reset ...
        }

        while ( c < l &&  null != token ) {
            c = token.end;
            token = null;
            for( TokenDefinition t : TokenDefinition.values()) {
                // find next matching pos
                if ( t.start  < c ) {
                    if (t.matcher.find(c)) {
                        t.start = t.matcher.start();
                        t.end = t.matcher.end();
                        if (t.end == t.start) {throw  new RuntimeException( "zero-length " + token.name() );}
                    } else {
                        t.start = l + 1; // move to end ...
                    }
                }
                // should be at least 1 exact match ....
                if ( c == t.start ){
                    if (null == token) { // orderd in prio ... might be changed by backtracing ...
                        token = t;
                        Token clone = new Token( t, text);
                        tokens.add(clone);
                        if (DEBUG_OUTPUT) {
                            System.out.println( "[" + c + ".." + t.end + "] found " +  String.format("%-20s", t.name()) + " " + ( 0 == t.start ? "" :  String.format("%" + t.start + "s",".").replace(" ",".")) + "'" + clone.value  + "'" );
                        }
                    } else {
                        if (DEBUG_OUTPUT) {
                            System.out.println( "[" + c + ".." + t.end + "] found " + t.name()  );
                        }
                    }
                }
            } // for token

        } // while ..

        if ( null == token && c < l) {
            System.out.println( c + " unparseable content  \"" + text.substring( c,  c + 5 < text.length() ? c + 5 : text.length() ) + "..." );
            TokenDefinition.LEFT_TEXT.start = c;
            TokenDefinition.LEFT_TEXT.end = text.length();
            Token clone = new Token( TokenDefinition.LEFT_TEXT, text);
            tokens.add( clone);
        }

        return tokens;
    }


}
