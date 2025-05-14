



// match BORDERS
// consider padding and border, but not margin
// assume top,left match - and bottom,right matches - so height/width has to compensate for padding and border
/*
  http://localhost:8080/?qry=dbTab
    at the time the routine jumps in here
    - e.style.width/height is change but that is not avail in e.style - but in e.getBoundingClientRect() is up-to-date ;((((

    !!!! --> e.style.width and getBoundingClientRect() might be not in sync !!!!

*/
function setBoxBorder( e, dim = null, alignDim = true ) {
    //margin = parseFloat(style.marginLeft) + parseFloat(style.marginRight),
    //padding = parseFloat(style.paddingLeft) + parseFloat(style.paddingRight),
    //border = parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth);
    //
    if (e.classList.contains("manualResize")) {
        console.debug("setBoxBorder: " + e.id + " - manualResize - skip");
    } else {
        if (dim && alignDim) {
            dim = alignPos( dim, 0, 0 );
        }
        if (dim) {
            //
            if (dim.left) e.style.left = dim.left + "px";
            if (dim.top) e.style.top = dim.top + "px";
            /* Padding from CSS is needed - and will be not retrieved by e.style.
            if (dim.width) e.style.width = (parseIntOr0(dim.width) - parseFloatOr0(e.style.paddingLeft) - parseFloatOr0(e.style.paddingRight) - parseFloatOr0(e.style.borderLeftWidth) - parseFloatOr0(e.style.borderLeftWidth))  + "px" ;
            if (dim.height) e.style.height = (parseIntOr0(dim.height) - parseFloatOr0(e.style.paddingTop) - parseFloatOr0(e.style.paddingBottom) - parseFloatOr0(e.style.borderTopWidth) - parseFloatOr0(e.style.borderBottomWidth)) + "px" ;
            */
            // calculate border and padding from getBoundingClientRect
            // may also use window.getComputedStyle(e).getPropertyValue('border-left-width')
            //var rect = e.getBoundingClientRect(); // including padding and border
            // might not be correct - TODO CHECK race condition !!!
            /* e.clientWidth --> width + padding !!!      https://developer.mozilla.org/en-US/docs/Web/API/CSS_Object_Model/Determining_the_dimensions_of_elements
            if (dim.width) e.style.width = (parseIntOr0(dim.width) - ( rect.width - e.clientWidth ) )  + "px" ;
            if (dim.height) e.style.height = (parseIntOr0(dim.height) - ( rect.height - e.clientHeight ) ) + "px" ;
            */
            var e__style = window.getComputedStyle(e);
            /* e__style is VOLATILE too !!!
            if (dim.width) e.style.width = (parseIntOr0(dim.width) - ( rect.width - parseFloatOr0(e__style.getPropertyValue("width")) ) )  + "px" ;
            if (dim.height) e.style.height = (parseIntOr0(dim.height) - ( rect.height - parseFloatOr0(e__style.getPropertyValue("height")) ) ) + "px" ;
            */
            // --> use values as static as possible
            gapWidth = parseFloatOr0(e__style.getPropertyValue("padding-left")) + parseFloatOr0(e__style.getPropertyValue("padding-right")) + parseFloatOr0(e__style.getPropertyValue("border-left-width")) + parseFloatOr0(e__style.getPropertyValue("border-right-width"));
            gapHeight = parseFloatOr0(e__style.getPropertyValue("padding-top")) + parseFloatOr0(e__style.getPropertyValue("padding-bottom")) + parseFloatOr0(e__style.getPropertyValue("border-top-width")) + parseFloatOr0(e__style.getPropertyValue("border-bottom-width"));
            gapWidth += 3;
            gapHeight += 3;
            // TODO: merge with raster - 
            if (dim.width) e.style.width = (parseIntOr0(dim.width) - gapWidth )  + "px" ;
            if (dim.height) e.style.height = (parseIntOr0(dim.height) - gapHeight) + "px" ;
        }
    }
    return dim;
}



const normalizePozitionCircular = (mouseX, mouseY) => {
  // ? compute what is the mouse position relative to the container element (scope)
  let {
    left: scopeOffsetX,
    top: scopeOffsetY,
  } = contextMenuCircularScope.getBoundingClientRect();

  scopeOffsetX = scopeOffsetX < 0 ? 0 : scopeOffsetX;
  scopeOffsetY = scopeOffsetY < 0 ? 0 : scopeOffsetY;

  const scopeX = mouseX - scopeOffsetX;
  const scopeY = mouseY - scopeOffsetY;

  let normalizedX = mouseX;
  let normalizedY = mouseY;

  // ? normalize on X Y
  if (scopeX + contextMenuCircular.clientWidth > contextMenuCircularScope.clientWidth) {
    normalizedX = scopeOffsetX + contextMenuCircularScope.clientWidth - contextMenuCircular.clientWidth;
  }
  if (scopeY + contextMenuCircular.clientHeight > contextMenuCircularScope.clientHeight) {
    normalizedY = scopeOffsetY + contextMenuCircularScope.clientHeight - contextMenuCircular.clientHeight;
  }

  return { normalizedX, normalizedY };
};




/*
    DEPRECATED use alignPosElem2

    What should match:
    - bounding box
    - border
    - content

*/
function alignPosElem( e, x_veloc = null , y_veloc = null, eTo = null) {
    var styleOrRect = e.style ?? e.contentRect;
    if( e.contentRect) {
        // e.contentRect,left/top might not match the top/left of origin element
        styleOrRect.left = e.target.style.left;
        styleOrRect.top = e.target.style.top;
    }
    if( styleOrRect ){
        // if target is given - we want to set the resizer-Box - to need width/height
        // console.debug("alignPosElem:: << " + styleOrRect.left + ", "+ styleOrRect.top + ", "+ styleOrRect.width + ", "+ styleOrRect.height + "    " + x_veloc + "/" + y_veloc);
        pos = { "left": styleOrRect.left, "top": styleOrRect.top, "width": styleOrRect.width , "height": styleOrRect.height };
        pos = alignPos( pos, x_veloc, y_veloc );
        var eTo_ = eTo ?? e;
        if ((eTo || eTo_.style.left ) && pos.left) eTo_.style.left = pos.left;
        if ((eTo || eTo_.style.top ) && pos.top) eTo_.style.top = pos.top;
        if ((eTo || eTo_.style.width ) && pos.width) eTo_.style.width = pos.width;
        if ((eTo || eTo_.style.height ) && pos.height) eTo_.style.height = pos.height;
        //console.debug("alignPosElem:: >> " + eTo_.style.left + ", "+ eTo_.style.top + ", "+ eTo_.style.width + ", "+ eTo_.style.height + "");
    }
    return eTo;
}

function alignPosElem2( e, x_veloc = null , y_veloc = null, eTo = null, veloc_Resize = false) {
    var eTo_ = eTo ?? e;
    var dim = getDim(e);
    dim = alignPos( dim, x_veloc, y_veloc , "", veloc_Resize);
    setBoxBorder( eTo_, dim, false /*already aligned*/ );
    return eTo_;

}

function getDim( e ) {
    var rect = e.getBoundingClientRect(); // including padding and border
    var dim = { "left": rect.left, "top": rect.top + window.scrollY, "width": rect.width + window.scrollX  , "height": rect.height };
    return dim;
}




function alignPos( pos, x_veloc = null , y_veloc = null, suffix = "px", veloc_Resize) {
    if ( !veloc_Resize && ( pos.left || pos.top)){
        [ pos.left, pos.top ] = alignPosXY( pos.left, pos.top, x_veloc, y_veloc , suffix);
    }
    if (pos.width || pos.height){
        [ pos.width, pos.height ] = alignPosXY( pos.width, pos.height, x_veloc ?? 1, y_veloc ?? 1 , suffix, +25); // better oversize, never smaller - as content will not fit in
    }
    return pos;
}

function alignPosXY( x, y, x_veloc = 0 , y_veloc = 0, suffix = "", offset = null) {
    if (offset) { // prevent overshooting
        x -= offset;
        y -= offset;
    }
    // always in current direction -> moving to top do floor, moving down do ceil -> +-0,5
    x = ((Math.round( (parseInt(x) ) / 50 + (x_veloc < 0 ? -0.4 : 0 == x_veloc ? 0 : 0.4 ) ) * 50) );
    y = ((Math.round( (parseInt(y) ) / 50 + (y_veloc < 0 ? -0.4 : 0 == y_veloc ? 0 : 0.4 ) ) * 50) );
    if (offset) { // used to create gap between elements
        x += offset;
        y += offset;
    }
    if (suffix) {
        x = defaultUnit(x, suffix);
        y = defaultUnit(y, suffix);
    }
    return [ x ,y ];
}



/*


    position

    https://javascript.info/size-and-scroll

*/

function getPosition(el) {
        if (!el) {
            return {top: null, left: null}
        }
	    var rect = el.getBoundingClientRect(),
	    scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
	    scrollTop = window.pageYOffset || document.documentElement.scrollTop;
	    return { top: rect.top + scrollTop, left: rect.left + scrollLeft }
	}

function getPositionAndSize(el, handleGap = false) {
        if (!el) {
            return {top: 0, left: 0, width: 0, height: 0}
        }
        let gapHeight = 0;
        let gapWidth = 0;
	    var rect = el.getBoundingClientRect(),
	    scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
	    scrollTop = window.pageYOffset || document.documentElement.scrollTop;
	    if (handleGap){
	        console.log("getPositionAndSize: " + el.id + " - \KLUDGE enlarge BOX to allow right size on restore " );
	        var e__style = window.getComputedStyle(el);
            /* e__style is VOLATILE too !!!
            if (dim.width) e.style.width = (parseIntOr0(dim.width) - ( rect.width - parseFloatOr0(e__style.getPropertyValue("width")) ) )  + "px" ;
            if (dim.height) e.style.height = (parseIntOr0(dim.height) - ( rect.height - parseFloatOr0(e__style.getPropertyValue("height")) ) ) + "px" ;
            */
            // --> use values as static as possible
            gapWidth = parseFloatOr0(e__style.getPropertyValue("padding-left")) + parseFloatOr0(e__style.getPropertyValue("padding-right")) + parseFloatOr0(e__style.getPropertyValue("border-left-width")) + parseFloatOr0(e__style.getPropertyValue("border-right-width"));
            gapHeight = parseFloatOr0(e__style.getPropertyValue("padding-top")) + parseFloatOr0(e__style.getPropertyValue("padding-bottom")) + parseFloatOr0(e__style.getPropertyValue("border-top-width")) + parseFloatOr0(e__style.getPropertyValue("border-bottom-width"));
            // match it up with setBoxBorder
	    }
	    return { top: rect.top + scrollTop, left: rect.left + scrollLeft, width: el.clientWidth + gapWidth , height: el.clientHeight + gapHeight }
	}

function defaultUnit(val, unit = "px") {
    return Number(val) ? val + unit : val;
}






/*
 adjust "Grid" to Data
*/
function fitToGridContainer(grid, forceProp = false, forceSet = false) {

    /*
        Approach:
        - if size is set - it will be kept !!  <-- (gridBox.style.width is set)
        - if size is NOT set
            - size in prop defined -> set to prop
            - size in prop NOT defined
                - NOT- forceSet
                    - just wait to be defined by column width (by content)
                - forceSet
                    - set height by line count
                    - set width by query / column count
                    - use very rough raster -> 200x200
    */
    var eGridBox = grid.eGridBox;
    var pos = null;
    if (!eGridBox.style.width || !eGridBox.style.height || forceProp) {
        // initial run -> assume size is always set
        pos = grid.prop_hidden && grid.prop_hidden.pos ? grid.prop_hidden.pos : { "left": null, "top": null, "width": null, "height": null };
        //
        if (!pos.width && forceSet){
            // set width by query / column count
            var eCdmSize = getBoxElem4Grid(grid,"CodeMirror-sizer");
            var eCdmHScroll = getBoxElem4Grid(grid,"CodeMirror-hscrollbar");
            var minWidth = parseInt(eCdmSize.style.minWidth) || 50;
            console.log("fitToGridContainer: eCdmHScroll.clientWidth: " + eCdmHScroll.clientWidth +  " / eCdmSize.style.minWidth: " + minWidth  );
            pos.width = eCdmHScroll.clientWidth;
            var colsCount = grid.items ? grid.items[0].length : 0;
            var colWidth = colsCount < 5 ? 250 : colsCount < 20 ? 450 : 1000;
            if (pos.width < colWidth ) {
                pos.width = colWidth;
            }
            if (pos.width < 150 + minWidth ) {
                pos.width = 150 + minWidth;
            }
        }
        //
        if (!pos.height){ // height will be set always ...
            rowCount = grid.items ? grid.items.length : 0;
            pos.height = rowCount < 20 ? 200 : rowCount < 25 ? 400 : 500;
        }
        //
        /* my be called by fitToGridContainer !!!

            createGridContainer > fitToRaster > stopDragAndResize > fitToGridContainer

            xLog("connect","util_pos::fitToGridContainer: pos: " + JSON.stringify(pos));

            fitToRaster( eGridBox );




        */
        //
    } else {
        // size is set -> keep it
        xDebug('connect','connect::fitToGridContainer: size is set -> keep it');
    }

    // should set size if width/height is set OR at least the min-height/min-width is set
    if (pos && pos.width && pos.height){

        // only if size is set
        setBoxBorder( eGridBox, pos, false ); // and align
        //
        if (grid.items.length > 0){
            grid.resizeInnerGrid( true );
            setTimeout( function(){ grid.resizeInnerGrid(); }, 100); // KLUDGE: wait for rendering / use window.getComputedStyle(e)
        }

    }
}




function fitToGridContainerOLD(grid, forceProp = false){
    fitToGridContainer(grid, forceProp, true);
}