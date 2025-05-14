/*

    call /notification periodically ...


*/
var msg_wait = 0;
var msg_skipp = 0;
var msg_skipp_counter = 0;
var msg_notification_mock = null;


let msg_outdated ;
function msg_set( msg ) {
    elem = document.getElementById("msg");
    elem.innerHTML = msg;
    elem.classList.remove("msgOutdated");
    elem.classList.add("msgNew");
    setTimeout(function() {
      elem.classList.add("saved_");
      setTimeout(function() {
        elem.classList.remove("msgNew");
        elem.classList.remove("saved");
      }, 500);
    }, 500);
    //
    if (msg_outdated) clearTimeout(msg_outdated);
    msg_outdated = setTimeout(function() {
                elem.classList.add("msgOutdated");
              }, 5000);
    msg_init(250); // re-init
}

function msg_init( delay = 3000 ) {
    setTimeout(function() {
        xDebug('connectNotification', "connect_n::msg_init");
        msgConnection( msg_set );
    }, delay);
}




async function msgConnection( callback) {
  xDebug('connectNotification', "connect_n::msgConnection");
  url = new URL("http://localhost:8080/info");
  let text = null;
  try{
    let response = await fetch(url);
    xDebug('connectNotification', response);
    text = await response.text(); // read response body as text
  } catch (e) {
   log('connect_n::msgConnection: !!! Connection fail !!' + url.href);
   xError('connectNotification', e);
  }
  if (callback){
    callback( text );
  }
}

/*
    msg_notification --> notifications from backend ...

*/



let msg_notification ;

function htmlFromJson( data ) {
    data_html=data;

    return data_html;
}

var msg_last = "";
var msg_last_e = "";
var msg_last_count = 0;
var msg_login = [];

function msg_notification_level( level, msg ) {
    msg_div = document.getElementById("msg_notification");
    let elem = null;
    if (msg) {
        if (" info debug warning warn error stmt important ".includes(" "+level+" ") ) {
            elem = document.createElement("div");
            elem.classList.add("msg_" + level);
            e_prefix = "error" == level ? "ERR " : "warning" == level || "warn" == level  ? "WRN " :  "stmt" == level ? "stmt" : "important" == level ? "IMP " : "";
            elem.innerHTML = e_prefix + (msg && msg.replace ? msg.replace(" ","&nbsp;") : msg); // Uncaught (in promise) TypeError: msg.replace is not a function
            elem.onclick = function() {toggle(this,'REMOVE')};
            if (e_prefix.startsWith("E") || e_prefix.startsWith("W")) {
                msg_div.insertBefore(elem, msg_div.firstChild);
            } else {
                msg_div.appendChild(elem);
            }
            toggle(elem,  e_prefix.startsWith("E") || e_prefix.startsWith("W") || e_prefix.startsWith("I") ? "+msgNew 1000 -msgNew 20000 REMOVE_SLOW" : "+msgNew 1000 -msgNew 5000 REMOVE_SLOW");
            // KLUDGE - bring Login-dialog to front ... {"warn":{"msg":"'FD_INTEG' unknown (HINT: login once 0)"}}
            // or just at startup ...
            if (msg && msg.includes && msg.includes("HINT: login once")) {
                let login_next = msg.replace(/^'(.*?)' .*$|^.*$/,"$1");
                if (!login_next || !msg_login.includes(login_next)) {
                    if (login_next) {
                        msg_login.push(login_next);
                    }
                    controller(null, "LOGIN::" + login_next); // open login dialog
                } // prevent endless loop ...
            }
        }
    }
    if (elem) {
        if (msg_last_e && msg_last && msg_last == elem.innerHTML) {
            msg_last_e.remove(); // as the timer is already started for old one ...
            msg_last_count = msg_last_count + 1;
            elem.innerHTML =  "(" + msg_last_count +"x) " + elem.innerHTML;
        } else {
            msg_last_count = 0;
            msg_last = elem.innerHTML;
        }
        msg_last_e = elem;
    }
}






function msg_notification_clbk( msg ) {
    if (msg && msg.startsWith("{")) {
        // { info: { msg: <msg> } }
        let msg_obj ="";
        try {
            msg_obj = JSON.parse(msg);
        } catch(err) {
            msg_obj = { error:  {msg: "illegal msg: " + msg+ " --> " + err.message}};
        }
        for (let x in msg_obj) {
            msg_notification_level( x, msg_obj[x].msg );
        }
    } else if ("" != msg) {
        msg_notification_level( "info", msg ?? 'timeout');
    }
}




function msg_important( msg ) {
    msg_notification_level("important",msg);
}

function msg_info( msg ) {
    msg_notification_level("info",msg);
}

function msg_warn( msg ) {
    msg_notification_level("warn",msg);
}

function msg_error( msg ) {
    msg_notification_level("error",msg);
}

function msg_notification_init() {
    msg_notification_mock ? msg_notification_mock() : msg_notification_call( msg_notification_clbk );
}

async function msg_notification_call( callback) {
  //xDebug('connectNotification', "connect_n::msg_notification");
  if (msg_wait) {
    xDebug('connectNotification', "still wait ..");
  } else {
      url = new URL("http://localhost:8080/notification");
      let text = null;
      let try_count = 0;
      if (msg_skipp_counter <= 0) {
          msg_skipp_counter = msg_skipp;
          do {
              if (try_count > 0) {
                // msg_warn('connection retry: ' + url.href);
                console.error('connection retry: ' + url.href);
              }
              try_count = try_count + 1;
              text = null;
              try{
                msg_wait = 1;
                let response = await fetch(url);
                // xDebug('connectNotification', response);
                text = await response.text(); // read response body as text
                msg_wait = 0;
                if (msg_skipp > 0 ) {
                    msg_info('connection recovered: ' + url.href);
                }
                msg_skipp = 0;
              } catch (e) {
                msg_warn('connection fail: ' + url.href);
                msg_wait = 0;
                msg_skipp = 10; // slow down ...
                //xError('connectNotification', e);
              }
              if (text && ( "" != text || "" != document.getElementById("msg_notification").innerHTML) && callback){
                callback( text );
              }
          } while (text && "" != text && 0 == msg_skipp  && try_count < 3)
      } else {
        msg_skipp_counter = msg_skipp_counter - 1;
      }
   } // msg_wait
}

setInterval(msg_notification_init, 1000);
