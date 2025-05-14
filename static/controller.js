function classNext(elem, classes) {
  // toggle classe with multiple states .. wenn nichts gefunden wird nimmt ersten wert
  let i = 0;
  for (let index = 0; index < classes.length; index++) {
    if (elem.classList.contains(classes[index])) {
      elem.classList.remove(classes[index]);
      i = index + 1 < classes.length ? index + 1 : 0;
      break;
    }
  }
  xDebug('controller', "classNext: " + i);
  elem.classList.add(classes[i]);
  return i; // not found
}


// obsolete - replaced by dragresize ....
function  controller_resize(grid,col, event, elem){
    // !! event.target is not column but flag in column !!
    let col2 = event.target.parentNode.id.replaceAll(/.*_/g,"");
    xDebug('controller', event.target.parentNode.parentNode.style.gridTemplateColumns);
    let cssGridNode = event.target.parentNode.parentNode;
    let sizes = cssGridNode.style.gridTemplateColumns.split(" ");
    sizes[ col2 ] = "10px";
    cssGridNode.style.gridTemplateColumns = sizes.join(" ");
    xDebug('controller', event.target.parentNode.parentNode.style.gridTemplateColumns);
    //xDebug('controller', grid);
    //xDebug('controller', grid);
    //xDebug('controller', grid.style.gridTemplateColumns);
}

function controller2(grid, qry, elem = null, closeContextMenu = true, event = null) {
    event.preventDefault();
    event.stopPropagation ();
    controller(grid, qry, elem, closeContextMenu, event);
}


function controller(grid, qry, elem = null, closeContextMenu = true, event = null) {
  /*
  if (!grid && elem) {
    grid = findGrid(elem).grid;
  }
  */
  if (!elem && event) {
    elem = event.target;
  } else if (elem && elem.target) {
    event = elem;
    elem = elem.target;
  }
  if (!grid && elem) {
    grid = findGrid(elem);
  } else if (grid && !grid.eGrid) { // assume gridBox - not grid
    grid = findGrid(grid);
  }
  if (grid) {
    contextMenuCircular.event_grid = grid;
  }
  xLog('controller', "controller grid: " + ( grid ? grid.id : "" ) + " >> "+ qry + "  " + (elem ?? ""));
  recordActionPrep("controller: grid: " + recElem( grid ) + ', qry: ' + recElem( qry )+ ', elem: ' + recElem( elem ) );

  if (closeContextMenu) {
    contextMenuCircularClose();
  }

  if (qry.match(/REFRESH:/)) {
    getUrl2GridAndHint(grid, ""); // read again - use Cache if available - used for long running queries
  } else if (qry.startsWith("MENU:")){
    let txt = elem.innerHTML ?? elem;
    xLog('controller', "create menu for " + txt );
    // new Grid( grid_master.getElementsByClassName( "grid-container" )[0], grid_master  );
    let grid_tmp = new Grid (null, null); // use grid to load and convert ...
    //
    //
    if (event) {
      const { clientX: mouseX, clientY: mouseY } = event;
      const { normalizedX, normalizedY } = normalizePozitionCircular(mouseX, mouseY);
      contextMenuCircular.style.top = `${normalizedY}px`;
      contextMenuCircular.style.left = `${normalizedX}px`;
    }
    //
    data2Grid( grid_tmp, txt, createMenuFromGrid); // as calling data is async ...
  } else if (qry.match(/UPDATE:ALL/)) {
    for( let grid_idx in grids) {
        getUrl2GridAndHint(grids[grid_idx], "UPDATE"); // match Cache.java  -- force DataConnection
    }
  } else if (qry.startsWith("LOGIN::")) { // LOGIN::<LOGIN_ID> - open login dialog
    let loginId_next= qry.replaceAll(/LOGIN::/g,"");
    document.getElementById('login').style.display = 'block';
    if (loginId_next) {
        loginId_current = loginId_next;
        document.getElementById('userPwd').focus();
    } else {
        document.getElementById('loginId').focus();
    }
    document.getElementById('loginId').value = loginId_current;
    loginUpdate();
  } else if (qry.startsWith("LOGIN:ALL:")) { // LOGIN:ALL:<ENV> - set loginId for all grids
    loginId_current= qry.replaceAll(/LOGIN:(ALL:)?/g,"");
    for( let grid_idx in grids) {
        let ctx = getBoxElem4Grid(grids[grid_idx],'context_env');
        if (ctx) {
            ctx.innerHTML = loginId_current;
            getUrl2GridAndHint(grids[grid_idx], "");
        }
    }
  } else if (qry.startsWith("LOGIN:")) { // LOGIN:(UPDATE:)?<ENV>
    loginId_current= qry.replaceAll(/LOGIN:(UPDATE:)?/g,"");
    getBoxElem4Grid(grid,'context_env').innerHTML = loginId_current;
    getUrl2GridAndHint(grid, qry.includes("UPDATE:") ? "UPDATE" : "");
  } else if (qry.match(/UPDATE:/)) {
    getUrl2GridAndHint(grid, "UPDATE"); // match Cache.java  -- force DataConnection
  } else if (qry.startsWith("UPLOAD_FILE:")) {
    toggle("form_upload","display"); // for now toggle dialog ...
  } else if (qry.startsWith("PASTE:")) {
    handlePaste(event);
  } else if (qry.startsWith("SCROLL_END:")) {
    var qry = grid2qry( grid ).qry;
    var rowCount = grid.items.length;
    var rowCount = rowCount < 100 ? 100 : rowCount < 250 ? 250 : rowCount + 500;
    if (qry.match(/.*\blimit\b.*/i)){
        qry = qry.replaceAll(/\blimit\s+([0-9,])+/g,"limit 1,"+rowCount);
        getUrl2GridAndHint(grid, ''); // adjust limit ... TODO do smooth scrolling/preloading ...
    } else {
        if (event && event.ctrlKey) { // allow to combine extend + UPDATE ...
            getUrl2GridAndHint(grid, ['limit: "' + rowCount + '"',"UPDATE"]);; // hint may be ... UPDATE - may be removed in getUrl2GridQry after reload
        } else {
            getUrl2GridAndHint(grid, 'limit: "' + rowCount + '"');
        }
    }
  } else if (qry.startsWith("GRID:NEWTAB:")) {
    var qry = grid2qry( grid );
    if (qry) {
        let sessGrids = [];
        let sess = {};
        delete qry.prop.pos ; // remove position if only one grid - but keep it if multiple grids inside ...
        sessGrids.push(qry);
        sess.session = "newTab";
        sess.grids = sessGrids;
        let qry_ = JSON.stringify(sess);
        console.log("session2qry " + qry_);
        //window.open("/?qry=" + encodeURIComponent(qry_), "_blank"); // new Tab
        window.open("/?qry=" + encodeURIComponent(qry_), "_blank",'location=no,height=570,width=520,scrollbars=no,status=no,menubar=no,resizable=yes,titlebar=no,toolbar=no');
        controller(grid,"CLOSE:");
    }
  } else if (qry.startsWith("CACHE:")) {
    getUrl2GridAndHint(grid, qry.replaceAll(/ .*/g,"")); // keep CACHE:1526324646_1296829861   value
  } else if (qry.match(/FILTER:/)) {
    elem.classList.toggle('icon-inactiv');
  } else if (qry.match(/SORT:/)) {
    //var sort = classNext( elem, [ "fa-sort", "fa-sort-alpha-asc", "fa-sort-alpha-desc" ]);
    //var sort = classNext( elem, [ "fa-sort", "fa-sort-asc", "fa-sort-desc" ]);
    var sortModeIdx = classNext(elem, ["fa-sort", "fa-chevron-down", "fa-chevron-up"]);
    // grid.sortModeAvail = ["","asc","desc","num-asc","num-desc"];
    // expect "SORT:<col>"
    grid.sort(1 * qry.replace(/.*:/, ""), grid.sortModeAvail[sortModeIdx]); // enforce int //or from parent node ...
  } else if (qry.startsWith("FILTER-BY-VALUE:") && elem) {
    let [ r ,c ] = grid.getRowAndColOf(elem);
    let val = grid.items[ r ][ c];
    let col = grid.items[ 0 ][ c];
    let exp = col + " " +  ( qry.includes(":LT:") ? "<" : qry.includes(":GT:") ? ">" : qry.includes(":LE:") ? "<=" : qry.includes(":GE:") ? ">=" : "=" )  + " \'" + val + "\'";
    if (qry.includes(":NOT:")){
        exp = " NOT ( " + exp + ")";
    }
    let op = qry.includes(":OR:") ? " OR " : " AND ";
    let gridqry = getQry2Grid(grid);
    gridqry = gridqry.replace(/ limit  /i," limit ");
    if (!gridqry.includes(" W ") && !gridqry.toLowerCase().includes(" where ")){
        if( gridqry.includes(" limit ") ){
            gridqry = gridqry.replace(/  *limit  */i," W " + exp + " limit ");
        } else {
            gridqry = gridqry + " W " + exp;
        }
    }else {
        gridqry = gridqry.replace(/  *(where|W)  */i," W " + exp + op);
    }
    data2Grid( grid, gridqry );
  } else if (qry.startsWith("REF-BY-VALUE:") && elem) {
    let ref = qry.replace("REF-BY-VALUE:","");
    let gridqry = getQry2Grid(grid);
    gridqry = gridqry.replace(/^([^\s]*)/,"$1"+ ref);
    data2Grid( grid, gridqry );
  } else if (qry.startsWith("LOOKUP:") && elem) {
    let [ r ,c ] = grid.getRowAndColOf(elem);
    let val = grid.items[ r ][ c];
    let col = grid.items[ 0 ][ c];
    if (val.match(/^[0-9]+$/)) {
        if (qry.startsWith("LOOKUP:TEXT:")) {
            createGridContainer("v_text where textid="+val);
        } else {
            createGridContainer("v_id where id="+val);
        }
    } else {
        msg_warn("LOOKUP only available for number values (" + val + ")")
    }
  } else if (qry.startsWith("LIMIT:")) {
    let gridqry = getQry2Grid(grid);
    let limit_clause = qry.toLowerCase().replaceAll(":"," ");
    if (gridqry.toUpperCase().includes(' LIMIT ')){
        gridqry = gridqry.replace(/ limit  *[0-9, ]*/i, " " + limit_clause);
    } else {
        gridqry = gridqry + " " + limit_clause;
    }
    data2Grid( grid, gridqry.trim());
  } else if (qry.startsWith("GRID:NAME:")) {
    let txt = (elem ? elem.innerHTML : null ) ?? elem ?? qry.replace("GRID:NAME:", "") ?? ""; // TODO - fix should change prio ... qry-text first and innerHTML second
    grid.setName( txt );
  } else if (qry.startsWith("GRID:") && elem) {
    let txt = elem.innerHTML ?? elem;
    if (qry.startsWith("GRID:BY_NAME[")) {
        var name = qry.replace("GRID:BY_NAME[","").replaceAll(/].*/g,"");
        let grid_tmp = grids_by_name[ name ];
        if (grid_tmp && grids[grid_tmp.gridNo]) {
            data2Grid( grid_tmp, txt);
        } else {
            grid_tmp = createGridContainer(txt, { "name": name});
        }
    } else if (qry.startsWith("GRID:ADD:")) {
        txt = qry.replace("GRID:ADD:","");
        txt = txt.length ? txt : elem.innerHTML ?? elem; // TODO use order a general approach
        createGridContainer(txt);
    } else if (qry.startsWith("GRID:NEW:")) {
        createGridContainer(null);
    } else {
        data2Grid( grid, txt);
    }
  } else if (null == grid && qry) { // â like GRID:ADD:... - but without grid / without
        createGridContainer(qry);
  } else if (qry.startsWith("CLOSE:")) {
    if (qry.startsWith("CLOSE:ALL")) {
        for( let grid_idx in grids) {
            grid = grids[grid_idx];
            if (grid) {
                if (grid.name) {
                    grids_by_name[ grid.name ] = null;
                }
                grid.eGridBox.remove();
                grids[grid_idx] = null;
            }
        }
    } else {
        grids[grid.gridNo] = null;
        if (grid.name) {
            grids_by_name[ grid.name ] = null;
        }
        grid.eGridBox.remove();
    }
    document.getElementById("cdm_hint_auto_sug").style.display = "none";
    removeTooltipForce(); // TODO should close by itself
  } else if (qry){
    data2Grid( grid, qry);
  }
  recordAction( ); /*DONE*/
}

