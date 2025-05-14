
  // https://api.jquery.com/toggle/
  function resizeFont( fontSizeDelta ) {
        const stylesheet = document.styleSheets[3]; // only allowed for local-css !!!
        let elementRules;

        // looping through all its rules and getting your rule
        for(let i = 0; i < stylesheet.cssRules.length; i++) {
          if(stylesheet.cssRules[i].selectorText === '.grid-container') {
            elementRules = stylesheet.cssRules[i];
            // modifying the rule in the stylesheet
            elementRules.style.setProperty('font-size', (fontSizeDelta + parseInt(elementRules.style.getPropertyValue('font-size').replace('px',''))) + "px" );
          }
        }
  }