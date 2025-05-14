package gridServer.QryParser2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QryParser {

    private final String qryDef = "" +
            "Hint => HINT \n" +
            "col => CMDS \n" +
            "colDelim => DELIMITER col \n" +
            "cols => col colDelim* \n" +
            "JoinLeftIndicator => OPERATION \n" +
            "JoinRightLeftIndicator => OPERATION \n" +
            "JoinCols => JoinLeftIndicator? BLOCK_START cols:colsSrc BLOCK_END BLOCK_START cols:colsTrg BLOCK_END JoinRightLeftIndicator? WORD:tabTrg \n" +
            "JoinNat => JoinLeftIndicator? BLOCK_START BLOCK_END JoinRightLeftIndicator? WORD:tabTrg \n" + // TODO JoinNat - rename to JoinInner
            "JoinNatRev => JoinLeftIndicator? BLOCK_REV JoinRightLeftIndicator? WORD:tabTrg \n" +
            "JoinUsing => JoinLeftIndicator? BLOCK_START cols:colsSrc BLOCK_END JoinRightLeftIndicator? WORD:tabTrg \n" +
            "JoinQry => JoinCols | JoinNatRev | JoinNat | JoinUsing \n" +
            "SelectCols => DELIMITER DELIMITER:groupModeIndicator cols:selectCols* \n" +
            "SelectAndGroup => BLOCK_START cols:selectOrGroupCols SelectCols? BLOCK_END \n" +
            "OrderByKeyword => 'ORDER BY' | 'order by' | 'O' \n" +
            "OrderBy => SPACE+ OrderByKeyword SPACE cols:orderByCols \n" +
            "WhereClause => '(?i)(.*?)(?= order by | O | group by | G | limit | l | H | HAVING | /\\*|$)' \n" +  // TODO backtracking, non-greedy - good thing - Hint-tokens already claimed
            "WhereKeyword => 'WHERE' | 'where' | 'W' \n" +
            "Where => SPACE+ WhereKeyword SPACE+  WhereClause \n" +
            "HavingClause => '(?i)(.*?)(?= limit | l | /\\*|$)' \n" +  // TODO backtracking, non-greedy - good thing - Hint-tokens already claimed
            "HavingKeyword => 'Having' | 'HAVING' | 'having' | 'H' \n" +
            "Having => SPACE+ HavingKeyword SPACE+  HavingClause \n" +
            "LimitClause => NUMBER:limitLower DELIMITER NUMBER:limitUpper | NUMBER:limitUpper \n" +
            "LimitKeyword => 'Limit' | 'LIMIT' | 'limit' | 'L' \n" +
            "Limit => SPACE+ LimitKeyword SPACE LimitClause \n" +
            "SelectKeyword => 'SELECT' | 'select' | 'S' \n" +
            "FromKeyword => 'FROM' | 'from' | 'F' \n" +
            "SelectColsSQL => cols:selectCols | ASTERIKS \n" +
            "Select => SelectKeyword SPACE+ SelectColsSQL SPACE+ FromKeyword SPACE \n" +
            "StmtQry => Select? SelectAndGroup? WORD:tabSrc JoinQry* SelectAndGroup? Where? OrderBy? Having? Limit?\n" +
            "Stmt => Hint? SPACE* StmtQry SPACE* "
            ;
    private static Block qryParser = null;
    public Block block;

    public QryParser(){
        if (qryParser == null){
            Block.registerDefaultBlocks();
            qryParser = Block.createBlock(qryDef); // might overwrite existing definitions
        }
    }


    public Block parse(List<QryLexer.Token> tokens) {
        block =  Block.getBlock("Stmt", true).parse(tokens);
        //
        System.out.println("Parsed: " + (null == block ? "-NONE-" : block.toString(true)));
        //
        return block;
    }


    public Block parse(String stmt) {
        List<QryLexer.Token> tokens = QryLexer.instance.tokenize(stmt);
        return parse(tokens);
    }


}
