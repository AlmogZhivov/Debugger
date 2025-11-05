const global_var = 50
const global_lambda = (aaa) => aaa+50;
const global_event = bp.Event('IM GLOBAL EVENT!');

bp.registerBThread('bt-world', function () {
    var myvar1 = {a: 10,b: 20};
    var myvar2 = 20;

    bp.registerBThread('bt-world-son', function () {
        const dynamic_son_recently_var = 20;
        const dynamic_son_recently_var_2 = 30;
        bp.sync({ request: bp.Event('son-e') });
        var myvar1 = 10;
        var myvar2 = 20;
        foo(3)
        var z = myvar1 + 5
        bp.sync({ request: bp.Event('world12121') });
    })
    bp.sync({ request: bp.Event('aba') });
    foo(1)
    var z = myvar1 + 5
    bp.sync({ request: bp.Event('world12121') });
})

function foo(bt) {
    var m = 50;
    var n = 100;
    bp.sync({ request: bp.Event('EventInFoo') });
    var p = m + n;
    goo()
    const t = 200
}

function goo() {
    var g1 = 50;
    var g2 = 100;
    const g3 = 200
}

bp.registerBThread('bt-hello1', function () {
    var x = 50;
    var y = 100;
    foo(2)
    bp.sync({ request: bp.Event('aba') });
    foo(2)
    var z = x + 5
    bp.sync({ request: bp.Event('world12121') });
})