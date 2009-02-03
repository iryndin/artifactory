@echo off
echo.
echo Starting the Artifactory server...
echo.
echo To stop , press Ctrl+c
echo.
setlocal

if "%JAVA_HOME%" == "" set _JAVACMD=java.exe
if not exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=java.exe
if "%_JAVACMD%" == "" set _JAVACMD="%JAVA_HOME%\bin\java.exe"

set ARTIFACTORY_HOME=%~dp0..

echo on
%_JAVACMD% -Xmx400m -Djetty.home="%ARTIFACTORY_HOME%" -Dartifactory.home="%ARTIFACTORY_HOME%" -cp "%ARTIFACTORY_HOME%\artifactory.jar" org.artifactory.standalone.main.Main %*

@endlocal
