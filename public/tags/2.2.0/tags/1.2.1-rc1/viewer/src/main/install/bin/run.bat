@echo off

@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

set JAR=%DIRNAME%\..\dependency-viewer.jar
if exist "%JAR%" goto FOUND_JAR
echo Could not locate %JAR%. Please check that you are in the
echo bin directory when running this script.
goto END

:FOUND_JAR

if not "%JAVA_HOME%" == "" goto RUN

set JAVA=java.exe

echo JAVA_HOME is not set.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.

:RUN

set JAVA=%JAVA_HOME%\bin\java.exe
set JAVA_OPTS=%JAVA_OPTS% -Dconfig.file.path="%DIRNAME%\..\config\viewer.properties" -Dlog4j.configuration="file:%DIRNAME%\..\config\log4j.properties"

"%JAVA%" %JAVA_OPTS% -cp %JAR% org.jfrog.maven.viewer.Main

goto END

:END
endlocal