let dragResizeGridCol = null;
let dragResizeGridColStartMouse = null;
let dragResizeGridColStartElem = null;
let dragResizeGridColColOffset = null;
let dragResizeGridColAction = null;

// https://codepen.io/lukerazor/pen/GVBMZK?editors=1111

function SetCursor(cursor) {
	let page = document.getElementById("grid_container");
	page.style.cursor = cursor;
}

function OnDragResizeGridCol(event) {
	if(dragResizeGridCol) {
		// xLog('dragResizeGridCol', event);
		xDebug('dragResizeGridCol', 'dragResizeGridCol::OnDragResizeGridCol ' +  event.target.id + " " + dragResizeGridColStartMouse + " + " + (event.clientX - dragResizeGridColStartMouse)  + " => " + event.clientX + " // " + dragResizeGridColStartElem + " + " + (event.clientX - dragResizeGridColStartMouse) + " => " + (dragResizeGridColStartElem + (event.clientX - dragResizeGridColStartMouse)) + "px");

        let cssGridNode = dragResizeGridCol.parentNode; // get node with gridTemplateColumns
        let col2 = parseInt(dragResizeGridCol.id.replaceAll(/.*_/g,""));
        xDebug('dragResizeGridCol', "cssGridNode.gridTemplateColumns:" +  cssGridNode.style.gridTemplateColumns  + " [ " + col2 + " ]");
        let sizes = cssGridNode.style.gridTemplateColumns.split(" ");
        sizes[ col2 ] =  (dragResizeGridColStartElem + (event.clientX - dragResizeGridColStartMouse)) + "px";
        xDebug('dragResizeGridCol', "cssGridNode.gridTemplateColumns:" +  cssGridNode.style.gridTemplateColumns + " -> " + sizes.join(" "));
        cssGridNode.style.gridTemplateColumns = sizes.join(" ");

        dragResizeGridColAction = "OnDragResizeGridCol::dragResizeGridCol " + ( cssGridNode ? cssGridNode.id ? '"' + cssGridNode.id + '"' : null : null ) + ', col:' + col2 + ', "' + sizes[ col2 ] + '"' ;

		event.preventDefault()
	}
}


function StartDragResizeGridCol(event, colOffset /*allow to resize previous col, as it is hard to have indicator show at right column-end - even if column is to small*/) {
	event.preventDefault();
	event.stopPropagation (); // prevent moving head-column
	xLog('dragResizeGridCol', "StartDragResizeGridCol: mouse down");
	document.getElementById("grid_container").addEventListener('mousemove', OnDragResizeGridCol);
	document.getElementById("grid_container").addEventListener('mouseup', EndDragGridColResize);
	dragResizeGridColStartMouse = event.clientX;
	current_col = event.target.parentNode.parentNode;
	dragResizeGridCol = -1 == colOffset ? current_col.previousSibling : current_col;
	dragResizeGridColStartElem = dragResizeGridCol.clientWidth;
	dragResizeGridColColOffset = colOffset;
	SetCursor("ew-resize");
	return false;
}

function EndDragGridColResize() {
	xLog('dragResizeGridCol', "EndDragGridColResize: mouse up");
	document.getElementById("grid_container").removeEventListener('mousemove', OnDragResizeGridCol);
	document.getElementById("grid_container").removeEventListener('mouseup', EndDragGridColResize);
	dragResizeGridCol = false;
	SetCursor("auto");
	recordAction(dragResizeGridColAction);
}

