bp.registerBThread('bt-test-1', function () {
    var myvar1 = 10;
    bp.sync({ request: bp.Event('bt-1-event-1') });
    foo(1)
    var z = myvar1 + 5
    bp.sync({ request: bp.Event('bt-1-event-2') });
})

function foo(bt) {
    var m = 50;
    var n = 100;
    var p = m + n;
    goo()
    const t = 200
}

function goo() {
    var g1 = 50;
    var g2 = 100;
    const g3 = 200
}

bp.registerBThread('bt-test-2', function () {
    var myvar2 = 10;
    bp.sync({ request: bp.Event('bt-2-event-1') });
    foo(1)
    var z = myvar2 + 5
    bp.sync({ request: bp.Event('bt-2-event-2') });
})