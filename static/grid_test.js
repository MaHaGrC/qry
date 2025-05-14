
function test_grid_base(params) {
  testStart("grid_base");

  grid.setData("a;b;c\n0;1;2\n3;4;5\n6;7;8");
  grid.load();

  assertEq(grid.items.length, 4, " match send rows (closing new line optional)");
  assertEq(grid.items[0].length, 3, " match send cols");
  assertEq(grid.items[grid.items.length - 1].length, 3, " match send cols - even last row");

  elem = assertId("grid_1_1");
  assertEq(elem.getAttribute("srcD"), "1,1", "refer to srcRow (need for update)");
  assertEq(elem.innerHTML, "1", "correct value / pure (Base0)");

  assertEq(assertId("grid_2_1").innerHTML, "4", "correct 2nd data row (Base0)");
  assertEq(assertId("grid_3_1").innerHTML, "7", "correct 3nd data row (Base0)");

  assertNotId("grid_4_1");

}
test_grid_base();

function readCol(col) {
  //  net to read from DIV as grid.items is not changed by order, filter ....
  let val = "";
  let cols = Array.isArray(col) ? col : [col];
  for (let i = 0; i < grid.items.length; i++) {
    val = val + (i > 0 ? "," : "");
    for (let ci = 0; ci < cols.length; ci++) {
      val = val + (ci > 0 ? "," : "") + document.getElementById("grid_" + i + "_" + cols[ci]).innerHTML.replace(/<.*/, ''); // remove inner tags ... like (sort-Order)
    }
  }
  return val;
}

function test_grid_sort(params) {
  testStart("grid_sort");

  grid.setData("a;b;c\n0;1;2\n3;4;5\n6;7;8");
  grid.load();

  assertEq(grid.items.toString(), "a,b,c,0,1,2,3,4,5,6,7,8", "start with loaded order");
  assertEq(readCol([0, 1, 2]), "a,b,c,0,1,2,3,4,5,6,7,8", "readCol matches");

  grid.sort(1, "asc");
  assertEq(readCol([0, 1, 2]), "a,b,c,0,1,2,3,4,5,6,7,8", "asc order");

  grid.sort(1, "desc");
  assertEq(readCol([0, 1, 2]), "a,b,c,6,7,8,3,4,5,0,1,2", "desc order");


}
test_grid_sort();
