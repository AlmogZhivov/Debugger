
bp.registerBThread("bt-world",function(){
    bp.sync({waitFor:bp.Event("hello")});
    bp.sync({request:bp.Event("aba")});
    var x = 10;
    var y = 20;
    foo()
    bp.sync({request:bp.Event("world12121")});
})

bp.registerBThread("bt-hello", function(){
    bp.sync({request:bp.Event("hello")});
    var z = 11;
    var y = 40;
    bp.sync({request:bp.Event("hello12312312")});
})

function foo() {
    var m = 50;
    var n = 100;
    var p = m+n;
    const t = 200
}



bp.registerBThread("bt-hello1",function(){
    var x = 50;
    var y = 100;
    foo(2)
    bp.sync({request:bp.Event("aba")});
    foo(2)
    var z = x +5
    bp.sync({request:bp.Event("world12121")});
})