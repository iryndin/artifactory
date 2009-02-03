set IT_HOME=%~dp0
rmdir /s/q %IT_HOME%m2repo\org\artifactory\deploy-test
%M2_HOME%/bin/mvn.bat -Dorg.apache.maven.user-settings=%IT_HOME%settings.xml -Dmaven.repo.local=%IT_HOME%m2repo %*