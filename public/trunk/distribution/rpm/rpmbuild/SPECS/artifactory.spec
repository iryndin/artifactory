Name:           artifactory
Version:        %{artifactory_version}
Release:        %{artifactory_release}
Summary:        Binary Repository Manager
Vendor:         JFrog Ltd.
Group:          Development/Tools
License:        LGPL
URL:            http://www.jfrog.org
Source0:        standalone.zip
BuildRoot:      %{_tmppath}/build-%{name}-%{version}
BuildArch:      noarch
Requires:       %{_sbindir}/useradd, %{_sbindir}/groupadd, %{_bindir}/pkill, %{_bindir}/rsync
#Prefix:         /opt

%define username artifactory
%define group_name artifactory
%define extracted_standalone %{_sourcedir}/*artifactory*
%define extracted_tomcat %{_sourcedir}/*tomcat*

%define artifactory_bin_home /opt/%{name}
%define tomcat_home %{artifactory_bin_home}/tomcat
%define user_home %{_localstatedir}/lib/%{name}
%define log_dir %{_localstatedir}/log/%{name}
%define etc_dir %{_sysconfdir}/%{name}
%define pid_dir %{_localstatedir}/lib/%{name}/run
%define _rpmfilename %{filename_prefix}-%{full_version}.rpm

%description
The best binary repository manager around.

%prep
%setup -q -T -c
%{__unzip} "%{SOURCE0}" -d "%{_sourcedir}"

%build

%install
%__rm -rf %{buildroot}

# Copy the etc dir to the from the build dir to the build root (currently contains default script)
%__install -d "%{buildroot}%{etc_dir}"

# Copy the contents of the standalone etc to the artifactory etc dir
rsync -r --exclude=jetty.xml --exclude=artifactory.config.xml %{extracted_standalone}/etc/ "%{buildroot}%{etc_dir}"
%__install -D %{extracted_standalone}/bin/%{name}.default "%{buildroot}%{etc_dir}/default"
%__install -D %{extracted_standalone}/misc/tomcat/%{name} "%{buildroot}%{_sysconfdir}/init.d/%{name}"

# Replace the vars in the init and default scripts

%__sed -r --in-place "s%#export ARTIFACTORY_HOME=.*%export ARTIFACTORY_HOME=%{user_home}%" "%{buildroot}%{etc_dir}/default"
%__sed -r --in-place "s%#export TOMCAT_HOME=.*%export TOMCAT_HOME=%{tomcat_home}%" "%{buildroot}%{etc_dir}/default"
%__sed -r --in-place "s/#export ARTIFACTORY_USER=.*/export ARTIFACTORY_USER=%{username}/" "%{buildroot}%{etc_dir}/default"
%__sed -r --in-place "s%#export CATALINA_PID=.*%export CATALINA_PID=%{pid_dir}/%{name}.pid%" "%{buildroot}%{etc_dir}/default"

# Create log dir
%__install -d "%{buildroot}%{log_dir}/catalina"

# Create home dir and symlinks
%__install -d "%{buildroot}%{user_home}"
%__install -d "%{buildroot}%{user_home}/temp"
%__install -d "%{buildroot}%{user_home}/work"
%__install -d "%{buildroot}%{user_home}/webapps"

%__ln_s "%{artifactory_bin_home}/bin" "%{buildroot}%{user_home}/bin"
%__ln_s "%{etc_dir}" "%{buildroot}%{user_home}/etc"
%__ln_s "%{log_dir}" "%{buildroot}%{user_home}/logs"
%__ln_s "%{artifactory_bin_home}/clilib" "%{buildroot}%{user_home}/clilib"
%__ln_s "%{artifactory_bin_home}/misc" "%{buildroot}%{user_home}/misc"
%__ln_s "%{tomcat_home}" "%{buildroot}%{user_home}/tomcat"

# Copy artifactory war
%__install -D %{extracted_standalone}/webapps/%{name}.war "%{buildroot}%{user_home}/webapps/%{name}.war"

# Create var - run dir
%__install -d "%{buildroot}%{pid_dir}"

# Create the bin dir
%__install -d "%{buildroot}/opt"
%__cp -r %{_builddir}/opt/* "%{buildroot}/opt/"
%__cp -r %{_builddir}/var/* "%{buildroot}/var/"

%__cp %{extracted_standalone}/bin/artadmin "%{buildroot}%{artifactory_bin_home}/bin/"
%__cp -r %{extracted_standalone}/clilib "%{buildroot}%{artifactory_bin_home}/"
%__cp -r %{extracted_standalone}/misc "%{buildroot}%{artifactory_bin_home}/"

%__cp %{extracted_standalone}/Artifactory-Third-Parties-Usage.html "%{buildroot}%{artifactory_bin_home}/"
%__cp %{extracted_standalone}/COPYING "%{buildroot}%{artifactory_bin_home}/"
%__cp %{extracted_standalone}/COPYING.LESSER "%{buildroot}%{artifactory_bin_home}/"
%__cp %{extracted_standalone}/README.txt "%{buildroot}%{artifactory_bin_home}/"

%__install -d "%{buildroot}%{tomcat_home}"
%__cp -r %{extracted_tomcat}/* "%{buildroot}%{tomcat_home}/"

# Copy the customized tomcat configs
%__cp %{extracted_standalone}/misc/tomcat/setenv.sh "%{buildroot}%{tomcat_home}/bin/"

%__cp %{extracted_standalone}/misc/tomcat/server.xml "%{buildroot}%{tomcat_home}/conf/"

%__install -d "%{buildroot}%{tomcat_home}/conf/Catalina/localhost"

%__cp %{extracted_standalone}/misc/tomcat/%{name}.xml "%{buildroot}%{tomcat_home}/conf/Catalina/localhost/"

%__sed -r --in-place 's%<Context path="/artifactory".*%<Context path="/artifactory">%' "%{buildroot}%{tomcat_home}/conf/Catalina/localhost/%{name}.xml"

%__ln_s "%{user_home}/logs/catalina" "%{buildroot}%{tomcat_home}/logs"

%__ln_s "%{user_home}/temp" "%{buildroot}%{tomcat_home}/temp"

%__ln_s "%{user_home}/work" "%{buildroot}%{tomcat_home}/work"

%__ln_s "%{user_home}/webapps" "%{buildroot}%{tomcat_home}/webapps"

%clean
%__rm -rf %{_builddir}/%{name}-%{version}

%pre

CURRENT_USER=`id -nu`
if [ "$CURRENT_USER" != "root" ]; then
    echo
    echo "ERROR: Please install Artifactory using root."
    echo
    exit 1
fi

SERVICE_FILE="%{_sysconfdir}/init.d/%{name}"
if [ -e "$SERVICE_FILE" ]; then
    # Checking same layout. If not fails upgrade
    if [ -n "`grep "/etc/opt/jfrog/artifactory/default" "$SERVICE_FILE"`" ]; then
        echo "ERROR: Currently installed Artifactory version does not have the same layout than this RPM!"
        echo "ERROR: Upgrade impossible!"
        exit 1
    fi

    # Shutting down the artifactory service if running
    SERVICE_STATUS="`$SERVICE_FILE status`"
    if [[ ! "$SERVICE_STATUS" =~ .*[sS]topped.* ]]; then
        echo "Stopping the artifactory service..."
        $SERVICE_FILE stop || exit $?
    fi
fi

echo "Checking if group %{group_name} exists..."
EXISTING_GROUP="`grep %{group_name} /etc/group | awk -F ':' '{ print $1 }' 2>/dev/null`"

if [ "$EXISTING_GROUP" != "%{group_name}" ]; then
  echo "Group %{group_name} doesn't exist. Creating ..."
  %{_sbindir}/groupadd -r %{group_name} || exit $?
else
  echo "Group %{group_name} exists."
fi

echo "Checking if user %{username} exists..."
EXISTING_USER=`id -nu %{username} 2>/dev/null`
if [ "$EXISTING_USER" != "%{username}" ]; then
  echo "User %{username} doesn't exist. Creating ..."
  %__mkdir_p %{user_home}
  %{_sbindir}/useradd %{username} -g %{username} -d %{user_home} || exit $?
else
  echo "User %{username} exists."
fi

# Cleaning the artifactory webapp and work folder

echo "Removing tomcat work directory"
if [ -d %{tomcat_home}/work ]; then
  %__rm -rf %{tomcat_home}/work || exit $?
fi

if [ -d %{tomcat_home}/webapps/%{name} ]; then
  echo "Removing Artifactory's exploded WAR directory"
  %__rm -rf %{tomcat_home}/webapps/%{name} || exit $?
fi
exit 0

%post

if [ "$1" = "1" ]; then
  echo "Adding the artifactory service to auto-start"
  /sbin/chkconfig --add %{name} || $?

  echo
  echo "The installation of Artifactory has completed successfully."
  echo
  echo "PLEASE NOTE: You can recover a backup done with Artifactory RPM 2.6.0 and above using '/opt/artifactory/bin/recover.backup.sh'."
  echo "PLEASE NOTE: It is highly recommended to use Artifactory in conjunction with MySQL. You can easily configure this setup using '/opt/artifactory/bin/configure.mysql.sh'."
  echo
elif [ "$1" = "2" ]; then
  echo "The upgrade of Artifactory has completed successfully."
fi

%preun
if [ "$1" = "0" ]; then
  # It's an un-installation

  CURRENT_USER=`id -nu`
  if [ "$CURRENT_USER" != "root" ]; then
    echo
    echo "ERROR: Please un-install Artifactory using root."
    echo
    exit 1
  fi

  SERVICE_FILE=%{_sysconfdir}/init.d/%{name}

  if [ -f $SERVICE_FILE ]; then
    SERVICE_STATUS="`$SERVICE_FILE status`"
    if [[ ! "$SERVICE_STATUS" =~ .*[sS]topped.* ]]; then
      echo "Stopping the artifactory service..."
      $SERVICE_FILE stop || exit $?
    fi
  fi

  echo "Removing the artifactory service from auto-start"
  /sbin/chkconfig --del %{name} || exit $?

  # if some files in data move them to a backup folder
  if [ -d "%{user_home}/data" ]; then
    TIMESTAMP=`echo "$(date '+%T')" | tr -d ":"`
    CURRENT_TIME="$(date '+%Y%m%d').$TIMESTAMP"
    BACKUP_DIR="%{user_home}.backup.${CURRENT_TIME}"

    echo "Creating a backup of the artifactory home folder in ${BACKUP_DIR}"
    %__mkdir_p "${BACKUP_DIR}" && \
     %__mv %{etc_dir} "${BACKUP_DIR}/etc" && \
     %__mv %{user_home}/data "${BACKUP_DIR}/data" && \
     %__mv %{log_dir} "${BACKUP_DIR}/logs" && \
     %__rm -rf "${BACKUP_DIR}/data/tmp" && \
     %__rm -rf "${BACKUP_DIR}/data/workspaces" || exit $?
    if [ -e %{tomcat_home}/lib/mysql-connector-java*.jar ]; then
      echo "MySQL connector found"
      %__mv %{tomcat_home}/lib/mysql-connector-java* "${BACKUP_DIR}"|| exit $?
    fi
  fi
fi
exit 0

%postun
if [ "$1" = "0" ]; then
  # It's an un-installation

  echo "Logging off user %{username}"
  %{_bindir}/pkill -KILL -u %{username}

  # Ignoring user folders since the home dir is deleted already by the RPM spec
  echo "Removing user %{username}"
  %{_sbindir}/userdel -r %{username} || exit $?

  EXISTING_GROUP="`grep %{group_name} /etc/group | awk -F ':' '{ print $1 }' 2>/dev/null`"
  if [ "$EXISTING_GROUP" == "%{group_name}" ]; then
    echo "Removing group %{group_name}"
    %{_sbindir}/groupdel %{group_name}
  fi

  %__rm -rf %{artifactory_bin_home} || exit $?
  %__rm -rf %{etc_dir} || exit $?
  %__rm -rf %{log_dir} || exit $?
fi
exit 0

%files
%attr(775,root,root) %config %{_sysconfdir}/init.d/%{name}
%attr(775,root,root) %dir %{artifactory_bin_home}
%attr(775,root,root) %config %{artifactory_bin_home}/bin
%attr(775,root,root) %config %{artifactory_bin_home}/clilib
%attr(775,root,root) %config %{artifactory_bin_home}/misc
%attr(774,root,root) %{artifactory_bin_home}/Artifactory-Third-Parties-Usage.html
%attr(774,root,root) %{artifactory_bin_home}/COPYING
%attr(774,root,root) %{artifactory_bin_home}/COPYING.LESSER
%attr(774,root,root) %{artifactory_bin_home}/README.txt
%attr(775,root,root) %dir %{tomcat_home}
%attr(775,root,root) %config %{tomcat_home}/bin
%attr(775,root,root) %config %{tomcat_home}/conf
%attr(775,root,root) %{tomcat_home}/lib
%attr(775,root,root) %{tomcat_home}/logs
%attr(775,root,root) %{tomcat_home}/temp
%attr(775,root,root) %{tomcat_home}/work
%attr(775,root,root) %{tomcat_home}/webapps
%attr(774,root,root) %{tomcat_home}/LICENSE
%attr(774,root,root) %{tomcat_home}/NOTICE
%attr(774,root,root) %{tomcat_home}/RELEASE-NOTES
%attr(774,root,root) %{tomcat_home}/RUNNING.txt

%defattr(770,%{username}, %{group_name}, -)
%{pid_dir}
%dir %{user_home}
%dir %{etc_dir}
%{log_dir}
%config(noreplace) %{etc_dir}/default
%config(noreplace) %{etc_dir}/artifactory.system.properties
%config %{etc_dir}/logback.xml
%config %{etc_dir}/mimetypes.xml
%config(noreplace) %{etc_dir}/repo
%{user_home}/bin
%{user_home}/clilib
%{user_home}/etc
%{user_home}/logs
%{user_home}/misc
%{user_home}/temp
%{user_home}/tomcat
%{user_home}/work
%{user_home}/webapps

%doc
