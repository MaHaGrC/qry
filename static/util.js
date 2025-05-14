
  function log(e) {
    xLog('util', e);
  }

  function hint(e) {
    eLog = document.getElementById("hint");
    eLog.value += e + "\n";
    eLog.scrollTop = eLog.scrollHeight;
  }


  function parseIntOptional(val, fallback) {
    val = parseInt(val);
    return Number.isInteger(val) ? val : fallback;
  }

  function parseIntOr0(val) {
    val = parseInt(val);
    return Number.isInteger(val) ? val : 0;
  }


  function parseFloatOptional(val, fallback) {
    val = parseFloat(val);
    return val ? val : fallback;
  }

  function parseFloatOr0(val) {
    val = parseFloat(val);
    return val ? val : 0;
  }

/*
       Array Helper
*/



  function array_resize(arr, size, defval="") {
    if (!arr) {
        arr=[];
    }
    while (arr.length > size) { arr.pop(); }
    while (arr.length < size) { arr.push(defval); }
    return arr;
  }

  function array_resize_2dim(arr, size1, size2, defval) {
    let a1 = arr[0];
    array_resize(a1, size2, "X");
    array_resize(arr, size1, a1);
  }


/*

    elemFromElemOrId

*/

  function elemFromElemOrId( elemIdOrElem ) {
    // allow function to be easy used toggle('div_log') and easy chained toggleClass(toggle('div_log'), 'fa-close fa-open')
    elem = undefined;
    if (typeof elemIdOrElem === 'string' || elemIdOrElem instanceof String) {
        if (!elemIdOrElem || "" == elemIdOrElem) {
            //console.warn("elemFromElemOrId called without params");
            elem = undefined;
        } else if (elemIdOrElem.includes(' ')) {
            elem = undefined;
        } else if (elemIdOrElem.startsWith('#')) {
            elem = document.getElementById( elemIdOrElem.substring(1) );
        } else {
            elem = document.getElementById( elemIdOrElem );
        }
    } else if (typeof elemIdOrElem instanceof Array){
        elem = undefined;
    } else {
        elem = elemIdOrElem;
    }
    if ( undefined == elem ) {
        xDebug('util', 'elemFromElemOrId: element(' + elemIdOrElem + ') unknown');
    } else {
       // xDebug('util', 'elemFromElemOrId: element(' + elemIdOrElem + ') => ' + elem + " (id: " + elem.id + ")");
    }
    return elem;
  }



  function elemChildByClass( elem, childClass, orSelf = true ) {
    // consider getBoxElem4Grid
    return !elem ? null : orSelf && elem.classList.contains( childClass ) ? elem : elem.getElementsByClassName(childClass)[0];
  }



  function classFilterMatch(elem, classFilter){
    var found = true;
    if (elem && classFilter) {
        found = false;
        for (let x of classFilter) {
            found = found || elem.classList.contains(x);
        }
    }
    return found;
  }



  function elemWithId(elem, classFilter = null) {
    while( elem && !(elem.id && classFilterMatch(elem, classFilter))) {
        elem = elem.parentNode;
    }
    return elem;
  }


/*

    Grid-Helper

*/

function findGrid( eventOrElement) {
    elem = eventOrElement.target ?? eventOrElement;
    while(elem && elem.parentNode && (!elem.classList || !elem.classList.contains("grid-with-filter")) ) {
        elem = elem.parentNode;
    }
    return elem && elem.grid ? elem.grid : eventOrElement.eGrid ? eventOrElement : null;
}

function getQry2Grid(grid) {
  return grid ? grid.codeMirror.getValue() : "";
}

function getBox4Grid(grid) {
    return grid.parentNode;
}

function getBoxElem4Grid(grid, className) {
    var gridBox = null == grid ? null : (grid.eGrid ?? grid).parentNode ?? (grids[grid] ? grids[grid].eGrid.parentNode : grid );
    var elements = gridBox ? gridBox.getElementsByClassName( className ) : null;
    return elements && elements.length>0 ? elements[0] : null ;
}

/*

*/

function optional(val, fallback) {
    return null == val || "" == val ? fallback : val ;
}



/*

    TIMESTAMP

*/


function formatTimestamp(timestamp = Date.now()) {
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  const formattedTimestamp = `${day}${month}${year}_${hours}${minutes}${seconds}`;
  return formattedTimestamp;
}









/*

Helper to handle mousedown, click, dblclick

*/

let ms_k = 0;

function mouseDownClickDbl(element, clbkMouseDown, clbkClick, clbkDblClick) {
    element.onmousedown = function(event) {msDown(event, clbkMouseDown)};
    element.onclick = function(event) {msClick(event, clbkClick, clbkDblClick)}; // DbClick may not work - so use event.Detail WO
    element.dblclick = function(event) {msDblClick(event, clbkDblClick)};
}


function msDown(e, clbk){
    if (1 == e.which) { // left mouse button -- 2: middle, 3: right -- right mouse for menu
        e.preventDefault();
        e.stopPropagation (); // prevent moving head-column
        ms_k = 0;
        console.log("msDown ??? " + e + " " + (new Date()).getTime());
        setTimeout(function(){
            if (0 == ms_k /*ms_t_down == ms_t_last*/) {
                console.log("msDown !!!" + e + " " + (new Date()).getTime());
                if (clbk) clbk(e);
                ms_k = null;
            }
        },500); // wait for click/dbDblClick
    }
}

function msClick(e, clbk, clbk2){
    if (1 == e.which && 0 == ms_k) { // left mouse button -- 2: middle, 3: right -- right mouse for menu
        ms_k = 1;
        e_detail = e.detail; //KLUDGE have to save it...
        console.log("msClick ???" + e.detail + " " + (new Date()).getTime());
        setTimeout(function(){
            if (1 == ms_k /*ms_t_click == ms_t_last*/) {
                ms_k = null;
                console.log("msClick !!!" + e_detail /*e.detail*/ + " " + (new Date()).getTime())
                clbk_ =  1==e_detail ? clbk : clbk2;
                if (clbk_) clbk_(e);
              }
        },300); // wait for dbDblClick
    } else {
        console.log("msClick SKIPP" + e + " " + (new Date()).getTime()); // suppress long-click =  mouseup after already processing msDown
    }
    return true;
}

function msDblClick(e, clbk){
    if (1 == e.which && 1 == ms_k) { // left mouse button -- 2: middle, 3: right -- right mouse for menu
        ms_k = 2;
        console.log("msDblClick !!!" + e + " " + (new Date()).getTime());
        if (clbk) clbk(e);
    } else {
        console.log("msDblClick SKIPP " + e + " " + (new Date()).getTime()); // already handle as down ...
    }
    return true;
}

/*
mouseDownClickDbl( document.getElementById("myBox"), clbk1,clbk2,clbk3  );
*/
function clbk1(e){console.log("clbk1 "+e)}
function clbk2(e){console.log("clbk2 "+e)}
function clbk3(e){console.log("clbk3 "+e)}

/*
   TODO move to GRID ???
*/
function clbkColDrag(event){return gridDragColumn(event,event.target);}
function clbk2ColFilter(e){
    console.log("clbk2ColFilter " + e);
    var grid_elem = elemWithId(e.target,["grid-item","grid-idx", "grid-head"]);
    if (grid_elem) {
        var grid_cell_elem = elemChildByClass(e.target,"colName", true);
        if (grid_cell_elem) {
            if (!grid_elem.classList.contains("filtered")){ // only reset on change from columnName to filter ...
                grid_cell_elem.innerHTML_BAK = grid_cell_elem.innerHTML;
                grid_cell_elem.innerHTML="";
                // reset HTML if cell losese focus
                grid_cell_elem.onblur = function(e){ // on focusOut
                    if (e.target.innerHTML == "") {
                        e.target.innerHTML = e.target.innerHTML_BAK;
                    }
                    e.target.onblur = null;
                }
            }
            enableEditing(e, grid_cell_elem);
        } else {
            enableEditing(e, grid_elem);
        }
    } else {
        enableEditing(e, grid_elem);
    }
};
