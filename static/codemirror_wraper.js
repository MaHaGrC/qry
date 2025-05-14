// import {CompletionContext} from "@codemirror/autocomplete" // Uncaught SyntaxError: import declarations may only appear at top level of a module

//var divCodeMirror = document.getElementById("inputMirror");
var codeSendOnEnter = true;
var cmd_hint_result_cache = null;
var cmd_hint_sug_cache = null;
var cmd_hint_cache_id = null;
var cmd_hint_sug_cur = null;
var cmd_hint_sug_next = null;
var cmd_hint_sug_eSug = document.getElementById("cdm_hint_auto_sug");


function createCodeMirror( elem ){
    if (!elem) {
        xError('codeMirrorWrapper', "createCodeMirror without node");
    }
    var myCodeMirror = CodeMirror(elem, {
      value: "\n",
      mode: "sql", //"qry", //"sql",  // import mode + hint !!
      extraKeys: {
        "Ctrl-Space": "autocomplete",
        "Enter": function(cm) {
          log("capture Enter");
          return CodeMirror.Pass;
        },
        "Ctrl-Enter": function(cm){
          log("capture Ctrl-Enter");
          //getUrl2Grid( cm.cm_grid, cm);
          data2Grid(cm.cm_grid, cm.getValue());
          //cm.getDoc().setValue('var msg = "Hi";');
          // return CodeMirror.Pass; // continue next handler
        },
        "Shift-Ctrl-Enter": function(cm){
          log("capture Shift-Ctrl-Enter");
          getUrl2GridAndHint(cm.cm_grid, "UPDATE");
        },
        // https://codemirror.net/examples/tab/
        "Tab": function(cm) {
                cdm_insert_sug(cm, null);
        },
        "Shift-Tab": function(cm) {
                cdm_insert_sug(cm, null, true);
        }
        //,"Ctrl-Tab": function(cm) { // << could not be triggered
        //,"Ctrl-PageUp": function(cm) { // << could not be triggered
        //
        // !!! TODO works only in one direction and messes up if codemirror is not visible
        //
        ,"Shift-PageUp": function(cm) { // << could not be triggered
                console.log("codemirror_wrapper - grid " + cm.cm_grid.gridNo + "switch to next grid ");
                // write a search to find next grid or first in grids[]
                let gridNo = cm.cm_grid.gridNo + 1;
                while (gridNo != cm.cm_grid.gridNo && (!grids[gridNo] || (getBoxElem4Grid(gridNo, "contextAndCM") ? getBoxElem4Grid(gridNo, "contextAndCM").classList.contains('hidden') : false ) )) {
                    gridNo++;
                    if (gridNo >= grids.length) {
                        gridNo = 0;
                    }
                }
                grids[gridNo].eGridBox.onclick(); // will only work ok if codeMirror is visible
                console.log("codemirror_wrapper - grid " + cm.cm_grid.gridNo + "switch to next grid " + gridNo);
                // w/o visible codemirror text might be selected - actively clear -> does stop flow ...

                if (window.getSelection) {
                  if (window.getSelection().empty) {  // Chrome
                    window.getSelection().empty();
                  } else if (window.getSelection().removeAllRanges) {  // Firefox
                    window.getSelection().removeAllRanges();
                  }
                } else if (document.selection) {  // IE?
                  document.selection.empty();
                }
        }
        ,"Shift-PageDown": function(cm) { // << could not be triggered
                console.log("codemirror_wrapper - grid " + cm.cm_grid.gridNo + "switch to next grid ");
                // write a search to find next grid or first in grids[]
                let gridNo = cm.cm_grid.gridNo - 1;
                while (gridNo != cm.cm_grid.gridNo && (!grids[gridNo] || (getBoxElem4Grid(gridNo, "contextAndCM") ? getBoxElem4Grid(gridNo, "contextAndCM").classList.contains('hidden') : false ) )) {
                    gridNo--;
                    if (gridNo < 0) {
                        gridNo = grids.length - 1;
                    }
                }
                grids[gridNo].eGridBox.onclick(); // will only work ok if codeMirror is visible
                console.log("codemirror_wrapper - grid " + cm.cm_grid.gridNo + "switch to next grid " + gridNo);
                // w/o visible codemirror text might be selected - actively clear -> does stop flow ...

                if (window.getSelection) {
                  if (window.getSelection().empty) {  // Chrome
                    window.getSelection().empty();
                  } else if (window.getSelection().removeAllRanges) {  // Firefox
                    window.getSelection().removeAllRanges();
                  }
                } else if (document.selection) {  // IE?
                  document.selection.empty();
                }
        }
        // POC change grid
        ,"Esc": function(cm) {
            // call context menu
            var eCdmCur = getBoxElem4Grid(cm.cm_grid,"CodeMirror-cursor");
            var pos = getPosition(eCdmCur);
            createContextMenu( eCdmCur , parseIntOr0(pos.left), parseIntOr0(pos.top), null );
        }
      }
    });

    myCodeMirror.on('cursorActivity', (cm) => {
      if (!cm.state.completeActive) {
        cdm_on_cursor_activity(cm, null);
        // cm.showHint(); // https://discuss.codemirror.net/t/autocomplete-always-show-suggestions/1822/3
      }
    });

    return myCodeMirror;
}


/*
// replace view-source:https://codemirror.net/5/addon/hint/sql-hint.js
CodeMirror.registerHelper("hint", "sql", function(editor, options) {
    xDebug('codeMirrorWrapper', "CodeMirror.registerHelper");
    var cur = editor.getCursor();
    var result = [];
    var Pos = CodeMirror.Pos;
    var token = editor.getTokenAt(cur), start, end, search;
    start = token.start;
    end = token.end;
    xDebug('codeMirrorWrapper', "CodeMirror.registerHelper -> '" + token.string + "'");
    result.push("article");
    result.push("article_av");
    result.push("article_av...");
    result.push("article[]article_av[<]attribute");
    result.push("article[]article_price[<]type");
    result.push("[productNo,articleNo,identifier,article_av.*]article[]article_av[<]attribute");
    result.push("attribute");
    result.push("type");
    result.push("erp_import_article[]erp_import_article_av");
    result.push({text: "attribute", className: "CodeMirror-hint-keyword" }); // className can be added if text is converted to object and property className is set
    result.push({text: "attribute", className: "CodeMirror-hint-table CodeMirror-hint-default-table" }); // className can be added if text is converted to object and property className is set
    result.push({text: "attribute", className: "CodeMirror-hint-table" }); // className can be added if text is converted to object and property className is set
    return {list: result, from: Pos(cur.line, start), to: Pos(cur.line, end)};
  }
);
*/

CodeMirror.registerHelper("hint", "sql", function (editor, options) {
    xDebug('codeMirrorWrapper', "CodeMirror.registerHelper");
    var res = cdm_hint(editor, options, true);
    return res;
    }
);



// replace view-source:https://codemirror.net/5/addon/hint/sql-hint.js
// -> called by ctrl-space
function cdm_insert_sug(editor, options, insertAll = false) {
    if (cmd_hint_sug_cur) {
        var cur = editor.getCursor();
        var token = editor.getTokenAt(cur);
        xLog('codeMirrorWrapper', "cdm_insert_sug");
        var hint = "";
        [ hint, cmd_hint_sug_next ]  =  cdm_insert_sug_help(token.string, cmd_hint_sug_cur, cmd_hint_sug_eSug.innerText, insertAll  );
        editor.replaceRange(hint, {line: cur.line , ch:token.start},{line:cur.line , ch:token.end});
        editor.setCursor(cur.line , token.start + hint.length);
        xLog('codeMirrorWrapper', "cdm_insert_sug -- done");
        cmd_hint_sug_eSug.style.display = "none";
        cmd_hint_sug_eSug.targetGrid = editor.cm_grid;
        cmd_hint_sug_cur = null;
        // check for resizing to match content --  https://discuss.codemirror.net/t/how-to-shrink-the-width-of-the-editorview-to-match-its-contents/5009
        setTimeout( function() {
                var eCdmSize = getBoxElem4Grid(editor.cm_grid,"CodeMirror-sizer");
                var eCdmHScroll = getBoxElem4Grid(editor.cm_grid,"CodeMirror-hscrollbar");
                var minWidth = parseInt(eCdmSize.style.minWidth);
                console.log("eCdmHScroll.clientWidth: " + eCdmHScroll.clientWidth +  " / eCdmSize.style.minWidth: " + minWidth  );
                if (eCdmHScroll.clientWidth && eCdmHScroll.clientWidth < minWidth){ // skipp if no scrollbar exists
                    var eBox = editor.cm_grid.eGridBox;
                    resizeTo( eBox, eBox.clientHeight , 150 + minWidth  );
                    // force suggestion to be re-aligned
                }
            }, 100 );

    }
}

//  [ hint, cmd_hint_sug_next ]  =  cdm_insert_sug_help(token.string, cmd_hint_sug_cur, cmd_hint_sug_eSug.innerText, insertAll  )
function cdm_insert_sug_help(token_string, hint_current, hint_current_innerText, insertAll){
        var hint_sug_next = null;
        var hint_2insert = hint_current;
        if (!hint_current_innerText.startsWith("=>") && !insertAll ) { // take first word ...
            if (hint_current.startsWith(token_string)) {
                //  keep token.string and APPEND next word/char
                //      "attribute" + hint "attribute[]attribute_value" --> "attribute["  (+="[")
                //      extract next-word/next-char AFTER token.string
                var hint_ = hint_current.substring(token_string.length).replaceAll(/^(\s*[\w\-]+|\s*.).*/g,"$1");
                var hint_2insert = token_string + hint_;
                var next_token = hint_2insert.replaceAll(/^(\s*[\w\-]+|\s*.).*/g,"$1");
                var next_token_e = hint_2insert.replaceAll(/^.*?([\w\-]+\s*|.)$/g,"$1");
                if (hint_2insert == hint_current) {
                    // finished hint - will be completely inserted ...
                } else if (token_string != next_token) {
                    // token = incomplete word -> complete current word with rest of hint -> completed token needed as anchor
                    //     du|mmy.csv  >>
                    //     dummy|.csv
                    //     dummy.|csv
                    //     dummy.csv|[<]article >>
                    //     dummy.csv[|<]article
                    //
                    if (next_token_e == next_token) {
                        hint_sug_next = hint_current;   // du|mmy.csv --> dummy|.csv
                    } else {
                        hint_sug_next = next_token_e + hint_current.substring(hint_2insert.length); //   .csv|[>]article --> ".csv"  [|<]article
                    }
                    //
                } else if(hint_current !=hint_2insert) {
                    // next token added
                    hint_sug_next = hint_current.substring(next_token.length);
                }
                //cmd_hint_sug_next = hint; // need next token as anchor
                //
                // dum|my.csv   = token: "dum" --tab-->    dummy|.csv   = token: "dummy"  --tab-->    dummy.|=>dummy.csv  = token: "."  --tab-->   dummydummy.csv
                // dummy.|=>dummy.csv   = token: "."   --tab-->    dummy.=>dummy.csv
                //
                //
            } else {
                //  REPLACE token.string with next word/char
                var hint_ = hint_current.replaceAll(/^(\s*[\w\-]+|\s*.).*/g,"$1");// extract next-word/next-char
                hint_sug_next = hint.substring(hint_.length);
                hint_2insert = hint_;
            }
        }
        return  [ hint_2insert, hint_sug_next];
}



function cdm_on_cursor_activity(editor, options) {
    xLog('codeMirrorWrapper', "cdm_on_cursor_activity");
    if (editor.getValue() && editor.getTokenAt(editor.getCursor()).end > 0) {
        var bestSug = cmd_hint_sug_next;
        cmd_hint_sug_cur = null;
        cmd_hint_sug_next = null; // only once
        if (!bestSug) {
            let result = cdm_hint( editor, options );
            if (result && result.list && result.list.length > 0 ) {
                bestSug = result.list[0].text;
            }
        }
        if (bestSug) {
            var eCdmCur = getBoxElem4Grid(editor.cm_grid,"CodeMirror-cursor");
            var eCdmCurPos = getPosition(eCdmCur);
            xLog('codeMirrorWrapper', "suggest " + JSON.stringify(eCdmCurPos) + " " + bestSug);
            var cur = editor.getCursor();
            var token = editor.getTokenAt(cur);
            // token may be continued ... or replaced (e.g. aPrice ->  article[]article_price[<]type[productNo,articleNo,identifier,article_price.*] )
            if (editor.getRange({line: cur.line , ch:token.start},{line:cur.line , ch:(token.start + bestSug.length)}).endsWith(bestSug) ) { // check if suggestion is already following ...

            } else if (bestSug.startsWith(token.string)) {
                cmd_hint_sug_cur = bestSug;
                cmd_hint_sug_eSug.innerText = bestSug.substring(token.string.length);
                cmd_hint_sug_eSug.style.color = "grey";
            } else {
                cmd_hint_sug_cur = bestSug;
                cmd_hint_sug_eSug.innerText = "=>" + bestSug;
                cmd_hint_sug_eSug.style.color = "lightgrey";
            }
            if (cmd_hint_sug_cur) {
                setTimeout( function() { // need to give codemirror time ti update cursors position
                    /* force cursor to be up-to-date - esp. when inserting sug questions the next sug */
                    editor.refresh(); // no improvement ...
                    // not implemented ... editor.save();
                    var eCdmCur = getBoxElem4Grid(editor.cm_grid,"CodeMirror-cursor");
                    var eCdmCurPos = getPosition(eCdmCur);
                    cmd_hint_sug_eSug.targetGrid = editor.cm_grid;
                    var endOfLine = editor.getLine(cur.line).length == cur.ch;
                    [ cmd_hint_sug_eSug.style.left, cmd_hint_sug_eSug.style.top ] = [ (eCdmCurPos.left + 3) + "px", (eCdmCurPos.top - 6 + ( endOfLine ? 0 : -15 )) + "px" ];
                    cmd_hint_sug_eSug.onclick= function() {this.style.display='none';grids[ editor.cm_grid.gridNo ].codeMirror.focus();}; // cmInstance.setCursor(cmInstance.lineCount(), 0);
                    xLog('codeMirrorWrapper', "suggest " + JSON.stringify(eCdmCurPos) + " " + bestSug);
                    cmd_hint_sug_eSug.style.display = "block" ;
                } , 50);
            }
        } // bestSug
    } // complete empty?
    // cmd_hint_sug_eSug.style.display = cmd_hint_sug_cur ? "block" : "none";
    cmd_hint_sug_eSug.style.display = "none";
}

function cdm_hint(editor, options, manual = false) {
    xDebug('codeMirrorWrapper', "CodeMirror.registerHelper");
    var cur = editor.getCursor();
    var result = [];
    var Pos = CodeMirror.Pos;
    //     var token = { string: editor.getTokenAt(cur), start: start, "curStart": cur.ch, "curEnd": cur.ch, end: end, search: search};
    var token_ = editor.getTokenAt(cur);
    var token = editor.getTokenAt(cur), start, end, search;
    //
    token.stringAll = editor.getValue();
    token.curLine = editor.getLine(cur.line);
    token.curStart = cur.ch;
    token.curEnd = cur.ch;
    token.preCurToken = token.curLine.substring(token.start,token.curStart);
    token.postCurToken = token.curLine.substring(token.curEnd,token.end);
    //start = token.start;
    //end = token.end;
    // CodeMirror.registerHelper -> '{"start":10,"end":15,"string":"asdad","type":null,"state":{"context":null},"curStart":12,"curEnd":12,"stringAll":"dummy.csv asdad xxxxxx","curLine":"dummy.csv asdad xxxxxx","preCurToken":"as","postCurToken":"dad"}'
    // CodeMirror.registerHelper -> '{"start":11,"end":16,"string":"adada","type":null,"state":{"context":null},"curStart":12,"curEnd":12,"stringAll":"dummy.csv  adada xxxxx","curLine":"dummy.csv  adada xxxxx"}'
    xLog('codeMirrorWrapper', "CodeMirror.cdm_hint -> '" + JSON.stringify(token) + "'");
    //  TODO  should migrate to https://codemirror.net/examples/autocompletion/ -> newer codemirror
    //
    // concepts...
    //  - guess context the current token is of (sql, sql-table .... )
    //  - check if cursor is
    //       - around no text
    //       - must cover prefix / prefix + suffix / must contain chars in order ( pa -> de(p)ric(a)ted )
    //  - sources
    //      - current grids headline
    //      - current grids first 2 rows
    //      - known abbreviation
    //      - known statements
    //
    // suggest parts of content
    //
    //
    //      keep  ordered by relevance ...
    //


    // {list: result, from: Pos(cur.line, token.start), to: Pos(cur.line, token.end)}
    var result = [];
    var sug = cmd_hint_sug_cache;
    var cache_id = "" + editor.cm_grid.gridNo + "-" + editor.cm_grid.items.length + "x" + (editor.cm_grid.items[0] ?? []).length ; // depend on gridNo and data
    // re-use as much as possible
    if (!sug || (cmd_hint_cache_id ?? "-" ) != cache_id ) {
        // from grid
        var sug = editor.cm_grid.getVisibleDataAsList(4,5) ?? [];
        // from BE
        var suggestionsBE = getSuggestionFromUrl(editor.cm_grid);
        var sugBE = suggestionsBE.suggestions ?? [] ;
        var descr_used = [];
        for(let i = 0; i < sugBE.length; i++) {
            var descr = sugBE[i].descr;
            if (sugBE[i].qry) {
                sug.push( sugBE[i].qry.trim() );
            }
        }
        cmd_hint_sug_cache = sug;
        cmd_hint_cache_id = cache_id;
    }
    //
    //      --> try to
    //
    //if (!token.stringAll.match(/[^a-zA-Z_0-9]/)) {
    result = evalSug(token, sug, editor, manual);


    //
    //  --> result  R E P L A C E S   token
    //
    /*
    result.push("article");
    result.push("article_av");
    result.push("article_av...");
    result.push("article[]article_av[<]attribute");
    result.push("article[]article_price[<]type");
    result.push("[productNo,articleNo,identifier,article_av.*]article[]article_av[<]attribute");
    result.push("attribute");
    result.push("type");
    result.push("erp_import_article[]erp_import_article_av");
    result.push({text: "attribute", className: "CodeMirror-hint-keyword" }); // className can be added if text is converted to object and property className is set
    result.push({text: "attribute", className: "CodeMirror-hint-table CodeMirror-hint-default-table" }); // className can be added if text is converted to object and property className is set
    */
    // not supported ....   result.push({label: "alabel", type: "keyword" , text: "attribute", className: "CodeMirror-hint-table" }); // className can be added if text is converted to object and property className is set
    xLog('codeMirrorWrapper', "CodeMirror.cdm_hint first result: " + (result && result[0] && result[0].text ? result[0].text : "" ) );
    xLog('codeMirrorWrapper', "CodeMirror.cdm_hint " + (result && result.length ? result.length + " results found " : "" ) );

    return {list: result, from: Pos(cur.line, token.start), to: Pos(cur.line, token.end)};
  }


   function evalSug(token, sug, editor, manual){
        var cur = editor.getCursor();
        let result=[];
        let resultSeen = {};
        //
        sug = sug.filter(item => item && item.substring);
        //
        if (token.string && "" != token.string) {
            if (token.string.length > 1){
                //  editor has only 1 word  -->
                for (const s of sug ) {
                    //      complete word
                    if (s && !resultSeen[ s ] && s.startsWith(token.string)) {
                        result.push({ text: s, className: "cm-sug-word"});
                        resultSeen[ s ] = 1;
                    }
                }
                // äquivalzenzklassen ... csv-files ... unabhöngig von ihrem namen ---
                //  "article.csv" -> token ".csv"  must win ".csv[]article" over "dummy.csv"
                if (token.string.endsWith(".csv") ||token.string.endsWith(".csv[") || token.string.endsWith(".csv[]")) {
                    result.push({ text: token.string + "[<]article", className: "cm-sug-equivalence-class"});
                }
                //      complete with stmt containing token
                for (const s of sug ) {
                    if (s && !resultSeen[ s ] && s.includes(token.string)) {
                        result.push({ text: s, className: "cm-sug-stmt"});
                        resultSeen[ s ] = 1;
                    }
                }
                //      complete with stmt containing all words - indicated by camel case  -- aPrice
                //      complete with stmt containing all words - indicated by ...         -- a...price
                //       --> !!!! ignorecase -> result can be lowercase only !! aPriceAv -> article[]article_price[<]type[productNo,articleNo,identifier,article_price.*]
                let reg = token.string.split(/(?=[A-Z])|\.\.\./).map(x => x.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join("[^\\s]+");
                if (token.string.match(/^[a-zA-Z_\[\]0-0.*-]+$/) && (reg.includes("+") || /^[A-Z]/.test(token.string) )) { // at least 1 camel-case
                    // aAV
                    console.debug("1 reg: '" + reg + "'");
                    let r = new RegExp(reg,'i');
                    for (const s of sug ) {
                        //console.debug("2 '" + s + "'");
                        //console.debug("2a " + (s && !resultSeen[ s ] ? "true" : "false"));
                        //console.debug("2b " + (s && !resultSeen[ s ] && r.test(s) ? "true" : "false"));
                        if (s && !resultSeen[ s ] && r.test(s) ) {
                           // console.debug("3 " + s);
                            result.push({ text: s, className: "cm-sug-abbreviation"});
                            resultSeen[ s ] = 1;
                        }
                    }
                } else{
                    //      complete with stmt containing all chars -- assume all lowercase
                    let reg = token.string.split("").map(x => x.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join("[^\\s]+");
                    if (token.string.match(/^[a-zA-Z_\[\]0-0.*-]+$/) && reg.includes("+")) {
                        let r = new RegExp(reg);
                        for (const s of sug ) {
                            if (s && !resultSeen[ s ] && r.test(s) ) {
                                result.push({ text: s, className: "cm-sug-abbreviation"});
                                resultSeen[ s ] = 1;
                            }
                        }
                    } // valid reg from token ..
                }
            }
        }
        if (token.stringAll) {
            for (const s of sug ) {
                //      complete word
                if (s && !resultSeen[ s ] && s.startsWith(token.stringAll)) {
                    result.push({ text: s.substr(token.start), className: "cm-sug-all"}); // as we start replacement with current token ...
                    resultSeen[ s ] = 1;
                }
            }
            for (const s of sug ) {
                //      complete String
                if (s && !resultSeen[ s ] && s.includes(token.stringAll)) {
                    result.push({ text: s.replace(token.stringAll,"-------").replaceAll(/.*-------/g,token.string), className: "cm-sug-all"}); // as we start replacement with current token ...
                    resultSeen[ s ] = 1;
                }
            }
        }
        if (!(token.string && "" != token.string) || manual)  {
            // completely empty
            for (const s of sug ) {
                result.push({ text: s, className: "cm-sug-all"}); // as we fill from scratch
            }
        }
        // remove
        //  if already there ..
        //  duplicates
        let curVal = editor.getRange({line: cur.line , ch:token.start},{line:cur.line , ch:(token.start + 100)});
        let curValLeft = editor.getRange({line: cur.line , ch:0},{line:cur.line , ch:token.end});
        let curLine = editor.getLine(cur.line );
        resultSeen=[];
        // prevent result to trigger itself right away ...
        // prevent
        result = result.filter(item => {
            let s=item.text; resultSeen[ s ] = (resultSeen[ s ] ?? 0) + 1 ;
            return !curVal.startsWith(s) && !curValLeft.endsWith(s) &&  !(s.length >100 && curLine.includes(s)) && 1 == resultSeen[s];
        });
        return result;
   }






// https://discuss.codemirror.net/t/autocompletion-merging-override-in-config/7853
//EditorState.languageData.of(() => ({autocomplete: myCompletions}))


/*

var myCodeMirror = CodeMirror(divCodeMirror, {
  value: "function myScript(){return 100;}\n",
  mode: "javascript",
  extraKeys: {
    "Enter": function(cm) {
      log("capture Enter");
      return CodeMirror.Pass;
    },
    "Ctrl-Enter": function(cm){
      log("capture Ctrl-Enter");
      //getUrl2Grid( cm.cm_grid, cm);
      data2Grid(cm.cm_grid, cm.getValue());
      //cm.getDoc().setValue('var msg = "Hi";');
      // return CodeMirror.Pass; // continue next handler
    }
  }
});

*/




    CodeMirror.defineSimpleMode("qry", {
      // The start state contains the rules that are intially used
      start: [
        // The regex matches the token, the token property contains the type
        {regex: /"(?:[^\\]|\\.)*?(?:"|$)/, token: "qryString"},
        {regex: /'(?:[^\\]|\\.)*?(?:'|$)/, token: "qryString"},

        {regex: /\b(from +)([^ ]+?) /, token: ["qryKeyword","qryJoinTab"]},
        {regex: /\b(join +)(.*?)( on) /, token: ["qryJoin2","qryJoinTab","qryJoin"]},

        {regex: /(?:W|select|where|join|from|GROUP_BY|group by|order by|having)\b/i,    token: "qryKeyword"},

        //{regex: /(?=\])([^ \]\[]+)(?=\[)/, token: "qryJoinTab"}, // codemirror does not allow lookbehinds ...
        // codemirror does not allow lookbehinds ...
        //{regex: /(\]?)([^ \]\[]+)(?=\[)/, token: "qryJoinTab"},
        //{regex: /([^ \]\[]*)(\[[^ ]*\])([^ \]\[]*)/, token: ["qryTab", "qryJoin", "qryTab"]},
        //{regex: /([^ \]\[]*)(\[[^ \]]*\](?!\[?))([^ \]\[]*)/, token: ["qryTab", "qryJoin", "qryTab"]},

        //{regex: /([^ \]]*)(\[[^ \]]*\]\[[^ \]]*\])([^ \]]*)/, token: ["qryTab", "qryJoin", "qryTab"]},

        // keep last "[" or "]" to indicate join-State (will not use state)
        {regex: /([^ \]\[]+)(?=\[)/, token: "qryTab"},
        {regex: /(\[[^ \]]*\]\[[^ \]]*)(?=\])/, token: "qryJoin"},
        {regex: /(\[[^ \]]*)(?=\])/, token: "qryJoin"},
        {regex: /(\])([^ \]\[]+)(?=\[)/, token: ["qryJoin","qryJoinTab"]},
        {regex: /(\])([^ \]\[]+)/, token: ["qryJoin","qryTab"]},


        {regex: /\b(min|max|decode|count)/i, token: "qryFunction"},
        {regex: /\b(over|partition by|or|and|in|like|case|when|then|end)\b/i, token: "qryFunction"},

        //{regex: /([^ \]\[]*)(\[[^ ]*\])([^ \]\[]*)/, token: ["qryTab", "qryJoin", "qryTab"]},
        //{regex: /(\[[^ \]]*\]\[[^ \]]*\])/, token: "qryJoin"},
        //{regex: /(\[[^ \]]*\])/, token: "qryJoin"},

        {regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,  token: "qryNumber"},

        {regex: /\/\/.*/, token: "qryComment"},
        {regex: /\/\*/, token: "qryComment", next: "qryComment"},

        {regex: /[^ ]+/, token: "word"}

      ],
      // The multi-line qryComment state.
      qryComment: [
        {regex: /.*?\*\//, token: "qryComment", next: "start"},
        {regex: /.*/, token: "qryComment"}
      ],
      // The meta property contains global information about the mode. It
      // can contain properties like lineComment, which are supported by
      // all modes, and also directives like dontIndentStates, which are
      // specific to simple modes.
      meta: {
        dontIndentStates: ["qryComment"],
        lineComment: "//"
      }
    });

var styles = `
    .cm-qryJoinTab {
        color: blue;
    }
    .cm-qryJoin {
        color: gray;
    }
    .cm-qryJoin2 {
        color: green;
    }
    .cm-qryFunction {
        color: green;
    }
    .cm-qryTab {
        color: blue;
    }
    .cm-qryString {
        color: #a11;
    }
    .cm-qryNumber {
        color: #164;
    }
    .cm-qryKeyword {
        color: gray;
        font-weight: bold;
    }
    .cm-qryComment {
        color: darkgrey;
    }
    // suggestions
    .cm-sug-word {
        color: darkgrey;
    }
    .cm-sug-stmt {
        color: grey;
    }
    .cm-sug-abbreviation {
        color: light;
    }


    .CodeMirror-hint-keyword{
        color: green;
    }
 `;
var styleSheet = document.createElement("style");
styleSheet.type = "text/css";
styleSheet.innerText = styles;
document.head.appendChild(styleSheet);
