bp.registerBThread("Thread1",function(){
    var innerVarT1Before = 1;
    bp.sync({request:bp.Event("Thread1-EVENT")});
    var innerVarT1After = 2;
})

bp.registerBThread("Thread2", function(){
    var innerVarT2Before = 1;
    bp.sync({request:bp.Event("Thread2-EVENT")});
    var innerVarT2After = 2;
})