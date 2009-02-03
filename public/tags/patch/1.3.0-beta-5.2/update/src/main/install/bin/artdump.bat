@echo off
echo.
echo Starting Artifactory Update Manager...
echo.
setlocal

if "%JAVA_HOME%" == "" set _JAVACMD=java.exe
if not exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=java.exe
if "%_JAVACMD%" == "" set _JAVACMD="%JAVA_HOME%\bin\java.exe"

set UPDATE_DIR=%~dp0..

echo on
%_JAVACMD% -Xmx500m -cp "%UPDATE_DIR%\artifactory-update.jar" org.artifactory.update.ArtifactoryUpdate %*

@endlocal
