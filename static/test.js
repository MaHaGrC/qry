let test_current = "";
let test_count_fail = 0;
let test_count_succ = 0;
let test_count_total = 0;
let test_run = "";
let test_run_row = 0;
let test_run_row_msg = 0;
let test_List = null;
/*

    Helper

*/


function logTest(msg,fail,succ) {
    if (test_List && test_run_row>0) {
        test_List.updateData(test_run_row, 1, test_count_fail);
        test_List.updateData(test_run_row, 2, test_count_succ);
        test_List.updateData(test_run_row, 3, test_count_total);
        //
        if (test_run_row_msg < test_run_row) {
            test_run_row_msg = test_run_row; // init, re-init if next text-function called
        }
        test_run_row_msg++;
        test_List.insertRow("-",test_run_row_msg);
        let pre = "<div style='color:"+ (fail > 0 ? "red" : succ > 0 ? "green" : "gray") +"'>" ;
        let suf = "</div>"
        test_List.items[test_run_row_msg] = [test_run,pre + (fail > 0 ? fail : "" ) + suf , pre + (succ > 0 ? succ : "" ) + suf, pre + (fail+succ > 0 ? fail+succ : "") + suf, pre + msg + suf];
        test_List.load();
        fitToGridContainer(test_List);

    }
}

function logOk(msg) {
  let cnt = msg && msg.startsWith('IGN:') ? 0 : 1;
  test_count_total = test_count_total + cnt;
  test_count_succ = test_count_succ + cnt;
  //logTest(test_count_total + "-" + test_count_fail + "  OK: " + msg);
  logTest("OK: " + msg, 0, 1);
}

function logFail(should, is, msg) {
  let cnt = msg && msg.startsWith('IGN:') ? 0 : 1;
  test_count_total = test_count_total + cnt;
  test_count_fail = test_count_fail + cnt;
  let logMsg = "FAIL: " + msg + (is || should ? "( expect: '" + should + "' <> is: '" + is + "' )" : "");
  logTest(logMsg, cnt, 0);
  msg_error( test_count_total + "-" + test_count_fail + " " + logMsg);
}

function assertChk(cond, msgOk, msgFail, should, is) {
  if (cond) {
    logOk(msgOk);
  } else {
    logFail(should, is, msgFail);
  }
}

function assertNotNull(is, msg) {
  assertChk(is, msg, msg);
}

function assertNull(is, msg) {
  assertChk(!is, msg, msg);
}


function assertId(id, msg) {
  e = document.getElementById(id);
  assertChk(e, (msg ?? " found ID") + "(\"" + id + "\") ", (msg ?? " miss ID") + "(\"" + id + "\") ");
  return e;
}

function assertNotId(id, msg) {
  e = document.getElementById(id);
  assertChk(!e, (msg ?? " free ID") + "(\"" + id + "\") ", (msg ?? " unexpected ID") + "(\"" + id + "\") ");
  return e;
}

function assertEq(should, is, msg) {
  assertChk(is == should, msg + " (" + is + ")", msg, should, is);
}

function assertEqIgnore(should, is, msg) {
  assertChk(is == should, 'IGN: ' + msg + " (" + is + ")", 'IGN: ' + msg, should, is);
}

/*

    connection-mock ...

*/

function test_rec_http_mock(url) {
    msg_info("mock-http-read "  + url.href);
    return mockableXMLHttpRequest_(url)
}

async function test_rec_fetch_mock(url) {
    msg_info("mock-fetch-read "  + url.href);
    if (url && url.href.includes("qry=Tests")) {
        location.reload();
    }
    return fetchMockable_(url)
}

function test_msg_notification_mock(){
    // just do nothing ...
}


function test_setup(){
    for( i in grids) {
        if (i != test_List.gridNo && grids[i]) { // skipp test-summary
            if (grids[i].eGridBox) grids[i].eGridBox.remove(); // skipp virtual grids..
            grids[i] = null;
        }
    }
    grids.filter(n => n); // remove empty elements
    gridNoCounter=grids.length-1;
}

/*




*/
function testStart(name) {
  logTest("---------------------------");
  logTest("   " + name);
  logTest("---------------------------");
}

function runTest(){
    // clean everything...
    msg_info("test cleanup");
    //  clean grids
    for( i in grids) {
        if (grids[i].eGridBox) grids[i].eGridBox.remove(); // skipp virtual grids..
        grids[i] = null;
    }
    grids = [];
    // clean message holder
    document.getElementById("msg_rec").innerHTML="";
    //
    rec_http_mock = test_rec_http_mock;
    rec_fetch_mock = test_rec_fetch_mock;
    msg_notification_mock = test_msg_notification_mock;
    msg_info("test: rec/msg_*_mock installed");
    //
    //
    test_List = createTestGridContainer("Tests","test;fail;succ;total;state\\nALL;0;0;0;todo                                  .\\n");
    //
    test_List.items.push( ["test_2_fixFirst",0,0,0,""] );
    test_List.items.push( ["test_grid_query",0,0,0,""]);
    test_List.items.push( ["test_grid_stmt",0,0,0,""]);
    test_List.items.push( ["test__extractAllPropFromQry",0,0,0,""]);
    test_List.items.push( ["test__extractProp",0,0,0,""]);
    test_List.items.push( ["test__grid_box",0,0,0,""]);
    //
    test_List.eGrid.style.gridTemplateColumns = "120px 50px 50px 50px auto"; // KLUDGE
    test_List.colsGrouped=1;
    test_List.load();
    fitToGridContainer(test_List);
    //
    //
    let row_total = 1;
    test_List.updateData(row_total, 4, 'running...');
    for (let row = 2; row < test_List.items.length; row++) {
        if (test_run != test_List.items[row][0]) {
            console.log("----------------------")
            console.log(test_List.items[row][0])
            test_run = test_List.items[row][0];
            test_run_row = row;
            msg_info("test " + test_run);
            test_List.updateData(row, 4, 'running...');
            test_setup();
            test_count_fail = 0;
            test_count_succ = 0;
            test_count_total = 0;
            try{
                window[test_run]();
            } catch(e){
                logFail("RUN","ERROR",e.message);
                console.error(e);
            }
            if (test_List.getData(row,4).endsWith('...')) test_List.updateData(row, 4, 'done');
            test_List.updateData(row_total, 1, parseInt(test_List.getData(row_total,1)) + test_count_fail);
            test_List.updateData(row_total, 2, parseInt(test_List.getData(row_total,2)) + test_count_succ);
            test_List.updateData(row_total, 3, parseInt(test_List.getData(row_total,3)) + test_count_total);
            if (2==row && test_count_fail > 0) { // fixFirst - stop all others ...
                row = test_List.items.length;
            }
        }
    }
    test_List.updateData(row_total, 4, 'done');
}





/*

https://www.educative.io/answers/how-to-dynamically-load-a-js-file-in-javascript

*/

function createTestGridContainer(qry, responseOrData){
    test_setup();
    /*
    let grid = createGridContainer("" ); // qry must be null or empty to prevent HTTP-Request
    handleQryResponse(grid, qry, responseOrData.startsWith("{") ? responseOrData : JSON.stringify({"data": responseOrData}) ); // qry must be filled to set
    */
    let grid = createGridContainer(qry, null, responseOrData.startsWith("{") ? responseOrData : JSON.stringify({"data": responseOrData}) ); // qry must be null or empty to prevent HTTP-Request
    return grid;
}

function test_grid_query(){
    let grid = createTestGridContainer("q", '{"date":"2024/03/13 16:04:53","data":"domain;topic;issue;date\\na;b;c;d\\n","lastAccessDate":"2024/03/21 17:51:55","firstDate":"2024/03/12 11:15:15","login":"Baur_VM","stmt":"dummy.csv"}');
    assertEq( "q", grid.codeMirror.getValue(), "create grid");


    grid = createTestGridContainer("q", '{"data":"domain;topic;issue;date\\n","login":"Baur_VM","stmt":"dummy.csv"}');
    assertEq( "q", grid.codeMirror.getValue(), "create grid");
    assertEq( "domain;topic;issue;date;\r\n", grid.getVisibleData(), "create headline on empty data if avail");

}

function test_grid_stmt(){
    let grid = createTestGridContainer("q", '{"data":"domain;topic;issue;date\\na;b;c;d\\n", "stmt":"dummy.csv"}');
    assertEq( "q", grid.codeMirror.getValue(), "do NOT with executed stmt");

    grid = createTestGridContainer("q /*{}*/", '{"data":"domain;topic;issue;date\\na;b;c;d\\n", "stmt":"dummy.csv"}');
    assertEq( "q", grid.codeMirror.getValue(), "remove empty hint");

    grid = createTestGridContainer('q /*{cdm: "none"}*/', '{"data":"domain;topic;issue;date\\na;b;c;d\\n", "stmt":"dummy.csv"}');
    assertEq( "q", grid.codeMirror.getValue(), "hide hints");

    grid = createTestGridContainer('q', '{"data":"domain;topic;issue;date\\na;b;c;d\\n", "stmt":"dummy.csv  /*{\\"valHndl\\":{\\"tab\\":\\"LINK\\"},\\"cdm\\":\\"none\\"}*/"}');
    assertEq( "q", grid.codeMirror.getValue(), "apply hints from stmt - query");
    assertEq( '{"tab":"LINK"}', JSON.stringify(grid.valHndl), "apply hints from stmt - hints");
    assertEq( '["s","s","s","s"]', JSON.stringify(grid.colDataType), "keep grid on invalid response");

    grid = createTestGridContainer('q', '{"data":"domain;topic;issue;date\\na;b;c;d\\n", "stmt":"dummy.csv  /*{"INVALID-NESTED":"X"}*/"}');
    assertEq( "q", grid.codeMirror.getValue(), "keep grid on invalid response");

    // handle Error-String... colDataType must not be empty even if Error ist treated as Heda
    grid = createTestGridContainer('q', 'Cannot invoke "String.replace(java.lang.CharSequence, java.lang.CharSequence)" because the return value of "gridServer.QryResponse.getData()" is null');
    assertEq( '["s","s"]', JSON.stringify(grid.colDataType), "def colDataType on headline only"); // no data - no data-types
    //assertEq( "", JSON.stringify(grid.getElement(0,0) ? encodeURIComponent(grid.getElement(0,0).outerHTML.substr(0,30)) : 'NONE'), "handle Error-String... keep it empty rows... "); // no data - no data-types

}

function test__grid_box(){
    grid = createTestGridContainer('q', JSON.stringify({"date":"2024/03/15 17:09:21","data":"\n\n","lastAccessDate":"2024/03/22 12:07:11","firstDate":"2024/03/15 17:09:21","login":"Baur_VM","stmt":"select from skn_pool limit 1"}));
    assertEq( "Baur_VM", getBoxElem4Grid( grid, "context_env").innerHTML, "w/o login");
}

function test__extractAllPropFromQry(){
    assertEq( '["q",{}]', JSON.stringify(extractAllPropFromQry('q')), "none");
    assertEq( '["q",{"cdm":"none"}]', JSON.stringify(extractAllPropFromQry('q /*{"cdm": "none"}*/')), "easy prop");
    assertEqIgnore( '["q",{"cdm":"none"}]', JSON.stringify(extractAllPropFromQry('q /*"cdm": "none"*/')), "fix missing {} - easy prop"); // accept that error - just do it right ...
    assertEqIgnore( '{"cdm":"none"}', JSON.stringify(extractAllPropFromQry('q /*cdm: "none"*/')), "fix json-like property syntax"); // accept that error - just do it right ...
}


function test__extractProp(){
    assertEq( '["q",{}]', JSON.stringify(extractProp('q','')), "none");

    assertEq( '["q",{"valHndl":{"tab":"LINK"},"cdm":"none"}]', JSON.stringify(extractProp('q',{stmt: 'dummy.csv  /*{"valHndl":{"tab":"LINK"},"cdm":"none"}*/'})), "extract from json");

    var [stmt, prop] = extractAllPropFromQry('q /*{"valHndl":{"tab":"LINK"},"cdm":"none"}*/',)
    assertEq("q",stmt,"extract q, remove hint");
    assertEq('{"valHndl":{"tab":"LINK"},"cdm":"none"}', JSON.stringify(prop) ,"remove hint as prop");
}

/*

    codemirror

*/

function test_2_fixFirst(){

    // var result = evalSug(token, sug, editor);
    console.error("TODO test suggestions!!");

    function cdm_insert_sug_help_tst( token_string, hint_current, hint_current_innerText, insertAll = false,    hint_2insert, hint_next, msg ) {
        [ hint_2insert_, hint_next_] = cdm_insert_sug_help( token_string, hint_current, hint_current_innerText ?? hint_current, insertAll );
        assertEq(hint_2insert ?? 'null', hint_2insert_ ?? 'null', msg + "(hint_2insert)");
        assertEq(hint_next ?? 'null', hint_next_ ?? 'null', msg + "(hint_next)");
    }

    cdm_insert_sug_help_tst("du"    ,"dummy.csv", null, false,      "dummy", "dummy.csv", "incomplete first word" );
    cdm_insert_sug_help_tst("dummy" ,"dummy.csv", null, false,      "dummy.", ".csv", "add 1 non-word char/ leave . as anchor" );
    cdm_insert_sug_help_tst("dummy.","dummy.csv", null, false,      "dummy.csv", null, "finish off current completion path" );
    //
    // reload next hints
    //
    cdm_insert_sug_help_tst(".csv",".csv[<]article", "[<]article", false,      ".csv[", "[<]article", "B2" );  // next token will be >>[<< !!



    //assertEq("1","0","STOP RUN here");


    /*
        aAV -> article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*]  -- camelCaseWord
        a...av -> same
    */

}


/*


*/
if (window.location.href.endsWith("/test")) {
    runTest();
}