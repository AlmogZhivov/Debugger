bp.registerBThread("bt-world",function(){
    var tt = 15;
    bp.sync({waitFor:bp.Event("hello")});
    bp.sync({request:bp.Event("aba")});
    var x = 10;
    var y = 20;
    print("HELLO")
    foo()
    bp.sync({request:bp.Event("world12121")});
})

bp.registerBThread("bt-hello", function(){
    bp.sync({request:bp.Event("hello")});
    var z = 11;
    var y = 40;
    foo()
    bp.sync({request:bp.Event("hello12312312")});
})

function foo() {
    var m = 50;
    var n = 100;
    bp.log.info("foo foo foo foo")
    foo();
    var p = m+n;
    const t = 200
}