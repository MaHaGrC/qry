  const resizeFrame = document.querySelector('#resizeFrame');
  const resizeFrameContent = document.querySelector('#resizeFrameContent');
  let dragResizeElement = null;
  let dragResizeHandle = null;
  let dre_offset_x, dre_offset_y;
  let dre_padBorder = { left: 1, top: 1, right: 2*(1+20), bottom: 2*1+5+10 }; // default padding and border - KLUDGE ...
  let resizeObserverSkippInitialCall = new Set();
  let resizeObserverDisabled = new Set();
  var rasterTrgtPosFirst = null; // needed for overdose movement
  var rasterTrgtPosCur = null;
  var rasterTrgtPosLast = null; // needed for speed up - skipp unchanged dim

    /* only for migration to new version */

    var resizeEndH = "";

    function dragElement(elmnt) {
      if (!elmnt) {
        return;
      }
      //var elemHeader = document.getElementById(elmnt.id + "header");
      //if (!elemHeader) {
        elemHeader = elmnt.getElementsByClassName("dragHandle")[0];
      //}
      elemHeader.classList.add('dragger');
      dragResizeDivInit(elemHeader);

    }


    function observeResizeInit(elem, delay = null) {
        setTimeout(() => {
            toggle(elem, '+resizable');
            dragResizeDivInit(elem);
        }, delay ?? 10);  // leave time for rendering, initial sizing grid ...
    }

    function observeResizeDisable(elem) {
        resizeObserverDisabled.add(elem);
    }

    function observeResizeEnable(elem, delay = null) {
        setTimeout(() => {
            resizeObserverDisabled.delete(elem);
        }, delay ?? 10);
    }

  /* main functions */

    function dragResizeDivInit( elem = null ) {
        if (elem) {
            if (elem.classList.contains('dragger')) {
                elem.removeEventListener('mousedown', initDrag);
                elem.addEventListener('mousedown', initDrag);
            } else if (elem.classList.contains('resizable')) { // resize by browser (! drag by handle - "draggable" must be available)
                resizeObserver.observe(elem);
            } else if (elem.classList.contains('resizer')) { // custom resize handle -> not by browser
                elem.removeEventListener('mousedown', initResize);
                elem.addEventListener('mousedown', initResize);
            }
        } else {
            // iterate over all elements with class 'draggable' or 'resizable' or 'resizer'
            document.querySelectorAll('.dragger').forEach(dragResizeDivInit);
            document.querySelectorAll('.resizable').forEach(dragResizeDivInit); // resize by browser-Default
            document.querySelectorAll('.resizer').forEach(dragResizeDivInit);
        }
    }



  function drd2_logData() {
    // show current position and size if possible
    if (resizeFrameContent) {
        // there is no good way to center text vertically in a div
        resizeFrameContent.style.top = ( parseInt(resizeFrame.style.height)/2 - 30 ) + 'px';
        resizeFrameContent.innerHTML = "" /* border and padding from frame - should be fix */
                            +  (parseInt(resizeFrame.style.left)  + 1  ) + ' , ' + (parseInt(resizeFrame.style.top) + 1) + ' <br> '
                            +  (parseInt(resizeFrame.style.width) + 1 + 5 /* gap */ ) + ' x ' + (parseInt(resizeFrame.style.height) + 1 + 5 /* gap */ )
                            + ' ' ; // consider border if avail ....
    }
  }

  function fixPosition(elem) {
    // create left and top properties
    //   boundingClientRect does not consider margin !!!
    if (elem.style.left === '' || elem.style.top === '' || elem.style.width === '' || elem.style.height === '') {
      if (elem.style.position !== 'absolute') {
        elem.style.position = 'absolute';
      }
      currentStyle = elem.currentStyle || window.getComputedStyle(elem);
      //
      elem.style.left = elem.style.left || currentStyle.left || '0px';
      elem.style.top = elem.style.top || currentStyle.top || '0px';
      //
      elem.style.width = elem.style.width || currentStyle.width || elem.getBoundingClientRect().width + 'px';
      elem.style.height = elem.style.height || currentStyle.height || elem.getBoundingClientRect().height + 'px';
      //
      console.log("drd fixPosition " + elem.id + ": Position " + elem.style.left + ", " + elem.style.top + ", " + elem.style.width + ", " + elem.style.height);
      //
      // fix border and padding
      elem.style.paddingLeft = elem.style.paddingLeft || currentStyle.paddingLeft || '0px';
      elem.style.paddingTop = elem.style.paddingTop || currentStyle.paddingTop || '0px';
      elem.style.paddingRight = elem.style.paddingRight || currentStyle.paddingRight || '0px';
      elem.style.paddingBottom = elem.style.paddingBottom || currentStyle.paddingBottom || '0px';
      console.log("drd fixPosition " + elem.id + ": Padding " + elem.style.paddingLeft + ", " + elem.style.paddingTop + ", " + elem.style.paddingRight + ", " + elem.style.paddingBottom);
      //
      elem.style.borderLeft = elem.style.borderLeft || currentStyle.borderLeft || '0px';
      elem.style.borderTop = elem.style.borderTop || currentStyle.borderTop || '0px';
      elem.style.borderRight = elem.style.borderRight || currentStyle.borderRight || '0px';
      elem.style.borderBottom = elem.style.borderBottom || currentStyle.borderBottom || '0px';
      //
      console.log("drd fixPosition " + elem.id + ": Border " + elem.style.borderLeft + ", " + elem.style.borderTop + ", " + elem.style.borderRight + ", " + elem.style.borderBottom);
    }
  }

  function initResizeOrDrag(e, drag = false) {
      // case
      //   - child: drag handle is child of dragged element
      //   - sibling: drag handle is sibling of dragged element (as this allows using css to address siblings)
      //   - self: browser handles resize, we use browser created resize handle
      //
      dragResizeHandle =  e ? e.target : null;
      dragResizeElement = dragResizeHandle ? dragResizeHandle.parentElement : null;
      if (dragResizeElement && dragResizeElement.id) {
          console.log("drd mousedown-" + (drag ? "drag" : "resize") + ": " + ( dragResizeElement ? dragResizeElement.id : "NULL" ) + " from " + ( dragResizeHandle ? dragResizeHandle.id : "NULL" ));
          e.preventDefault();
          e.stopPropagation();

          // prevent to move current element to last position of last element
          rasterTrgtPosFirst = null; // needed for overdose movement
          rasterTrgtPosCur = null;
          rasterTrgtPosLast = null; // needed for speed up - skipp unchanged dim

          dragResizeHandle.classList.add('active');

          fixPosition(dragResizeElement);
          dre_offset_x = parseInt( drag ? dragResizeElement.style.left : dragResizeElement.style.width) - e.clientX;
          dre_offset_y = parseInt( drag ? dragResizeElement.style.top : dragResizeElement.style.height) - e.clientY;

          // bring parent to front
          if (dragResizeElement.parentElement && dragResizeElement.parentElement.lastElementChild !== dragResizeElement) {
            console.log("drd initResizeOrDrag: bring " + dragResizeElement.id + " to front");
            dragResizeElement.parentElement.appendChild(dragResizeElement);
          }

          window.addEventListener('mousemove', drag ? doDrag : doResize);
          window.addEventListener('mouseup', stopDragAndResize);
      } else {
          console.log("drd mousedown-" + (drag ? "drag" : "resize") + ": SKIPP (NULL-element)");
          dragResizeElement = null; // disable end of drag-event
      }
  }

  function initResize(e) {
    initResizeOrDrag(e, false);
  }

  function initDrag(e) {
    initResizeOrDrag(e, true);
  }

  function doResize(e) {
      console.log("drd doResize   " + ( dragResizeElement ? dragResizeElement.id : "NULL" ) + " [" + e.clientX + ", " + e.clientY + "]");
      pos = {
        width: e.clientX + dre_offset_x,
        height: e.clientY + dre_offset_y
      };
      drd2_logData(e);
      resizeFrameTo(dragResizeElement, pos, true);
  }

  function doDrag(e) {
      console.log("drd doDrag   " + ( dragResizeElement ? dragResizeElement.id : "NULL" ) + " [" + e.clientX + ", " + e.clientY + "]");
      pos = {
        left: e.clientX + dre_offset_x,
        top: e.clientY + dre_offset_y
      };
      drd2_logData(e);
      resizeFrameTo(dragResizeElement, pos, true);
  }


  function stopDragAndResize() {
      console.log("drd stopDragAndResize   " + ( dragResizeElement ? dragResizeElement.id : "NULL" ));
      if (dragResizeElement) {
        resizeObserverDisable(dragResizeElement, true);
        if (dragResizeHandle) {
          dragResizeHandle.classList.remove('active');
        }
        //
        if (null != rasterTrgtPosLast) {
            //TODO prevent moving only on click
            setPosAndSize(resizeFrame, dragResizeElement);

        }
        resizeFrameTo(null);
        //
        window.removeEventListener('mousemove', doDrag);
        window.removeEventListener('mousemove', doResize);
        window.removeEventListener('mouseup', stopDragAndResize);
        //
        if (dragResizeElement.grid && dragResizeElement.grid.eGridBox) {
            let grid = dragResizeElement.grid;
            if (grid.items.length > 0){
                    grid.resizeInnerGrid( true );
                    setTimeout( function(){ grid.resizeInnerGrid(); }, 100); // KLUDGE: wait for rendering / use window.getComputedStyle(e)
            }
        }
        //
        resizeObserverDisable(dragResizeElement, false);
        dragResizeElement = null;
      }
  }


  function fitToRaster( elem, pos = null ) {
      dragResizeElement = elem;
      toggle(dragResizeElement, 'dragging', true);
      resizeFrameTo(dragResizeElement, pos, false, true);
      toggle(dragResizeElement, 'dragging', false);
       stopDragAndResize();
       /*

       e = new Event('fitToRaster');
       e.target = elem; //
       e.clientX = 0;
       e.clientY = 0;
       initResize(e);
       resizeFrameTo(dragResizeElement, { width: 26 + dre_offset_x, height: 26 + dre_offset_y }, true);
       stopDragAndResize();
       */
  }

  /*

    RASTER

  */


  function setPosAndSize( srcElem, trgElem ) {
    // https://stackoverflow.com/questions/21064101/understanding-offsetwidth-clientwidth-scrollwidth-and-height-respectively
    // https://jsfiddle.net/y8Y32/25/
    // cssWidth = elem.style.width
    // clientWidth = cssWidth + padding(left/right)
    //    scrollWidth = clientWidth + overflow-right
    //    offsetWidth = clientWidth + scrollbar(left/right) + border(left/right)
    trgElem.style.left = srcElem.style.left;
    trgElem.style.top = srcElem.style.top;
    /*
    // V1
    trgElem.style.width = srcElem.style.width;
    trgElem.style.height = srcElem.style.height;
    // V1 + V2
    // ensure same size - subtract padding and border
    trgElem.style.width =  (parseInt(trgElem.style.width) - ( trgElem.offsetWidth - srcElem.offsetWidth )) + 'px';
    trgElem.style.height =  (parseInt(trgElem.style.height) - ( trgElem.offsetHeight - srcElem.offsetHeight )) + 'px';
    */
    // V3
    trgElem.style.width =  (  srcElem.offsetWidth - ( trgElem.offsetWidth - parseInt(trgElem.style.width) ) ) + 'px'; // subtract padding and border
    trgElem.style.height =  (  srcElem.offsetHeight - ( trgElem.offsetHeight - parseInt(trgElem.style.height) ) ) + 'px'; // subtract padding and border
  }

  function getLRTBValues( elemStyle ) {
    return { left: parseInt(elemStyle.left), top: parseInt(elemStyle.top), width: parseInt(elemStyle.width), height: parseInt(elemStyle.height) };
  }

  function addPos( elem1, elem2, faktor = 1 ) {
    return {
      left: elem1.left + faktor * elem2.left,
      top: elem1.top + faktor * elem2.top,
      right: elem1.right ? elem1.right + faktor * elem2.right : null,
      bottom: elem1.bottom ? elem1.bottom + faktor * elem2.bottom : null,
      width: elem1.width ? elem1.width + faktor * elem2.width : null,
      height: elem1.height ? elem1.height + faktor * elem2.height : null
    };
  }


  function rasterPx( px, pxLast , borderWidth = 0, padding = 0, suffix = "px") {
    var diff = (pxLast ? pxLast - px : 0);
    var pxNew = rasterPos(  pxLast &&  (diff < -12 || diff > 12) ? px <= pxLast ? px : px + 24 - 6 : px + 12 , borderWidth, padding ) ;
    if (suffix) {
        pxNew = defaultUnit(pxNew, suffix);
    }
    // console.log("rasterPx: " + px + " -> " + pxLast + " && " + Math.abs(diff) + " > 12 ? " + px + " <= " + pxLast + " ? " + px + " : " + (px + 24 - 6) + " : " + (px + 12) + " == " + ( pxLast &&  Math.abs(diff) > 12 ? px <= pxLast ? px : px + 24 - 6 : px + 12) + ", " + borderWidth + " ) == " + pxNew);
    return pxNew;
  }

  function rasterPos( i , borderWidth = 0, padding = 0) {
    var borderGap = borderWidth / 2  - Math.abs( borderWidth  ) * 5 /*gap*/;
    i = i - borderGap + padding;
    return i - (i % 50) + borderGap;
  }

  function resizeFrameTo( elem, positionHint = null, updateElem = false , autoResize = false) {
    //console.log("resizeFrameTo: " + (elem ? elem.id : "NULL") + " [ " + (positionHint ? positionHint.left + ", " + positionHint.top + ", " + positionHint.width + ", " + positionHint.height : "NULL") + " ], updateElem: " + updateElem);
    if (null == elem) {
      resizeFrame.style.display = 'none';
      rasterTrgtPosFirst = null;
      rasterTrgtPosCur = null;
      rasterTrgtPosLast = null;
    } else if (autoResize && elem.classList.contains('manualResize')) {
      // skip resize if manually resized
      console.log("drd resizeFrameTo: " + elem.id + " SKIPP (manually resized)");
    } else {
      // overdose movement
      rasterTrgtPosLast = rasterTrgtPosCur;
      rasterTrgtPosCur = positionHint;
      if (!rasterTrgtPosCur || null == rasterTrgtPosFirst) { // enforce raster even if positionHint does not use all dimensions
        // is element
        rasterTrgtPosCur = {
          left: parseInt(elem.style.left),
          top: parseInt(elem.style.top),
          width: parseInt(elem.style.width),
          height: parseInt(elem.style.height)
        };
        if (null == rasterTrgtPosFirst) {
          resizeFrame.style.display = 'block';
          setPosAndSize(elem, resizeFrame); // init non-changed pos
          if ( !autoResize ) {
            toggle( elem, 'manualResize', true); // mark as manually resized - keep size ...
          }
          rasterTrgtPosFirst = rasterTrgtPosCur;
          rasterTrgtPosLast = { left: 0, top: 0, width: 0, height: 0 };
          dre_padBorder = { left: -2, top: -2, right: 4, bottom: 4 };
          //
          currentStyle = elem.currentStyle || window.getComputedStyle(elem);
          dre_pad = { left: parseInt(currentStyle.paddingLeft), top: parseInt(currentStyle.paddingTop), right: parseInt(currentStyle.paddingRight), bottom: parseInt(currentStyle.paddingBottom) };
          dre_border = { left: parseInt(currentStyle.borderLeft), top: parseInt(currentStyle.borderTop), right: parseInt(currentStyle.borderRight), bottom: parseInt(currentStyle.borderBottom) };
          dre_padBorder = addPos(dre_pad, dre_border);
          // aggregate padding and border for right and bottom
          //dre_padBorder = { left: dre_border.left, top: dre_border.top, right: dre_border.left + dre_pad.left + dre_pad.right + dre_border.right, bottom: dre_border.top + dre_pad.top + dre_pad.bottom + dre_border.bottom };
          dre_padBorder = { left: dre_border.left, top: dre_border.top, right: dre_pad.left + dre_pad.right , bottom: dre_pad.top + dre_pad.bottom };
          console.log("drd resizeFrameTo: " + elem.id + " [ " + dre_padBorder.left + ", " + dre_padBorder.top + ", " + dre_padBorder.right + ", " + dre_padBorder.bottom + " ] (init - padding + border)");

        }
        console.log ("drd resizeFrameTo: " + elem.id + " [ " + rasterTrgtPosCur.left + ", " + rasterTrgtPosCur.top + " / " + rasterTrgtPosCur.width + " x " + rasterTrgtPosCur.height + " ] (by style)");
      } else {
        console.log ("drd resizeFrameTo: " + elem.id + " [ " + rasterTrgtPosCur.left + ", " + rasterTrgtPosCur.top + " / " + rasterTrgtPosCur.width + " x " + rasterTrgtPosCur.height + " ] (by positionParam)");
      }
      //

      // calc raster
      if (rasterTrgtPosCur.left && rasterTrgtPosLast.left != rasterTrgtPosCur.left) {
        resizeFrame.style.left = rasterPx(rasterTrgtPosCur.left, rasterTrgtPosFirst.left, -dre_padBorder.left);
        if (updateElem) {
          elem.style.left = rasterTrgtPosCur.left + 'px';
        }
      }
      if (rasterTrgtPosCur.top && rasterTrgtPosLast.top != rasterTrgtPosCur.top) {
        resizeFrame.style.top = rasterPx(rasterTrgtPosCur.top, rasterTrgtPosFirst.top, -dre_padBorder.top);
        if (updateElem) {
          elem.style.top = rasterTrgtPosCur.top + 'px';
        }
      }
      if (rasterTrgtPosCur.width && rasterTrgtPosLast.width != rasterTrgtPosCur.width) {
        resizeFrame.style.width = rasterPx(rasterTrgtPosCur.width, rasterTrgtPosFirst.width, -dre_padBorder.left, dre_padBorder.right);
        if (updateElem) {
          elem.style.width = rasterTrgtPosCur.width + 'px';
        }
      }
      if (rasterTrgtPosCur.height && rasterTrgtPosLast.height != rasterTrgtPosCur.height) {
        resizeFrame.style.height = rasterPx(rasterTrgtPosCur.height, rasterTrgtPosFirst.height, -dre_padBorder.top, dre_padBorder.bottom);
        if (updateElem) {
          elem.style.height = rasterTrgtPosCur.height + 'px';
        }
      }
      //
      drd2_logData();
      console.log("drd resizeFrameTo: " + elem.id + " [ " + resizeFrame.style.left + ", " + resizeFrame.style.top + " / " + resizeFrame.style.width + " x " + resizeFrame.style.height + " ] (by raster)");
      //
    }
  }


  /*

      Resize by Browser

  */


  function resizeObserverDisable( elem, disable = true ) {
    // as ResizeObserver will fire on insert/delete in dom, display-change, resize by function ...
    // console.log("resizeObserverDisable: " + elem.id + " -> " + disable);
    if (disable) {
      resizeObserverDisabled.add(elem);
    } else {
      setTimeout(() => {
        resizeObserverDisabled.delete(elem); // or resize by drag will be triggered by async Observer
      }, 100);
    }
  }

  function resizeObserverResizeFinished() {
    // console.log("resizeObserverResizeFinished");
    toggle(dragResizeElement, 'dragging', false);
    stopDragAndResize();
  }

  const resizeObserver = new ResizeObserver(entries => {
    /* !! Observation will fire when watched Element (https://drafts.csswg.org/resize-observer/#ref-for-element%E2%91%A3)
      - inserted/removed from DOM.
      - display gets set to none.
    */

    for (let entry of entries) {
      if (resizeObserverDisabled.has(entry.target)) {

         console.log("drd resizeObserver: " + entry.target.id + " DISABLED");
      } else if ( 0 == entry.borderBoxSize[0].inlineSize ) {
         // handle just closed by mouse-click on Window-Close or Controller
         // tricky as object might still be visible according to css !!
         console.log( "resizeObserver: " + entry.target.id + " SKIPP (0 size - hidden/about closing)");
      } else if (resizeObserverSkippInitialCall.has(entry.target)) {

        // console.log("resizeObserver: " + entry.target.id);
        if (null == dragResizeElement || dragResizeElement !== entry.target) {
          // there is no default event when resizing stops
          // on start of resize
          console.log("drd resizeObserver: " + entry.target.id + " init EventListener  mouseup");
          window.addEventListener('mouseup', resizeObserverResizeFinished);
          dragResizeElement = entry.target;
        }
        toggle(dragResizeElement, 'dragging', true);
        resizeFrameTo(dragResizeElement);

      } else {
        console.log("drd resizeObserverSkippInitialCall: " + entry.target.id + " SKIPP( initial call )");
        resizeObserverSkippInitialCall.add(entry.target);
      }
    }

  });



  dragResizeDivInit();
