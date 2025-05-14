// const debugList = {
//   general:{ // schaltet alle console Befehle ein oder aus
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   // ab hier hat jede Datei die Möglichkeit console-Ausgaben ein oder auszuschalten
//   // diese könnten auch für einzelne Funktionen angelegt und genutzt werden
//   codeMirrorWrapper: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   connect: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   connectNotification: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   controller: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   dragDropResize: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   dragResize: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   event: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   grid: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   init: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   menuCircular: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   util: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   },
//   utilToggle: {
//     log: true,
//     debug: true,
//     info: true,
//     error: true,
//     warn: true
//   }
// }

const debugList = {
  general:{ // schaltet alle console Befehle ein oder aus
    log: true,
    debug: true,
    info: true,
    error: true,
    warn: true
  },
  // ab hier hat jede Datei die Möglichkeit console-Ausgaben ein oder auszuschalten
  // diese könnten auch für einzelne Funktionen angelegt und genutzt werden
  codeMirrorWrapper: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  connect: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  connectNotification: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  controller: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  dragDropResize: {
    log: true,    debug: true,    info: true,    error: true,    warn: true
  },
  dragResizeGridCol: {
    log: true,    debug: true,    info: true,    error: true,    warn: true
  },
  event: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  grid: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  init: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  menuCircular: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  session: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  util: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  },
  utilToggle: {
    log: true,    debug: false,    info: true,    error: true,    warn: true
  }
}


function shallShow(parameter, type) { // regelt, ob eine x-Funktion Inhalte in der Konsole anzeigt
  if(debugList.general[type]) {
    if(typeof parameter === "string") { return debugList[parameter][type]; }
    else { return parameter; }
  } else {
    return false;
  }
}

function xDebug(parameter, message1, message2 = null, message3 = null) {
  if (shallShow(parameter, 'debug') ){
    if (message3 !== null) { console.debug(message1, message2, message3); }
    else if (message2 !== null) { console.debug(message1, message2); }
    else { console.debug(message1); }
  }
}

function xLog(parameter, message1, message2 = null, message3 = null) {
  if (shallShow(parameter, 'log')){
    if (message3 !== null) { console.log(message1, message2, message3); }
    else if (message2 !== null) { console.log(message1, message2); }
    else { console.log(message1); }
  }
}

function xInfo(parameter, message1, message2 = null, message3 = null) {
  if (shallShow(parameter, 'info')){
    if (message3 !== null) { console.info(message1, message2, message3); }
    else if (message2 !== null) { console.info(message1, message2); }
    else { console.info(message1); }
  }
}

function xError(parameter, message1, message2 = null, message3 = null) {
  if (shallShow(parameter, 'error')){
    if (message3 !== null) { console.error(message1, message2, message3); }
    else if (message2 !== null) { console.error(message1, message2); }
    else { console.error(message1); }
    msg_error(message1);
  }
}

function xWarn(parameter, message1, message2 = null, message3 = null) {
  if (shallShow(parameter, 'warn')){
    if (message3 !== null) { console.warn(message1, message2, message3); }
    else if (message2 !== null) { console.warn(message1, message2); }
    else { console.warn(message1); }
    msg_warn(message1);
  }
}