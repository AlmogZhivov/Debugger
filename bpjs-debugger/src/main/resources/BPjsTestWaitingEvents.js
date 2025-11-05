bp.registerBThread('bt-world', function () {
    var myvar1 = {a: 10,b: 20};
    var mybt = 1;

    bp.sync({ request: bp.Event('world')});
    bp.registerBThread('bt-world-son', function () {
        bp.sync({ request: bp.Event('son-e'), waitFor: [bp.Event('hello'), bp.Event('world12121')] });
        var myvar1 = 10
        var mybt = 3
        foo(mybt)
        var z = mybt + 5
    })
    bp.sync({ request: bp.Event('aba') });
    foo(mybt)
    var z = mybt + 5
    bp.sync({ request: bp.Event('world12121') });
})

function foo(bt) {
    var m = 50;
    var p = m + bt;
    bp.log.info("50 + my bt: "+ p)
    const t = goo()
    bp.log.info("t: "+ t)
}

function goo() {
    var g1 = 50;
    var g2 = 100;
    const g3 = 200
    return g1+g2+g3
}

bp.registerBThread('bt-hello', function () {
    var x = 50;
    var y = 100;0
    bp.sync({ request: bp.Event('hello'), block: bp.Event('world')});
    foo(2)
    var z = x + 5
    bp.sync({ request: bp.Event('world12121') });
})