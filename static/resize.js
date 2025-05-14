
// just catch




// http://jsfiddle.net/3jMQD/

// Alternative: https://api.jqueryui.com/resizable/    like   https://codepen.io/webDsign/pen/yLgPMPW

// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize
// OBSOLETE - use css - resize


var pResize = null;
var pResizeStartX, pResizeStartY, pResizeStartWidth, pResizeStartHeight;
var pResizeCoolDown = 0; /* prevent reaktivation on inital-click-event to resizeDragStop */

/*
pResize.addEventListener('click', function init() {
    resizeInit( this );
}, false);
*/

function resizeInit( e ) {
  if ((new Date).getTime() > pResizeCoolDown){
    if(pResize) {
      log("resizeInit " + e.id + "  (treat click as stop)");
      resizeDragStop();
    } else {
      log("resizeInit " + e.id + "  ");
      pResize = e; //  document.querySelector( selector );
      pResize.className = pResize.className + ' resizable';
      //
      // ensure to see handles ..
      pResize.scrollTop = pResize.scrollHeight;
      //
      var resizer = document.createElement('div');
      resizer.id = 'resizer';
      resizer.className = 'resizer';
      pResize.appendChild(resizer);
      resizer.addEventListener('mousedown', resizeDragStart, false);
      //resizer.top= (pResize.scrollTop -10) + "px";// move to bottom of textarea 
      //
      var resizerHorizontal = document.createElement('div');
      resizerHorizontal.id = 'resizerHorizontal';
      resizerHorizontal.className = 'resizerHorizontal';
      //resizerHorizontal.classList.add("fa-solid", "fa-grip-lines");
      pResize.appendChild(resizerHorizontal);
      resizerHorizontal.addEventListener('mousedown', resizeDragStart, false);
      //resizerHorizontal.top= (pResize.scrollTop-10) + "px";// move to bottom of textarea  -- only works by manuel set ?????
      //      
    }
  }
};

function resizeDragStart(e) {
   log("resizeDragStart ... " + e);
   pResizeStartX = e.clientX;
   pResizeStartY = e.clientY;
   pResizeStartWidth = parseInt(document.defaultView.getComputedStyle( pResize ).width, 10);
   pResizeStartHeight = parseInt(document.defaultView.getComputedStyle( pResize ).height, 10);
   document.documentElement.addEventListener('mousemove', resizeDragDo, false);
   document.documentElement.addEventListener('mouseup', resizeDragStop, false);
}

function resizeDragDo(e) {
   pResize.style.width = (pResizeStartWidth + e.clientX - pResizeStartX) + 'px';
   pResize.style.height = (pResizeStartHeight + e.clientY - pResizeStartY) + 'px';
}

function resizeDragStop(e) {
  log("resizeDragStop ... " + e);
  pResizeCoolDown = (new Date()).getTime() + 500;
  //
  document.getElementById("resizer").remove();
  document.getElementById("resizerHorizontal").remove();
  //
  document.documentElement.removeEventListener('mousemove', resizeDragDo, false);   
  document.documentElement.removeEventListener('mouseup', resizeDragStop, false);
  //
  pResize.classList.remove( 'resizable');
  pResize = null;
  return false;
}
