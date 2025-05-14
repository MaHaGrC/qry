package gridServer.QryParser2;

import java.util.List;

class BlockToken extends Block {

    static Block init = Block.registerBlock(new BlockToken());

    QryLexer.TokenDefinition tokenDef = null;
    String string = null;


    BlockToken(String name, QryLexer.TokenDefinition tokenDef, String string) {
        super(name);
        this.tokenDef = tokenDef;
        this.string = string;
    }
    BlockToken(String name, QryLexer.TokenDefinition tokenDef) {
        super(name);
        this.tokenDef = tokenDef;
    }

    BlockToken() {
        super("Token");
    }

    public boolean validate() {
        return require(tokenDef) && (null == string || string.equals(getCurToken().value));
    }

}
