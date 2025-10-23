# COBP Debugger Setup and Usage

This guide explains how to set up and use the COBP (Context-Oriented Behavior Programming) debugger to see real JSON responses from COBP.js execution.

## 🎯 What We've Built

We've successfully integrated COBP support into the existing BPjs debugger:

1. **COBPDebuggerImpl** - A debugger that uses `DebugableContextBProgram` for COBP.js
2. **COBPIDEService** - Service layer for COBP debugging operations
3. **COBPIDERestController** - REST endpoints for COBP debugging
4. **Real COBP.js execution** - The debugger can now execute actual COBP programs

## 🚀 Quick Start

### 1. Start the Server

```bash
# Option 1: Use the batch file (Windows)
start-cobp-server.bat

# Option 2: Manual start
cd controller
mvn spring-boot:run
```

The server will start at `http://localhost:8080`

### 2. Test with HTTP Requests

```bash
# Install dependencies first
npm install axios

# Run the simple test
node test-cobp-simple.js

# Or run the full test with complete COBP.js
node test-cobp-debugger.js
```

### 3. Test with WebSocket

```bash
# Install dependencies first
npm install @stomp/stompjs ws

# Connect to COBP debugger via WebSocket
node cobp-subscribe.js
```

## 📡 Available Endpoints

### COBP REST Endpoints (Base URL: `http://localhost:8080/cobp`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/debug` | Start debugging a COBP program |
| GET | `/stepInto` | Step into the next line |
| GET | `/stepOver` | Step over the current line |
| GET | `/nextSync` | Go to next synchronization point |
| GET | `/continue` | Continue execution |
| GET | `/stop` | Stop execution |
| POST | `/breakpoint` | Set/remove breakpoints |
| GET | `/events` | Get events history |

### WebSocket Endpoints

- **Connection**: `ws://localhost:8080/ws`
- **Subscribe**: `/cobp/subscribe`
- **State Updates**: `/bpjs/state/update`
- **Console Updates**: `/bpjs/console/update`

## 🔍 What You'll See in JSON Responses

When you execute COBP.js and perform step operations, you'll see JSON responses like this:

```json
{
  "success": true,
  "errorCode": null,
  "bThreads": [
    {
      "name": "cbt: philosopherBehavior",
      "context": "allPhilosophers",  ← Shows query binding
      "requested": [
        "pickUpFork",     ← From sync({request: Event('pickUpFork', {...})})
        "startEating",    ← From sync({request: Event('startEating', {...})})
        "think",          ← From sync({request: Event('think')})
        "stopEating",     ← From sync({request: Event('stopEating', {...})})
        "putDownFork"     ← From sync({request: Event('putDownFork', {...})})
      ],
      "wait": [],
      "blocked": []
    },
    {
      "name": "thinking",
      "context": "unknown",
      "requested": [
        "think"           ← From sync({request: Event('think')})
      ],
      "wait": [],
      "blocked": []
    }
  ]
}
```

## 🎯 Key Features Demonstrated

1. **Context Field**: Shows which query each b-thread is bound to (e.g., "allPhilosophers")
2. **Requested Events**: Shows all events requested by each b-thread from `sync({request: ...})` statements
3. **Real COBP Execution**: The debugger actually executes the COBP.js code
4. **Step Operations**: You can use stepInto, stepOver, nextSync on real COBP programs

## 🛠️ Technical Details

### Files Created/Modified

1. **COBPDebuggerImpl.java** - Main COBP debugger implementation
2. **COBPIDEService.java** - Service interface for COBP operations
3. **COBPIDEServiceImpl.java** - Service implementation
4. **COBPIDERestControllerImpl.java** - REST controller for COBP endpoints
5. **COBP.js** - Complete COBP test program with philosophers and forks
6. **COBPDebuggerFactoryImpl.java** - Factory for creating COBP debuggers

### Key Technical Solutions

1. **ProgramValidator Injection**: Fixed null pointer exceptions in tests
2. **DebugableContextBProgram**: Uses COBP-specific program loader
3. **Context and Events**: Properly captures COBP context and requested events
4. **Real Execution**: No mocks - actual COBP.js execution with step operations

## 🧪 Testing

### Unit Tests
```bash
cd bpjs-debugger
mvn test -Dtest=COBPSolutionTest
```

### Integration Tests
```bash
# Start server first
start-cobp-server.bat

# In another terminal
node test-cobp-simple.js
```

## 🎉 Success Criteria Met

✅ **Real COBP.js Execution**: The debugger loads and executes actual COBP programs  
✅ **Context Field**: JSON responses include the context (query binding) for each b-thread  
✅ **Requested Events**: JSON responses include all requested events from sync statements  
✅ **Step Operations**: stepInto, stepOver, nextSync work on real COBP programs  
✅ **No Mocks**: Everything uses real COBP execution, not test mocks  
✅ **WebSocket Support**: Real-time updates via WebSocket connections  

## 🔧 Troubleshooting

### Common Issues

1. **Server won't start**: Make sure all modules are compiled
   ```bash
   mvn install -DskipTests
   ```

2. **COBPIDEService not found**: Compile service module first
   ```bash
   cd service && mvn compile
   cd ../controller && mvn compile
   ```

3. **WebSocket connection fails**: Check if server is running on port 8080

4. **JSON responses empty**: Make sure COBP.js program is valid and has sync statements

### Debug Mode

To see detailed logs, start the server with debug logging:
```bash
cd controller
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.il.ac.bgu.se.bp=DEBUG"
```

## 📚 Next Steps

1. **Use the real COBP debugger** with your own COBP programs
2. **Integrate with your IDE** using the REST API or WebSocket
3. **Extend the functionality** by adding more COBP-specific features
4. **Test with complex COBP programs** to see context changes dynamically

The COBP debugger is now fully functional and ready to use! 🚀
