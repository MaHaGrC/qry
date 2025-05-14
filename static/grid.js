//  https://www.ag-grid.com/javascript-data-grid/column-sizing/
let grids = [];
let grids_by_name = {};
let gridNoCounter = 0;

function nextGridNo(){
    gridNoCounter = gridNoCounter + 1;
    for( i = 1; i < grids.length ; i++){
        if (!grids[i]) {
            gridNoCounter = i; // found empty slot - reuse
            break;
        }
    }
    return gridNoCounter; // must not be 0 !! - as check per "if (gridNoCounter) " would not work ..
}

/**
 * Convert a string to HTML entities
 */
String.toHtmlEntities = function(string) {
    return (string+"").replace(/./gm, function(s) {
        // return "&#" + s.charCodeAt(0) + ";";
        return (s.match(/[a-z0-9\s]+/i)) ? s : "&#" + s.charCodeAt(0) + ";";
    });
};

/**
 * Create string from HTML entities
 */
String.fromHtmlEntities = function(string) {
    return (string+"").replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
    })
};

String.decodeHTML = function(input) {
   // &lt;img src='myimage.jpg'&gt;
   // !!! loses spaces, indent ....  > consider decodeURIComponent
   // !!! loses linebreaks
   // &nbsp; -> " "
   input = "X" + input;
   input = input.replace(/<br>/,'\n');
   input = input.replaceAll("&nbsp;",' ');
   var doc = new DOMParser().parseFromString(input, "text/html");
   return doc.documentElement.textContent.slice(1);
};


class Grid {

  constructor( elem, elemBox ) {
    this.gridNo = nextGridNo();
    this.eGridBox = elemBox;
    this.eGrid = elem; // document.getElementById( id ); // "grid-container"
    if (elem) elem.id = 'grid' + this.gridNo + "_"; /* need suffix to separate from grid-box */
    this.items = [["", 2, 3], [4, 5, 6], [7, 8, 9], [10, 11, 12], [13, 14, 15]];
    this.colsGrouped = 0 ; // how many cols are grouped -- grouping  = sorting and skipping redundant
    this.groupingSmart = true;
    this.lenMax = [];
    this.colSizeType = 3; // good for dbTabUsage - as it would be too small
    this.colSizeType = 1; // good for erp_import_article[]erp_import_article_av - as col1 in colSizeType 3 got word-wrapped
    this.colDataType = [];
    this.sortModeIcons = ["fa-sort", "fa-chevron-down", "fa-chevron-up"];
    this.sortModeAvail = ["", "asc", "desc", "num-asc", "num-desc"];
    this.sortMode = null;
    this.sortOrder = null;
    this.sortIdx = null;   // row mapping from items to sorted-display
    this.filterRowColAttr = null; // filter rows by attributes per Column
    this.filterRowIdx = null; // filter rows - rows that are filtered .. filter = Skipped ...
    this.rowOffset = 0; // allow to scroll via rows ...
    this.colOrderIdx = null; // allow to reorder, skipp columns - without changing data
    this.colFilterIdx = null; // just skipp columns in view ...
    this.colsDef = [];
    this.valHndl = {};
    this.prop_hidden = {}; // store non-grid-relevant props that should not be shown in qry ...
    this.prop_manual = {}; // override Hinted-Props like position by manual setting ... (e.g. by drag-drop of grid frame - the frame must stay in place)
    this.name = null;
    //
    grids[ this.gridNo ] = this;
  }

  get_property(skippNulls = false){
    var grid_properties = {};
    for( var prop in this) {
        if ("colsGrouped colOrderIdx sortIdx".includes(prop)){
            if (!skippNulls || (null != this[prop] && 0 != ("colsGrouped".includes(prop) ? this[prop] : this[prop].length))) {
                grid_properties[prop] = this[prop];
           }
        }
    }
    return grid_properties;
  }

  set_property(property_new){
    this.prop_hidden = {};
    if (property_new) {
        for( var prop in property_new) {
            if ("pos cdm limit".includes( prop)) {
                xDebug('grid', 'grid::set_status prop hidden: '+ JSON.stringify(prop) +'');
                this.prop_hidden[ prop ] = property_new[prop];
            } else if (!Object.prototype.hasOwnProperty.call(this, prop)) {
                xError('grid', 'grid::set_status prop unknown: '+ JSON.stringify(prop) +'')
            } else if( "colsGrouped valHndl colOrderIdx name prop_manual".includes( prop ) ) {
                xInfo('grid', 'grid::set_status prop '+ JSON.stringify(prop) + ' => '+  JSON.stringify(property_new[prop]) + '');
                this[prop] = property_new[prop];
                if ("name" == prop) {
                    grids_by_name[ property_new[ prop ] ] = this;
                }
            } else {
                xError('grid', 'grid::set_status prop denied: '+ JSON.stringify(prop) +'')
            }
        }
    }
  }


  setName( name ) {
    if (this.name != name) {
        if (this.name) {
            delete grids_by_name[ this.name ];
        }
        this.name = name;
        grids_by_name[ this.name ] = this;
    }
    getBoxElem4Grid(this, "grid-name").innerHTML = this.name ?? ""; // enforce sync
  }

  input_end(e, val) {
    xLog('grid',"< END > " + e.target.id + ": " + val);
  }

  getData(r, c) {
    return this.items[r][c];
  }

  getVisibleDataTxt( rowlimit = null,  colLimit = null ) {
    var data = "";
    rowlimit = rowlimit ?? this.items.length;
    colLimit = colLimit ?? (this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length );
    for (let r_ = 0; r_ < rowlimit; r_++) {
        let r = this.sortIdx ? this.sortIdx[r_][0] : r_;
        if ( this.items[r]) {
            data = data + "|";
            for (let c_ = 0; c_ < colLimit; c_++) {
                let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                let data_cur = this.items[r][c];
                // remove html-tags
                data_cur = data_cur.replace(/<[^>]*>/g, "");
                // replace nnbsps
                data_cur = data_cur.replace(/&nbsp;/g, " ");
                if (this.colDataType && this.colDataType[c] && this.colDataType[c].startsWith("n")) {
                    data = data + data_cur.padStart(Math.max(this.lenMax[c],(this.items[0][c]??"").length)) + "|" ; // align right
                } else {
                    data = data + data_cur.padEnd(Math.max(this.lenMax[c],(this.items[0][c]??"").length)) + "|" ; // align left
                }
            }
        }
        if (0==r) {
            data = data + "\r\n";
            data = data + "|";
            for (let c_ = 0; c_ < colLimit; c_++) {
                let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                data = data + "".padStart(Math.max(this.lenMax[c],(this.items[0][c]??"").length),'-') + "|" ;
            }
        }
        data = data + "\r\n";
    }
    return data;
  }


  getVisibleDataJson( rowlimit = null,  colLimit = null ) {
    var data = "";
    rowlimit = rowlimit ?? this.items.length;
    colLimit = colLimit ?? (this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length );
    for (let r_ = 1; r_ < rowlimit; r_++) {
        let r = this.sortIdx ? this.sortIdx[r_][0] : r_;
        if ( this.items[r]) {
            data = data + "{";
            let c_first = 0;
            for (let c_ = 0; c_ < colLimit; c_++) {
                let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                if (this.items[r][c]){
                    data = data + " " + ( c_first ? ", " : "" ) + this.items[0][c] + ": \""+ this.items[r][c] + "\"" ;
                    c_first++;
                }
            }
            data = data + "}" +  (r_ < rowlimit-1 ? "," : "");
            data = data + "\r\n";
        }
    }
    return "{  \"" + this.codeMirror.getValue() + "\":  [\r\n" + data + "]}";
  }


  getVisibleData( rowlimit = null,  colLimit = null ) {
    var data = "";
    rowlimit = rowlimit ?? this.items.length;
    colLimit = colLimit ?? (this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length );
    for (let r_ = 0; r_ < rowlimit; r_++) {
        let r = this.sortIdx ? this.sortIdx[r_][0] : r_;
        if ( this.items[r]) {
            for (let c_ = 0; c_ < colLimit; c_++) {
                let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                data = data + this.items[r][c].replace(";","\;") + ";" ;
            }
        }
        data = data + "\r\n";
    }
    return data;
  }

  getVisibleDataAsList( rowlimit = null,  colLimit = null ) {
    var data = [];
    rowlimit = rowlimit ?? this.items.length;
    colLimit = colLimit ?? (this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length );
    for (let r_ = 0; r_ < rowlimit; r_++) {
        let r = this.sortIdx ? this.sortIdx[r_][0] : r_;
        if (this.items[r]) {
            for (let c_ = 0; c_ < colLimit; c_++) {
                let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                data.push( this.items[r][c] );
            }
        } else {
            data.push([]);
        }
    }
    return data;
  }



  updateData(r, c, value) {
    this.items[r][c] = value;
    let elem = document.querySelector('[srcd="' + r + ',' + c + '"]');
    if (elem) {
      elem.innerHTML = value;
    }
    // check if visible and update grid ...
  }

  storeCell(elem) {
    if (elem) {
      let src = this.getIdxOf(elem).split(",");
      this.items[src[0], src[1]] = elem.innerHTML;
    }
  }

  insertRow(elem, row) {
    if (elem) {
      //let src = this.getIdxOf(elem).split(",");
      let newRow = [...this.items[0]];
      for (let c = 0; c < newRow.length; c++) {
        newRow[c] ="";
      }
      this.items.splice( row, 0, newRow);
      this.load(); // need re-numbering ids on cells ...
    }
  }

  getIdxOf(elem) {
    // reverse sorting, paging and filtering, editing cells ...
    let attr = elem.getAttribute("srcd");
    while ( null == attr) {
        elem = elem.parentNode;
        attr = elem.getAttribute("srcd")
    }
    return attr;
  }

  getElement( row, col) {
    return document.getElementById("grid" + this.gridNo + "_" + row + "_" + col);
  }

  getFullRowOf(elem) {
    let row = null;
    if (elem) {
        row = this.getRowOf(elem);
    }
    return row ? this.items[ row ] : null ;
  }

  getRowOf(elem) {
    return this.getIdxOf(elem).replace(/,.*/,"");
  }

  getColOf(elem) {
    return this.getIdxOf(elem).replace(/.*,/,"");
  }

  getRowAndColOf(elem) {
    return this.getIdxOf(elem).split(/,/);
  }

  // todo: @martin zur optimierung
  resizeInnerGrid( handleNotUpdatedGetBoundingClientRect = false ) {
    const gridContainer = this.eGrid;
    const gridBox = this.eGridBox;

    //


    // check height - might change by codemirror( long text -> add scrollbar / multi-line increase height ...

    gridContainer.style.height = "";
    xLog('grid', "resizeInnerGrid: " + parseInt(gridBox.style.top) + "(gridBox.top)  " + (parseInt(gridBox.style.top)+ resizeEndH) + "(gridBox.bottom) " );
    xLog('grid', "resizeInnerGrid: " + gridContainer.getBoundingClientRect().top + "(my.top)  " + gridContainer.parentNode.getBoundingClientRect().bottom + "(parent.bottom) " + gridContainer.getBoundingClientRect().bottom + "(my.bottom) ");
    var heightDefault = gridContainer.getBoundingClientRect().height - 4 /* padding myself */ ;
    var heightGap = gridContainer.parentNode.getBoundingClientRect().bottom - 6  /*padding bottom parent*/ - gridContainer.getBoundingClientRect().top - 5 /* padding myself */ ;
    if ( heightDefault > heightGap || heightGap > heightDefault * 1.75 ) {
        // as it might be incorrect on fast grids ...       gridContainer.style.height = heightGap + "px" ;
        // but we need it to get scrollbars ... so we need to set it to max-content
        // !!! gridContainer.parentNode.getBoundingClientRect().bottom might be incorrect - as it is not updated yet ...
        if (!handleNotUpdatedGetBoundingClientRect) {
            gridContainer.style.height = heightGap + "px" ;
        }
        gridContainer.style.gridAutoRows = "max-content"; // prevent shrinking and enforce scrolling
        xLog('grid', "resizeInnerGrid: - fit " + gridContainer.id + " to " + heightGap + " (default height: " + heightDefault + ")");
    }


    // Prüfen wie viele ch spalten mit Prozentwerten ersetzt werden müssen
    if (true || handleNotUpdatedGetBoundingClientRect) {
        console.warn("grid.resizeInnerGrid - DISABLED for cols");
    } else {
        const colsArray = gridContainer.style.gridTemplateColumns.split(" ");
        const colsToChange = colsArray.reduce((count, str) => {
          if (str.endsWith("ch") || str.endsWith("%")) return count + 1;
          return count;
        }, 0);
        xDebug('grid', "colsToChange:", colsToChange);


        // Restlichen prozentualen Platz ermitteln
        const colsWithFixedWidth = colsArray.filter((string) => string.endsWith("px"));
        let reservedPixel = 0;
        for (const value of colsWithFixedWidth) { reservedPixel += parseInt(value.replace("px", "")); }
        const gridContainerWidth = gridContainer.clientWidth;
        const remainingWidthPercentage = ((gridContainerWidth - reservedPixel) * 100) / gridContainerWidth;
        xDebug('grid', "remainingWidthPercentage:", remainingWidthPercentage, );


        // Erstellung des neuen Wertes für style grid-template-columns
        let gridTemplateColumnsValue = ''
        const colsPercentage = `${remainingWidthPercentage / colsToChange}%`;
        colsArray.forEach((col, index) => {
          gridTemplateColumnsValue += col.endsWith("ch") || col.endsWith("%")  ? `${colsPercentage} ` : `${col} `;
        });
        xDebug('grid', "resizeInnerGrid: " + gridTemplateColumnsValue);
        gridContainer.style.gridTemplateColumns = gridTemplateColumnsValue;
    }

    // setzen des neuen Styles
  }


toggleColsDetail(toggleWidth = false) {
    // 0 will be handled as 1
    // - colOrderIdx-Cols visible
    //      - 1 size by values               << DEFAULT
    //      - 2 size by header/values
    //      - 3 auto-width                    << Default if 1 is to small for Box
    //      - fixed size by init or manual
    //      - percent size by init or manual
    // - all-Cols visible
    //      - -1 all-Cols sized by values
    //      - -2 all-Cols sized by header/values
    //      - -3 all-Cols auto-width
    this.colSizeType = 0 == this.colSizeType ? 1 : this.colSizeType;
    if (toggleWidth) {
       this.colSizeType = Math.sign(this.colSizeType) * (Math.abs(this.colSizeType) % 3 + 1) ;
    } else {
       this.colSizeType = - this.colSizeType ;
    }
    if (toggleWidth) {

        if (Math.abs(this.colSizeType) == 1) { // has been toggled -> so we are here only after switching
            let gridTemplateColumnsOld = this.eGrid.style.gridTemplateColumns;
            this.resizeCols();
            if (gridTemplateColumnsOld == this.eGrid.style.gridTemplateColumns) {
                xLog('grid', "toggleColsDetail - no change - skip 2 max-header-size-mode");
                this.colSizeType = 2 * Math.sign(this.colSizeType) ; // skipp max-header-size-mode if without effect
            }
        }

    } else {

        // add or remove cols
        if (this.colSizeType > 0) {
            // reset cols to this.colOrderIdx
            // cut additional cols from this.colOrderIdx
            //this.colOrderIdx = this.colOrderIdx.slice(0, this.colSizeType_ColCountBefore);
            this.colOrderIdx = this.colOrderIdx_bak;
            this.colOrderIdx_bak = null;
        } else {
            // append cols to this.colOrderIdx that are not in this.colOrderIdx
            // --> appending is BAD - as id and all creation/update details not near to data - alle bunched right at the end
            if (null == this.colOrderIdx_bak) {
                this.colOrderIdx_bak = this.colOrderIdx;
            }
            this.colOrderIdx = [];
            for (let c = 0; c < this.items[0].length; c++) {
                this.colOrderIdx.push(c);
            }
        }  // add or remove cols

    }  // toggleWidth or cols

    this.resizeCols();
}

resizeCols() {
    // toggleColsDetail ...
    this.colSizeType = 0 == this.colSizeType ? 1 : this.colSizeType;
    xLog('grid', "resizeCols", this.colSizeType + "-" + (this.colSizeType == -3 ? "all-cols" : this.colSizeType == -2 ? "all-cols-header" : this.colSizeType == -1 ? "all-cols-values" : this.colSizeType == 1 ? "cols-values" : this.colSizeType == 2 ? "cols-header-values" : this.colSizeType == 3 ? "cols-auto" : "cols-fixed"));

    // 1 = by max(column-value[].len), 2 = by max(column_name.len,column-value[].len), 3 = auto
    let useWidthType = Math.abs(this.colSizeType); //
    let colSize_ = this.eGrid.style.gridTemplateColumns ;
    let colSize = colSize_ || "";
    let colSizes = colSize.split(/ +/);
    //
    let colLimit = this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length ;
    if ( "" == colSize ){
        colSizes = [];
    } else if ( colLimit < colSizes.length ) {
        colSize = colSizes.slice(0, colLimit);
    }
    // keep if possible - re-fit if needed
    colSize = "";
    // manual set sizes in px - keep them
    // dybanic pixel in px+0.1 - adjust
    // dynamic sizes in % - adjust
    // calculated sizes in ch - adjust
    // added cols - add them
    let lenFixed = 0;
    let lenReq = 0;
    let lenReqShort = 0;
    let lenPx = [];
    // FIXME
    this.eGrid.style.width="100%";
    let curWidth = this.eGrid.clientWidth;
    this.eGrid.style.width="";
    //
    xLog('grid', "resizeCol2 " + this.gridNo + ": " + colSize + " (curWidth: " + curWidth + ") ... ");
    let canvas = null;
    let ctx = null;
    for (let c_ = 0; c_ < colLimit; c_++) {
        if (c_ < colSizes.length && (colSizes[c_].includes("px") && !colSizes[c_].includes(".01px"))) {
            lenFixed = lenFixed + parseInt(colSizes[c_]);
            lenPx[c_] = null;
        } else  {
            let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
            let len = ( useWidthType <= 1 ? Math.max( this.items[0][c].length, this.lenMax[c]) : useWidthType <= 2 ? -1 /*auto*/ : this.lenMax[c] );
            if (len > 200) {
                lenPx[c_] = 200 * 5; // limit max size - eg. xml-col of iPIM-job-tab // may be enlarged manually
            } else if (len < 0 ) {
                lenPx[c_] = -1;
            } else {
                // get accurate pixel size of char ???
                //
                // KLUDGE if  data is a data - is is "s" with length of 29 or 26   -- (29) 2024-05-22 11:29:50.819053+02 //(26) 2024-05-22 11:29:50.819053 // (23) 2024-06-20 11:45:00.06
                //  2023-10-25
                //    so it needs space per char as numbers ....
                //lenPx[c_] = len * ( len < 3 ? 12 : this.colDataType[c].startsWith("n") || 29==len || 26 == len || 23 == len || 10 == len || len < 5 ? 9 : 5); // px per char

                if (null == canvas || null == ctx) {
                    canvas = document.createElement('canvas');
                    ctx = canvas.getContext('2d');
                    ctx.font = window.getComputedStyle(this.eGrid).getPropertyValue('font');
                }
                lenPx[c_] = null;
                let lenPx_ = 0;
                for (let r_ = 1; r_ < this.items.length; r_++) {
                    let r = this.sortIdx && this.sortIdx[r_] ? this.sortIdx[r_][0] : r_;
                    if (this.items[r] && this.items[r][c]) {
                        lenPx_ = ctx.measureText(this.items[r][c]).width;
                        if (lenPx_ >0) {
                            if (lenPx_ < 8) {
                                lenPx_ = 8;
                            }
                            lenPx_ += 4 + 2; // padding + buffer + space-for-grouping-symbol
                        }
                        if (null == lenPx[c_] || lenPx[c_] < lenPx_) {
                            console.log("resizeCol2 " + this.gridNo + " [" + r + ", " + c_ + "] " + lenPx_ + " px (" + this.items[r][c] +")" );
                            lenPx[c_] = Math.ceil(lenPx_);
                        }
                    }
                }

            }
            lenReq = lenReq + lenPx[c_];
        }
    }
    //
    if ( "" == this.eGrid.style.gridTemplateColumns  // only on first call - init
            && lenReq > 0
            && 1 == this.colSizeType
            && this.prop_hidden && this.prop_hidden.pos && this.prop_hidden && this.prop_hidden.pos.width
            && lenReq < this.prop_hidden.pos.width
            ) {

        console.log("resizeCol2 " + this.gridNo + " - switch to auto-size - as " + lenReq + " < " + this.prop_hidden.pos.width);
        // initially try to fill grid as good as possible -> so if more space switch to auto-col-size
        this.colSizeType = 3; // auto-size
        for (let c_ = 0; c_ < colLimit ; c_++) {
            colSize = colSize + "auto ";
        }

    } else {


        //
        curWidth = this.eGridBox.clientWidth - 40; // padding
        //
        let lenFlex = curWidth - lenFixed;
        let lenMiss = ""==colSize ? 0 : lenReq - lenFlex; // on init colSize is empty -> take all needed space
        for (let c_ = 0; c_ < colLimit ; c_++) {
            if (null == lenPx[c_]) {
                colSize = colSize + colSizes[c_] + " ";
            } else  {
                let lenPxCur = lenPx[c_];
                if (lenPxCur >= 0) { // empty cols will be auto-sized
                    if (lenMiss > 0) {
                        lenPxCur = lenPxCur - lenPxCur* (lenMiss / lenReq); // distribute missing space to all cols
                    }
                    colSize = colSize + Math.floor(lenPxCur) + ".01px "; // indicate calculated size
                } else {
                    colSize = colSize + "auto ";
                }
            }
        }
        //
        //  https://stackoverflow.com/questions/21064101/understanding-offsetwidth-clientwidth-scrollwidth-and-height-respectively
        //

    }
    xLog('grid', "resizeCol2 " + this.gridNo + ": " + colSize + " (curWidth: " + curWidth + " lenReq: " + lenReq + ")");
    this.eGrid.style.gridTemplateColumns = colSize;
}



 /*

       L O A D

 */
  load(rowLimit = 0, updateDataOnly = 0) {


    let mermaidMode = this.colDataType.length == 1 && this.colDataType[0].includes('s') && this.items[0][0] == "doc" ; // first col is "doc" and only one col is string

    // KLUDGE - set name at getter/setter
    this.setName( this.name );

    if (0 == rowLimit || this.items.length < rowLimit) {
      rowLimit = this.items.length;
    }
    let colLimit = this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length ;

    if (this.colDataType.length < colLimit) {
        // may happen if only headline is supplied ...
        for (let c_ = 0; c_ < colLimit; c_++) {
            this.colDataType[c_] = this.colDataType[c_] ?? "s";
        }
    }

    if (0 == updateDataOnly) {
        this.eGrid.innerHTML = "";
        this.resizeCols();
    }
    // if (this.sortIdx) xDebug('grid', "use sortIdx " + this.sortIdx[ 1 ][ 1 ] + " "+ this.sortIdx[ 2 ][ 1 ] + " "+ this.sortIdx[ 3 ][ 1 ] + " ");

   // to be used by onclick-events to reference to current grid
   let grid = this;
   let val_undefined = false;


      var group_vals = [];
      /* FAKE */
      //this.colsGrouped = 2;

    for (let r_ = updateDataOnly; r_ < rowLimit; r_++) {

      let r = this.sortIdx &&  this.sortIdx[r_] ? this.sortIdx[r_][0] : r_;
      // xDebug('grid', 'resort row: ' + r_ + " -> "+ r);

      // TODO if r is filteredRow - than skipp it and go for next ...
      if (!this.filterRowIdx ||  (typeof this.filterRowIdx[r] === 'undefined') || this.filterRowIdx[r] ) {

          for (let c_ = 0; c_ < colLimit; c_++) {

            let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;

            // TODO move grouping to group function - using row-Index to insert grouping/aggregation rows ...
            // insert Grouping
            //  assumptions
            //      - grouping cols in front of all cells (from left)
            //      - grouped values are ordered
            //      - at least 1 column is not grouped !!
            //
            //  grouping-nested-idx / grouping-index      / grouping-placeholder
            //  grouping-nested-idx / grouping-nested-idx / non-grouped data of row ...
            if ( r > 0 && c_ < this.colsGrouped ) {
                for (; c_ < this.colsGrouped ; c_++){
                    c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                    let val = this.items[r] ? this.items[r][c] : "" ; // KLUDGE might happen on REFRESH of 2-grouped-cols and 3rd col is sorted ... article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*]  W articleno = '07daf1fb-186f-45b5-a0f7-cdf0704fab84'
                    if (group_vals.length <= c_ || val != group_vals[ c_ ]){
                        // TODO "smart-grouping only group if at least 2 row match
                        if (this.groupingSmart && (r_ == rowLimit -1 || val != this.items[ (this.sortIdx ? this.sortIdx[r_][0] : r_) + 1 ][c])) {
                            // c_--; // undo incr
                            break;
                        }
                        group_vals[ c_+1 ] = "-"; // invalidate ...srcd
                        group_vals[ c_ ] = val;
                        // 1. group changed -> insert while row
                        // 2nd - ff. -> insert grouping-nested-idx
                        // add dummy row ...
                        for (let c_2 = 0; c_2 < colLimit + 1; c_2++) {
                            var item = document.createElement('div');
                            item.id = "grid" + this.gridNo + "_" + ( r_ + "." + c + ( c + c_2 < colLimit ? "": ".1" ) ) + "_" + ( c + c_2 - (c + c_2 < colLimit ? 0: colLimit));
                            item.innerHTML = val;
                            item.classList.add( 0 == c_2 ? "grid-group-idx" : "grid-group-val");
                            item.classList.add("grid-group");
                            item.setAttribute('srcD', r + "," + c);
                            if ( 0 == c_2) {
                                item.setAttribute("onclick","gridToggleGroup(event, this);");
                                item.setAttribute("title","click to (un)fold");
                            }
                            this.eGrid.appendChild(item);
                        }
                    } else { // cell is grouped but matching current group-val
                        var item = document.createElement('div');
                        item.id = "grid" + this.gridNo + "_" + r_  + "." + c + "_" + c ; // r_ is not correct ...
                        item.innerHTML = val;
                        item.classList.add("grid-group");
                        item.classList.add("grid-group-val");
                        item.setAttribute('srcD', r + "," + c);
                        this.eGrid.appendChild(item);
                    } // changed group-val
                } // for group cols
                c = this.colOrderIdx ? this.colOrderIdx[c_] : c_ ;
            }

            /*
                usual
            */

            var item = document.createElement('div');
            //item.setAttribute("contenteditable", "true");
            let val = this.items[r] ? this.items[r][c] : undefined; // KLUDGE might happen on REFRESH of 2-grouped-cols and 3rd col is sorted ... article[]article_av[<]attribute[productNo,articleNo,identifier,article_av.*]  W articleno = '07daf1fb-186f-45b5-a0f7-cdf0704fab84'
            if (val === undefined) {
                msg_warn("grids[" + this.gridNo + "].items[" + r + "," + c + " ] undefined value - check data encoding!!");
                if (val_undefined){
                    val_undefined = true;
                    console.debug( this.items );
                }
                val = "";
            }
            let icon = "";
            let iconL = "";
            item.id = "grid" + this.gridNo + "_" + r_ + "_" + c_;
            if (0 == r) {
              item.classList.add("grid-head");
              if ( c_ < this.colsGrouped) {
                item.classList.add("grid-group-head");
              }
              //item.setAttribute('onmousedown', 'return gridDragColumn(event,this);');
              //mouseDownClickDbl( item, function(){return gridDragColumn(event,this);},clbk2,clbk3  );
              mouseDownClickDbl( item, clbkColDrag,clbk2ColFilter, clbk2ColFilter  );
              // item.setAttribute('dblclick', 'alert("asda");return false;'); // interfere with onmousdown ... https://stackoverflow.com/questions/62500629/javascript-how-to-distinguish-between-mousedown-click-and-doubleclick-events
              if (c > -1) {
                icon += '<i class="fa ' + this.sortModeIcons[ this.sortModeAvail.indexOf( this.sortMode ? (this.sortMode[  c ] ?? "" ) : "" ) ] + '" style="cursor: alias;" onclick="event.stopPropagation();controller(grids[' + this.gridNo + '], \'SORT:' + c + '\', this);return false;" >';
                if (this.sortOrder && this.sortOrder[c]>0) {
                  icon += '<i class="superscript">' + this.sortOrder[c] + '</i>';
                }
                icon += '</i>';
                icon += '<i class="fa fa-filter icon-inactiv" style="cursor: alias;" onclick="event.stopPropagation();controller(grids[' + this.gridNo + '], \'FILTER:' + c + '\', this);return false;" ></i>';
                //--- resize at right end -> might shrink column until resizer is not visible any more ...
                //if (c_ == colLimit-1) {
                  icon += '<div class="fa icon-inactiv" style="cursor: ew-resize; -webkit-text-stroke: 0px" onmousedown="event.stopPropagation();StartDragResizeGridCol(event);return false;" >|</div>';
                //}
                // if some columns are hidden - the resizer is not visible - so it is not possible to resize the column -> also add resizer at right for last one ...
                //    but resizing the last one means to resize the internal grid itself ... (as there may be a gap between grid and grid-box)
                if (c_ == colLimit-1) {
                  iconL += '<div class="fa icon-inactiv" style="cursor: ne-resize; -webkit-text-stroke: 0px" onmousedown="event.stopPropagation();StartDragResizeGridCol(event);return false;" >&gt;</div>';
                }
                //icon += '<i class="fa fa-filter icon-inactiv" style="cursor: ew-resize" onclick="controller_resize("' + this.eGrid.parentNode.id + '",'+c+',event,this);" ></i>';
                // iconR += '<i class="fa fa-arrow-left icon-inactiv" style="cursor: ew-resize" onmousedown="StartDragResizeGridCol(event);" ></i>';
                //iconR += '<i class="fa fa-arrow-left icon-inactiv" style="cursor: ew-resize; padding: 0px" onmousedown="StartDragResizeGridCol(event);" ></i>';

                if (c_ > 0) {
                  iconL += '<div class="fa icon-inactiv" style="cursor: ew-resize; -webkit-text-stroke: 0px" onmousedown="event.stopPropagation();StartDragResizeGridCol(event,-1);return false;" >|</div>';
                }
              }
            } else if (0 == c) {
              item.classList.add("grid-idx");
            } else {
              item.classList.add("grid-item");
              // allow edit on HTML-Link-Elements -- oneclick = link / doubleclick = edit=?
              if (this.colDataType[c].includes('u') || this.colDataType[c].includes('U')){
              } else if (!this.colDataType[c].includes('h') || !val.match("<a href=.*")) {
                  item.onclick = function(e) { enableEditing(e, e.target) };
              }
              if (r_ % 2 == 1) item.classList.add("grid-odd"); // ignore head row ...
            }
            if (c >0 && r_ % 3 == 0) item.classList.add("grid-3rd");
            //https://stackoverflow.com/questions/18749591/encode-html-entities-in-javascript
            if (0 == r) {
                val = "<div class='colName'>" + val + "</div>"
                if ("" != icon) {
                     icon = "<div class='headlineIconSpace'>" +  icon + "</div>";
                }
                if ("" != iconL) {
                     iconL = "<div class='headlineIconSpaceL'>" +  iconL + "</div>";
                }
            } else if (this.colDataType[c] ) {
                if (!this.colDataType[c].includes('M')){ // markdown / mermaid
                    if (this.colDataType[c].includes('H')){
                        val = String.toHtmlEntities(val);
                    }
                    if (this.colDataType[c].includes('L') ){ // should be excluded for mermaid
                        val = val.replaceAll('\n','<br>');
                        item.style.cssText  += "height: 1em; overflow: hidden;";  // use cssText  instead of style
                        //icon = '<i class="fa fa-filter arrow-turn-down-left" onclick="" ></i>';
                    }
                    if (this.colDataType[c].startsWith("s")) { // s + sL +
                        if (!this.colDataType[c].includes("h") && val && val.replaceAll){
                            val = val.replaceAll(' ','&nbsp;'); // see indent of Text // keep HTML as is
                        }
                        item.style.cssText  += "text-align: left;";
                        if (this.colDataType[c].includes("S")){
                        item.classList.add("grid-monospace");
                        }

                    }

                    if( 1 == this.items[0].length && this.items[0][0] == "doc" && r == 1 && this.items[1][0] && ( this.items[1][0].startsWith("```mermaid") ||  this.items[1][0].startsWith("graph ")  ) ) {
                       console.warn("render mermaid (doc detected ...)");
                       let mm_val = "";
                       for( var i = r; i < this.items.length; i++){ // collect all rows
                           mm_val = mm_val + this.items[i][c] + "\n";
                       }
                       // remove tailing "\"
                       mm_val = mm_val.replaceAll(/\\$/gm,"");
                       mm_val = mm_val.replaceAll("<br>", "\n");
                       console.log(mm_val);
                       //controller2(grids[this.gridNo], 'GRID:BY_NAME[mermaid]:' , ""); // spawn/reuse new grid for preview
                        let grid_tmp = grids_by_name[ 'mermaid' ];
                        if (!grid_tmp) {
                            grid_tmp = createGridContainer("", { "name": 'mermaid', "cdm": "none" });
                        }
                        grid_tmp.setData( "md\n" + mm_val.replace(/;/g, "\;").replace(/\n/g, "\\n")); // into 1 column
                        grid_tmp.colDataType[0]="sM"; // format as markdown
                        grid_tmp.load(0, 0);
                        grid_tmp.eGridBox.classList.remove("grid-outdated");
                    }

                }
            }
            let mermaid = this.colDataType.length == 1 && this.colDataType[c].includes('sM') && this.items[0][0] == "md" ;
            if( mermaid &&  r == 1) {

                console.warn("render mermaid (md detected ...) -> render myself ....");
                // KLUDGE surpress handling as long text .... as image will disappear  --> see this.colDataType[c].includes('L')
                item.innerHTML = '<pre class="mermaid" >';
                let elem = item.getElementsByTagName("pre")[0];
                let mm_val = "";
                for( var i = r; i < this.items.length; i++){ // collect all rows
                    mm_val = mm_val + this.items[i][c] + "\n";
                }
                mm_val = mm_val.replaceAll(/\\$/gm,""); // KLUDGE remove tailing "\"
                mm_val = mm_val.replaceAll('\n','<br>');
                mm_val = mm_val.replaceAll("<br>", "\n");
                mm_val = mm_val.replaceAll("\\n", "\n");
                console.log(mm_val);
                elem.innerHTML = mm_val; // just to show the content
                try{
                    // mermaid.init();
                    // mermaid.render('mermaid_svg', mm_val, (svg, bindFunctions) => {elem.innerHTML = svg;}) // mermaid.render is not a function -> need own wrapper
                    mermaid___render(item.id + '_svg', mm_val, elem);
                } catch (e) {
                    msg_error("mermaid error: " + e);
                }

            } else {

              if (1 == this.items[0].length && this.colDataType[c].includes('s') && this.items[0][0] == "doc") {
                item.onclick = function(e) { enableEditing(e, e.target) };
              }

                // adjust values ...
                if (r > 0) {
                    var colName = this.items[0][c];
                    var valHndl = this.valHndl[colName];
                    if (valHndl && "LINK" == valHndl) {
                        // val = "!!" + val + "!!";
                        // leave focus on new grid - prevent focus on current grid
                        //   see dragdropresize::onMouseClick
                        //val = "<a href=\"#\" onclick=\"controller(grids[" + this.gridNo + "], 'GRID:ADD:', this); event.preventDefault(); event.stopPropagation ();return false ;\">" + val + "</>";
                        val = "<a href=\"#\" onclick=\"return controller2(grids[" + this.gridNo + "], event.ctrlKey ? 'GRID:ADD:' : 'GRID:BY_NAME[detail]:' , this, true, event);\" >" + val + "</>";
                        //item.onclick=" ";
                    } else if (valHndl && valHndl.includes('HREF_')) {
                        item.onclick = null; // KLUDGE: remove onlcick event - to prevent editing
                        let hrefID = valHndl.replace('HREF_',''); // HREF_ + ID of column
                        let href = this.items[r][hrefID];
                        val = "<a href=\"" + href + "\" target='_blank' data-tooltip=''>" + val + "</a>";
                    } else if (this.colDataType[c].includes('U')) { // URL
                        val = val.replace(/(https?:\/\/[^\s]+)/g, '<a href="$1"  target="_blank">$1</a>');
                    } else if (this.colDataType[c].includes('u')) { // URL in Markdown format  [text](url)
                        let val_ = val.split(/[\[\]\(\)]+/);
                        if (val_.length > 2) {
                            if (val_[2].startsWith("http")) {
                                // global url
                                val = "<a href=\"" + val_[2] + "\" target='_blank' data-tooltip=''>" + val_[1] + "</a>";
                            } else {
                                // local url -> grid:add
                                val = "<a href=\"#\" onclick=\"return controller2(grids[" + this.gridNo + "], 'GRID:ADD:" + val_[2]  + "', this, true, event);\" >" + val_[1] + "</>";
                            }
                        } // u global / local
                    } // u
                }


                item.innerHTML = iconL + val + icon ; /* to run as float right -- reverse order */
            } // not for rendered content - mermaid
            // back-refer
            item.setAttribute('srcD', r + "," + c); // here use r/c of item - no matter of sort and filter

            // -------  append   or   replace (by id)

            if (0 == updateDataOnly) {
              this.eGrid.appendChild(item);
            } else {
              let elem = document.getElementById(item.id);
              if (!elem) {
                // only limited rows in view ... just stop
                rowLimit = r_; // just break outer loop
                break;
              } else {
                // xDebug('grid', "update " + item.id);
                item.classList.remove("hidden");
                elem.replaceWith(item);
              } // view-row-limit reached ...
            } // updateDataOnly
          } // c
      } else {// filerRow
          for (let c_ = 0; c_ < colLimit; c_++) {
            let c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
            var item_id = "grid" + this.gridNo + "_" + r_ + "_" + c_;
            var item = document.getElementById(item_id);
            item.classList.add("hidden");
          }
      } // filerRow
    } // r
    if (0 == updateDataOnly) {
       //   this.resizeInnerGrid();
    }
    xDebug('grid',(updateDataOnly > 0 ? "updated" : "loaded ") + (this.items.length > 1 ? this.items.length + " x " + this.items[0].length : "NONE"));
    // document.getElementById("grid_size").innerHTML = (this.items.length < 1 ? 0 : this.items[0].length) + " x " + this.items.length;

  }



  /*
  */
  load_add(params) {
    load_add();
  }

 /*

       G R O U P

 */


    group() {
            // insert virtual rows to
            //  group and aggregate data
        var rowLimit =0 ;
        if (0 == rowLimit || this.items.length < rowLimit) {
          rowLimit = this.items.length;
        }
        //let colLimit = this.colOrderIdx ? this.colOrderIdx.length : this.items[0].length ;
        //for (let c_ = 0; c_ < colLimit; c_++) {

        // empty row
        var emptyRow = [];
        var zeroRow = [];
        for (let index = 0; index < this.items.length; index++) {
            emptyRow[index] = "";
            zeroRow[index] = 0;
        }

        var group_vals=[];
        for (let r_ = 1 /*skipp head */; r_ < rowLimit; r_++) {
            let r = this.sortIdx ? this.sortIdx[r_][0] : r_;
            for (let c_ = 0; c_ < this.colsGrouped; c_++) {
                    c = this.colOrderIdx ? this.colOrderIdx[c_] : c_;
                    let val = this.items[r][c];
                    if ( (group_vals.length <= c || val != group_vals[ c ])){ // TODO "smart-grouping only group if at least 2 row match
                        group_vals[ c+1 ] = "-"; // invalidate ...
                        group_vals[ c ] = val;
                        // 1. group changed -> insert while row
                        // 2nd - ff. -> insert grouping-nested-idx
                        // add dummy row ...

                        for (let c_2 = 0; c_2 < colLimit + 1; c_2++) {
                            var item = document.createElement('div');
                            item.id = "grid" + this.gridNo + "_" + ( r_ + ( c + c_2 < colLimit ? "" : ".g" ) ) + "_" + ( c + c_2 - (c + c_2 < colLimit ? 0: colLimit));
                            item.innerHTML = val;
                            item.classList.add( 0 == c_2 ? "grid-group-idx" : "grid-group-val");
                            item.classList.add("grid-group");
                            this.eGrid.appendChild(item);
                        }
                    } else { // cell is grouped but matching current group-val
                        var item = document.createElement('div');
                        item.id = "grid" + this.gridNo + "_" + r_ + "_" + c ; // r_ is not correct ...
                        item.innerHTML = val;
                        item.classList.add("grid-group");
                        item.classList.add("grid-group-val");
                        this.eGrid.appendChild(item);
                    } // changed group-val
                } // for group cols
                c_ = this.colsGrouped ; // skipp
                c = this.colOrderIdx ? this.colOrderIdx[c_] : c_ ;

        }
    }

 /*

       S O R T

 */
  sort(col, sortMode) {
    if (!this.sortMode || !this.sortOrder) {
      // init
      this.sortMode = [];
      this.sortOrder = [];
      for (let index = 0; index < this.items.length; index++) {
        this.sortMode[index] = "";
        this.sortOrder[index] = 0;
      }
    }
    //
    this.sortMode[col] = sortMode;
    //   sortOrder[ col ] = position  --> wenn sortMode=0 dann löschen und neu idx // sonst aktuallisiern // sonst position anfügen 
    //   sortOrderIdx[ position ] = col --> !!! position 0 is to skipp
    let sortOrderIdx = [];
    for (let c = 0; c < this.sortOrder.length; c++) {
      sortOrderIdx[this.sortOrder[c]] = c;
    }
    //
    if (sortMode != "" && 0 == this.sortOrder[col]) {
      // add col
      this.sortOrder[col] = sortOrderIdx.length; // tricky at least Dummy-[0] makes inital len -> 1
      sortOrderIdx[this.sortOrder[col]] = col;
    } else if ("" == sortMode && this.sortOrder[col] > 0) {
      // remove col
      sortOrderIdx.splice(this.sortOrder[col], 1);
      for (let i = this.sortOrder[col]; i < sortOrderIdx.length; i++) { // updated shifted indexes
        this.sortOrder[sortOrderIdx[i]] = i;
      }
      this.sortOrder[col] = 0;
    }
    //
    if (sortOrderIdx.length > 1) {
      let idx = [];
      // should have idx-column to ease sorting -  just store key-values-joined and use custom-sorting by only that column
      for (let r = 1; r < this.items.length; r++) {  // exlude haedlines from sorting
        let keys = [r]; // current row
        for (let i = 1; i < sortOrderIdx.length; i++) { // skipp dummy at index 0
          // TODO FIXME -- create inverse key for reverse sorting !!!!
          keys.push('' + this.items[r][sortOrderIdx[i]]); // enforce string for sorting ... later numberformat
        }
        idx.push(keys);
      }
      // get sortMode indexed by keys
      let idxSortOrderModeId = [""]; // dummy for row-num at position 0
      for (let i = 1; i < sortOrderIdx.length; i++) { // skipp dummy at index 0
        idxSortOrderModeId.push(this.sortModeAvail.indexOf(this.sortMode[sortOrderIdx[i]]));
      }
      xDebug('grid', idxSortOrderModeId);
//      console.table(idx);
      xDebug('grid',"sorting ... (" + this.sortMode.toString() + ")");  // sorting ... (,asc,,,,,,)
      //
      idx.sort(function(a, b) {
        let cmp = 0;
        let i = 1;
        do {
          cmp = (a[i]).localeCompare(b[i]);
          if (0 == idxSortOrderModeId[i] % 2) {
            cmp = -cmp; // reverse order
          }
          i = i + 1; // skipp row at position 0
        } while (0 == cmp && i < a.length);
        return cmp;
      });
      idx.unshift([0, 0]); // add Head-Dummy
//      console.table(idx);
      xDebug('grid',this.sortMode.toString() + " " + (idx.length > 1 ? idx[1][1] : "") + " " + (idx.length > 2 ? idx[2][1] : "") + " " + (idx.length > 3 ? idx[3][1] : ""));
      /*
      let items=[ this.items[0] ]; // exclude headlines from sorting
      for (let r = 0; r < idx.length; r++) {  
          items.push( this.items[ idx[ r ][ 1 ] ] );
      }    
      this.items = items;
      */
      this.sortIdx = idx;

      //this.load(0, 1); // do not update head !!
      //this.load(0, 0);// do update head as Sort-Order may have changed !!
      this.load(0, 0);// but keep changed column-sizes

    }
  }


  reset() {
    let i = 1;
    for (let r = 0; r < this.items.length; r++) {
      for (let c = 0; c < this.items[0].length; c++) {
        this.items[r][c] = i++;
      }
    }
  }


  resize_items(size1, size2) {
    if (!this.items || !this.items[0] ) {
        this.items = [[""]]; // TODO sometimes it is not initiated ..
    }
    array_resize_2dim(this.items, size1, size2, 'X');
  }

  setData(r) {
    xDebug('grid',"setData");
    let delim_ = ";";
    let delim = delim_;
    let char0 = "";
    r=r.replaceAll('\\\\t','\t'); // KLUDGE ... for mouse-dragDrop Excel-Cells to Grid ..
    if (r.length > 0) {
        if (r.match(/[,;\t]/)) {
            delim_ =  r.match(/[,;\t]/)[0] ;
        }
        delim  = delim;
        let char0_ = r.charAt(0);
        if ( '"' == char0_ || "'" == char0_ ) {
            char0 = ('"' == char0_ ? '\\"' : char0_ );
            delim = new RegExp( char0 + delim_ + char0);  // separator ...
        } else {
            delim = new RegExp( "(?<!\\\\)" + delim_);
        }
    }
    //
    this.filterRowColAttr = null; // filter rows by attributes per Column
    this.filterRowIdx = null; // filter rows - rows that are filtered .. filter = Skipped ...
    //xDebug('grid', r);
    //let d1 = r.split("\n");
    //let d1 = r.split(";\n");  // handle \n in content ...
    //let d1 = r.split("(?<!\\)\n");  // handle
    // let d1 = r.split("(?<=["+delim+char0+"])\r?\n");  // handle
    // let delim_line =  "" == char0 ? "\r?(?<!\\)\n" : "(?<=["+delim_+ char0 +"])\r?\n" ; // unterminated parenthetical
    let delim_line =  "" == char0 ? "\r?(?<!\\\\)\n" : "(?<=["+delim_+ char0 +"])\r?\n" ;
    delim_line =  new RegExp( delim_line );
    xDebug('grid', delim_line);
    let d1 = r.split( delim_line );  // handle
    //if (d1[d1.length-1].length < 1 ) {
    xDebug('grid',"getDataFromUrl  delim(" + delim_line + " x " + delim + ") quote(" + char0 + ") -> found " + d1.length + " rows and " + (d1.length < 1 ? "" : " " + d1[0].split( delim ).length + " cols") );
    if (d1.length > 0 && "" == d1[d1.length - 1]) {
      d1.pop(); // closing \n  optional
    }
    //}
    if (d1.length > 0) {
        this.resize_items(d1.length, d1[0].split( delim ).length);
        //for (let i = 0; i < 5; i++) {
        this.lenMax=[];
        this.colDataType=[];
        for (let i = 0; i < d1.length; i++) {
            let d2 = d1[i];
            if (char0 != "") {
                d2 = d2.substring( 1, d2.length - 1  );
            }
            d2 = d2.replaceAll('\\\\','\\').replaceAll('\\n','\n'); // match main.java::data2Result() -- per line
            let d3 = d2.split( delim );

           // unescape
           if (d2.match(/\\/)){
               for (let ii = 0; ii < d3.length; ii++) {
                    d3[ ii ] = d3[ ii ].replaceAll('\\n','\n').replaceAll('\\t','\t').replaceAll(/\\(.)/g,'$1'); // match QryResponse.java::append() - per cell
               }
           }

            this.items[i] = d3;

            // determine max len per col
            let allowedFormatting = new RegExp("<(div |a href=).*?>","g");
            if ( i > 0 ) {
                for (let ii = 0; ii < d3.length; ii++) {


                    // len is not Width .... !!! check for max line in multiline Data ...
                    if (!d2.match(/\\/) || !d3[ii].match(/\n/)) {
                        let len = d3[ii].length;
                        if (d3[ii].match(/^<(div |a href=)/)) {
                            len = d3[ii].replaceAll( allowedFormatting ,"").length; // replaceAll must be called with a global RegExp --> "g"-Flag
                            // allow formatting, ignore in length ..
                        }
                        if (!this.lenMax[ii] || this.lenMax[ii] < len){
                            this.lenMax[ii] = len;
//xDebug('grid', "lenMax["+i + ","+ ii+"] -> " + this.lenMax[ii] + "   '" + d3[ii]+"'");
                        }
                    } else {
                        let d3_line_parts = d3[ii].split('\n');
                        for (let iii = 0; iii < d3_line_parts.length; iii++) {
                            let len = d3_line_parts[iii].length;
                            if (d3_line_parts[iii].match(/^<(div |a href=)/)) {
                                len = d3_line_parts[iii].replaceAll( allowedFormatting,"").length;
                                // allow formatting, ignore in length ..
                            }
                            if (!this.lenMax[ii] || this.lenMax[ii] < len){
                                this.lenMax[ii] = len;
//xDebug('grid', "lenMax["+i + ","+ ii+"] -> " + this.lenMax[ii] + "   ...'" + d3_line_parts[iii]+"'...");
                            }
                        }
                    }

                    if (!this.colDataType[ii] || "s" != this.colDataType[ii][0] ) { // (s)tring stays ...
                        if ( d3[ii].match(/^[0-9,.]+$/)  ) {
                            let type = 'n' + d3[ii].replace(/.*[,.]|.*/,"").length ; // add decimal points ...
                            if ( !this.colDataType[ii] || this.colDataType[ii] < type || this.colDataType[ii].length < type.length){ // win if decimal points larger
                                this.colDataType[ii]=type;
                            }
                        } else if ( d3[ii].match(/^[0-9,.: T/-]+$/)  ) {
                            this.colDataType[ii]='t';
                        } else if (d3[ii]) {
                            this.colDataType[ii]='s';
                            if (d3[ii].match(/&nbsp;/)) { // as the first line might be just  "&nbsp;" -- see ~/status.sh -a
                                this.colDataType[ii]=this.colDataType[ii] + 'hS'; // recognize space
                            } else if (d3[ii].match(/</) || d3[ii].match(/&nbsp;/)) {
                                //if (d3[ii].match(/^\s*<(span |div |a href=)/)) {
                                if (d3[ii].match(/^\s*<(span |div |a href=)/)) {
                                    this.colDataType[ii]=this.colDataType[ii] + 'h'; // allow formatting as part of data - like color green and read for url.check
                                } else {
                                    this.colDataType[ii]=this.colDataType[ii] + 'H';
                                }
                            } else if (d3[ii].match(/^https?:/)) {
                                this.colDataType[ii]=this.colDataType[ii] + 'U'; // recognize URL
                            }
                            if (d3[ii].includes('\n')) {
                                this.colDataType[ii]=this.colDataType[ii] + 'L';
                            }
                        } else if (!this.colDataType[ii]) {
                            this.colDataType[ii]='';
                        }
// xDebug('grid', "colDataType["+i + ","+ ii+"] -> " + this.colDataType[ii] + "   '" + (d3[ii].length < 30 ? d3[ii] : d3[ii].substring(1,30) + "...") +"'");
                    } else if (!this.colDataType[ii].includes('u') && ( d3[ii].match(/^\[.*\]\(https?:.*\)/) ||  d3[ii].match(/^\[.*\]\(.*\)$/))) { // allow query-links  (core.log)[~/core.log]
                       this.colDataType[ii]=this.colDataType[ii] + 'u'; // recognize URL in Markdown  (name)[link]
                   }


                } // for cols
            } // skipp head
            //


        } // per row

        xDebug('grid',"this.colDataType");
//        console.table(this.colDataType);

        xDebug('grid',"this.lenMax");
//        console.table(this.lenMax);

    } else {
        this.resize_items(0, 0);
    }
//    console.table(this.items);


      let pos = 0 ;
      this.colOrderIdx = [];
      if (this.items.length > 0) {
          for (let c = 0; c < this.items[0].length; c++) {
            if (this.items[0][c].match(/^id$|creationdate|lastmodified|updateuser|createuser/) ) {
                xDebug('grid',"this.colOrderIdx hide " + this.items[0][c] + " @ column " + c + " (by name)"  );
            } else if (0 == this.lenMax[c])  {
                xDebug('grid',"this.colOrderIdx " + this.items[0][c] + " @ column " + c + " (emtpy)"  );
            } else {
                this.colOrderIdx[pos] = c;
                pos = pos + 1;
            }
          }
      } else {
          xWarn('grid',"grid::load -  empty data");
      }
      xDebug('grid',"this.colOrderIdx");
//      console.table(this.colOrderIdx);
      if (this.eGrid) this.eGrid.style.gridTemplateColumns =""; /* force re-init*/

  }

  setDataJson(data){
    data = data.replace("\\n","\n"); // unescape
    this.setData( data );
    
  }



  setData2(rowIdx, r /*data*/) {
    xDebug('grid',"setData2");
    let delim_ = ";";
    let delim = delim_;
    let char0 = "";
    if (r.length > 0) {
        if (r.match(/[,;\t]/)) {
            delim_ =  r.match(/[,;\t]/)[0] ;
        }
        delim  = delim;
        let char0_ = r.charAt(0);
        if ( '"' == char0_ || "'" == char0_ ) {
            char0 = ('"' == char0_ ? '\\"' : char0_ );
            delim = new RegExp( char0 + delim_ + char0);  // separator ...
        } else {
            delim = new RegExp( "(?<!\\\\)" + delim_);
        }
    }
    let delim_line =  "" == char0 ? "\r?(?<!\\\\)\n" : "(?<=["+delim_+ char0 +"])\r?\n" ;
    delim_line =  new RegExp( delim_line );
    xDebug('grid', delim_line);
    let d1 = r.split( delim_line );  // handle
    xDebug('grid',"setData2  delim(" + delim_line + " x " + delim + ") quote(" + char0 + ") -> found " + d1.length + " rows and " + (d1.length < 1 ? "" : " " + d1[0].split( delim ).length + " cols") );
    if (d1.length > 0 && "" == d1[d1.length - 1]) {
      d1.pop(); // closing \n  optional
    }
    //}
    if (d1.length > 0) {
        for (let i = 0; i < d1.length; i++) {
            let d2 = d1[i];
            if (char0 != "") {
                d2 = d2.substring( 1, d2.length - 1  );
            }
            let d3 = d2.split( delim );
           // unescape
           if (d2.match(/\\/)){
               for (let ii = 0; ii < d3.length; ii++) {
                    d3[ ii ] = d3[ ii ].replaceAll('\\\\','\\').replaceAll('\\n','\n').replaceAll('\\t','\t'); // match  main.java::data2Result()
               }
           }
            //
            this.items[rowIdx + i] = d3;
        } // per row
    }
      xDebug('grid',"this.setData2");
  }




    /*
    */



}

/*

Event handler - TODO find better place ...

*/


/*
    <div id="grid2_3.1_1" class="grid-group-idx grid-group" onclick="gridToggleGroup(event, this);">b3</div>

    <div id="grid2_3.1_2" class="grid-group-val grid-group" onclick="gridToggleGroup(event, this);">b3</div>   << skipp own placeholders
    ...
    nested groups ..
    nested items ...
    ...
    until grouping same ore higher level is found ...

*/
function gridToggleGroup(event, elem){
    //
    var cur = elem;
    display_new = elem.classList.contains("gridFolded") ? "block" : "none";
    toggle(elem, "gridFolded");
    elem_col = parseInt(elem.id.replace(/.*_/, "")); // grid2_3.1_>1< -> 1 == col == groupingLevel
    // go to next row - skipp grid2_3.1_*
    cur = cur.nextSibling;
    while ( null != cur && ( !cur.id.endsWith('_0') ) ) {
        cur = cur.nextSibling;
    }
    // toggle up to next group - at same or higher level...  or row without any group
    while ( null != cur && !cur.classList.contains('grid-idx')  &&   ( !cur.classList.contains('grid-group-idx') || parseInt(cur.id.replace(/.*_/, "")) > elem_col) )  {
        cur.style.display = display_new;
        if (cur.classList.contains('grid-group-idx')) { // nested group ...
            cur.classList.remove('gridFolded'); // ensure handling (on parent-unfold) - or just reset ....
        }
        cur = cur.nextSibling;
    }
    // backtrack - show current row in complete
    while ( null != cur &&  !cur.id.endsWith('_0')  ) {
        cur.style.display = 'block';
        cur = cur.previousSibling ;
    }
    return;
}

gridDC_elem = null;
gridDC_pos_old = 0;
gridDC_pos = 0;
gridDC_grid = null;
gridDC_cursor_x = null;
gridDC_cursor_y = null;

function gridDragColumn(event, elem) {
    event = event || window.event;
    if ( event && event.target && (event.target.classList.contains("grid-head") || event.target.classList.contains("colName"))  ) {// keep resizing, filtering ... ...
        event.preventDefault();
        while (elem && !elem.id) { // have have clicked on text
            elem=elem.parentNode;
        }
        xLog('grid', "grid::gridDragColumn() " + elem.id)
        //
        gridDC_elem = elem;
        document.onmouseup = gridDragColumnEnd;
        document.onmousemove = gridDragColumnDrag;
        // remember current cursor --> fails ... on first move ... ;-(
        gridDC_cursor_y = event.clientY;
        gridDC_cursor_x = event.clientX;
        //
        toggle(elem, '+grid-resizeOrMove');
        // elem.style.top  // elem.getBoundingClientRect().top  //
        divPositionSet( 'resizeFramePure', elem.getBoundingClientRect().top, elem.getBoundingClientRect().left, parseInt(elem.clientWidth) < 50 ? 50 : elem.clientWidth /* ensure HINT text fits*/ , elem.clientHeight );
        //
        gridDC_pos_old = parseInt(gridDC_elem.id.replace(/.*_/,''));
        gridDC_pos = gridDC_pos_old;
        gridDC_grid = elemFromElemOrId( gridDC_elem.id.replace(/_.*/,'') ).grid ;
    }
    return true;
}

function gridDragColumnDrag(event) {
    event = event || window.event;
    event.preventDefault();
    // calculate the new cursor position:
    delta_y = event.clientY - gridDC_cursor_y;
    delta_x = event.clientX - gridDC_cursor_x;
    gridDC_cursor_y = event.clientY;
    gridDC_cursor_x = event.clientX;
    // logical limit - col-header only horizontal or little-bit-up for grouping...
    grid_header_top = parseInt(gridDC_elem.getBoundingClientRect().top);
    f = elemFromElemOrId('resizeFramePure');
    let y = parseInt(f.style.top) + parseInt(f.style.height)/2;
    let x = parseInt(f.style.left) + parseInt(f.style.width)/2;
    if ( (y < grid_header_top - 50 && delta_y < 0 )||  (grid_header_top + 20 < y && delta_y > 0)) {
        delta_y = 0;
    }
    if ( null != gridDC_elem.previousSibling && event.x < parseInt(gridDC_elem.getBoundingClientRect().left)) {
        swap( gridDC_elem, gridDC_elem.previousSibling);
        gridDC_pos--;
    } else if ( null != gridDC_elem.nextSibling && parseInt(gridDC_elem.getBoundingClientRect().left) + parseInt(gridDC_elem.getBoundingClientRect().width) < event.x ) {
        swap( gridDC_elem, gridDC_elem.nextSibling);
        gridDC_pos++;
    }
    // set the element's new position:
    f.innerHTML =  x < gridDC_grid.eGrid.getBoundingClientRect().left || y < grid_header_top - 40 ? 'hide'
                        : y < grid_header_top - 20  &&  gridDC_grid.colsGrouped <= gridDC_pos_old && gridDC_pos <= gridDC_grid.colsGrouped ?  'group'
                        : grid_header_top + 20 < y && gridDC_pos_old < gridDC_grid.colsGrouped ? 'ungroup'
                        : 'move';
    // f.style.background = "" == f.innerHTML  ? 'none' : 'white' ; /* transparency to move */
    // f.style.background = "" == f.innerHTML  ? 'none' : 'white' ; /* transparency if empty */
    divPositionSet( 'resizeFramePure', delta_y, delta_x, -1 , -1 );
}

function gridDragColumnEnd(event, elem) {
    event.preventDefault();
    document.onmouseup = null;
    document.onmousemove = null;
    rszFrm = elemFromElemOrId('resizeFramePure');
    //
    recordAction("grid::gridDragColumnEnd " + recElem(rszFrm.innerHTML) + ", elem: "+ recElem(elem) + ", " + gridDC_pos_old + ", " + gridDC_pos )
    //
    if ('group' == rszFrm.innerHTML  ) {
        gridDC_grid.colsGrouped++;
    } else if ('ungroup' == rszFrm.innerHTML && gridDC_grid.colsGrouped>0) {
        gridDC_grid.colsGrouped--;
    }
    if (!gridDC_grid.colOrderIdx) {
        // colOrderIdx is not already init ...
        for (let c = 0; c < gridDC_grid.items[0].length; c++) {
            gridDC_grid.colOrderIdx[ c ] = c ;
        }
    }
    if ('hide' == rszFrm.innerHTML  ) {
        gridDC_grid.colOrderIdx.splice( gridDC_pos_old ,1 );
        gridTemplateColumns = gridDC_grid.eGrid.style.gridTemplateColumns.split(/ +/); // try keep column-sizing
        gridTemplateColumns.splice( gridDC_pos_old ,1 );
        gridDC_grid.eGrid.style.gridTemplateColumns = gridTemplateColumns.join(' ');

        gridDC_grid.resizeInnerGrid();
    } else {
        gridDC_grid.colOrderIdx = array_move( gridDC_grid.colOrderIdx, gridDC_pos_old, gridDC_pos)  ;
    }
    //
    toggle(gridDC_elem, '-grid-resizeOrMove');
    gridDC_elem = null;
    divPositionSet( rszFrm,-1,-1,0,0);
    //

  if (gridDC_pos_old != gridDC_pos || "" != rszFrm.innerHTML ) {
        xLog('grid', "grid::gridDragColumnEnd() --> insert " + gridDC_pos_old + " before " + gridDC_pos);
        // force renaming columns properly !!
        gridDC_grid.load(0,0);

        const gridTemplateColumnsArray = gridDC_grid.eGrid.style.gridTemplateColumns.split(" ");
        const newGridTemplateColumnsArray = gridTemplateColumnsArray.map((value, index) => {
          if (index === gridDC_pos_old) { return gridTemplateColumnsArray[gridDC_pos];}
          else if (index === gridDC_pos) { return gridTemplateColumnsArray[gridDC_pos_old]; }
          return value;
        });
        gridDC_grid.eGrid.style.gridTemplateColumns = newGridTemplateColumnsArray.join(" ");
    }
    gridDC_pos_old = gridDC_pos;
    rszFrm.innerHTML='';
}


function divPositionSet(f, top, left, width, height) {
    m = "grid::divPositionSet " + f + ", "+ top + ", "+ left + ", "+ width  + ", "+ height ;
    f = elemFromElemOrId(f);
    // allow using element to transfer position from ...
    if ( top && top.nodeType ) top = top.style.top;
    if ( left && left.nodeType ) left = left.style.left;
    if ( width && width.nodeType ) width = width.style.width;
    if ( height && height.nodeType ) height = height.style.height;
    //
    if ( 0 == width || 0 == height) {
        f.style.display='none';
    } else if (width == -1 || height == -1) { // hint for DELTA
        if (null != top) f.style.top = ( parseInt(f.style.top.replace("px","")) + top) + "px";
        if (null != left) f.style.left = (parseInt(f.style.left.replace("px","")) + left) + "px";
    } else if (width < -1 || height < -1) { // hint for ABSOLUTE
        if (null != top) f.style.top = isNaN(top) && top.endsWith("px") ? top : top + "px";
        if (null != left) f.style.left = isNaN(left) && left.endsWith("px") ? left : left + "px";
    } else {
        f.style.position ="fixed";
        if (null != top) f.style.top =  isNaN(top) && top.endsWith("px") ? top : top + "px";
        if (null != left) f.style.left = isNaN(left) && left.endsWith("px") ? left : left + "px";
        if (null != width) f.style.width = isNaN(width) && width.endsWith("px") ? width : width + "px";
        if (null != height) f.style.height = isNaN(height) && height.endsWith("px") ? height : height + "px";
        f.style.display='block';
    }
    xDebug('grid', m + " -> " + f.style.top  + ", "+ f.style.left  + ", "+ f.style.width  + ", "+ f.style.height)
}


function swap(nodeA, nodeB) {
    const parentA = nodeA. parentNode;
    const siblingA = nodeA. nextSibling === nodeB ? nodeA : nodeA. nextSibling;
    nodeB. parentNode. insertBefore(nodeA, nodeB);
    parentA. insertBefore(nodeB, siblingA);
}

// https://stackoverflow.com/questions/5306680/move-an-array-element-from-one-array-position-to-another
function array_move(arr, old_index, new_index) {
    if (new_index >= arr.length) {
        var k = new_index - arr.length + 1;
        while (k--) {
            arr.push(undefined);
        }
    }
    arr.splice(new_index, 0, arr.splice(old_index, 1)[0]);
    return arr; // for testing
};

// obsolete ???? by dragresizediv.js ???
function gridFocus(event = null) {
        if ( !event) {
            // KLUDGE called from code - outside of drag-drop - grids[0].eGridBox.onclick();
            event = { target: this };
            elem = this;
        } else {
            elem = event.target;
        }
        grid = elem && elem.grid ? elem.grid : null;
        if (null != grid) {
            if ( elem.parentNode &&  !(elem === elem.parentNode.lastChild)) {
                elem.parentNode.appendChild(elem); // move to be last -> on top
                //
                if (event && event.target && event.target.classList.contains('CodeMirror-line')) {
                    // KLUDGE focusAfterDrag
                    event.target.parentNode.parentNode.parentNode.focus();
                }
                grid.codeMirror.focus();
                //elem.grid.codeMirror.setFocus();
            } else if (!elem.contains(document.activeElement)) {
                // KLUDGE come from outside of grid
                // force blur even if next grid has no codeMirror
                document.activeElement.blur();
                grid.eGridBox.focus();
                grid.codeMirror.focus();
            }
        }
    }