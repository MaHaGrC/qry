
//    https://javascript.info/bubbling-and-capturing

//document.getElementById("grid-container").addEventListener("input", function() {
// should work...
//document.querySelectorAll('.some-class').forEach(item => {

let enableEditingElementEnlargeLast;

  function enableEditing(e , element) {
    if (!e.button === 1 /*left*/) {
      xDebug('event', "enableEditing surpressed");
      return true;
    }
    xDebug('event', "enableEditing");
    //Adds the content editable property to passed element
    // resize to see whole content?
    if(enableEditingElementEnlargeLast && enableEditingElementEnlargeLast != element) {
        enableEditingElementEnlargeLast.setAttribute('contenteditable', false)
        enableEditingElementEnlargeLast.style.cssText += "height: 1em; overflow: hidden;";
        enableEditingElementEnlargeLast.classList.remove("enlarge");
    }
    enableEditingElementEnlargeLast = element;
    element.setAttribute('contenteditable', true);
    if (element.style.cssText.includes('height: 1em; overflow: hidden;')){
        element.style.cssText = element.style.cssText.replace("height: 1em; overflow: hidden;","");
        element.classList.add("enlarge");
    }
    //Focuses the element
    element.focus()
  }



function initGridInputListener(gridElem) {
    gridElem.querySelector(".grid-container").addEventListener("input", function(e) {
      xDebug('event', e);
      //document.getElementById("log").value += e.data;  // just the current char
      // log( "<input> " + e.target.id + ": "+ e.target.innerHTML );
      //if (e.key === 'Enter' || )
      if (e.inputType == 'insertParagraph') { // ENTER
        e.preventDefault();
        let grid = gridElem.grid;
        grid.input_end(e, value_current);
        e.target.removeAttribute('contenteditable');
        document.activeElement.blur(); // unset focus

        // remove multiline final <br> from contenteditable
        value_current = value_current.replace(/<br>$/m,"");
        e.target.innerHTML = value_current;
        // convert back to Src-Format
        // !! keep trailing spaces/ indent ...
        // value_current =  grid && grid.colDataType[grid.getColOf(e.target)].includes('H') ? value_current : String.fromHtmlEntities(value_current);
        // value_current =  grid && grid.colDataType[grid.getColOf(e.target)].includes('H') ? e.target.innerHTML : String.decodeHTML(value_current)  ; //: e.target.innerText;
        value_current =  grid && (grid.colDataType[grid.getColOf(e.target) ??  0 ] ?? "").includes('H') ? e.target.innerHTML : String.decodeHTML(value_current)  ; //: e.target.innerText;

        // if grid ist just input-field (snapshot-naming ...) - )
        clbk = grid.updateGridValue ?? updateGridValue;

        var grid_elem = elemWithId(e.target,["grid-item","grid-idx", "grid-head"]);
        if (grid_elem.classList.contains("grid-head")) {
            // skipp modification for HEAD - if it is used for FILTERING ...
            var elemColName = elemChildByClass(e.target,"colName", true);
            var curVal = elemColName.innerHTML;
            curVal = curVal.replace("<br>","");
            if (!curVal) { // restore colName
                elemColName.innerHTML = grid.getData( 0, grid.getColOf(grid_elem));
                grid_elem.classList.remove("filtered");
            }
        } else if(clbk) {
            e.target.classList.add("modified");
            clbk( grid, grid.codeMirror.getValue(), value_current, grid.getIdxOf(e.target)
                        ,function() {
                            if (e.target.classList.contains("enlarge")){
                                e.target.style = e.target.style + "height: 1em; overflow: hidden;";
                            }
                            // update data behind grid ...
                            grid.storeCell( e.target);
                            // TODO use Toggle
                            e.target.classList.add("saved");
                            setTimeout(function() {
                                xDebug('event', "SAVE after CHANGE-VALUE");
                                e.target.classList.remove("enlarge");
                                e.target.classList.remove("modified");
                                e.target.classList.remove("saved");
                            }, 3000);
                        }
                        , grid.getFullRowOf(e.target)
                    );
        }

        /*
        setTimeout(function() {
          grid.storeCell( e.target); // update data behind grid ...
          e.target.classList.add("saved");
          setTimeout(function() {
            xDebug('event', "simulated SAVE after CHANGE-VALUE");
            e.target.classList.remove("modified");
            e.target.classList.remove("saved");
          }, 3000);
        }, 2000);
        */

        return false;
      } else {
        var elem_target = e.target;
        var grid_elem = elemWithId(e.target,["grid-item","grid-idx", "grid-head"]);
        grid_elem.classList.add("filtered");
        log("event::initGridInputListener " + grid_elem.id + ": " + e.target.innerHTML);
        //
        if (grid_elem.classList.contains("grid-head")) { // KLUDGE detected
            let grid = findGrid(grid_elem);
            let col = grid.getColOf(grid_elem);
            if (!grid.filterRowColAttr) {
                grid.filterRowColAttr = array_resize(grid.filterRowColAttr, grid.items[0].length);
            }
            if (!grid.filterRowIdx) {
                grid.filterRowIdx=[];
            }
            var elemColName = elemChildByClass(e.target,"colName", true);
            //
            var filterVal = elemColName.innerHTML;
            filterVal = filterVal.replace("<br>","");
            grid.filterRowColAttr[ col ] = filterVal;
            log("event::initGridInputListener " + grid_elem.id + ": filter >> " + filterVal);
            var rowLimit = grid.items.length;
            for (let r_ = 1; r_ < rowLimit; r_++) {
                let r = grid.sortIdx ? grid.sortIdx[r_][0] : r_;
                let cellVal = grid.getData( r, col);
                grid.filterRowIdx[r] = !filterVal || cellVal.includes( filterVal);
            }
            //
            grid.load(0, 1 /*updateDataOnly*/);
        }
        //
      }
      value_current = e.target.innerHTML; // as ENTER already create sub-divs...

    }, false);
}


document.querySelector(".grid-container").addEventListener("input", function(e) {}, false);
