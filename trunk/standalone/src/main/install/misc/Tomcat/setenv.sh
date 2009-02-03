. /etc/artifactory/default
export JAVA_OPTS="$JAVA_OPTIONS -Dartifactory.home=$ARTIFACTORY_HOME"
export CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER=true"
