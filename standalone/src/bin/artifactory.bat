setlocal
cd ..
%JAVA_HOME%\bin\java.exe -Dlog4j.configuration=file:etc/log4j.properties -jar artifactory.jar
endlocal