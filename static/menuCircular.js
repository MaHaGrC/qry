const contextMenuCircularId = "context-menu-circular";
const contextMenuCircular = document.getElementById(contextMenuCircularId);
const contextMenuCircularScope = document.querySelector("body"); // to DISABLE set body___
contextMenuCircular_lastFocus = null;
contextMenuCircular_on = false;
contextMenuCircularItems = null;
contextMenuCircularFilterBar = [
                                   {
                                     name: 'file',
                                     checked: true
                                   },
                                   {
                                     name: 'window-close',
                                     checked: true
                                   },
                                   {
                                     name: 'arrow-circle-right',
                                     checked: true
                                   },
                                   {
                                     name: 'window-close',
                                     checked: true
                                   },
                                   {
                                     name: 'arrow-circle-right',
                                     checked: true
                                   },
                                   {
                                     name: 'window-close',
                                     checked: true
                                   },
                                   {
                                     name: 'arrow-circle-right',
                                     checked: true
                                   },
                                   {
                                     name: 'window-close',
                                     checked: true
                                   },
                                   {
                                     name: 'arrow-circle-right',
                                     checked: false
                                   },
                                   {
                                     name: 'image',
                                     checked: true
                                   }
                                 ];

//contextMenuCircular.style.top = "150px";
//contextMenuCircular.style.left = "400px";
//contextMenuCircular.style.height = "200px";
/*
contextMenuCircular.classList.add("visible");
contextMenuCircular.innerHTML += '<div>tada</div>';
contextMenuCircular.innerHTML += '<div class="cmc-item" style="top: 39px;left: 50px">tadata</div>';
contextMenuCircular.innerHTML += '<div class="cmc-item" style="top: 78px;left: 100px">tadata</div>';
*/
function insertRow(){
    let cell = contextMenuCircular.event_gridCellElem;
    if (cell) {
        let grid = contextMenuCircular.event_gridElem.grid;
        let qry = contextMenuCircular.event_grid.codeMirror.getValue();
        let val = "";
        let id = cell.id.replace(/^.*_(.*)_.*/, "$1" ) ; // gridX_1_2 --> 2
        if (id.match(/^[0-9]+$/)) {
            id++; // keep headline ...
            updateGridValue(grid, qry, val, id,
                function() {
                    grid.insertRow(cell, id) ; // replay locally without loading ...
                    console.debug("menuCircular::ROW " + id +  " inserted");
                }
            );
        }
    }
}

    function getSystemMenuItems() {
      //contextMenuCircular.style.top = "150px";
      //contextMenuCircular.style.left = "400px";
      //contextMenuCircular.style.heigth = "200px";
      //contextMenuCircular.classList.add("visible");

      //                 -                        3  |  0       4 | 1
      //              <-   ->                    ----------    -------
      //             <-  x  ->                    2  |  1       3 | 2
      //
      items = ["read", "reset", "a", "b", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13];
      items = [
          { descr: "login", icon: 'fa fa-arrow-circle-right', action: "controller(null,'LOGIN::');" }
        //, { descr: "update 2,2", icon: 'fa fa-file', action: "contextMenuCircular.event_grid.updateData( 2,2, 'value from extern');return true;" }
        //, { descr: "logFill", icon: 'fa fa-file', action: "log('a');log('ab');log('abc');log('abcd');log('a');log('ab');log('abc');log('abcd');" }
        , { descr: "url.check", icon: 'fa fa-file', action: "controller(contextMenuCircular.event_grid,'GRID:ADD:','url.check');" }
        , { descr: "grid add", icon: 'fa fa-window-restore', action: "controller(contextMenuCircular.event_grid,'GRID:ADD:','');createContextMenu4NewGrid(event);" }
        //, { descr: "grid close", icon: 'fa fa-window-close', action: "contextMenuCircularClose();contextMenuCircular.event_gridElem.remove();" }
        //, { descr: "insert", icon: '', action: "contextMenuCircularClose();insertRow();" }   // --> id=row and val is null
        //, { descr: "show all columns", icon: 'fa fa-eye', action: "contextMenuCircularClose(); console.debug('show-all-cols')  ; let g=contextMenuCircular.event_grid; let t = g.colOrderIdx_OLD ;  g.colOrderIdx_OLD = g.colOrderIdx ; g.colOrderIdx = t  ;contextMenuCircular.event_grid.load(0,0);" }
        , { descr: "++FONT", icon: 'fa fa-font', action: "event.preventDefault;event.stopPropagation(); resizeFont(1,'.grid-container');return false;"}
        , { descr: "font--", icon: 'fa fa-font', action: "event.preventDefault;event.stopPropagation(); resizeFont(-1,'.grid-container');return false;"}
        , { descr: "dbTabusage", icon: "fa fa-database", action: "controller(contextMenuCircular.event_grid,'GRID:ADD:','v_dbTabusageLinked');"}
        , { descr: "TEST", icon: 'fa fa-bug', action: "runTest();"}
        , { descr: "Save Session" , icon: 'fa fa-regular fa-bookmark', action: "contextMenuCircularClose();session2qry();"}
        , { descr: "Edit Session" , icon: 'fa fa-solid fa-book-open,fa fa-solid fa-book-open',action: "controller(contextMenuCircular.event_grid,'GRID:ADD:','SESSIONS UPDATE');" }  // will check for Sessions in SQL-LITE via Synonym
        , { descr: "Load Session" , icon: 'fa fa-solid fa-book-open',action: "controller(contextMenuCircular.event_grid,'MENU:','SESSIONS UPDATE');" }  // will check for Sessions in SQL-LITE via Synonym
        , { descr: "Upload File" , icon: 'fa fa-file',action: "controller(contextMenuCircular.event_grid,'UPLOAD_FILE:');" }  // will check for Sessions in SQL-LITE via Synonym
        , { descr: "Paste" , icon: 'fa fa-paste',action: "controller(contextMenuCircular.event_grid,'PASTE:',null,true,event);" }  // will check for Sessions in SQL-LITE via Synonym
        , { descr: "UPDATE ALL", icon: 'fa fa-sync',  action: "controller(null,'UPDATE:ALL:');" }
        , { descr: "CLOSE ALL", icon: 'fa fa-window-close',  action: "controller(null,'CLOSE:ALL:');" }
        /* assume pre sorted --- 1st-col / 2nd-col  */
        //, { descr: "group more("+(contextMenuCircular.event_grid.colsGrouped+1)+")", icon: 'fa fa-solid fa-chevron-down',  action: "contextMenuCircularClose();let g=contextMenuCircular.event_grid;g.colsGrouped++;g.load(0, 0);"}
        //, { descr: "group less("+(contextMenuCircular.event_grid.colsGrouped-1)+")", icon: 'fa fa-solid fa-chevron-left',  action: "contextMenuCircularClose();let g=contextMenuCircular.event_grid;if (g.colsGrouped > 0) --g.colsGrouped;g.load(0, 0);"}
        //, 13, 14, "a", "b"
        ];
      return items
  }

function createFilterBarForItems( filters ) {
  xLog('menuCircular', 'create filter bar icons with ', filters, ( (filters.length * 16) + ((filters.length - 1) * 5) / 2 ));
   //KLUDGE
   removeTooltipForce(); // starting key-in menu-prefix will open a tooltip that is not removable ...

  const circularMenu = document.querySelector('#context-menu-circular');
  let filterHtml = document.querySelector('#circular-menu-filter-bar');
  if( !filterHtml) {
      filterHtml = document.createElement('div');
      filterHtml.classList.add('cmc-filter-bar');
      filterHtml.id = 'circular-menu-filter-bar';
      circularMenu.appendChild(filterHtml);
  }

  let filtersCount = 0;
  let filterHtmlContent = '';
  let filterHtmlCurrent = '';
  let filterChecked = 0;
  let half = -1; // bottom / top - until end of screen is reached ...
  let top = parseInt(circularMenu.style.top) + 8; // compensate half of first button - is centered
  let i2 = 0;
  for( var filterName in filters) {
    i2++;
    if (filterName.includes(" ")) { // KLUDGE skipp lookups...
        filtersCount++; // as we skipp some - we have to count ...
        let action = "handleMenuFilter(this);";
        let action_custom = null;
        let tooltip = null;
        if (1 == filters[filterName]) {
            action_custom = filters[ "ACTION::" + filterName.replaceAll(/[^a-zA-Z]+/g,"") ];
            if (action_custom) {
                //action = action_custom;
                action = "event.preventDefault();event.stopPropagation();" + action_custom; // as is would execute twice --  try with column-head-link-ref-Tab
                tooltip = "data-tooltip='" + filters[ "TOOLTIP::" + filterName.replaceAll(/[^a-zA-Z]+/g,"")] + "'";
            }
        }
        filterHtmlCurrent = "";
        filterHtmlCurrent += `<label class='cmc-filter-bar-label ${ action_custom ? "action_custom tooltip" : "" }' ${ tooltip ?? "" }  `;
        if (1 == filters[filterName] ) {
            let refMenuId = filters[ "MENU::" + filterName.replaceAll(/[^a-zA-Z]+/g,"")];
            filterHtmlCurrent += ' onmouseenter=\'fhEnter(event, "' +  refMenuId + '")\'  onmouseleave=\'fhLeave(event, "' +  refMenuId + '")\' ';
        }
        filterHtmlCurrent += ' tt="TTTTT" ';
        filterHtmlCurrent += '>';
        filterHtmlCurrent += `<input type='checkbox' name='filters' value='${filterName.replaceAll(/[^a-zA-Z]+/g,"")}' ${ filters[filterName] < 0 ? 'checked' :  ''}  onclick="${action}" /> `;
        let filterIcon = filters[filterName] < 0 ? filterName : filterName.replaceAll(/color:[^;'"]*/g,"");
        // KLUDGE prevent circular-menu-close as default action by click on icon - as show-cols want's to stay open ...
        filterIcon = filterIcon.replaceAll(/<i /g,`<i onclick="${action}"`);
        filterHtmlCurrent += `${ filterIcon }`; // KLUDGE remove color - can't define non-checked important! by css without effecting checked one ...
        // add top or bottom alternating - if place
        top = half < 0 ? top - 16 : top ;
        half = top < 20 || filtersCount < 4 ? 1 : -1 * half; // alternating until out of screen

        filterHtmlCurrent = filterHtmlCurrent.replaceAll(/TTTTT/g, filtersCount + ":top:" + top + ";half: " + half);
        filterHtmlCurrent += `</label>`;

        filterHtmlContent = half < 0 ? filterHtmlCurrent + filterHtmlContent : filterHtmlContent + filterHtmlCurrent ;
    }
  }
  /*
  const heightFix = 13;
  const halfFilterBarHeight = ( ( (filtersCount * 16) + ( (filtersCount - 1) * 5) ) / 2 ) - heightFix;
  filterHtml.style.top = `${circularMenu.clientTop - halfFilterBarHeight}px`;
  */
  filterHtml.style.top = `${ - parseInt(circularMenu.style.top) + top}px`; // relative to center of menu === center of menu is top=0 -->
  filterHtml.style.left = `${circularMenu.clientLeft + 2}px`;

  filterHtml.innerHTML = filterHtmlContent;
  //
  //
  //  initTooltip(); // TOOLTIP or highlight corresponding menu ...
  //
  //
  console.log("menuCircular::createFilterBarForItems ");
  console.log(filterHtmlContent);
}

function fhEnter(event, refMenuId) {
    toggle(refMenuId, "+cmc-item-highlight");
    //setTooltip(e);
}

function fhLeave(event, refMenuId) {
    toggle(refMenuId, "-cmc-item-highlight");
    //removeTooltip(event);
}


function handleMenuActive(e){
    // active next menu item on keydown
    // active previous menu item on keyup
    var keyCode = e.keyCode || e.which;
    if(keyCode=='37' || keyCode=='39' || keyCode=='38' || keyCode=='40'){
        keyDirectionTD = keyCode=='38' ? 1 /* UP */ : keyCode=='40' ? -1 /* DOWN */: 0 /*LR-Switch*/;
        console.debug("menuCircular::handleMenuActive " + (keyCode=='37' ? "LEFT" : keyCode=='39' ? "RIGHT" : keyCode=='38' ? "UP" : keyCode=='40' ? "DOWN" : keyCode) );
        let elem = document.querySelector('#context-menu-circular  .cmc-item-byKey');
        if (!elem) {
            elem = document.querySelector('#context-menu-circular > div');
        } else {
            elem.classList.remove('cmc-item-byKey');
            //elem = event.keyCode == '38' ? elem.previousElementSibling : elem.nextElementSibling;
            // as circular menu is a "spiral" it starts and runs clockwise - so we have to jump...
            //
            // 11-l-Top <> 8-r-TOP         /\
            //  7-l-Top <> 4-r-TOP         |  2
            //  3-l-Top <> 0-r-TOP    <<< Start
            //  2-l     <> 1-r            |  1
            //  6-r     <> 5-l           \/
            //  10-r    <> 9-l
            var index = Array.prototype.indexOf.call(elem.parentNode.children, elem);
            var nextIndex = null;
            if ( 0 == keyDirectionTD) {
                var orientationLR = elem.classList.contains('cmc-item-left') ? -1 : 1;
                var orientationTD = elem.classList.contains('cmc-item-top') ?  3 : 1;
                nextIndex = index + orientationLR * orientationTD;
            } else {
                let orientationTD = elem.classList.contains('cmc-item-top') ? 1 : -1;
                nextIndex = index + keyDirectionTD * 4 * orientationTD; // DOWN in TOP-Half means lower index / DOWN in BOTTOM-Half means higher index
                console.debug("menuCircular::handleMenuActive " + (keyDirectionTD < 1 ? "DOWN" : "UP") + " index: " + index + " nextIndex: " + nextIndex);
                if (nextIndex < 0) {
                    // 0 > -4 -> 1
                    // 1 > -3 -> 0
                    // 2 > -2 -> 3
                    // 3 > -1 -> 2
                    nextIndex =  nextIndex <= -4 ? 1 : nextIndex <= -3 ? 0 : nextIndex <= -2 ? 3 : 2;
                }
            }
        }
        if (nextIndex >= 0 && nextIndex < elem.parentNode.children.length) {
            elem = elem.parentNode.children[ nextIndex ];
        } else {
            elem = document.querySelector('#context-menu-circular > div');
        }
        elem.classList.add('cmc-item-byKey');
    } else if (keyCode=='13') {
        execMenuFiltered();
    }

}

function execMenuFiltered(){
    // execute the most relevant, if only one is left execute this ...
    // get first child of context-menu-circular
    if  (document.getElementById('inputDummy') === document.activeElement) {
        let elem = document.querySelector('#context-menu-circular  .cmc-item-byKey') || document.querySelector('#context-menu-circular > div');
        console.log('execMenuFiltered ' + ( elem ? elem.id : ""));
        document.getElementById('inputDummy').value = "";
        elem.click();
    } else {
        console.log('execMenuFiltered - SKIPP (inputDummy not active)'); // onchange might be raised if after key-in a button is clicked ...
    }
}


function handleMenuFilter(elem){
    if (elem) {
        let filterName = elem.value;
        //
        filterName = contextMenuCircularFilterBar[ filterName ]; // tricky lookup filter by identifier
        contextMenuCircularFilterBar[ filterName ] = contextMenuCircularFilterBar[ filterName ] < 0 ? 0 : -1 ; // -1=activeFilter <-> 0=ResetCount
    }
    //
    let filterText = document.getElementById('inputDummy').value;
    createMenuFromItems( contextMenuCircularItems, contextMenuCircularFilterBar, filterText);
    createFilterBarForItems( contextMenuCircularFilterBar );
    var elem = document.querySelector('#context-menu-circular > div');
    elem.classList.add('cmc-item-byKey');
    // hack to enable animation
    setTimeout(() => {
        toggle( contextMenuCircular, "+visible #menuBackground +display ");
    });
}

function createMenuFromItems( items, filters = null, txtFilter = null) {
    createMenuFromItems_( items, filters, txtFilter );
    if ( "" == contextMenuCircular.innerHTML){
        createMenuFromItems_( items, filters, txtFilter, true );
    }
}

function createMenuFromItems_( items, filters = null, txtFilter = null, tolerant = false ) {
  if (!items) return;
  contextMenuCircularItems=items; // load current items
  //

  //
  var bounding = contextMenuCircular.getBoundingClientRect();
  contextMenuCircular.innerHTML = "";
  filters = filters ?? {};
  let i = 0;
  let l = 1;
  let width = 150;
  const gap = 30; // Kontrolliert den zwischenraum zwischen der Linken und Rechten Buttonleiste
  let q = 0;
  // check if at least one is set ...
  filters["filterChecked"] = 0;
  let camelCase = (txtFilter ?? "").replaceAll(/([A-Z])/g, ".*$1");
  for( var filterName in filters) {
    if (filterName.includes(" ")) { // KLUDGE skipp lookups...
        filters["filterChecked"] += filters[filterName] < 0 ? 1 : 0 ; // is at least 1 filter active?
        if (filters[ filterName ] > 0) { // keep -1 == filter[ filterName ]  as this is the active filter
            filters[ filterName ] = 0; // reset for count ...
        }
    }
  }
  //
  while (i < items.length) {
      // skipp non flagged icon
        let icon_ = null; // not null as null = Signal to skipp
        if (filters["filterChecked"] || txtFilter) { // only activate if at least on is switched - if non is switched show all
            while ( i < items.length ) {
                let icon_ = items[i].icon;
                let descr_ = items[i].descr;
                if (icon_ || txtFilter) {
                    let icon_matched = false;
                    for( var filterName in filters) {
                        if ((!filters["filterChecked"]) || (filters[filterName] < 0 && icon_ && icon_.includes(filterName))) {
                            if (!txtFilter || descr_.includes(txtFilter)
                                    || (tolerant && (
                                                 descr_.toLowerCase().includes(txtFilter)
                                              || descr_.match(camelCase)
                                            )
                                    )
                                ) {
                                icon_matched = true; // if contains at least 1 relevant icon  - leave it ...
                                break;
                            }
                        }
                    };
                    if (icon_matched) { // stop skipping - show current one
                        break;
                    }
                }
                i++;
            }
        }

      //

      if (i >= items.length) {
        break;
      }
      // draw action
      x = (22 * (l < 4 ? 4 - l : 0) + gap) * (q < 2 ? 1 : -1) - (q < 2 ? 0 : width); // 1 & 3  ---
      y = (23 * l - 10) * (3 === q || 0 === q ? -1 : 1);
      // txt = items[ i ];
      if ((bounding.top + y > 0) && (bounding.left + x > 0)) {
        if (!(typeof items[i] === 'object')) {
          items[i] = { descr: items[i], action: 'alert(\'' + items[i] + '\');' };
        }
        //xLog('menuCircular', items[i]);
        if (!(typeof items[i].icon === 'undefined' || items[i].icon.match(/^</))) {
            let icon="";
            for (let icon_ of items[i].icon.split(",")) {
                icon += '<i class="' + icon_ + '" style="color: rgba(0, 0, 255, 0.5);"></i>';
            }
            items[i].icon = icon;
        }
        // if (items[i].icon) {
        //     items[i].icon = items[i].icon.replace(' style="',' style="display:none;')
        // }
        let cmdId = "cm" + Math.random().toString(16).slice(2);
        txt = 'id="' + cmdId + '" class="cmc-item ' + (q < 2 ? '' : 'cmc-item-left') + " " + (y < 0 ? 'cmc-item-top' : '') + '" ';
        txt = txt + ' style="top: ' + y + 'px;left: ' + x + 'px"';
        txt = txt + ( items[i].tooltip ? " data-tooltip='" + items[i].tooltip + "'" : "" );
        txt = txt + ' onclick="event.preventDefault();event.stopPropagation();console.log(' + "'onclick: " + cmdId + "');" + items[i].action + ';return false;"'; // prevent inputDummy from get changeEvent
        txt = txt + ' >';
        //txt = txt + '<i class="fa fa-file" style="font-size:10px;color:red;"></i>';
        if (q < 2) { txt = txt + (items[i].icon ?? "") + '        '; }
        txt = txt + "<div class='cmc-item-descr " + (q < 2 ? '' : 'cmc-item-descr-left') ;
        txt = txt +  "'>"+items[i].descr+"</div>";
        if (q > 1) { txt = txt + '        ' + (items[i].icon ?? ""); }
        contextMenuCircular.innerHTML += '<div ' + txt + '</div>';
        //
        if (items[i].icon) {
            let icons =  items[i].icon.split(/(?=<i)/);
            for( icon_ of icons ){
                if (null != icon_ && "" != icon_ && !icon_.includes('class=""') && (filters[ icon_ ] ?? 1) >= 0 /*keep active filters ... at -1 */ ){
                    filters[ icon_ ] = ( filters[ icon_ ] ?? 0 ) + 1 ; // count
                    filters[ icon_.replaceAll(/[^a-zA-Z]+/g,"") ] = icon_; // TRICKY use parallel as lookup ... create identifier
                    filters[ "ACTION::" + icon_.replaceAll(/[^a-zA-Z]+/g,"") ] = 1 == filters[ icon_ ] ? items[i].action : null ;// TRICKY2 - add link to element to icon - allow to fast execute when only 1 entry matches ...
                    filters[ "TOOLTIP::" + icon_.replaceAll(/[^a-zA-Z]+/g,"") ] = 1 == filters[ icon_ ] ? items[i].descr : null ;// TRICKY2 - add link to element to icon - allow to fast execute when only 1 entry matches ...
                    filters[ "MENU::" + icon_.replaceAll(/[^a-zA-Z]+/g,"") ] = 1 == filters[ icon_ ] ? cmdId : null ;// TRICKY2 - add link to element to icon - allow to fast execute when only 1 entry matches ...
                }
            }
        }
        //
        i = i + 1;
      } // check if inside viewport ...
      if (q<3) {
        q++;
      } else {
        q = 0;
        l++;
      }

  }
  // store to be updated by filter-change or reset by new-grid
  contextMenuCircularFilterBar = filters;
}


// -------------------------------------------------





function icon2sug( sug ){
    let icon = sug.icon ;
    if (!icon) {
			// sorted ... from general to special
			var txt = sug.descr ?? sug.qry ;
			var t2i = { "---": "----"
			            , "dbTabusage": "database"
			            , "dbTabUsage": "database"
			            , "dbTab": "database"
			            //
			            , ".csv": "file-csv"
			            , "process_log": "list-ol"
			            , "progress": "list-ol"
			            , "job_history": "running"
			            , "erp_": "file-import"
			            , "datafeed": "file-excel"
			            , "report": "file-alt"
			            , "job_": "running"   // wranche
			            , "users": "user"
			            , "_av": "tag"
						, "rating": "star-half"
						, "asset": "image"
						, "price": "euro-sign"
						, "text": "align-center" // color: gray;
						, "special": "rocket"
						, "item": "tshirt"
						, "article": "tshirt"
						//
						,"attribute": "cog" // = gear
						, "type":  "cogs"
						//
						, "diff sibling": "tasks"
						, "diff parent": "tasks"
						, "insert rows": "plus"
						, "delete rows": "trash"
						, "search value": "search"
						, "filter by value": "filter"
						, "order by value": "sort"
						, "add new link": "link"
						, "select whole grid": "copy"
						, "use as query": "comment-o"
						, "follow id": "eye"
						//
						, "create insert": "create_ins"
						, "create select": "create_sel"
						//
						, "use child as filter": "angle-double-up"
						//
						, "UPDATE": "sync"
						, "UPDATE ALL": "sync"
						, "LIMIT": "expand"
						, "show all columns": "eye"
					};
			var t2i_match = "";
			txt = txt.replace(/\[[^\]]*\]/g,"");
			for ( var key in t2i ) {
                xDebug('menuCircular',  "menuCircular::icon2sug " + txt + ": " + key + " -> " + t2i[key]);
				if (txt.match(key)) {
					icon = "fa fa-" + t2i[key];
					break;
				}
			}
      xDebug('menuCircular',  "menuCircular::icon2sug addIcon: " + txt + ": " + " --> " + t2i_match );
    }
    return icon;
}


function createContextMenu4NewGrid(event) {
    event.preventDefault();
    event.stopPropagation();
    const clientX = event.mouseX || event.clientX || window.innerWidth*0.5;
    const clientY = event.mouseY || event.clientY || window.innerHeight*0.5;
    var mostRecentGrid = document.getElementById( 'grid_container' ).lastChild;
    toggle (mostRecentGrid, "1000 -visibility");
    setTimeout(function() { // why did i postpone menu? - have grid created ...
        var mostRecentGrid = document.getElementById( 'grid_container' ).lastChild;
        var event_target = mostRecentGrid.getElementsByClassName('codeMirrorAnker')[0];
        createContextMenu(event_target, clientX, clientY  , null);
        toggle (mostRecentGrid, "1000 +visibility"); // TODO unhide when needed - not earlier
        }
    ,500);
}


function createContextMenu4Event(event) {
  event.preventDefault();
  event.stopPropagation();
  const { clientX: mouseX, clientY: mouseY } = event;
  createContextMenu(event.target, mouseX, mouseY, event);
}


function createContextMenu(event_target, mouseX, mouseY, event) {

   //KLUDGE
  removeTooltipForce();

  const { normalizedX, normalizedY } = normalizePozitionCircular(mouseX || window.innerWidth*0.5, mouseY || window.innerHeight*0.5);
  contextMenuCircular.style.top = `${normalizedY}px`;
  contextMenuCircular.style.left = `${normalizedX}px`;

  // prepare grid to apply action to grid
  contextMenuCircular.event = event;
  contextMenuCircular_on = true;
  if (!contextMenuCircular_lastFocus){
    contextMenuCircular_lastFocus = document.activeElement;
  }

  let gridElem = null;
  var items = [];

  if (event_target && event_target.nodeName == "BODY") {

        // create new grid and new context menu
        // KLUDGE rekursiv call
        // KLUDGE create TEMP-Grid - to allow context-menu to act on grid --> if system menu is wanted the grid is obsolete
        addMenuGridGeneral(items, null); // grid to be created ...

  } else { // check if grid is present ...

      // wind up to grid node ...
      gridElem = event_target;
      let menuElem = event_target;
      if (gridElem) {
          // allow cdm_hint_auto_sug
          if (gridElem.targetGrid) {
            gridElem = gridElem.targetGrid.eGrid;
          } else if ("resizeFrame" == gridElem.id || "cdm_hint_auto_sug" == gridElem.id ){ // TODO Kludge
            xError('menuCircular', "menuCircular::context ---  context-menu while resizing/on suggestion");
            //event_target = elemFromElemOrId(resizeEndE.id + "_");
            event_target = document.getElementById( 'grid_container' ).lastChild.getElementsByClassName('CodeMirror-cursor')[0]; /* KLUDGE - get most recent grid */
          }
          //
          while(gridElem && (!gridElem.classList || !gridElem.classList.contains("grid-with-filter")) ) {
            gridElem = gridElem.parentNode;
          }
          // wind up to relevant node - codeMirror / headline-column / grid-cell / group-cell
          while(gridElem && menuElem && (!menuElem.classList || ! ( menuElem.classList.contains("codeMirrorAnker") || menuElem.classList.contains("grid-head") || menuElem.classList.contains("grid-item") || menuElem.classList.contains("grid-idx") || menuElem.classList.contains("grid-group")  ) ) ) {
            menuElem = menuElem.parentNode;
          }
      }

      recordAction("contextmenu: gridElem: " + ( gridElem ? '"' + gridElem.id + '"' ?? "null" : "null" ) + ", " + ( menuElem ? '"' + menuElem.id + '"' ?? "null" : "null" ) + "");
      contextMenuCircular.event_gridElem = gridElem;
      contextMenuCircular.event_grid = gridElem ? gridElem.grid : null;
      contextMenuCircular.event_gridCellElem = menuElem;

      if (gridElem && gridElem.classList.contains("grid-with-filter")) {
          if ( (menuElem &&  menuElem.classList.contains("codeMirrorAnker"))  ||   !menuElem   ) {
              xLog('menuCircular', "menuCircular::ContextMenu for " + gridElem.id + " query " + (menuElem ? menuElem.id : ""));
              addMenuGridGeneral(items, gridElem);
          } else if ( menuElem ) {
              if (menuElem.classList.contains("grid-head")) {
                addMenuGridColHeader(items, gridElem);
              } else {
                addMenuGridCell(items, gridElem);
              }
          } else {
              xDebug('menuCircular', "menuCircular::ContextMenu for " + gridElem.id + " ");
          }
      } else {
        gridElem = null;
      }
  } // BODY or GRID or ...

  if (!gridElem) {
      xDebug('menuCircular', "menuCircular::ContextMenu without grid ");
      contextMenuCircular.event_gridCellElem = null;
      contextMenuCircular.event_gridElem = null;
      contextMenuCircular.event_grid = null;
  }

  // fallback to system menu
  if (items.length < 1 ) {
    items = getSystemMenuItems();
  }

  toggle( contextMenuCircular, "-visible");
  contextMenuCircular.style.top = `${normalizedY}px`;
  contextMenuCircular.style.left = `${normalizedX}px`;
  //
  createMenuFromItems( items );
  createFilterBarForItems( contextMenuCircularFilterBar );
  // menu might have new tooltips
  initTooltip();

  // hack to enable animation
  setTimeout(() => {
    toggle( contextMenuCircular, "+visible #menuBackground +display ");
    toggle( 'inputDummy', '+display +focus');
    //document.getElementById('inputDummy').onkeyup = function(event) {console.debug(event); if (event.key === 'Escape') {contextMenuCircularClose();};};
  }, 250);
};

//----------------------------------------------



// add menu entries to items-list
function addMenuItem(items, descr, icon, action) {
  items.push({ descr: descr, icon: icon, action: action });
}

function addMenuGridColHeader(items, gridElem) {
    xDebug('menuCircular', "menuCircular::addMenuGridColHeader for " + gridElem.id + " header " + contextMenuCircular.event_gridCellElem.id);
    // todo: add filter by references of column ...
    let [ r ,c ] = gridElem.grid.getRowAndColOf(contextMenuCircular.event_gridCellElem);
    let colName = gridElem.grid.items[ 0 ][ c];
    let ref ="";
    if ("freetext".includes(colName.toLowerCase())) {
        ref = "["+colName+"][textid]locale_freetext";
        addMenuItem(items, ref, 'fa fa-link', "controller(contextMenuCircular.event_grid,'REF-BY-VALUE:"+ref+"',contextMenuCircular.event_gridCellElem);");
    } else if ("description".includes(colName.toLowerCase())) {
        ref = "["+colName+"][textid]locale_freetext";
        addMenuItem(items, ref, 'fa fa-link', "controller(contextMenuCircular.event_grid,'REF-BY-VALUE:"+ref+"',contextMenuCircular.event_gridCellElem);");
        ref = "["+colName+"][textid]locale_lookuptext";
        addMenuItem(items, ref, 'fa fa-link', "controller(contextMenuCircular.event_grid,'REF-BY-VALUE:"+ref+"',contextMenuCircular.event_gridCellElem);");
    } else {
        // need to have 2 different icons - to allow fast selection
        tabName = "userid" == colName ? "users" : colName;
        ref = "["+colName+"][id]" + tabName;
        addMenuItem(items, ref, 'fa fa-angle-right', "controller(contextMenuCircular.event_grid,'REF-BY-VALUE:"+ref+"',contextMenuCircular.event_gridCellElem);");
        ref = "[.."+colName+"][id]" + tabName; // assume col is unique -> allow random order of join
        addMenuItem(items, ref, 'fa fa-angle-double-right', "controller(contextMenuCircular.event_grid,'REF-BY-VALUE:"+ref+"',contextMenuCircular.event_gridCellElem);");
    }
}


function addMenuGridCell(items, gridElem) {
    xDebug('menuCircular', "menuCircular::addMenuGridCell for " + gridElem.id + " cell " + contextMenuCircular.event_gridCellElem.id);
// refactor to use addMenuItem(items, descr, icon, action)
    addMenuItem(items, 'filter by value', 'fa fa-filter', "controller(contextMenuCircular.event_grid,'FILTER-BY-VALUE:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'OR', 'fa fa-filter', "controller(contextMenuCircular.event_grid,'FILTER-BY-VALUE:OR:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'le', 'fa fa-filter', "controller(contextMenuCircular.event_grid,'FILTER-BY-VALUE:LE:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'gt', 'fa fa-filter', "controller(contextMenuCircular.event_grid,'FILTER-BY-VALUE:GE:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'NOT', 'fa fa-filter', "controller(contextMenuCircular.event_grid,'FILTER-BY-VALUE:NOT:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'use as query', 'fa fa-search', "controller(contextMenuCircular.event_grid, event.ctrlKey ? 'GRID:' : 'GRID:ADD:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'UPDATE', 'fa fa-sync', "controller(contextMenuCircular.event_grid,'UPDATE:');");
    addMenuItem(items, 'lookup link', 'fa fa-link', "controller(contextMenuCircular.event_grid,'LOOKUP:',contextMenuCircular.event_gridCellElem);");
    addMenuItem(items, 'lookup text', 'fa fa-link', "controller(contextMenuCircular.event_grid,'LOOKUP:TEXT:',contextMenuCircular.event_gridCellElem);");
}


function addMenuGridGeneral(items, gridElem) {

          suggestions = getSuggestionFromUrl(gridElem ? gridElem.grid : null); // w/o grid get general suggestions
          sug = suggestions.suggestions ?? [] ;
          //
          //  add tab - tab by tab - rather than replace all
          xLog('menuCircular', "menuCircular::addMenuGridGeneral STEP-BY-STEP");

          //
          var descr_used = [];
          for(let i = 0; i < sug.length; i++) {
            //items.add( { descr: "read", icon: 'fa fa-file', action: "controller(contextMenuCircular.event_grid,'READ:');" } );
            var descr = sug[i].descr;
            if (!descr) {
                descr = sug[i].qry;
                if (descr.length>30) {
                    var descr_avail = [];
                    descr = descr.replace(/^  */,''); descr_avail.push(descr);
                    descr = descr.replace(/ .*/,' ...') ;  descr_avail.push(descr); // job[]job_history[job.identifier,job_history.*] order by job_history.id desc --> job[]job_history[job.identifier,job_history.*] order by job_history.id desc
                    descr = descr.replace(/\[[^\]]*\]$/,' ...') ; descr_avail.push(descr); // job[]job_history[job.identifier,job_history.*] --> job[]job_history
                    descr = descr.replace(/^\[[^\]]*\]/,'... ') ; descr_avail.push(descr); // [productNo,articleNo,identifier,article_av.*]article[]article_av[<]attribute -> article[]article_av[<]attribute
                    descr = descr.replace(/ order by .*/i,' ...') ; descr_avail.push(descr); // article[]article_detail_tab[content][textid]locale_text --> article[...]locale_text
                    if (descr.length>30) {
                        descr = descr.replace(/([a-z_]+)\[.*\](\1)/,'...$1') ; descr_avail.push(descr); // article[]article_detail_tab[content][textid]locale_text --> article[...]locale_text
                    }
                    if (descr.length>30) {
                        descr = descr.replace(/(article)\[.*\]article/,'...$1') ; descr_avail.push(descr); // article[]article_detail_tab[content][textid]locale_text --> article[...]locale_text
                    }
                    if (descr.length>30) {
                        descr = descr.replace(/\[.*?\]/g,'[]') ; descr_avail.push(descr); // article[]article_detail_tab[content][textid]locale_text --> article[...]article_detail_tab[...]locale_text
                        descr = descr.replace(/\[\] /g,'') ; descr_avail.push(descr);
                        descr = descr.replace(/\[\]\[\]/g,'[]') ; descr_avail.push(descr);
                    }
                    if (descr.length>30) {
                        descr = descr.replace(/\[.*\]/,'[]') ; descr_avail.push(descr); // article[]article_detail_tab[content][textid]locale_text --> article[...]locale_text
                        descr = descr.replace(/\[\] /g,'') ; descr_avail.push(descr);
                    }
                }
                while (descr_used.includes( descr ) && descr_avail.length > 0 ) {
                    descr = descr_avail.pop();
                }
            }
            descr_used.push(descr);
            descr = descr.replace(/\s/g,"&nbsp;")
            sug[i].descr = descr;
            //items.push( { descr: descr, qry: sug[i].qry, icon: icon2sug( sug[i] ) , action: sug[i].action ??  "controller(contextMenuCircular.event_grid,'" +  sug[i].qry.replace(/\'/g,'\\\'') + "');" } );
            addMenuItem(items, descr, icon2sug( sug[i] ), sug[i].action ??  "controller(contextMenuCircular.event_grid,'" +  sug[i].qry.replace(/\'/g,'\\\'') + "');" );
          }

        addMenuItem(items, 'toggle columns/width', 'fa fa-eye', "event.preventDefault;event.stopPropagation();toggle('#menuBackground','-display');let p = document.getElementById('context-menu-circular')  ; if (true) { let pos = this.getBoundingClientRect(); this.style.position='relative'; this.style.top=(pos.top - parseFloatOr0(p.style.top)) +'px';} ;p.innerHTML='';p.appendChild('I' == this.tagName || 'INPUT' == this.tagName ? this.parentNode : this );let grid = contextMenuCircular.event_grid; grid.toggleColsDetail(!event.shiftKey);grid.load(0,0);removeTooltipForce();return false;" );
        addMenuItem(items, 'REFRESH', 'fa fa-file', "controller(contextMenuCircular.event_grid,'REFRESH:');" );
        addMenuItem(items, 'grid new tab/ ctrl-win', 'fas fa-external-link-alt', "controller(contextMenuCircular.event_grid,'GRID:NEWTAB:','');" );
}




// --------------------------------------------------

function contextMenuCircularClose(params) {
  console.log('contextMenuCircularClose ' + " " + (contextMenuCircular_on ? "on" : "off") );
  let autodetect = contextMenuCircular.classList.contains("visible") || contextMenuCircular.style.display === "block" ;
  if (contextMenuCircular_on || autodetect ) {
        contextMenuCircular.event = null;
        if (!contextMenuCircular_on) {
            console.warn('contextMenuCircularClose ' + " FORCE by INVALID " + (contextMenuCircular_on ? "on" : "off") );
        }
        toggle(contextMenuCircular, '! +visible #menuBackground +display');
        toggle( 'inputDummy', '! +display +focus');
        document.getElementById('inputDummy').value = "";
        //document.getElementById('inputDummy').onkeyup = null;
        contextMenuCircular_on = false;
        if (contextMenuCircular_lastFocus){
            contextMenuCircular_lastFocus.focus();
            contextMenuCircular_lastFocus= null;
        }
  }
}

contextMenuCircularScope.addEventListener("click", (e) => {
  // ? close the menu if the user clicks outside of it
  contextMenuCircularClose();
  /*
  if ((null != contextMenuCircular.event && e.target.offsetParent !== contextMenuCircular) || e.target.offsetParent === null) {
    contextMenuCircular.event = null;
    toggle(contextMenuCircular, '! +visible #menuBackground +display');
    toggle( 'inputDummy', '! +display +focus');
    document.getElementById('inputDummy').value = "";
  }
  */
});



function createMenuFromGrid(grid_tmp, qry) {
    // assume first col contains executable query
    let items = [];
    if (grid_tmp.items.length>1) {
        if (qry.startsWith("CACHE ")) {
            // simulate click on sync icon to enable autoMode
            // <i class="fa fa-sync context-icon" aria-hidden="true" onclick="if (event && event.ctrlKey) { toggle(this, 'autoMode')}; let autoMode = this.classList.contains('autoMode'); if (autoMode) { let x = this; setTimeout(function() {x.onclick(null);}, 10000);  };  if (autoMode || (event && !event.ctrlKey))controller( findGrid( this ),'UPDATE:');" data-tooltip="Update Data (ctrl - every 10 sec)" ></i>
            items.push( { descr: "Update", action:  "controller(contextMenuCircular.event_grid,'UPDATE:');" } );
            items.push( { descr: "Update every 10 sec", action: "eSync = contextMenuCircular.event_grid.eGridBox.getElementsByClassName('contextRight')[0].getElementsByClassName('fa-sync')[0];toggle(eSync,'+autoMode');eSync.onclick();" } );
            //
        }
        // search for description column
        let descr_col = 0;
        for(let c = 0; c < grid_tmp.items[0].length; c++) {
            if (grid_tmp.items[0][c].toLowerCase().includes("descr")) {
                descr_col = c;
                break;
            }
        }
        //
        for (let r = 1; r < grid_tmp.items.length && r < 20; r++) {
            let val = grid_tmp.items[r][0];
            let descr_val = grid_tmp.items[r][descr_col];
            let action = "controller(contextMenuCircular.event_grid,'GRID:ADD:','"+ val +"');";
            if (qry.startsWith("LOGIN")) {
                action = "controller(contextMenuCircular.event_grid,'LOGIN:'+ (event.ctrlKey ? 'ALL:':'') + '"+ val +"');";
            } else if (qry.startsWith("CACHE ")) {
                val_from = grid_tmp.items[r][0];
                val_until = grid_tmp.items[r][1];
                val_until_dt = (""+val_until).split(/ /); // enforce String ..
                val = (grid_tmp.items[r][3] ? grid_tmp.items[r][3] + ": " : "" ) + val_from; // will create to long values ..
                if (val_from == val_until) {
                } else if ( val_from.includes(val_until_dt[0]) ) {
                    val = val + " - " + val_until_dt[1];
                } else  {
                    val = val + " - " + val_until_dt[0];
                }
                val = val.replaceAll(/\s+/g,"&nbsp;");
                action = "controller(contextMenuCircular.event_grid,'CACHE:"+ grid_tmp.items[r][2] +"');"; // add hash-Value / file-suffix
            } else if (qry.startsWith("SESSIONS") && 0 == descr_col) {
                descr_val = descr_val.replaceAll(/SES_|_/g," ");
                descr_val = descr_val.replaceAll(/([0-9]{4})([0-9]{2})([0-9]{2})[_ -]?([0-9]{2})([0-9]{2})([0-9]{2})/g,"$1-$2-$3 $4:$5:$6"); // format date
                descr_val = descr_val.replaceAll(/\s*([0-9  :-]*[0-9])\s\s*(.+[^0-9 :-].*)\s*/g,"$2 ($1)"); // move date to end
            }
            let item = { descr: descr_val, action: action };
            items.push( item );
        }
        toggle( contextMenuCircular, "-visible ");
        createMenuFromItems(items);
        //
        toggle( contextMenuCircular, " 0 +visible #menuBackground +display "); // use 0 to enable animation
    } else {
        msg_warn("no cached versions found");
    }
}



// --------------------------------------------------


function addMenuItemDiv() { }


document.addEventListener("contextmenu", (event) => {
        createContextMenu4Event(event);
    });

// addMenuItem();