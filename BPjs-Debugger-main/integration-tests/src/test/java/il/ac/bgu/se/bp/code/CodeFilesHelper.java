package il.ac.bgu.se.bp.code;

public class CodeFilesHelper {

    public static String getCodeByFileName(String filename) {
        switch (filename) {
            case "testFile1":
                return testFile1();
            case "testFile2":
                return testFile2();
            case "testFile3":
                return testFile3();
            case "testFile4":
                return testFile4();
        }
        return null;
    }

    private static String testFile1() {
        return
                "bp.registerBThread('bt-world', function () {\n" +
                        "    var myvar1 = 10;\n" +
                        "    var myvar2 = 20;\n" +
                        "    bp.registerBThread('bt-world-son', function () {\n" +
                        "        bp.sync({ request: bp.Event('son-e') });\n" +          // L5
                        "        var x = 5;\n" +
                        "        var y = 16.7;\n" +
                        "        foo(3)\n" +
                        "        var z = \"alex\"\n" +
                        "        var tt = \"hello\"\n" +                                // L10
                        "        bp.sync({ request: bp.Event('world12121') });\n" +
                        "    })\n" +
                        "    foo(1)\n" +
                        "    var z = myvar1 + 5\n" +
                        "})\n" +                                                        // L15
                        "\n" +
                        "function foo(bt) {\n" +
                        "    var m = 50;\n" +
                        "    var n = 100;\n" +
                        "    var p = m + n;\n" +                                        // L20
                        "    goo()\n" +
                        "    bp.log.info('grrrrrr')\n" +
                        "    const t = 200\n" +
                        "}\n" +
                        "\n" +
                        "function goo() {\n" +
                        "    var g1 = 50;\n" +
                        "    var g2 = 100;\n" +
                        "    const g3 = 200\n" +
                        "}\n" +
                        "\n";
    }

    private static String testFile2() {
        return
                "bp.registerBThread(\"Thread1\",function(){\n" +
                        "    var t = 6;\n" +
                        "    bp.sync({request:bp.Event(\"Thread1-EVENT\")});\n" +
                        "    var innerVarT1After = 2;\n" +                              // 4
                        "})\n" +
                        "\n" +
                        "bp.registerBThread(\"Thread2\", function(){\n" +
                        "    var varT2B = 1;\n" +
                        "    bp.sync({request:bp.Event(\"Thread2-EVENT\")});\n" +       // 9
                        "    var var2 = \"alex\";\n" +
                        "    var var3 = 2;\n" +
                        "})";
    }

    private static String testFile3() {
        return "bp.registerBThread('bt-world', function () {\n" +
                "    bp.sync({ request: bp.Event('aba') });\n" +
                "    foo(1)\n" +                                                // 3
                "})\n" +
                "\n" +
                "function foo(bt) {\n" +
                "    var m = 50;\n" +
                "    var n = 100;\n" +
                "    var p = m + n;\n" +
                "    goo()\n" +                                                 // 10
                "    const t = 200\n" +
                "}\n" +
                "\n" +
                "function goo() {\n" +
                "    var g1 = 50;\n" +                                          // 15
                "    var g2 = 100;\n" +
                "    const g3 = 200\n" +
                "}";
    }

    private static String testFile4() {
        return "bp.registerBThread(\"bt-1\",function() {\n" +
                "  bp.sync({request:bp.Event(\"developers\")});\n" +
                "})\n" +
                "\n" +
                "bp.registerBThread(\"bt-2\", function() {\n" +
                "  bp.sync({waitFor:bp.Event(\"israel\"), block:bp.Event(\"developers\")});\n" +
                "})\n" +
                "\n" +
                "bp.registerBThread(\"bt-3\", function() {\n" +
                "  bp.sync({waitFor:bp.Event(\"from\"), block:bp.Event(\"israel\")});\n" +
                "})\n" +
                "\n" +
                "bp.registerBThread(\"bt-4\", function() {\n" +
                "  bp.sync({waitFor:bp.Event(\"world\"), block:bp.Event(\"from\")});\n" +
                "})\n" +
                "\n" +
                "bp.registerBThread(\"bt-5\", function() {\n" +
                "  bp.sync({waitFor:bp.Event(\"hello\"), block:bp.Event(\"world\")});\n" +
                "})";
    }

}
