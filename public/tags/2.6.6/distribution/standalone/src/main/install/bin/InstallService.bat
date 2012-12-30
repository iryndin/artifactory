@echo off

set "CURRENT_DIR=%cd%"
cd ..
set "ARTIFACTORY_HOME=%cd%"
cd "%CURRENT_DIR%"

::--
set SERVICE_NAME=Artifactory
set EXECUTABLE=%ARTIFACTORY_HOME%\bin\artifactory-service.exe

::-- Make sure prerequisite environment variables are set
if not "%JAVA_HOME%" == "" goto gotJdkHome
if not "%JRE_HOME%" == "" goto gotJreHome
echo Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
echo Service will try to guess them from the registry.
goto okJavaHome
:gotJreHome
if not exist "%JRE_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JRE_HOME%\bin\javaw.exe" goto noJavaHome
goto okJavaHome
:gotJdkHome
if not exist "%JAVA_HOME%\jre\bin\java.exe" goto noJavaHome
if not exist "%JAVA_HOME%\jre\bin\javaw.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javac.exe" goto noJavaHome
if not "%JRE_HOME%" == "" goto okJavaHome
set "JRE_HOME=%JAVA_HOME%\jre"
goto okJavaHome
:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
echo NB: JAVA_HOME should point to a JDK not a JRE
goto end
:okJavaHome

::-- The fully qualified start and stop classes
set START_CLASS=org.artifactory.standalone.main.Main
set STOP_CLASS=org.artifactory.standalone.main.Main

::-- The classpath for all jars needed to run your service
set LIB_DIR=%ARTIFACTORY_HOME%\lib\*
set CLASSPATH=%ARTIFACTORY_HOME%\artifactory.jar;%LIB_DIR%

::-- Set to auto if you want the service to startup automatically.
set STARTUP_TYPE=auto

::---- Set other options via environment variables -------

rem Set the server jvm from JAVA_HOME
set "JVM_PATH=%JRE_HOME%\bin\server\jvm.dll"
if exist "%JVM_PATH%" goto foundJvm
rem Set the client jvm from JAVA_HOME
set "JVM_PATH=%JRE_HOME%\bin\client\jvm.dll"
if exist "%JVM_PATH%" goto foundJvm
set JVM_PATH=auto

:foundJvm
::---- Install the Service -------
echo Installing service '%SERVICE_NAME%' ...
echo JAVA_HOME:        "%JAVA_HOME%"
echo JRE_HOME:         "%JRE_HOME%"
echo JVM:              "%JVM_PATH%"

::--- To redirect the stdout to a file add '--StdOutput artifactory-stdout' to the command
"%EXECUTABLE%" //IS//%SERVICE_NAME% --StartClass %START_CLASS% --StopClass %STOP_CLASS% --StopMethod stop --StartMode jvm --StopMode jvm --Install %EXECUTABLE%
if not errorlevel 1 goto installed
goto end

:installed
::--- Clear the environment variables. They are not needed any more.
"%EXECUTABLE%" //US/%SERVICE_NAME% --DisplayName Artifactory --Description "Artifactory Binary Repository" --Jvm "%JVM_PATH%" --Classpath %CLASSPATH% --LogPrefix artifactory-service --LogPath="%ARTIFACTORY_HOME%\logs" --Startup %STARTUP_TYPE% --StdError artifactory-stderr ++JvmOptions "-XX:PermSize=128m;-XX:MaxPermSize=128m;-XX:NewSize=512m;-XX:MaxNewSize=512m;-XX:+UseConcMarkSweepGC;-Dartifactory.home=%ARTIFACTORY_HOME%;-Djava.io.tmpdir=%ARTIFACTORY_HOME%\work" --JvmMs 1024 --JvmMx 1024
echo The service '%SERVICE_NAME%' has been installed.

:end