@echo off
setlocal

rem Adjust the following...
set M2_HOME=c:\java\m2

set IT_HOME=%~dp0
if exist "%IT_HOME%m2repo\org\artifactory\deploy-test" rmdir /s/q %IT_HOME%m2repo\org\artifactory\deploy-test

set MAVEN_OPTS=-Xmx512m
rem set MAVEN_OPTS=-Xmx512m -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
if "%M2_HOME%" == "" set M2_HOME=%MAVEN_HOME%

%M2_HOME%\bin\mvn.bat -Dorg.apache.maven.user-settings=%IT_HOME%settings.xml -Dmaven.repo.local=%IT_HOME%m2repo %*

::..\mvnit deploy:deploy-file -Durl=repo://localhost:8080/artifactory/local-repo@repo -DrepositoryId=central -Dfile=c:/downloads/registry_stable.reg -DgroupId=test -DartifactId=test123 -Dversion=1
endlocal
@echo on