package gridServer.QryParser2;

import org.junit.jupiter.api.Test;

import java.util.List;

class QryParserTest {

    @Test
    void parse() {

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[b,c]d");

        QryParser parser = new QryParser();
        Block block = parser.parse(tokens);

        System.out.println("--------------");
        System.out.println( block.toString() );

    }


    @Test
    void parseMulti() {

        QryLexer lexer = new QryLexer();
        List<QryLexer.Token> tokens = lexer.tokenize("a[b,c]d[e,f][g,h]i[j,k]");

        QryParser parser = new QryParser();
        Block block = parser.parse(tokens);

        System.out.println("--------------");
        System.out.println( block.toString() );

    }


}