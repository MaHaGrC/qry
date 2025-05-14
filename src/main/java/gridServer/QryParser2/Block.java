package gridServer.QryParser2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Block {

    private static final String VIRTUAL_BLOCK_SUFFIX = "_";
    static Map<String, Block> blockMap = new HashMap<>();
    static int maxOffset;

    String name = "block";
    List<Block> blocks = new ArrayList<>();
    QryLexer.TokenDefinition tokenDef = null;
    List<QryLexer.Token> tokens = new ArrayList<>();  /* tokens - a copy of tokens */
    public List<Integer> tokensFree = new ArrayList<>(); /* !!! GLOBAL index of free tokens */
    List<Integer> tokensClaimed = new ArrayList<>();  /* indexes of LOCALLY (in block) claimed tokens / ordered !! */
    private Integer curIdx = null;
    List<List<String>> blockSpecification = new ArrayList<>();
    public boolean orCondition = false;
    private boolean valid = false;
    protected String indent = "";
    protected boolean requireSomeWhere = false;
    static final String requireSomeWhereIndicator = ">";
    private String constant = null;
    private static String tokenStringLast;
    private boolean optional = false; // is allowed to fail - so log as SKIPPED


    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Block> getBlocks(String blockPathOrName, boolean rekursiv ){
        ArrayList<Block> nameBlocks = new ArrayList<>();
        if (this.name.equals(blockPathOrName)) {
            nameBlocks.add(this);
        }
        if (rekursiv || nameBlocks.isEmpty()) {
            // remove name from path
            String blockPathOrName_ = blockPathOrName.replaceFirst("^" + name + "[\\./]", "");
            // if path is correct all parents meet and removed
            for (Block block : this.blocks) {
                nameBlocks.addAll(block.getBlocks(blockPathOrName_, rekursiv));
            }
        }
        return nameBlocks;
    }
    public List<Block> getBlocks(String name){
        return getBlocks(name, false);
    }

    QryLexer.Token getFirstToken(){
        QryLexer.Token token = tokensClaimed.isEmpty() ? null : tokens.get(tokensClaimed.get(0));
        for (Block block : blocks) {
            QryLexer.Token tokenChild = block.getFirstToken();
            if (tokenChild != null && (token == null || tokenChild.start < token.start)) {
                token = tokenChild;
            }
        }
        return token;
    }

    QryLexer.Token getLastToken(){
        QryLexer.Token token = tokensClaimed.isEmpty() ? null : tokens.get(tokensClaimed.get(tokensClaimed.size()-1));
        for (Block block : blocks) {
            QryLexer.Token tokenChild = block.getLastToken();
            if (tokenChild != null && (token == null ||  token.end < tokenChild.end)) {
                token = tokenChild;
            }
        }
        return token;
    }

    List<Integer> getTokensClaimedNested(){
        List<Integer> tokens = new ArrayList<>();
        tokens.addAll(this.tokensClaimed);
        for (Block block : blocks) {
            tokens.addAll(block.getTokensClaimedNested());
        }
        return tokens;
    }

    String getValue(String blockPathOrName, String fallbackValue){
        String value = getValue(blockPathOrName);
        return value.isEmpty() ? fallbackValue : value;
    }
    String getValue(String blockPathOrName){
        StringBuilder sb = new StringBuilder();
        for (Block block : getBlocks(blockPathOrName, true)) {
            sb.append(block.getValue());
        };
        return sb.toString();
    }


    public String getValue(){
        StringBuilder sb = new StringBuilder();
        for (Integer i : getTokensClaimedNested()) {
            sb.append(tokens.get(i).value);
        }
        return sb.toString();
    }


    static void registerDefaultBlocks() {
        QryLexer.TokenDefinition[] tds = QryLexer.TokenDefinition.values();
        for (QryLexer.TokenDefinition td : tds) {
            Block block = new Block(td.name());
            block.tokenDef = td;
            registerBlock(block);
        }
        registerBlock(new Block("CMDS"));
    }

    static Block registerBlock(Block block ) {
        blockMap.put(block.name, block);
        return block;
    }

    static Block getBlock( String name ) {
        return getBlock(name, true);
    }

    static Block getBlock( String name , String alias) {
        return getBlock(name, alias, null);
    }

    static Block getBlock( String name , String alias, String indent) {
        Block block = getBlock(name, true);
        if (null != alias &&  !alias.isEmpty()) {
            block.name = alias;
        }
        if (null != indent) {
            block.indent = indent;
        }
        return block;
    }


    static Block getBlock( String name, boolean cloned ) {
        Block block = blockMap.get(name);
        if (cloned) {
            if (null != block) {
                block = block.clone();
            } else {
                throw new RuntimeException("undefined Block: " + name);
            }
        }
        return block;
    }

    public Block(String name) {
        this.name = name;
    }

    public Block() {
    }

    public Block clone() {
        Block block = new Block();
        block.name = name;
        block.tokenDef = tokenDef;
        block.blockSpecification = blockSpecification;
        block.orCondition = orCondition;
        block.constant = constant;
        return block;
    }



    static Block createBlock(String blockNameAndDefs){
        Block block = null;
        for (String blockNameAndDef : blockNameAndDefs.split("\\n")) {

            String[] splitNameAndDef = blockNameAndDef.split("\\s*=>?\\s*",2);
            String name = splitNameAndDef[0];
            String value = splitNameAndDef.length <= 1 ? "" : splitNameAndDef[1];
            block = blockMap.get(name);
            String msg_suffix = "";
            if (null != blockMap && blockMap.containsKey(name)) {
                msg_suffix = "  (redefining)";
                blockMap.remove(name);
            } else {
            }
            block = new Block(name);
            if (splitNameAndDef.length <= 1) {
                // Dummy Block for Token - "WORD"
                if (!name.equals("CMDS")){ // CMDS is special
                    block.tokenDef = QryLexer.TokenDefinition.valueOf(block.name);
                }
            } else if (value.startsWith("'") && value.trim().endsWith("'") & !value.matches(".*'\\s+\\|\\s+'.*")) {
                // Constant Block  "orderKeyword => 'ORDER BY'"
                // RegExp Block    "whereClause => '.*?(?=ORDER BY|$)'"
                // But Not         "whereKeyWort => 'WHERE' | 'where' | 'W'"
                value = value.trim();
                block.constant = value.substring(1, value.length() - 1);
                if (!msg_suffix.isEmpty()) {
                    System.out.println( block.name + msg_suffix);
                }
            } else if (value.contains("|")) {
                // OR-Construct included --> split OR-Branches and create sub-blocks
                //  D =  A  | B C        --> D = A | D.2  ,   D.2 = B C
                String[] blockDefElems = value.split("\\s*\\|\\s*");
                block.orCondition = true;
                int i = 0;
                for (String blockDefElem : blockDefElems) {
                    blockDefElem = blockDefElem.trim();
                    //if (blockDefElem.contains(" ")) { // is combined block -> must create virtual sub-block
                    if ( !blockMap.containsKey(blockDefElem)) { // skipp if block is known like "WORD" or already existing
                        i++;
                        String subBlockName = name + VIRTUAL_BLOCK_SUFFIX + i + VIRTUAL_BLOCK_SUFFIX;
                        createBlock(subBlockName + " => " + blockDefElem);
                        blockDefElem = subBlockName;
                    }
                    blockDefElem = blockDefElem + "    ."; // ensure sufficient space for split
                    String[] split = blockDefElem.split("(?<=:)|(?=[?*+:])| ");
                    if (!split[3].isEmpty()) {
                        // handle    col:alias* --> col*alias  (similar to col*
                        split[1] = split[3];
                    }
                    split[1] = split[1].replace(":", ""); // remove colon-helper for alias
                    block.blockSpecification.add(List.of(split));
                    System.out.println(split[0] + msg_suffix);
                }
            } else {
                // split with positive look ahead
                String[] blockDefElems = value.split("\\s+");
                for (String blockDefElem : blockDefElems) {
                    // split with positive look ahead
                    blockDefElem = blockDefElem + "    ."; // ensure sufficient space for split
                    String[] split = blockDefElem.split("(?<=:)|(?=[?*+:])| ");
                    if (!split[3].isEmpty()) {
                        // handle    col:alias* --> col*alias  (similar to col*
                        split[1] = split[3];
                    }
                    split[1] = split[1].replace(":", ""); // remove colon-helper for alias
                    block.blockSpecification.add(List.of(split));
                    System.out.println(split[0] + msg_suffix);
                }
            }

            // register
            Block.registerBlock(block);
        }

        return block;
    }

    private String alignedString( boolean valid, String indent, String s, List<Integer> tokenFree) {
        s = ( null == indent ? "" : indent) + s;
        int offset = s.length() + 3;
        maxOffset = Math.max(maxOffset, offset);
        String tokenString = (null == tokenFree || tokenFree.isEmpty() ? "" : "  " + tokens.get(tokenFree.get(0)) + "..." );
        if (null == tokenStringLast || !tokenStringLast.equals(tokenString)) {
            String x = (null == tokenStringLast ? "" : "  " + tokenStringLast);
            tokenStringLast = tokenString;
            tokenString = x;
        } else {
            tokenString = "";
        }
        s = (valid ? "OK.." : optional ? "SKIP" : "FAIL") + String.format(" %-" + maxOffset + "s", s) + " " + tokenString;
        System.out.println(s);
        return s;
    }

    boolean add( Block block, List<QryLexer.Token> tokens, boolean optional) { // optional = is allowed to fail
        Block blockResult = null;
        block.optional = optional;
        if (!tokensFree.isEmpty()) {

            ArrayList<Integer> tokensFreeBackup = new ArrayList<>(tokensFree);
            if (null== block.tokenDef && null == constant && !block.name.endsWith(VIRTUAL_BLOCK_SUFFIX)) {
                System.out.println("...."+ ( null == indent ? "" : indent)  + "   " + block.name );
            }
            blockResult = block.parse(tokens, tokensFree);
            if (null == block.tokenDef && null != blockResult && null == constant && !block.name.endsWith(VIRTUAL_BLOCK_SUFFIX)) {
                alignedString(null!=blockResult ,indent , "   " + block.name + "  (" + ( null == blockResult ? " " + tokensFreeBackup.size() + " reseted" : blockResult.tokensFree.size()  ) + " tokens left)", null);
            }
            if (null != blockResult) {
                blocks.add(blockResult);
            }
            tokensFree =  null == blockResult ? tokensFreeBackup : blockResult.tokensFree;

        }
        return null != blockResult;
    }

    public Block parse( List<QryLexer.Token> tokens, List<Integer> tokenFree) {
        this.tokens = tokens;
        this.tokensFree = tokenFree;
        valid = true;
        if (tokenFree.isEmpty()) {
            valid = false;
        } else if (null != constant) {
            // assume complete tokens
            String constant = this.constant; // !!! convert RegExp to Constant - so use copy
            Matcher matcher = null;
            if (true /* RegExp ???? */) {
                // get all free tokens (HINT might already remove tailing tokens)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tokenFree.size(); i++) {
                    sb.append(tokens.get(tokenFree.get(i)).value);
                }
                // create RegExp from constant
                matcher = Pattern.compile(constant).matcher(sb.toString());
                if (matcher.find()) {
                    constant =  matcher.group( matcher.groupCount() > 0 ? 1 : 0 ); // have now to find tokens included ...
                } else {
                    matcher = null;
                }
            }
            curIdx = tokensFree.get(0);
            QryLexer.Token curToken = getCurToken();
            while (null != curToken && constant.contains(curToken.value) && constant.startsWith(getValue() + curToken.value) && getValue().length() + curToken.value.length() <= constant.length()) {
                acceptNext();
                curToken = getCurToken();
            }
            valid = getValue().equals(constant);
            if (constant.length() < 20 ) {
                alignedString(valid ,indent ,  "'" + constant  + "' (" + (null!=matcher ? "RegExp" : "constant" ) + ", " + tokensFree.size() + " tokens left)",tokenFree);
            } else {
                alignedString(valid ,indent ,  name  + " (" + (null!=matcher ? "RegExp" : "constant" ) + ", " + tokensFree.size() + " tokens left)",tokenFree);
            }
        } else if (null != tokenDef) {
            if (name.startsWith(requireSomeWhereIndicator) || QryLexer.TokenDefinition.HINT.equals(tokenDef)) {
                valid = requireSomeWhere(tokenDef);  // claims and validate at once
                alignedString(valid ,indent , " >" + tokenDef.name() + ( tokenDef.name().equals(name) ? "" : " : " + name ) + "< (somewhere token, " + tokensFree.size() + " tokens left)", tokenFree);
            } else {
                valid = require(tokenDef);  // claims and validate at once
                alignedString(valid ,indent , " >" + tokenDef.name() + ( tokenDef.name().equals(name) ? "" : " : " + name ) + "< (token, " + tokensFree.size() + " tokens left)", tokenFree);
            }
        } else if (name.equals("CMDS")) {
            // special case for CMDS  --> stop at non-quoted / non-embraced "," or closing bracket
            // WORDS and nested (...) and {...} and [...] and "..." and '...'
            //   ignore if escaped by \
            //  example:   c (d e) {f g} [h i] 'j k'
            valid = true;
            List<String> openPart = List.of("({['\"".split(""));
            List<String> closePart = List.of(")}]'\"".split(""));
            List<String> allParts  = new ArrayList<>();
            allParts.addAll(openPart);
            allParts.addAll(closePart);
            List<String> allowedToken = List.of(QryLexer.TokenDefinition.WORD.name(), QryLexer.TokenDefinition.SPACE.name());
            List<String> waitingBrackets = new ArrayList<>();
            String previouseValue = "";
            while (tokensFree.size() > 0 && valid) {
                QryLexer.Token curToken = getCurToken();
                String tokenName = curToken.tokenDefinition.name();
                if (allowedToken.contains(tokenName)) {
                } else if (allParts.contains(curToken.value)) {
                    String value = curToken.value;
                    String expectedClose = waitingBrackets.isEmpty() ? "" : waitingBrackets.get(waitingBrackets.size() - 1);
                    int curPrio = waitingBrackets.isEmpty() ? 0 : closePart.indexOf(waitingBrackets.get(waitingBrackets.size() - 1));
                    if (previouseValue.equals("\\")) { // escaped - so just claim
                    } else if (openPart.indexOf(value) >= curPrio) {
                        // higher prio opening
                        waitingBrackets.add(closePart.get(openPart.indexOf(value))); // add matching closing
                    } else if (value.equals(expectedClose)) {
                        // OR matching closing
                        waitingBrackets.remove(waitingBrackets.size() - 1);
                    } else if (closePart.indexOf(value) >= curPrio) {
                        // unexpected closing bracket with higher prio -> assume end of CMDS by outer bracket
                        break;
                    } else {
                        // lower prio tag
                    }
                } else if (!waitingBrackets.isEmpty()) {
                    // waiting for closing bracket
                } else {
                    // non - closing bracket // non allowed token --> invalid
                    break;
                }
                acceptNext();
                previouseValue = curToken.value;
            } // while
            valid &= waitingBrackets.isEmpty() && tokensClaimed.size() > 0;
            alignedString(valid ,indent , " >" + name + "< (CMDS, " + tokensFree.size() + " tokens left)", tokenFree);
        } else {
            valid = orCondition ? false : true;
            for (List<String> def : blockSpecification) {
                String tokenOrBlockName = def.get(0);
                String tokenMode = def.get(1);  // : or * or + or ? -> * - repeat, + - repeat at least one, ? - optional
                String blockAlias = def.get(2);
                boolean validDef = orCondition ? false : true;
                //
                Block block = getBlock(tokenOrBlockName, blockAlias, indent + "  ");
                block.optional = this.optional;
                //
                if (null != block) {
                    if (tokenMode.isEmpty() || tokenMode.equals(":")) {
                        validDef = add( block, tokens, this.optional);
                    } else if (tokenMode.equals("*")) {
                        while (add(block, tokens, true)) {
                            block = getBlock(tokenOrBlockName, blockAlias, indent + "  ");
                        }
                    } else if (tokenMode.equals("+")) {
                        validDef = add(block, tokens, this.optional);
                        if (validDef) {
                            block = getBlock(tokenOrBlockName, blockAlias, indent + "  ");
                            while (add(block, tokens, true)) {
                                block = getBlock(tokenOrBlockName, blockAlias, indent + "  ");
                            }
                        }
                    } else if (tokenMode.equals("?")) {
                        add(block, tokens, true);
                    }
                }
                valid = orCondition ? validDef || valid : validDef && valid;
                if (orCondition == valid || tokenFree.isEmpty()) {
                    break; // short circuit
                }
            } // for blocks

        } // token vs block
        return validate() ? this : null;
    }

    public Block parse( List<QryLexer.Token> tokens ) {
        tokensFree.clear();
        for (int i = 0; i < tokens.size(); i++) {
            tokensFree.add(i);
        }
        Block block = parse(tokens, tokensFree);
        for (Integer i : tokensFree) {
            System.out.println("MISS " + tokens.get(i).toString().replaceFirst("^\\[([^.]*)\\]", "[$1..$1]" )); // force exception

        }
        return block;
    }

    public boolean validate() {
        /*
        boolean valid = false;
        if (tokenDef != null) {
            if (!tokensClaimed.isEmpty()) {
                valid = tokens.get(tokensClaimed.get(0)).tokenDefinition.equals(tokenDef); //
            }
        } else {
            valid = !blocks.isEmpty();
        }
        */
        return valid;
    }

    /*
        show named blocks and tokens below ...
     */
    public String toString(List<Block >  blocks,boolean showPos, boolean showType, boolean showValue, boolean showBasicTokens ) {
        StringBuilder sb = new StringBuilder();
        for (Block block : blocks) {
            if (showBasicTokens || null == block.tokenDef || !block.name.equals(block.tokenDef.name()) ) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(block.toString(showPos, showType, showValue, showBasicTokens));
            }
        }
        sb.append(toStringToken(tokensClaimed, showPos, showType, showValue, sb.length() > 0 ));
        return sb.toString();
    }

    public String toStringToken(List<Integer> tokensClaimed,boolean showPos, boolean showType, boolean showValue, boolean delimiter) {
        StringBuilder sb = new StringBuilder();
        if ((showPos || showType || showValue) && tokensClaimed.size() > 0) {
            if (delimiter) {
                sb.append(", ");
            }
            sb.append("tokens: [");
            for (Integer i : tokensClaimed) {
                if (i < tokens.size()){
                    sb.append(tokens.get(i).toString( showPos, showType, showValue ));
                    sb.append(", ");
                } else {
                    System.out.println("ERROR: " + i + " >= " + tokens.size());
                    System.out.println(tokens.get(i)); // force exception
                }

            }
            // cut last ", "
            if (tokensClaimed.size() > 0) {
                sb.delete(sb.length() - 2, sb.length());
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /*
        list (no-tokenDef) blocks with all nested tokens
     */

    public String toStringNestedToken(List<Block >  blocks , boolean tokenDetails ) {
        //return toString(blocks, false, false, false, false);
        StringBuilder sb = new StringBuilder();
        int blockCount = 0;
        for (Block block : blocks) {
            if (block.tokenDef != null && block.name.equals(block.tokenDef.name())) {
            } else {
                blockCount++;
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(block.name + ": ");
                // sb.append("\"" + block.getValue().replace("\"","\\\"") + "\"");
                sb.append("{ \"" + block.getValue() + "\"");
                String s = block.toStringNestedToken(block.blocks, tokenDetails);
                sb.append(s.length() > 4 ? ", " + s : "");
                sb.append("}");
            }
        }
        return blockCount < 2 ? sb.toString() :  "[ " + sb.toString() + " ]";
    }


    public String toString(boolean showPos, boolean showType, boolean showValue, boolean showBasicTokens  ) {
        String str = toString(this.blocks, showPos, showType, showValue, showBasicTokens);
        str = name  + ( str.length()>0 ? ": {" + str.toString() + "}" : "");
        return str;
    }

    public String toStringSimplified() {
        return toString(false, false, false, false);
    }
    public String toString() {
        return toString(false, false, false, true);
    }
    public String toString(boolean showTokenDetails) {
        return showTokenDetails ? toString(true, true, true, true) : toString(false, true, false, true);
    }

    /*
        TOKENS
     */


    public QryLexer.Token getCurToken() {
        curIdx = tokensFree.size() > 0 ? tokensFree.get(0) : null;
        return null == curIdx ? null : tokens.get(curIdx);
    }

    public boolean require(QryLexer.TokenDefinition td) {
        boolean found = false;
        if (!tokensFree.isEmpty()) {
            curIdx = tokensFree.get(0);
            found = tokens.get(curIdx).tokenDefinition.equals(td);
            if (found) {
                tokensClaimed.add(curIdx);
                tokensFree.remove(0);
            }
        }
        return found;
    }

    public boolean requireSomeWhere(QryLexer.TokenDefinition td) {
        boolean found = false;
        if (!tokensFree.isEmpty()) {
            curIdx = null;
            for (int i = 0; i < tokensFree.size() ; i++) {
                if (tokens.get(tokensFree.get(i)).tokenDefinition.equals(td)) {
                    curIdx = tokensFree.get(i);
                    tokensFree.remove(i);
                    tokensClaimed.add(curIdx);
                    found = true;
                    break;
                }
            }
        }
        return found;
    }



    public boolean acceptNext() {
        tokensClaimed.add(tokensFree.get(0));
        tokensFree.remove(0);
        return true;
    }


    /*
        BLOCKS

     */


    boolean validateBlock( Block block ) {
        block = block.parse(tokens, tokensFree);
        if (null != block) {
            blocks.add(block);
        }
        return null != block;
    }
}
