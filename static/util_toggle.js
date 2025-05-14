
  // https://api.jquery.com/toggle/


    //  toggle( this );
    // -- with string
    //  toggle('div_x');
    //  toggle('log_div','fa-window-close fa-window-restore');
    //  toggle('log_div','#log_div2')
    //  toggle('log_div','fa-window-close fa-window-restore   #log_div2 fa-window-close fa-window-restore');
    // -- as 1 param
    //  toggle('log_div fa-window-close fa-window-restore   #log_div2 fa-window-close fa-window-restore');
    //  toggle('log_div #log_div2')
    //      ===> toggle('log_div display #log_div2 display')
    // -- delay ....
    //  toggle('log_div 5000 #log_div_open');
    // -- enforce values
    //  toggle('log_div +display -fa-window-close +fa-window-restore  ');
    //  -- REMOVE_SLOW
    //  -- negation of following operations ... by "!"  (only supported for +/- prefixed operations)
    //  toggle('log_div ! +display -fa-window-close +fa-window-restore  ');
    //  -- inital Val
    //  toggle('file_dialog', 'display', x.shouldDisplayDialog);
    // ????? TODO:
    //  -- FORGET ....
    //  disable all old timers for element - e.g. tooltips will be shown after 1 sec but not if mouse already moved away
    //  -- Position by px %
    //
  function toggle( elemId, itemOrList, initVal = null ) {
    invert = !( initVal ?? true );
    //xLog('utilToggle', "toggle " + (elemId.id ?? elemId) + ' "' + itemOrList + '"' + " ..");
    let elem = elemFromElemOrId( elemId );
    //xLog('utilToggle', "toggle " + (undefined == elem ? "undefined" : elem.id ?? elem.classList.join(" ") ) + ' "' + itemOrList + '"' + " " + initVal + " ...");
    if (undefined == itemOrList && undefined != elem && null == initVal ) {
        // toggle display none
        elem.classList.toggle("hidden");
    } else {
        if (undefined == itemOrList ) {
            itemOrList = elemId; // assume multi-element-def in first param ...
        } else if ("" == itemOrList) {
            itemOrList = "display";
        }
        //itemOrList = itemOrList.replace("REMOVE_SLOW", "-display 500 REMOVE");
        itemOrList = itemOrList.replace("REMOVE_SLOW", "-opacity 2000 REMOVE");
        let itemsList = itemOrList.split(/ +/);
        // toggle classes or display ...
        for (var i = 0; i < itemsList.length; i++) {
            let item = itemsList[i];
            if ( (0 == i && undefined == elem) || item.startsWith("#") ) {
                // switch to other element .... allow multi-element-def
                elem = elemFromElemOrId( item );
                if (null == elem) {
                    // console.warn('toggle ' + item + ' SKIPP "' + itemsList.join(" ") + '" (miss elem)');
                } else if ( i + 1 >= itemsList.length  || itemsList[i+1].startsWith("#") || !isNaN(itemsList[i+1]) ) {
                    // per just calling elements = changing display
                    // elem.style.display = 'none' == elem.style.display ? 'block' : 'none' ;
                    elem.classList.toggle("hidden");
                }
            } else if ( "!" == item  ) {
                invert = !invert;
            } else if ( "" == item  ) {
            } else if ( !isNaN(item)  ) {
                // param =  i + 1 < itemsList.length - 1 ?  itemsList.slice( i + 1 ).join(" ") : itemsList[ i + 1 ];
                let param =  itemsList.slice( i + 1 ).join(" ") ;
                if (invert) {
                    param = "! " + param;
                }
                if (elem){
                    if (!elem.id) {
                        elem.id = "tg" + Math.random().toString(16).slice(2);
                    }
                    // xDebug('utilToggle', "toggle " + (elem.id ?? elem.classList.join(" ") ) + ' "' + param + '"' + " (DELAY by " + item + ")");
                    setTimeout(function() {
                        //toggle( elem, param ); // seam not to work properly with element
                        toggle( elem.id, param, initVal); // at least is now is traceable ... --> ensure used Vars are not re-used!!!
                        // toggle( ""+elem.id, ""+param); // at least is now is traceable ...+ FORCE copy of Value ...
                    }, item);
                }
                i = itemsList.length; // skipp rest
            } else if ("" != item) {
                op = item.startsWith('-') ? -1 : item.startsWith('+') ? 1 : null == initVal ? 0 : 1 ;
                if ( item.startsWith('-') || item.startsWith('+')) {
                    item = item.substring(1); // remove prefix
                }
                if (invert) {
                    op = -op;
                }
                // xDebug('utilToggle', "toggle " + (elem.id ?? elem.classList.join(" ") ) + ' "' + item + '"' + ( op < 0 ? " (remove forced)" : 0 < op ? " (add forced)" : ""));
                if ("REMOVE" == item) { // just handle as virtual class ...
                    if (elem.parentNode) {
                        elem.parentNode.removeChild(elem);
                    }
                    elem = null;
                } else if ("display" == item ) { // just handle as virtual class ...
                    elem.style.display = ( 0 == op && 'none' == elem.style.display) || 0 < op ? 'block' : 'none' ;
                } else if ("focus" == item ) { // just handle as virtual class ...
                    if (op > 0) elem.focus();
                    else elem.blur();
                } else if ("opacity" == item ) { // just handle as virtual class ...
                    elem.style.opacity = op <= 0 ? 0 : 1  ;
                } else if ("visibility" == item ) { // just handle as virtual class ...
                    elem.style.visibility = op <= 0 ? 'hidden' : 'visible'  ;
                } else if ( (0 == op && elem.classList.contains(item)) || op < 0) {
                    elem.classList.remove(item);
                } else {
                    elem.classList.add(item);
                }
            }
        }
    }
    return elem
  }
