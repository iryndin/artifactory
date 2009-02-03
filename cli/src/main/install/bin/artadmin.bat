@echo off
setlocal

if "%JAVA_HOME%" == "" set _JAVACMD=java.exe
if not exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=java.exe
if "%_JAVACMD%" == "" set _JAVACMD="%JAVA_HOME%\bin\java.exe"

set CLI_DIR=%~dp0..

echo on
%_JAVACMD% -jar "%CLI_DIR%\artifactory-cli.jar" %*

@endlocal
