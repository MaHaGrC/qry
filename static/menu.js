const contextMenu = document.getElementById("context-menu");
const scope = document.querySelector("body__"); // to DISABLE set body___

const normalizePozition = (mouseX, mouseY) => {
  // ? compute what is the mouse position relative to the container element (scope)
  let {
    left: scopeOffsetX,
    top: scopeOffsetY,
  } = scope.getBoundingClientRect();
  
  scopeOffsetX = scopeOffsetX < 0 ? 0 : scopeOffsetX;
  scopeOffsetY = scopeOffsetY < 0 ? 0 : scopeOffsetY;
 
  const scopeX = mouseX - scopeOffsetX;
  const scopeY = mouseY - scopeOffsetY;

  // ? check if the element will go out of bounds
  const outOfBoundsOnX =
    scopeX + contextMenu.clientWidth > scope.clientWidth;

  const outOfBoundsOnY =
    scopeY + contextMenu.clientHeight > scope.clientHeight;

  let normalizedX = mouseX;
  let normalizedY = mouseY;

  // ? normalize on X
  if (outOfBoundsOnX) {
    normalizedX =
      scopeOffsetX + scope.clientWidth - contextMenu.clientWidth;
  }

  // ? normalize on Y
  if (outOfBoundsOnY) {
    normalizedY =
      scopeOffsetY + scope.clientHeight - contextMenu.clientHeight;
  }

  return { normalizedX, normalizedY };
};

if (scope){

  
    scope.addEventListener("contextmenu", (event) => {
      event.preventDefault();
    
      const { clientX: mouseX, clientY: mouseY } = event;
    
      const { normalizedX, normalizedY } = normalizePozition(mouseX, mouseY);
    
      contextMenu.classList.remove("visible");
    
      contextMenu.style.top = `${normalizedY}px`;
      contextMenu.style.left = `${normalizedX}px`;
      // hack to enable animation
      setTimeout(() => {
        contextMenu.classList.add("visible");
      });
    });
    
  
    scope.addEventListener("click", (e) => {
      // ? close the menu if the user clicks outside of it
      if (e.target.offsetParent != contextMenu) {
        contextMenu.classList.remove("visible");
      }
    });
    
          
}

function contextMenuClose(params) {
    contextMenu.classList.remove("visible");
}

/*
   UL - Menu    https://www.w3schools.com/howto/howto_js_treeview.asp
*/

var toggler = document.getElementsByClassName("caret");
var i;

for (i = 0; i < toggler.length; i++) {
  toggler[i].addEventListener("click", function() {
    this.parentElement.querySelector(".nested").classList.toggle("active");
    this.classList.toggle("caret-down");
  });
} 