@echo off
setlocal

set SERVICE_NAME=Artifactory
set EXECUTABLE=..\bin\artifactory-service.exe

"%EXECUTABLE%" //DS//%SERVICE_NAME%
echo The service '%SERVICE_NAME%' has been removed