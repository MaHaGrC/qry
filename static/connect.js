  // https://javascript.info/fetch
  // fetch("http://localhost:8080/size", {mode: "no-cors"}).then( response  => setData( response.text()  ));
  // let response = await fetch("http://localhost:8080/size", {mode: "no-cors"});
  // https://stackoverflow.com/questions/43262121/trying-to-use-fetch-and-pass-in-mode-no-cors/43268098#43268098


var rec_session = "";
var rec_session_old = "";
var rec_session_active = 1;  // 0-disabled / 1-local / 2-db
var rec_fetch_mock = null;
var rec_http_mock = null;
var rec_msg = ""; // need last action for recordSend


/*

*/

async function fetchMockable_( url) {
    let text = null;
    try{
        let response = await fetch(url);
        xDebug('connect', response);
        text = await response.text(); // read response body as text
    } catch (e) {
        msg_error('connect::Fetch !!! Connection fail !!' + url.href);
        xError('connect', e);
    }
    return text;
}

async function fetchMockable( url) {
    return rec_fetch_mock ? await rec_fetch_mock( url ) : await fetchMockable_(url);
}

function mockableXMLHttpRequest_( url) {
    let json = {};
    try{
        let request = new XMLHttpRequest();
        request.open("GET", url, false);
        request.send(null);
        xDebug('connect', request);
        if (request.status === 200) {
          let text = request.responseText; // read response body as text
          json = JSON.parse(text);
        }
    } catch (e) {
        msg_error('connect::XMLHttpRequest: !!! Connection fail !!' + url.href);
        xError('connect', e);
    }
    return json;
}


function mockableXMLHttpRequest( url) {
    return rec_http_mock ?  rec_http_mock(url) :  mockableXMLHttpRequest_( url );
}


/*


*/


function handleQryResponse(grid, qry, response, propForced = null){
    var response_json = null;
    if (response) {
        if (response.data || response.errorMsg ){
            response_json = response;
        } else if (response.startsWith("{")){
            try {
                response_json = JSON.parse( response ); // TODO remove 2nd parse from grid.setDataJson
            } catch (e) {
                msg_error( "invalid response: '" + response + "' " + e.message);
                console.error(response);
                console.error(e);
            }
        } else {
            msg_error( "invalid response:" + response);
        }
    }
    if (response_json){
        if (response_json.errorMsg){
            msg_error( response_json.errorMsg);
        }
        if (response_json.hint){
            if( response_json.errorMsg) {
                msg_warn( response_json.hint);
            } else {
                msg_info( response_json.hint);
            }
        }
        if ("" == response_json.login || "" == response_json.data) {
            msg_warn( response_json.hint ?? "please Login first (credentials)");
            console.warn("please Login first (credentials)");
            console.error("open LOGIN-DIALOG");
        }
    }
    handleQryResponseGrid( grid, qry, response_json, propForced); // might be invalid response - but still update grid ...
}

function handleQryResponseGrid(grid, qry, response_json, propForced = null){
  if (response_json && response_json.data && response_json.data.startsWith('{"session":') ) {

    // SESSION
    console.log("handleQryResponseGrid: SESSION");
    var session = JSON.parse(response_json.data);
    for( i in grids) {
        if (grids[i] && grids[i].eGridBox) grids[i].eGridBox.remove(); // skipp virtual grids..
        grids[i] = null;
    }
    grids = [];
    gridNoCounter=0;
    for( i in session.grids){
        let grid = createGridContainer( session.grids[i].qry ?? "" ,session.grids[i].prop);
    }

  } else {


    // QRY -- keep always up-to-date ... even if no response is coming - work with current qry/data

    if (grid.eGrid && grid.eGridBox) { // skipp for virtual grid (parsing data to menu etc..)
        observeResizeDisable(); // tricky will also prevent resizeEvent for just-happend-action-of-load
        handleQryResponseGridBox(grid, qry, response_json);
    }
    if (response_json && response_json.data) { // may not be filled on re-creation of sessions ... recreation will only load qry (must be resolved afterwards)
        grid.setDataJson(response_json.data);
    }
    // prop from
    //  - qry ( called Statement in grid)
    //  - prop ( explicit prop from response )
    //  - stmt ( executed Statement )
    //  - propForced
    var prop = {};
    //
    //  grid.prop_hidden --> keep hidden >cmd:"none"< of v_dbTabUsage after refresh ..
    [qry, prop] = extractProp( qry, response_json, grid.prop_hidden, propForced, grid.prop_manual);
    handleProp( grid, prop, qry);
  }

}



function handleQryResponseGridBox(grid, qry, response_json){
    if (grid.eGrid && grid.eGridBox && response_json) { // skipp for virtual grid (parsing data to menu etc..)
        var ctxElement = getBoxElem4Grid( grid, "context"); // TODO refactor - same pattern on 3 Call-Handlers ...
        if (ctxElement) {
            getBoxElem4Grid( grid, "context_env").innerHTML = response_json.login && "" != response_json ? response_json.login : loginId_current;
            let con_date = null ;
            if (response_json.date) {
                con_date = response_json.date;
                if (con_date == "1970/01/01 01:00:00") {
                    con_date = "EXAMPLE";
                } else {
                    var d = new Date();
                    cur_date = d.getFullYear() + "/" +  ((d.getMonth()+1) < 10 ? "0" : "") + (d.getMonth()+1) + "/" + (d.getDate() < 10 ? "0" : "") + d.getDate();
                    con_date = con_date.replace( cur_date + " ",""); // keep day or time ...
                    con_date = con_date.replace(/ .*/,""); // shorten date ...
                }
            }
            if (!con_date) {
                var d = new Date();
                con_date = (d.getHours() < 10 ? "0" : "") + d.getHours() + ":" + (d.getMinutes() < 10 ? "0" : "") + d.getMinutes();
            }
            getBoxElem4Grid( grid, "context_date").innerHTML = con_date ;
            toggle( getBoxElem4Grid( grid, "context_date"), "+msgNew 5000 -msgNew");
            getBoxElem4Grid( grid, "context_version").innerHTML = response_json.version ?? "";
        }
        //
        var stmtElement = getBoxElem4Grid( grid, "stmt");
        stmtElement.innerHTML = response_json.stmt ?? "";
        toggle( getBoxElem4Grid( grid, "stmt_icon") , response_json.stmt && "" != response_json.stmt ? "-icon-inactiv" : "+icon-inactiv" );
    }
}



function handleProp(grid, prop, qry = null) {
    grid.set_property( prop);
    if (grid.eGrid && grid.eGridBox) { // skipp for virtual grid (parsing data to menu etc..)
        //
        observeResizeDisable(grid.eGridBox); // tricky will also prevent resizeEvent for just-happend-action-of-load
        //
        var cdm = getBoxElem4Grid( grid, "contextAndCM");
        if (qry) {
            grid.codeMirror.setValue(qry ?? "");
            toggle(grid.eGridBox,'grid-outdated',false);
            fitToGridContainer(grid, true, false); // force using position from prop, but only if width and height are known
            grid.load(0); // defines grid-size by content
        }
        if (cdm && prop) {
            toggle( cdm, prop.cdm && "none" == prop.cdm ? "+hidden" : "-hidden" );
            grid.codeMirror.refresh();
        }
        //
        fitToGridContainer(grid, true, true); // ensure to show grid - size might be set according to content -
        // "..." for limited data
        var sob = getBoxElem4Grid(grid, "scrollOnButton");
        if (sob){
            var stmt = getBoxElem4Grid( grid, "stmt");
            toggle( sob, "display", stmt.innerHTML.toLowerCase().includes("limit") || (grid.items.length > 25 /* DEFAULT-Limit in BE */) );
        }
        //
        observeResizeEnable(grid.eGridBox,250); // tricky will also prevent resizeEvent for just-happend-action-of-load
    }
}



async function data2Grid(grid, qry, callback = null, response = null, propForced = null) {
  xDebug('connect', "connect::data2Grid");
  // todo - check if grid-query should be updated immediately ... (show the request is running and what to expect)
  if (grid && grid.codeMirror) {
    grid.codeMirror.setValue(qry ?? "");
    toggle(grid.eGridBox,'grid-outdated',true);
  }
  if (!response) {
      url = new URL("http://localhost:8080/query");
      url.searchParams.append("qry", qry);
      if (cmd_hint_sug_eSug && cmd_hint_sug_cur) url.searchParams.append("qrySug", cmd_hint_sug_cur); // for fast response triggered by suggestion if qry fails
      // TODO always???
      var prop = grid.get_property(true);
      prop.pos = getPositionAndSize( grid.eGrid );
      url.searchParams.append("prop", JSON.stringify( prop ));
      // virtual grids (for Menu) do not have context !!
      url.searchParams.append("login", grid.eGrid ? getBoxElem4Grid(grid,"context_env").innerHTML :  loginId_current );
      url.searchParams.append("version", grid.eGrid ? getBoxElem4Grid(grid,"context_version").innerHTML : "" );
      response = await fetchMockable(url); // read response body as response
  }
  if (response){
    qry = qry.replaceAll(/ UPDATE *$/g,"").replaceAll(/ UPDATE  */g," "); // remove hints ... should have 2nd channel or be part of ???
    qry = qry.replaceAll(/CACHE:[0-9_]* */g,""); // remove hints ... should have 2nd channel or be part of ???
    //
    // long running query ... like url.check ...
    if (response.includes('"data":"Status\\ncurrently loading ..."')){
        msg_warn("refresh in 10 sec");
        setTimeout(function() {
                    controller( findGrid( grid ),'REFRESH:'); // at least is now is traceable ... --> ensure used Vars are not re-used!!!
                }, 10000);
    }
    //
  } else {
    msg_warn("data2Grid qry w/o response: grids[" + grid.gridNo + "] -> " + qry);
  }
  handleQryResponse(grid, qry, response, propForced);
  if (callback) callback(grid, qry);
}


function getUrl2Grid(grid) {
  data2Grid( grid, grid.codeMirror.getValue());
}

// getURL2GridAndHint( grid, "cmd", "::toggle", false);
// getUrl2GridAndHint(grid, ['limit: "' + rowCount + '"',"UPDATE"])
function getUrl2GridAndHint(eGridOrGrid, hints = "", value = "", reload = true) {
  let grid = findGrid(eGridOrGrid);
  let qry = grid.codeMirror.getValue();
  hints = Array.isArray(hints) ? hints : [hints];
  for( hint of hints) {
      if ("" == hint) {
      } else if ("UPDATE" == hint ) {
          let hint_ = " "+hint+" "; // use " " to ease match
          if (!qry.endsWith(hint_) && !qry.endsWith(" " + hint)){
            qry = qry + hint_;
          }
      } else if (hint.startsWith("CACHE:") ) { // KLUDGE
        qry = hint + " " + qry; // --> use HINT as Prefix, ease CACHE-Handling
      } else {
          // if hint contains value   >> limit: "1,200"
          if (hint.includes(":")) {
            value = hint.replaceAll(/.*:\s*"/g,"").replaceAll(/".*/g,"");
            hint = hint.replaceAll(/:.*/g,"");
          }
          //
          [ qry, prop] = extractAllPropFromQry(qry, grid.prop_hidden);
          if ( null == value ) {
            delete prop[ hint ];
          } else if ("::toggle" != value){
            prop[ hint ] = value;
          } else {
            // toggle (  value == null means  prop is not set )
            // default-Yes   value == null or value == "yes" -->  value = "none"  //  value == "none" --> value = null
            // default-No    value == "yes" --> value = "yes"  //  value == "none" or value == null  --> value = null
            defaultYes = "cdm" == hint;
            value = prop[hint] ?? (defaultYes ? "yes" : "none");
            value = "none" == value ? "yes" : "none"; // toggle
            if (value == (defaultYes ? "yes" : "none")) {
                delete prop[ hint ];
            } else {
                prop[ hint ] = value;
            }
          }
          if (prop) {
            // inject Hint into query - to be stored and executed later
            prop_str = JSON.stringify(prop);
            qry = qry.replaceAll(/(.*?)\s*\/\*\{.*/g,"$1");
            if ("{}"!=prop_str) {
                qry = qry + " /*" + JSON.stringify(prop) + "*/";
            }
          }
      }
  }
  if (reload) {
    data2Grid( grid, qry); // hint may be ... UPDATE - may be removed in data2Grid after reload
  } else if ( hints.length == 1 && hints.includes("cdm") ) {
    handleProp( grid, prop); // toggle cmd w/o BE
  } else {
    grid.codeMirror.setValue(qry ?? ""); // persist changed props ..
    handleProp( grid, prop); // toggle cmd w/o BE
  }
}


async function updateGridValue(grid, qry, val, id, callback, rowData) {
  xDebug('connect', "connect::updateGridValue");
  url = new URL("http://localhost:8080/query");
  url.searchParams.append("qry", qry);
  url.searchParams.append("val", val);
  url.searchParams.append("id", id);
  url.searchParams.append("login", getBoxElem4Grid(grid,"context_env").innerHTML);
  url.searchParams.append("version", getBoxElem4Grid(grid,"context_version").innerHTML);
  if (rowData) {
    url.searchParams.append("rowData", JSON.stringify(rowData));
  }
  let response = await fetchMockable(url); // read response body as response
  qry = qry.replaceAll(/ UPDATE *$/g,"").replaceAll(/ UPDATE  */g," "); // remove hints ... should have 2nd channel or be part of ???
  if ("dummy.csv" == qry && id.endsWith(",3") ) {
      console.log("KLUDGE for Testing we disabled it on dummy.csv + column issue ...");
      callback( response );
  } else if (response){
    //
    // assume we get data back -> render it / if not stick to data we have and mark changed value ....
    //
    handleQryResponse(grid, qry, response);
    //
  }  else if (callback){ // if grid is complete reload - there is no need to mark the cell ...
    callback( response );
  }
}



  function getSuggestionFromUrl(grid) {
    xDebug('connect', "connect::getSuggestionFromUrl");
    let json = mockableXMLHttpRequest("http://localhost:8080/suggest?qry=" + encodeURIComponent(getQry2Grid(grid)));
    return json;
  }

/*  Recording ...

    recordActionPrep("create new grid...")
        ... do things like create new grid ...
    recordAction()

    or
        ... do things like create new grid ...
    recordAction("create new grid")

*/

  function recordActionPrep(msg) {
    rec_msg = msg;
  }

  function recordAction( msg = null ) {
    /* TODO Session list is not up-to-date after updating */
    setTimeout(function() {
                        recordActionCall( msg ); // at least is now is traceable ... --> ensure used Vars are not re-used!!!
                    }, 250);
  }

  function recordToggle(event) {
    rec_session_active = rec_session_active > 1 ? 0 : rec_session_active +1;
    event.target.style.color=['grey','orange','red'][rec_session_active];
    if (0==rec_session_active) {
        document.getElementById('msg_rec').innerHTML='';
        document.getElementById('msg_rec_session_old').innerHTML='';
        document.getElementById('msg_rec_session').innerHTML='';
    }
  }


  async function recordSnapshotNameCallback(grid, qry, val, id, callback, rowData){
    val = val.replaceAll(/<br>/g," ").trim();
    recordSend(val.replace("<br>"," ").trim()); // save as record
    callback(""); // color fading
    // TODO use controller w/o trigger myself...
    grid.eGridBox.remove();
    delete grids[ grid.gridNo ];
    // save as session
    name = val ? val.replace(/\s/,"_") : "";
    session2qry( name ); // save as session
  }

  function recordSnapshot( ) {
    // create grid with formular-data to insert
    initFormInput(); // TODO
    var grid_input = createGridContainer("", { pos: { top: "300", left: "600"}, "cdm": "none" }); // TODO cdm - does not work
    var dummy_response = {data :"name and comment snapshot;\nnew_name_here;\n ", prop: { cdm: "none" } }; // TODO dummy col/row to circumvent Layout issues on edited cells - border is cut off by grid with 1 row/1 col only
    handleQryResponseGrid(grid_input, " " /* force handleProp to grid.load(0) // execute cdm-none */, dummy_response ); // init Grid --- kludge hand it over as json ...
    // grid.load(0);
    grid_input.updateGridValue = recordSnapshotNameCallback;
    toggle(grid_input.eGridBox, "+grid-resizeOrMove"); // get shadow ...
    enableEditing({button: 1} /* sim left click*/, grid_input.getElement(1,0));
  }



  function recordActionCall( msg = null ) {
    let text = null;
    rec_session = JSON.stringify(session2qry_()); // always track to allow snapshot
    if (rec_session_active>0){
        xDebug('connect', "connect::recordAction");
        document.getElementById("msg_rec_session_old").innerHTML = rec_session_old;
        document.getElementById("msg_rec_session").innerHTML = rec_session;
        rec_msg = msg ?? rec_msg;
        msg_rec(rec_msg); // log to screen
        //msg_rec("   " + rec_session);
        json = {};
        //
        if (rec_session_active>1) {
            recordSend();
        }
        rec_msg = null;
    }
    rec_session_old = rec_session;

    return text;
  }

    async function recordSend(name = null) {
        url = new URL("http://localhost:8080/rec");
        url.searchParams.append("before", rec_session_old ?? "");
        url.searchParams.append("action", rec_msg ?? "");
        url.searchParams.append("after", rec_session ?? "");
        url.searchParams.append("comment", name ?? "");
        let txt = await fetchMockable( url );
        if (txt) {
            msg_info("record " + ( name ? name + " " : "") + "saved");
            toggle("#msg_shnapshot_icon +msgNew 1000 -msgNew");
        }
    }


    function msg_rec(msg) {
        msg_div = document.getElementById("msg_rec");
        elem = document.createElement("div");
        var x = !msg || msg.includes("dragDropResize") ? "error" : msg.includes("contextmenu") ? "warn"  : msg.includes("contextmenu") ? "warn"  : 'info';
        elem.classList.add("msg_"+x);
        elem.innerHTML = msg ?? "EMPTY MESSAGE";
        elem.onclick = function() {toggle(this,'REMOVE')};
        msg_div.append(elem);
        toggle(elem,  msg && msg.startsWith("  ") ?  "+msgNew 1000 -msgNew 3000 REMOVE_SLOW"    : "warning" == x || "error" == x ? "+msgNew 1000 -msgNew 30000 REMOVE_SLOW" : "+msgNew 1000 -msgNew 30000 REMOVE_SLOW");
    }

    function recElem( elem) {
        return null == elem ? 'null'
                : elem.id ? '"' + elem.id + '"'
                : elem.gridNo ? '"' + elem.gridNo + '"'
                : elem.classList ? '"' + elem.classList + '"'
                : elem ? '"' + elem + '"' : '?';
    }


/*

    UPLOAD FILES ...

*/

    function uploadFiles( droppedFiles ){
      var file = droppedFiles[0];
      var formData = new FormData();
      formData.append('file', file);
      formData.append('fileName', file.name);
      var xhr = new XMLHttpRequest();
      xhr.open('POST', '/upload', true);

      xhr.onload = function() {
        if (xhr.status === 200) {
          msg_info('connect::uploadFiles File uploaded successfully: ' + file.name);
           createGridContainer(file.name + " UPDATE");
        } else {
          msg_error('connect::uploadFiles File upload failed: ' + file.name);
        }
      };

      xhr.send(formData);

    }


/*

    UPLOAD DATA ...

*/

    function uploadData( data ){
      //var fileName = 'drop' + formatTimestamp()+".csv";
      var fileName = 'data.csv';
      var formData = new FormData();
      formData.append('fileName', fileName);
      formData.append('data', data);

      var xhr = new XMLHttpRequest();
      xhr.open('POST', '/upload', true);

      xhr.onload = function() {
        if (xhr.status === 200) {
          msg_info('connect::uploadData Data uploaded successfully: ' + fileName);
           createGridContainer(fileName + " UPDATE");
        } else {
          msg_error('connect::uploadData Data uploaded failed: ' + fileName);
        }
      };

      xhr.send(formData);

    }


