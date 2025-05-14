let value_current = "";
let resizeTimer = "";

function extractProp( qry, text_json, propDefault = null, propForced = null, propManual = null) {
    var stmt = "";
    var txt = "";
    console.debug("qry        : " + (qry ?? ""));
    var text_json_woData = JSON.parse(JSON.stringify(text_json)); // deep copy
    if (text_json_woData && text_json_woData.data) text_json_woData.data = "...";
    console.debug("init::extractProp text_json  : " + (text_json ? JSON.stringify(text_json_woData) : ""));
    console.debug("init::extractProp propDefault: " + (propDefault ? JSON.stringify(propDefault) : ""));
    console.debug("init::extractProp propManual : " + (propManual ? JSON.stringify(propManual) : ""));
    console.debug("init::extractProp propForced : " + (propForced ? JSON.stringify(propForced) : ""));
    var prop = propDefault ?? {};
    // last wins ...
    if ( text_json && text_json.stmt) [stmt, prop] = extractAllPropFromQry( text_json.stmt, prop);
    [qry, prop] = extractAllPropFromQry( qry, prop);
    if ( text_json && text_json.prop) [txt, prop] = extractAllPropFromQry( text_json.prop, prop );
    if ( text_json && text_json.pos) prop.pos = text_json.pos;
    if (propManual) prop = {...prop, ...propManual};
    if (propForced) prop = {...prop, ...propForced};
    console.debug("init::extractProp --> qry    : " + (qry ?? ""));
    console.debug("init::extractProp --> prop   : " + (prop ? JSON.stringify(prop) : ""));
    return [qry, prop];
}



function extractAllPropFromQry(qry, propDefault = {}) {
    let qry_prop = {};
    let prop = propDefault;
    if (qry ) {
        try {
            let qry_prop_text = "";
            if (qry.includes && qry.includes("/*{") && qry.includes("}*/")) {
                try {
                    qry_prop_text = qry.replaceAll(/.*\/\*\{/gs,"{").replaceAll(/\}\*\/.*/gs,"}"); // extract json from qry
                    qry = qry.replaceAll(/\s*\/\*\{.*/gs,"");
                    qry_prop = JSON.parse( qry_prop_text ); // TODO remove 2nd parse from grid.setDataJson
                } catch(e) {
                    qry_prop_text = qry_prop_text.replaceAll(/([^\s]+[^"])(:[\s{])/g, '"$1"$2'); // " assume missing quotes on properties     {a: 1, b: "2"} -> {"a": 1, "b": "2"}
                    qry_prop = JSON.parse( qry_prop_text); // TODO remove 2nd parse from grid.setDataJson
                    console.warn("fixed props: '" + qry_prop_text + "' (added quotes)");
                }
            }
            prop = { ...prop, ...qry_prop };
        } catch(e) {
            console.error(e);
            console.warn("invalid props removed: '" + qry_prop_text + "'");
            msg_warn( "invalid props removed: '" + qry_prop_text + "'")
            msg_warn(e.message); // got issue on transforming it to json ...
        }
    }
    return [qry, prop];
}

//////////////////////////////////////////////////////////////

function calculateContainerPosition(newElement) {
    const newElementRect = newElement.eGridBox.getBoundingClientRect();
    const offsets = {
        top: newElementRect.top,
        left: newElementRect.left
    };

    const filteredGrids = grids.filter(grid => typeof(grid) !== 'undefined' && grid !== null && grid !== newElement && grid.eGridBox);
    xDebug('init', 'filtered grids', filteredGrids)

    if(filteredGrids.length > 0) {
        // Sammle die Rechteck-Informationen aller vorhandenen Elemente außer dem neuen Element
        const gridRects = filteredGrids.map(element => element.eGridBox.getBoundingClientRect());

        // Sortiere die Rechtecke nach right und bottom Werten
        gridRects.sort((rectA, rectB) => {
            return rectA.left - rectB.left
        });

        // Finde die nächstmögliche freie Position für das neue Element
        let probablePosition = null;
        let targetPosition = null

        gridRects.forEach((rect) => {
            probablePosition = {
                bottom: rect.top + newElementRect.height + 1,
                left: rect.right + 1,
                right: rect.right + newElementRect.width + 1,
                top: rect.top + 1
            };
            // Prüfe, ob das neue Element in den sichtbaren Bereich des Viewports passt && ob probablePosition mit anderen Objekten kollidiert
            if (targetPosition === null) {
                if((probablePosition.right < window.innerWidth && !isColliding(probablePosition, gridRects))) targetPosition = probablePosition;
            }
        })

        if(targetPosition === null) {
            const possibleHeights = filteredGrids
              .filter(element => element !== newElement)
              .map(element => element.eGridBox.getBoundingClientRect().bottom);

            gridRects.forEach((rect) => {
                possibleHeights.forEach(height => {
                    probablePosition = {
                        bottom: height + newElementRect.height + 1,
                        left: rect.left,
                        right: rect.left + newElementRect.width + 1,
                        top: height + 1
                    };
                    // Prüfe, ob das neue Element in den sichtbaren Bereich des Viewports passt && ob probablePosition mit anderen Objekten kollidiert
                    if (targetPosition === null) {
                        if((probablePosition.right < window.innerWidth && !isColliding(probablePosition, gridRects))) targetPosition = probablePosition;
                    }
                })
            })
        }
        if (targetPosition) {
            // Verwende die nächste freie Position für das neue Element
            offsets.top = targetPosition.top;
            offsets.left = targetPosition.left;
        }
    } else {
        // Wenn es keine bestehenden Elemente gibt
        offsets.top = 50;
        offsets.left = 50;
    }

    xDebug('init', '==> newElement.offset.top: ' + offsets.top, ' newElement.offset.left:' + offsets.left);

    return offsets;
}

function isColliding(newRect, rects) {
    // Überprüfe, ob position mit anderen Objekten kollidiert
    // Gib true zurück, wenn eine Kollision vorliegt, andernfalls false
    // Iteriere über alle bereits platzierten Rechtecke

    let isColliding = false
    rects.forEach(rect => {
        xDebug('init', 'rect', rect);
        xDebug('init', 'newRect', newRect);

        // Überprüfe, ob das gegebene Rechteck mit dem aktuellen Rechteck kollidiert

        if (
          newRect.right >= rect.left &&
          newRect.left <= rect.right &&
          newRect.bottom >= rect.top &&
          newRect.top <= rect.bottom
        ) {
            // Es liegt eine Kollision vor
            isColliding = true;
        }
    });

    // Keine Kollision gefunden
    xDebug('init', 'is colliding', isColliding);
    return isColliding;
}

// properties in following prio
//    - per prop-param
//    - per qry-param (as Hint)
//    - inside initialResponse->stmt
//    - inside initialResponse->prop
//    - ? grid.prop
//
//
function createGridContainer(qry, prop = {}, initialResponse = null) { // use initialResponse to prevent initial loading ... as for locale grids for Input e.g.
    let container = document.getElementById( "grid_container" );
    let grid_tmpl = document.getElementById( "grid_tmpl" );

    let grid_master = grid_tmpl.cloneNode(true);
    observeResizeDisable(grid_master);
    container.appendChild( grid_master );
    let grid = new Grid( grid_master.getElementsByClassName( "grid-container" )[0], grid_master  );
    //
    grid_master.grid=grid; // allow access to grid via div ...
    grid_master.id='grid'+grid.gridNo;
    dragElement(grid_master);
    //
    contextElem = grid_master.getElementsByClassName("context")[0];
    contextElem.id='grid'+grid.gridNo+'_context';
    contextElem.getElementsByClassName("context_env")[0].innerHTML =  loginId_current;
    //
    grid.codeMirror = createCodeMirror( grid_master.getElementsByClassName("codeMirrorAnker")[0] );
    grid.codeMirror.cm_grid = grid; // add grid to cm - to allow cm call matching  grid

    cnt = grids.filter(Boolean).length;

    grid_master.style.visibility = "hidden";
    grid_master.style.display = "block";
    toggle( getBoxElem4Grid( grid, "dragHandle"), "dragHandleFix", dragHandleFix);
    grid_master.getElementsByClassName("codeMirrorAnker")[0]

    if (prop && prop.pos && prop.pos.top && prop.pos && prop.pos.left) {
        grid_master.style.top = defaultUnit(prop.pos.top);
        grid_master.style.left = defaultUnit(prop.pos.left);
    } else {
        const gridOffsetValues = calculateContainerPosition(grid);
        grid_master.style.top = gridOffsetValues.top + 'px';
        grid_master.style.left = gridOffsetValues.left + 'px';
        // KLUDGE - fit to raster finally - should be done on calcContainerPosition already
        //fitToRaster(grid_master);
    }

    // skipp Request if Grid is used for local input - then provide initialResponse to do so
    if (qry || initialResponse ) {
        data2Grid(grid, qry, null, initialResponse, prop);
    }
    initGridInputListener(grid_master);
    initTooltip();
    dragSourceListenerUpdate();
    // KLUDGE - fit to raster finally - should be done on calcContainerPosition already
    // Data needs to be loaded to calculate the width/height of the grid
    setBoxBorder(grid_master, prop.pos, false);
    fitToRaster(grid_master);
    // bring to front - simulate click
    //grid.eGridBox.onclick({target: grid});
    //grid.eGridBox.click(); // set focus ...
    setTimeout(function() {
        contextMenuCircularClose();
        console.debug("init.sh click on grid " + gridNoCounter);
        grids[gridNoCounter].codeMirror.focus();
        console.debug("init.sh click on grid " + gridNoCounter + " DONE");
    } , 500 );

    //
    // as css-resize has no constraints ... add constraints ...
    //new ResizeObserver(console.log).observe(grid_master);
    observeResizeInit(grid_master, 2000);
    xLog('init', "prevent resize trigger on creation");
    if (qry || initialResponse ) {
        toggle (grid_master, "+visibility");
    }
    return grid;
}

function initTooltip() {
    const tooltip = document.querySelector('.js-tooltip');
    const tooltipTriggers = document.querySelectorAll('[data-tooltip]');

    function setTooltip(event) {
        tooltip.innerHTML = event.target.dataset.tooltip;
        tooltip.style.left = (event.clientX + 15 ) + 'px';
        tooltip.style.display = 'none';
        tooltip.id="js_tooltip"; // KLUDGE restore ...
        var tooltipHeight = tooltip.getBoundingClientRect().height;
        tooltip.style.top =  (event.clientY + tooltipHeight < window.innerHeight ? event.clientY + 30: window.innerHeight - tooltipHeight - 30 )  + 'px';
        toggle( tooltip, " 1000 +display "); // use 0 to enable animation
    }

    function removeTooltip() {
        tooltip.style.display = 'none';
        // on removing grids  (e.g. drag and drop)  we must remove tooltip savely
        tooltip.id = "" ; // TRICKY remove id to prevent toggle form displaying outdated Tooltip
    }

    tooltipTriggers.forEach(tooltipTrigger => {
        tooltipTrigger.removeEventListener('mouseenter', setTooltip);
        tooltipTrigger.addEventListener('mouseenter', setTooltip);
        tooltipTrigger.removeEventListener('mouseleave', removeTooltip);
        tooltipTrigger.addEventListener('mouseleave', removeTooltip);
    })
}
function removeTooltipForce() {
    const tooltip = document.querySelector('.js-tooltip');
    tooltip.style.display = 'none';
    tooltip.id = "" ;
}

if (window.location.href.endsWith("/") || window.location.href.endsWith("/index.html")) {
    //createGridContainer('dbTab', {"valHndl":{"tab":"LINK"}, pos: {width: "400", height: "525"}} );
    createGridContainer('v_dbTabusage limit 300 /*{"valHndl":{"tab":"LINK"}, "pos": {"width": "397", "height": "497"}, "cdm":"yes"}*/');
    //createGridContainer('v_dbTabusage limit 300 /*{"valHndl":{"tab":"LINK"},"cdm":"yes"}*/');
    //createGridContainer('v_dbTabusageLinked', {"valHndl":{"tab":"LINK"}, pos: {width: "400", height: "525"}, "cdm": "yes"} );
    //createGridContainer('v_dbTabusageLinked /*{ "cdm": "none" }*/');
    //createGridContainer('v_dbTabusageLinked', { "cdm": "none" }); // cdm will be restored by laoding data...
    //createGridContainer('dummy.csv',  { colsGrouped: 2 /*, unknownProp: 'lol' */ , pos: { top: "50", left: "600"} , "valHndl": { "issue": "LINK" }}  );
    //
    //createGridContainer('app',  { pos: { top: "50", left: "600"}, "colOrderIdx":[5,4,1,3] , "colsGrouped": 2 } );
    //createGridContainer('app[module,identifier,url] order by 1,2,3',  { pos: { top: "50", left: "600"}, "colsGrouped": 1 } );
    //createGridContainer('select module,\'(\'||identifier||\')[\'||url||\']\' url from app order by 1,2',  { pos: { top: "50", left: "600", width: "200", height: "800"}, "colsGrouped": 1 } );
    //createGridContainer('app[module,identifier,url] O 1,2',  { pos: { top: "50", left: "600", width: "200", height: "800"}, "colOrderIdx":[0,1], "colsGrouped": 1, valHndl :{"identifier":"HREF_2"} } );
    //createGridContainer('APPS===app[module,identifier,url] O 1,2 /*{ pos: { top: "50", left: "600", width: "200", height: "800"}, "colOrderIdx":[0,1], "colsGrouped": 1, valHndl: {"identifier":"HREF_2"}, limit: 500 }*/');
  //  createGridContainer('APPS',  { pos: { top: "50", left: "500", width: "400", height: "800"} } );
    //createGridContainer('select module,identifier||url url from app order by 1,2,3',  { pos: { top: "50", left: "600"}, "colsGrouped": 1 } );
    //
    //createGridContainer('v_dbTabusage limit 100 /*{ "colsGrouped": 0, "pos": { "top": "50", "left": "50"} , "valHndl": { "tab": "LINK" }, "cdm": "none" } */');
  //  createGridContainer('ipim-supply.tabUsage /*{"valHndl":{"collection":"LINK"},"cdm":"none", "pos": {"left": 1020, "top": "81", width: "458", "height": "500"} }*/', { pos: { top: 50, left: 950, width: 500, "height": 500 }  });
    //createGridContainer();
  //  createGridContainer('dummy.csv',  { pos: { top: "600", left: "50", width: "400", height: "250"} } );

    // {"qry":"app","prop":{"sortIdx":[[0,0],[25,"10000000","portal","portal"],[1,"10000002","export","export"],[3,"10000003","dam","dam"],[12,"10000004","portal","toolbox"],[15,"10000006","portal","toolbox"],[21,"10000007","portal","toolbox"],[16,"10000101","portal","\"module_admin\\;module_dam\\;module_export\\;module_workflow\\;toolbox\""],[18,"10000102","portal","toolbox"],[20,"10000103","portal","toolbox"],[17,"10000200","portal","toolbox"],[19,"10000201","portal","toolbox_framework"],[22,"10000202","portal","toolbox"],[23,"10000204","portal","\"toolbox_framework\\;module_dam\\;module_admin\""],[2,"10000300","workflow","workflow"],[4,"10000301","admin","admin"],[8,"10000302","dam","\"toolbox_framework\\;module_dam\\;module_admin\""],[13,"10000303","portal","toolbox_framework"],[24,"10000306","portal","toolbox_framework"],[5,"10000400","export","\"module_admin\\;module_dam\\;module_export\\;module_workflow\\;toolbox\""],[6,"10000401","workflow","\"module_admin\\;module_dam\\;module_export\\;module_workflow\\;toolbox\""],[9,"10000402","admin","\"module_admin\\;module_dam\\;module_export\\;module_workflow\\;toolbox\""],[14,"10000403","portal","toolbox"],[7,"10000500","dam","\"module_admin\\;module_dam\\;module_export\\;module_workflow\\;toolbox\""],[10,"10000600","admin","\"toolbox_framework\\;module_dam\\;module_admin\""],[11,"10010600","ssp","ssp"]],"colOrderIdx":[1,3,4,5],"pos":{"top":116,"left":621,"width":842,"height":564}}}

} else if (true){
    // handle http://localhost:8080/
    // handle http://localhost:8080/?qry=article
    // handle http://localhost:8080/index.html?qry=article
    var query = window.location.href.replaceAll(/^.*\/|.*\?qry=|\?qry=/g,"");
    if (query.includes("%")) {  // query: "ls%20-ltr"
    //if (query.startsWith("%")) {  // query: "%7B%22session%22%3A%22...........
        query = decodeURIComponent(query);
    }
    msg_info("starting with " + query);
    console.warn(query);
    if (query.startsWith("{session")) {
        // wrap it as BE-response
        handleQryResponse( null /*grid*/ , null /*qry*/, { "data": query} /*response*/, /*propForced*/ null);
    } else {
        // TODO merge with handleQryResponse
        createGridContainer(query); // treat as query
    }
    // window.history.pushState("object or string", "Query", "/"); //
} else {
    createGridContainer('WELCOME',  { pos: { top: "50", left: "600"} } );
}

dragElement(document.getElementById( "grid__0" ));


// msg_init();

// dragElement(document.getElementById("mydiv"));

