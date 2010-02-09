setlocal
cd ..

if "%JAVA_HOME%" == "" set _JAVACMD=java.exe
if not exist "%JAVA_HOME%\bin\java.exe" set _JAVACMD=java.exe
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

"%_JAVACMD%" -Dlog4j.configuration=file:etc/log4j.properties -jar artifactory.jar %*
endlocal