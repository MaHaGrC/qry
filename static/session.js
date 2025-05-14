
function grid2qry(grid) {
    let qry = null;
    if (grid && grid.gridNo) {
        let context_version = getBoxElem4Grid( grid, "context_version");
        if (!context_version) {
            xError('session', "grid " + grid.gridNo + "missing context_version")
        }
        let version = context_version ? context_version.innerHTML : "";
        qry = {};
        // still outdated query ...   grid.codeMirror.refresh(); // ensure to get current value ... ...
        qry.qry = ("" == version ? "" : "CACHE:" + version + " " ) + grid.codeMirror.getValue();
        qry.prop = grid.get_property(true);
        qry.prop.pos = getPositionAndSize( grid.eGridBox );
    }
    return qry;
}


function session2qry_( name = "", ignoreGrid = null){
    let sessGrids = [];
    let sess = {};
    for( i in grids) {
        if (ignoreGrid && i == ignoreGrid || !grids[i] || !grids[i].codeMirror) {
            // allow recordAction/snapshot to open Input-grids silently ...
            console.log("session2qry: " + i + " skipped (ignore)");
        } else {
            // console.log("session2qry: " + i);
            let qry = grid2qry( grids [i]);
            if (qry) {
                sessGrids.push(qry);
            }
        }
    }
    sess.session = name;
    sess.grids = sessGrids;
    xLog('session', "session2qry " + JSON.stringify(sess));
    return sess;
}


async function setSynonym(syn , val) {
  xDebug('session', "session::setSynonym");
  url = new URL("http://localhost:8080/query");
  url.searchParams.append("qry", syn + "===" + val+ " UPDATE");
  let text = null;
  try{
    let response = await fetch(url);
    xDebug('session', response);
    text = await response.text(); // read response body as text
    msg_important("session " + ( name ? name + " " : "") + "saved");
    //
  } catch (e) {
   log('session::data2Grid: !!! Connection fail !!' + url.href);
   xError('session', e);
  }
}


function session2qry( name = ""){
    if (!name) {
        var d = new Date();
        cur_date = d.getFullYear() + "" +  ((d.getMonth()+1) < 10 ? "0" : "") + (d.getMonth()+1) + "" + (d.getDate() < 10 ? "0" : "") + d.getDate();
        cur_date = cur_date + "_" + ( d.getHours() < 10 ? "0" : "") + d.getHours() + "" +  ( d.getMinutes() < 10 ? "0" : "") + d.getMinutes() + "" + ( d.getSeconds() < 10 ? "0" : "") + d.getSeconds() ;
        name="SES_" + cur_date;
    }
    let sess = session2qry_();
    // "post" via Define-Synonyme-By-QrySyntax ...    syn===val   // access value just by using syn as query or part of query
    setSynonym( name, JSON.stringify(sess) );
}
