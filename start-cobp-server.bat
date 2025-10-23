@echo off
echo Starting COBP Debugger Server...
echo.
echo This will start the server with COBP support at http://localhost:8080
echo.
echo COBP endpoints will be available at:
echo   - http://localhost:8080/cobp/debug
echo   - http://localhost:8080/cobp/stepInto
echo   - http://localhost:8080/cobp/stepOver
echo   - http://localhost:8080/cobp/nextSync
echo   - WebSocket: ws://localhost:8080/ws
echo.
echo Press Ctrl+C to stop the server
echo.


cd controller
mvn spring-boot:run
