package gridServer.QryParser2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QryToSqlTranslator {

    private StringBuilder sb;
    public Block block;

    /*
        for Test
     */

    public String translate(String stmt){
        return translate(stmt, true, true);
    }

    /*
        partial translation - is needed if Where Or Order-Clause contain strange tokens (like Postgres matcher " identifier ~ 'CMYK' ")
     */
    public String translate(String stmt, boolean includeHint, boolean allowPartialTranslation){
        Block block1 = new QryParser().parse(stmt);
        return null == block1 || (!allowPartialTranslation && !block1.tokensFree.isEmpty()) ? stmt : translate(block1, includeHint);
    }

    /*

     */

    public void append(String prefix, String stmt, String suffix) {
        if (null != stmt && !stmt.isEmpty()) {
            System.out.println( (null != prefix ? prefix : "") + stmt + (null != suffix ? suffix : ""));
            if (null != prefix)
                sb.append(prefix);
            sb.append(stmt);
            if (null != suffix)
                sb.append(suffix);
        }
    }

    private void append(String s) {
        if (null != s)
            sb.append(s);
    }

    private void appendValue(String prefix, String key, String suffix) {
        append(prefix, block.getValue(key), suffix);
    }

    private String colDef(String tab, String col) {
        return  tab.equals(".") || col.startsWith(".") ? col.replaceFirst("^[.]+","") : col.contains(".") ? col : tab + "." + col;
    }

    public String translate(Block block, boolean includeHint) {
        sb = new StringBuilder();
        this.block = block;

        //
        List<Block> selectOrGroupCols = block.getBlocks("selectOrGroupCols");
        if (!selectOrGroupCols.isEmpty()) {
            if (block.getBlocks("groupModeIndicator").isEmpty() && !block.getBlocks("selectCols").isEmpty()) {
                BlockTest.showBlock(block);
                throw new RuntimeException("selectCols can only exists if groupModeIndicator is present (when selectOrGroupCols is present)");
            }
            String nameOld = selectOrGroupCols.get(0).name;
            selectOrGroupCols.get(0).name =   block.getBlocks("groupModeIndicator").isEmpty() ? "selectCols" : "groupByCols";
            System.out.println("RENAME!! selectOrGroupCols.get(0).name: " + nameOld + " -> " + selectOrGroupCols.get(0).name);
        }

        String select = block.getValue("Select");
        if (select.isEmpty() && !(block.tokens.size() > 0 && block.tokens.get(0).value.toLowerCase().startsWith("select"))) { // if stmt starts with "select " and is to be taken 1:1 (NON-Query-Syntax)
            String groupByColsBlock = block.getValue("groupByCols");
            String selectCols = block.getValue("selectCols");
            if (!groupByColsBlock.isEmpty() ) {
                selectCols = groupByColsBlock;
                for (Block col : block.getBlocks("selectCols.col")) {
                    selectCols += ", min("+col.getValue()+"), max("+col.getValue()+")";
                }
                selectCols += ", count(*) cnt";
            }
            //
            select = "select " + (selectCols.isEmpty() ? "*" : selectCols) +" from ";
        }
        append("", select, "");

        List<Block> joinRelevantBlocks =  new java.util.ArrayList<>();
        // keep order
        joinRelevantBlocks.addAll( block.getBlocks("tabSrc"));
        joinRelevantBlocks.addAll( block.getBlocks("JoinQry"));
        //
        String tabSrc = "";
        String colsSrc = "";
        String colsTrg = "";
        String tabTrg = "";
        Map<String, Integer> usedTab = new java.util.HashMap<>();
        for (Block joinQry : joinRelevantBlocks) {

            System.out.println("JoinQry: " + joinQry);
            tabSrc = joinQry.getValue( "tabSrc" , tabTrg); // chain up
            colsSrc = joinQry.getValue( "colsSrc");
            colsTrg = joinQry.getValue( "colsTrg");
            tabTrg = joinQry.getValue( "tabTrg").replace("__", " "); // might include alias "a a2"
            if (tabTrg.isEmpty()) {
                append("" , tabSrc, "");
                tabTrg = tabSrc;
                usedTab.put(tabTrg, 1);
            } else {
                String joinType = joinQry.getValue( "JoinLeftIndicator").isEmpty() ? joinQry.getValue( "JoinRightLeftIndicator").isEmpty() ? "" : "right " : "left ";
                // inject alias
                Integer cnt = usedTab.containsKey(tabTrg) ? usedTab.get(tabTrg) + 1 : 1;
                usedTab.put(tabTrg, cnt);
                if (cnt > 1 && !tabTrg.contains(" ")) {
                    tabTrg = tabTrg + " "+ tabTrg + cnt;
                }
                // append(  " " + joinType + "join " + tabTrg + " on " + tabSrc + "." + colsSrc + " = " + tabTrg + "." + colsTrg);
                append(  " " + joinType + "join " + tabTrg + " on " );
                //
                String tabSrcPure = tabSrc.replaceFirst(" .*", ""); // refer to pure table name
                String tabSrcAlias = tabSrc.replaceFirst(".*? ", ""); // from now on only alias
                String tabTrgPure = tabTrg.replaceFirst(" .*", ""); // refer to pure table name
                String tabTrgAlias = tabTrg.replaceFirst(".*? ", ""); // from now on only alias
                //
                if (colsSrc.isEmpty() && colsTrg.isEmpty()) {

                    if (joinQry.getBlocks("JoinNatRev").size() > 0) {
                        // [<] - Natural Join Reverse
                        colsSrc = tabTrgPure;
                        colsTrg = "id";
                    } else {
                        // [] - Natural Join
                        colsSrc = "id";
                        colsTrg = tabSrcPure.replace("_", "");
                    }
                    append( colDef(tabSrcAlias, colsSrc ) + " = " + colDef(tabTrgAlias, colsTrg));

                } else {

                    List<Block> colsSrcS = joinQry.getBlocks("colsSrc").get(0).getBlocks("col");
                    List<Block> blocksColsTrg = joinQry.getBlocks("colsTrg");
                    List<Block> colsTrgS = null != blocksColsTrg && !blocksColsTrg.isEmpty() ? blocksColsTrg.get(0).getBlocks("col") : colsSrcS; // might be JoinUsing
                    for (int i = 0; i < colsSrcS.size(); i++) {
                        String colSrc = colsSrcS.get(i).getValue();
                        String colTrg = colsTrgS.get(i).getValue();
                        if (i > 0) {
                            append(" and ");
                        }
                        append( colDef(tabSrcAlias, colSrc) + " = " + colDef(tabTrgAlias, colTrg));
                    }

                }
            }

        } // joinQry

        //
        appendValue(" where ", "WhereClause", "");
        appendValue(" group by ", "groupByCols", "");
        appendValue(" order by ", "orderByCols", "");
        appendValue(" having ", "HavingClause", "");
        appendValue(" limit ", "LimitClause", "");

        if (block.tokensFree.size() > 0) {
            // KLUDGE ... space might be missing ... -- "article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*] W identifier ~ 'CMYK' O article_av.lastmodified desc"
            sb.append(" ");
            block.tokensFree.forEach( token -> {
                sb.append( block.tokens.get(token).value);
            });
        }

        if (includeHint) {
            appendValue(" ", "Hint", "");
        }
        return sb.toString();
    } // translate()



}
