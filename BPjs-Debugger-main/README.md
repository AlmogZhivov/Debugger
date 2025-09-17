# BP JS IDE
The project contains 5 Modules:
- api-ext - introduces the projects interfaces
- api-int - introduces the projects internal components
- bpjs-debugger - the debugger engine
- service - manipulates clients and their running sessions - run/debug sessions
- controller - responsible for the incoming HTTP/WS requests and messages

If you want to run the BPjs React IDE with this debugger, there's no need to clone this project; just head to https://github.com/bThink-BGU/BPjs-React-IDE and use the docker image.

---
## Compiling the code
run ```mvn install -DskipTests```

## Running the CLI Debugger
- under bpjs-debugger:
    - run il.ac.bgu.se.bp.mains.BPJsDebuggerCliRunner 

## Running the server
IntelliJ:
- choose both clean and install on maven tool window and run
- under controller module:
    - run il.ac.bgu.se.bp.app.Application 

Terminal:
```bash
cd controller
mvn spring-boot:run
```
- 
- 

---

## Using the server

Base URL is localhost:8080/bpjs 

Supported endpoints:

| Command                     | Endpoint           | Method | Request / Params                                                                                       | Headers |
| --------------------------- | ------------------ | ------ | ------------------------------------------------------------------------------------------------------ | ------- |
| Run                         | bpjs/run           | POST   | {sourceCode: String}                                                                                   | userId  |
| Debug                       | bpjs/debug         | POST   | {sourceCode: String, breakpoints: int[], skipBreakpointsToggle: boolean, skipSyncStateToggle: boolean, waitForExternalEvents: boolean} | userId  |
| Add / remove Breakpoint     | bpjs/breakpoint    | POST   | {lineNumber: number, stopOnBreakpoint: boolean}                                                        | userId  |
| Toggle Mute Breakpoints     | bpjs/breakpoint    | PUT    | {skipBreakpoints: boolean}                                                                             | userId  |
| Toggle Mute Sync States     | bpjs/syncStates    | PUT    | {skipSyncStates: boolean}                                                                              | userId  |
| Toggle Wait for external events   | bpjs/waitExternal   | PUT    | {waitForExternal: boolean}                                                                              | userId  |
| Stop                        | bpjs/stop          | GET    | None                                                                                                   | userId  |
| Step Out                    | bpjs/stepOut       | GET    | None                                                                                                   | userId  |
| Step Into                   | bpjs/stepInto      | GET    | None                                                                                                   | userId  |
| Step Over                   | bpjs/stepOver      | GET    | None                                                                                                   | userId  |
| Continue                    | bpjs/continue      | GET    | None                                                                                                   | userId  |
| Next Sync                   | bpjs/nextSync      | GET    | None                                                                                                   | userId  |
| Add / Remove External Event | bpjs/externalEvent | POST   | {externalEvent: String, addEvent: boolean}                                                             | userId  |
| Set Sync Snapshot           | bpjs/syncSnapshot  | PUT   | {snapShotTime: long}                                                                                   | userId  |
| Get Events History          | bpjs/events        | GET    | from={int}&to{int}                                                                                     | userId  |

---

[BP JS Framework](http://wwww.bpjside.tk)
