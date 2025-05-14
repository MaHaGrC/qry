package gridServer.QryParser2;

import java.util.ArrayList;
import java.util.List;

class BlockList extends Block {

    List<Block> blocks = new ArrayList<>();
    boolean mandatory = true; // every block is optional = OR

    BlockList(String name, List<Block> blocks) {
        super();
        this.name = name;
        this.blocks = blocks;
    }


    public Block parse(List<QryLexer.Token> tokens, List<Integer> tokenFree ) {
        this.tokens = tokens;
        this.tokensFree = tokenFree;
        // first block might not be satisfied
        for (Block block : blocks) {
            if (mandatory && !validateBlock( block)) {
                break;
            }
        }
        //
        return validate() ? this : null;
    }

    public boolean validate() {
        return tokensFree.isEmpty();
    }
}
