//todo use function param
let resizeEndE="";
let resizeEndW="";
let resizeEndH="";
let resizeEndACTIVE=-1; // -1 = disable, 0 = enable, 1 = running


// https://www.w3schools.com/howto/howto_js_draggable.asp
//
// Make the DIV element draggable:
//dragElement(document.getElementById("mydiv"));
let zIndexMax=2;
let focusAfterDrag= null;

function tst2(){
    console.debug("tst2");
}





function dragElement(elmnt) {
  var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;

  if (!elmnt) {
    return;
  }

  var elemHeader = document.getElementById(elmnt.id + "header");
  if (!elemHeader) {
    elemHeader = elmnt.getElementsByClassName("dragHandle")[0];
  }
  if (elemHeader) {
    // if present, the header is where you move the DIV from:
    elemHeader.onmousedown = dragMouseDown;
    elmnt.onclick = onMouseClick; // bring in front ...
  } else {
    // otherwise, move the DIV from anywhere inside the DIV:
    elmnt.onmousedown = dragMouseDown;
  }

  function onMouseClick(e) {
    event_ = e;
    if ( !event_) {
        // KLUDGE called from code - outside of drag-drop - grids[0].eGridBox.onclick();
        event_ = { target: this };
        elmnt = this;
    }
    // TODO strange behaviour - as elemnt is static !!!
    if ( elmnt.parentNode &&  !(elmnt === elmnt.parentNode.lastChild)) {
        elmnt.parentNode.appendChild(elmnt); // move to be last -> on top
        //
        if (event_ && event_.target && event_.target.classList.contains('CodeMirror-line')) {
            // KLUDGE
            focusAfterDrag = event_.target;
            focusAfterDrag.parentNode.parentNode.parentNode.focus();
        }
        elmnt.grid.codeMirror.focus();
        //elmnt.grid.codeMirror.setFocus();
    } else if (!elmnt.contains(document.activeElement)) {
        // KLUDGE come from outside of grid
        // force blur even if next grid has no codeMirror
        document.activeElement.blur();
        elmnt.grid.eGridBox.focus();
        elmnt.grid.codeMirror.focus();
    }
    xLog('dragDropResize', "dragdrop::onMouseClick() " + elmnt.id + "( resizeEndACTIVE: " + resizeEndACTIVE + ")");
    // END OF DragDropResize-ACtion
    if (resizeEndACTIVE == -1) {
        resizeEndACTIVE=0;
    }
    return true;
  }

  function dragMouseDown(e) {
    event_ = e || window.event;
    if ( ( "dragHandle" == event_.srcElement.className || event_.srcElement.className.includes("dragHandle ") ) /* exclude dragHandleBtn */  ) { // prevent acting on close-button
        event_.preventDefault();
        xDebug('dragDropResize', "dragdrop::dragMouseDown()");
        // get the mouse cursor position at startup:
        pos3 = event_.clientX;
        pos4 = event_.clientY;
        document.onmouseup = closeDragElement;
        // call a function whenever the cursor moves:
        document.onmousemove = elementDrag;
        //

        /*
        if (elmnt.style.zIndex < zIndexMax){
            zIndexMax = zIndexMax + 1; // need to add +10 or so...
            elmnt.style.zIndex = zIndexMax;
        }
        */
        if ( elmnt.parentNode && !(elmnt === elmnt.parentNode.lastChild)) {
            elmnt.parentNode.appendChild(elmnt); // move to be last -> on top
        }
        xDebug('dragDropResize', "dragdrop::dragMouseDown() " + elmnt.id);
        toggle(elmnt, '+grid-resizeOrMove');

        // KLUDGE
        if (elmnt.style.left.includes("%")) {
            elmnt.style.left = elmnt.getBoundingClientRect().left + "px";
        }

        // match to init::resize()
        f = document.getElementById('resizeFrame');
        f.style.top = elmnt.style.top;
        f.style.left = elmnt.style.left;
        f.style.width = elmnt.clientWidth + "px";
        f.style.height = elmnt.clientHeight+ "px";
        //f.style.position ="fixed";
        f.style.display='block';
        xLog('dragDropResize', "dragdrop::dragMouseDown() - fix to raster )" + elmnt.style.left + ", " + elmnt.style.top + ") " + elmnt.clientWidth + " x " + elmnt.clientHeight + " >> f: (" + f.style.left + ", " + f.style.top + ") " + f.style.width + " x " + f.style.height);
    } else {
        xLog('dragDropResize', "dragdrop::dragMouseDown() - SKIPP no dragHandle");
    }
  }

  function elementDrag(e) {
    e = e || window.event;
    e.preventDefault();
    // calculate the new cursor position:
    pos1 = e.clientX - pos3;
    pos2 = e.clientY - pos4;
    pos3 = e.clientX;
    pos4 = e.clientY;
    // set the element's new position:
    xDebug('dragDropResize', "drag " + elmnt.style.left + " " + elmnt.offsetLeft + " " + pos1);
    xDebug('dragDropResize', "drag " + elmnt.style.top + " " + elmnt.offsetTop + " " + pos2);
    // orig
    //elmnt.style.top = (elmnt.offsetTop - pos2) + "px";
    //elmnt.style.left = (elmnt.offsetLeft - pos1) + "px";
    // FOX
    elmnt.style.left = (parseInt(elmnt.style.left) + pos1) + "px";
    elmnt.style.top = (parseInt(elmnt.style.top) + pos2) + "px";

    xDebug('dragDropResize', "drag " + elmnt.style.top + " " + elmnt.offsetTop + " ");

    f = document.getElementById('resizeFrame');
    alignPosElem(elmnt, pos1, pos2, f);
    resizeEndACTIVE=1; // keep it down ...

  }

  function closeDragElement(e) {
    // stop moving when mouse button is released:
    //e.target.parentNode.codeMirror.setFocus(); // handles parent is grid with codeMirror-Attribute
    //elmnt.parentNode.codeMirror.setFocus(); // handles parent is grid with codeMirror-Attribute
    elmnt.grid.codeMirror.focus();
    document.onmouseup = null;
    document.onmousemove = null;

    // elmnt.style.top = (Math.round( elmnt.style.top.replace("px","") / 50) * 50) + "px";
    // elmnt.style.left = (Math.round( elmnt.style.left.replace("px","") / 50) * 50) + "px";
    elmnt.style.top = f.style.top;
    elmnt.style.left = f.style.left;
    // keep grid in place - override future HINTS
    var pos = elmnt.grid.prop_manual.pos ?? {};
    pos.top = elmnt.style.top;
    pos.left = elmnt.style.left;
    elmnt.grid.prop_manual.pos = pos;
    //
    recordAction("dragDropResize::closeDragElement " +  ( elmnt ? elmnt.id ? '"' + elmnt.id + '"' : "null" : "null"  )  + ",  " + elmnt.style.left + ", " + elmnt.style.top );
    xLog('dragDropResize', "dragdrop::closeDragElement - fix to raster " + elmnt.style.left + " " + elmnt.style.top + " ");
    //
    setTimeout(function() {
                toggle(elmnt, '-grid-resizeOrMove');
                f = document.getElementById('resizeFrame');
                f.style.display='none';
        }, 250);
    resizeEndACTIVE=-1; // re-enable trigger
  }
}


/*
 for observer ...

*/

function observeResizeInit(elem, delay = null) {
    const myObserver = new ResizeObserver(entries => {
        entries.forEach(entry => observeResize(entry));
    });
console.debug("observeResizeInit-DISABLED " + elem.id);
//    myObserver.observe(elem);
    observeResizeEnable(elem, delay);
}

function observeResizeEnable(elem = null, delay = null) {
    if (elem) {
        toggle(elem, (delay ?? "") + ' +resizable');
    }
    resizeEndACTIVE=0;
    xDebug('dragDropResize', "dragdrop::observeResizeEnable " + ( null == elem ? "" : elem.id ) + " resizeEndACTIVE => " + resizeEndACTIVE);
}
function observeResizeDisable(elem = null) {
    if (elem) {
        toggle(elem, '-resizable');
        resizeEndACTIVE=-1;
    } else {
        resizeEndACTIVE=-1;
    }
    xDebug('dragDropResize', "dragdrop::observeResizeDisable " + ( null == elem ? "" : elem.id ) + " resizeEndACTIVE => " + resizeEndACTIVE);
}

function observeResize(entry){
    if (0 == resizeEndACTIVE && entry && entry.target && entry.target.classList &&  entry.target.classList.contains("resizable") )  { // prevent calling on initial sizing grid, on manually dragging column-header etc ...
        observeResizeDisable();
        //  !!
        //  width = contentRect
        //  width + padding + border = size
        //  width + padding + border + margin = full size
        //
        //  is also trigger by just toggling
        //
        if  ( entry.contentRect.width > 0 || entry.contentRect.height >0 ) {
            var elem = entry.target;
            var elem_dim = elem.getBoundingClientRect(); // including padding and border
            if (elem_dim.width == 0 || elem_dim.height == 0 || elem_dim.width == entry.contentRect.width || elem_dim.height == entry.contentRect.height) {
                xLog('dragDropResize', "dragdrop::observeResize " + elem.id + " " + elem_dim.width + " x " + elem_dim.height + " SKIPPED");
            } else {
                xLog('dragDropResize', "dragdrop::observeResize " + elem.id + " " + elem_dim.width + " x " + elem_dim.height + " -> " + entry.contentRect.width + " x " + entry.contentRect.height);
                //entry.target.style.width = (Math.trunc(entry.contentRect.width/20)*20)+"px";
                resizeEndE=elem;
                let resizeEndWOld = resizeEndW;
                let resizeEndHOld = resizeEndH;
                resizeEndW=elem_dim.width;
                resizeEndH=elem_dim.height;
                // KLUDGE add je 2 x (padding + border) - due to rounding ....
                /*
                resizeEndW += (40 + 2);
                resizeEndH += (10 + 2);
                resizeEndW=(Math.ceil(resizeEndW/50)*50);
                resizeEndH=(Math.ceil(resizeEndH/50)*50);
                */
                /*
                resizeEndW = (Math.round( parseInt(resizeEndW) / 25 ) * 25) + "px"; //
                resizeEndH = (Math.round( parseInt(resizeEndH)  / 25 ) * 25) + "px";  // always in current direction -> moving to top do floor, moving down do ceil -> +-0,5
                */
                xDebug('dragDropResize', "dragdrop::observeResize " + elem.id + " " + elem_dim.width + " x " + elem_dim.height + " -> " + resizeEndW + " x " + resizeEndH);
                //f.style.position ="fixed";
                f = document.getElementById('resizeFrame');
                if ('block' != f.style.display ) {
                    xDebug('dragDropResize', "dragdrop::observeResize ---");
                    f.style.display='block';
                    // left/top is not part of entry.contentRect
                    f.style.top = elem.style.top ;
                    f.style.left = elem.style.left;
                }
                //alignPosElem(elem, resizeEndW - resizeEndWOld, resizeEndH - resizeEndHOld, f);
                alignPosElem2(elem, resizeEndW - resizeEndWOld, resizeEndH - resizeEndHOld, f, true /*resize-Mode*/);
                //f.style.width = resizeEndW + "px";
                //f.style.height = resizeEndH + "px";
                //---------------------
                //
                if (null == document.onmouseup) {
                    // only once
                    toggle(entry.target, '+grid-resizeOrMove');
                    document.onmouseup = closeResizeElement;
                }
                xDebug('dragDropResize', "dragdrop::observeResize " + elem.id + " "  + elem_dim.width + " x " + elem_dim.height + " >> f: " + f.style.width + " x " + f.style.height);
            }
        } else {
            // during removing grid ... resize is called with 0x0
            xDebug('dragDropResize', "dragdrop::observeResize SKIPPED " + entry.target.id + " " + entry.contentRect.width + " x " + entry.contentRect.height + "");
        }
        observeResizeEnable();
    } else {
        xLog('dragDropResize', "dragdrop::observeResize SKIPPED( resizeEndACTIVE: " + resizeEndACTIVE + ") " + entry.target.id);
    }
    return true;
}

function closeResizeElement(e) {
    xDebug('dragDropResize', "dragdrop::closeResizeElement wait");
    // give a litte time to process left-over-observed moves ..
    setTimeout(function() {
        xDebug('dragDropResize', "dragdrop::closeResizeElement ... run");
        document.onmouseup = null;
        resizeEnd();
        toggle('resizeFrame','-display'); // hide late - ease debugging
    }, 250);
    return true;
}

function resizeTo( elem, height, width) {
    observeResizeDisable();
    resizeTo_( elem, height, width);
    observeResizeEnable();
}

function resizeTo_( elem, height, width) {
    var elem_dim = { "width": width, "height": height };
    setBoxBorder(elem, elem_dim);
}

// resize is resize on DRAG/RESIZE-DUMMY-DIV
function resizeEnd(){
    if ( 0 == resizeEndACTIVE && resizeEndE && resizeEndE.id) {
        resizeEndACTIVE = 1;
        var elem = resizeEndE;

        clearTimeout(resizeEnd);
        f = document.getElementById('resizeFrame');
        if (resizeEndW>50 && resizeEndH>50) {
            var elem_dim = getDim(f);
            resizeTo_( elem, elem_dim.height, elem_dim.width);
            gridsGrid = elemFromElemOrId(elem.id + "_"); // "grid1" -> "grid1_"
            if (gridsGrid) {
                xLog('dragDropResize', "dragdrop::resizeTo - fit " + gridsGrid.id + " in .. " +  JSON.stringify( getDim(elem) ));
                // fixate size to override future HINTS
                var grid = elem.grid;
                grid.resizeInnerGrid();
                var pos = grid.prop_manual.pos ?? {};
                pos.width = elem_dim.width;
                pos.height = elem_dim.height;
                grid.prop_manual.pos = pos;
                recordAction("dragDropResize::resizeTo: " + ( elem ? '"' + elem.id + '"' : 'null' ) + ", " + pos.width + " x " + pos.height );
            } else {
                xLog('dragDropResize', "dragdrop::resizeTo - no Grid: " + elem.id );
            }
        } else {
            xLog('dragDropResize', "dragdrop::resizeEnd - SKIPP to-small - to raster " + resizeEndE.id + " " + resizeEndW + " x " + resizeEndH);
        }
        f.style.display='none';
        toggle(resizeEndE.id, '-grid-resizeOrMove');
        //myObserver.observe(resizeEndE);
        //resizeEndACTIVE=0;
        setTimeout(function() {
            xDebug('dragDropResize', "dragdrop::resizeEnd ... finished");
            resizeEndACTIVE=-1;
        }, 1000); // give time for animation without triggering myself
    } else {
        xLog('dragDropResize', "dragdrop::resizeEnd SKIPPED( resizeEndACTIVE: " + resizeEndACTIVE + " " + ( resizeEndE ? resizeEndE.id : "null" ) + ")");
    }
}

