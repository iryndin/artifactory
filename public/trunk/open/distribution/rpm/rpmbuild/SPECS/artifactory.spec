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

%define username artifactory
%define group_name artifactory
%define extracted_standalone %{_sourcedir}/*artifactory*
%define extracted_tomcat %{extracted_standalone}/tomcat

%define _rpmfilename %{filename_prefix}-%{full_version}.rpm

%define target_jfrog_home /opt/jfrog
%define target_artifactory_install %{target_jfrog_home}/%{name}
%define target_jfrog_doc %{target_jfrog_home}/doc/%{name}-%{full_version}
%define	target_tomcat_home %{target_artifactory_install}/tomcat
%define target_etc_dir /etc%{target_artifactory_install}
%define target_var_dir /var%{target_jfrog_home}
%define target_artifactory_home %{target_var_dir}/%{name}

%description
The best binary repository manager around.

%prep
%setup -q -T -c
%{__unzip} "%{SOURCE0}" -d "%{_sourcedir}"

%build

%install
%__rm -rf %{buildroot}

# Install and copy the files in the install dir (opt) directories
%__install -d "%{buildroot}%{target_jfrog_home}"
%__install -d "%{buildroot}%{target_artifactory_install}/bin"
%__install -d "%{buildroot}%{target_artifactory_install}/misc"
%__install -d "%{buildroot}%{target_tomcat_home}/webapps"

# The war file is in Artifactory not tomcat webapps
%__install -D %{extracted_standalone}/webapps/%{name}.war "%{buildroot}%{target_artifactory_install}/webapps/%{name}.war"

%__cp -r %{extracted_standalone}/bin/* "%{buildroot}%{target_artifactory_install}/bin/"
%__cp -r %{extracted_standalone}/misc/* "%{buildroot}%{target_artifactory_install}/misc/"
rsync -r --exclude 'work' --exclude 'temp'  %{extracted_tomcat}/* "%{buildroot}%{target_tomcat_home}/"
%__cp %{extracted_standalone}/misc/service/setenv.sh "%{buildroot}%{target_tomcat_home}/bin/"

# Copy the etc dir to the from the build dir to the build root (currently contains default script)
%__install -d "%{buildroot}%{target_etc_dir}"

# Copy the contents of the standalone etc to the artifactory etc dir
%__cp -r %{extracted_standalone}/etc/* "%{buildroot}%{target_etc_dir}"
%__install -D %{extracted_standalone}/bin/%{name}.default "%{buildroot}%{target_etc_dir}/default"
%__install -D %{extracted_standalone}/misc/service/%{name} "%{buildroot}%{_sysconfdir}/init.d/%{name}"

# Replace the vars in the init and default scripts
%__sed -r --in-place "s%#export ARTIFACTORY_HOME=.*%export ARTIFACTORY_HOME=%{target_artifactory_home}%g;" "%{buildroot}%{target_etc_dir}/default"
%__sed -r --in-place "s%export TOMCAT_HOME=.*%export TOMCAT_HOME=%{target_tomcat_home}%g;" "%{buildroot}%{target_etc_dir}/default"
%__sed -r --in-place "s/#export ARTIFACTORY_USER=.*/export ARTIFACTORY_USER=%{username}/g;" "%{buildroot}%{target_etc_dir}/default"
%__sed -r --in-place "s%export ARTIFACTORY_PID=.*%export ARTIFACTORY_PID=%{target_var_dir}/run/%{name}.pid%g;" "%{buildroot}%{target_etc_dir}/default"

# Create artifactory home dir (var) and symlinks to install dir (opt)
%__install -d "%{buildroot}%{target_artifactory_home}"
%__install -d "%{buildroot}%{target_artifactory_home}/temp"
%__install -d "%{buildroot}%{target_artifactory_home}/work"
%__install -d "%{buildroot}%{target_var_dir}/run"

# Link the folders
%__ln_s "%{target_artifactory_install}/webapps" "%{buildroot}%{target_artifactory_home}/webapps"
%__ln_s "%{target_artifactory_home}/temp" "%{buildroot}%{target_tomcat_home}/temp"
%__ln_s "%{target_artifactory_home}/work" "%{buildroot}%{target_tomcat_home}/work"
%__ln_s "/etc/opt/jfrog/artifactory" "%{buildroot}/var/opt/jfrog/artifactory/etc"
%__ln_s "%{target_artifactory_install}/misc" "%{buildroot}%{target_artifactory_home}/misc"
%__ln_s "%{target_artifactory_install}/tomcat" "%{buildroot}%{target_artifactory_home}/tomcat"

# log directories installation
%__install -d "%{buildroot}%{target_artifactory_home}/logs/catalina"
%__ln_s "%{target_artifactory_home}/logs/catalina" "%{buildroot}%{target_tomcat_home}/logs"

# Fill the documentation
%__install -d "%{buildroot}%{target_jfrog_doc}"
%__cp %{extracted_standalone}/Third-Parties-Usage-About-Box.html "%{buildroot}%{target_jfrog_doc}"
%__cp %{extracted_standalone}/COPYING "%{buildroot}%{target_jfrog_doc}"
%__cp %{extracted_standalone}/COPYING.LESSER "%{buildroot}%{target_jfrog_doc}"
%__cp %{extracted_standalone}/README.txt "%{buildroot}%{target_jfrog_doc}"

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
    if [ -z "`grep "%{target_etc_dir}/default" "$SERVICE_FILE"`" ]; then
        echo "ERROR: Currently installed Artifactory version does not have the same layout than this RPM!"
        echo "NOTE: To upgrade follow these instructions:"
        echo "NOTE: - Uninstall the previous RPM (rpm -e artifactory),"
        echo "NOTE: - Then install this one (rpm -i this_rpm),"
        echo "NOTE: - And finally recover from backup (/opt/jfrog/artifactory/bin/recover.backup.sh)"
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
EXISTING_GROUP="`grep "%{group_name}:" /etc/group | awk -F ':' '{ print $1 }' 2>/dev/null`"

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
  %__mkdir_p %{target_artifactory_home}
  %{_sbindir}/useradd %{username} -g %{username} -d %{target_artifactory_home} || exit $?
else
  echo "User %{username} exists."
fi

# Cleaning the artifactory webapp and work folder

echo "Removing tomcat work directory"
if [ -d %{target_tomcat_home}/work ]; then
  %__rm -rf %{target_tomcat_home}/work || exit $?
fi

if [ -d %{target_tomcat_home}/webapps/%{name} ]; then
  echo "Removing Artifactory's exploded WAR directory"
  %__rm -rf %{target_tomcat_home}/webapps/%{name} || exit $?
fi
exit 0

%post

if [ "$1" = "1" ]; then
  echo "Adding the artifactory service to auto-start"
  /sbin/chkconfig --add %{name} || $?

  echo
  echo "The installation of Artifactory has completed successfully."
  echo
  echo "PLEASE NOTE: You can recover a backup done with Artifactory RPM 3.0 and above using '/opt/jfrog/artifactory/bin/recover.backup.sh'. For upgrading from previous version of Artifactory please refer to the wiki http://wiki.jfrog.org/confluence/display/RTF/Upgrading+Artifactory"
  echo "PLEASE NOTE: It is highly recommended to use Artifactory in conjunction with MySQL. You can easily configure this setup using '/opt/jfrog/artifactory/bin/configure.mysql.sh'."
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

  # Create backups
  echo "Creating a backup of the artifactory home folder in ${BACKUP_DIR}"
  TIMESTAMP=`echo "$(date '+%T')" | tr -d ":"`
  CURRENT_TIME="$(date '+%Y%m%d').$TIMESTAMP"
  BACKUP_DIR="%{target_var_dir}/%{name}.backup.${CURRENT_TIME}"

  %__mkdir_p "${BACKUP_DIR}" && \
  %__mv %{target_etc_dir} "${BACKUP_DIR}/etc" && \
  %__mv %{target_artifactory_home}/logs "${BACKUP_DIR}/logs" || exit $?

  if [ -d "%{target_artifactory_home}/data" ]; then
    %__rm -rf "%{target_artifactory_home}/data/tmp" && \
    %__rm -rf "%{target_artifactory_home}/data/work" || exit $?

    if [ $(stat -c "%d" %{target_artifactory_home}/data/) -eq $(stat -c "%d" ${BACKUP_DIR}) ]; then
      echo "Backup %{target_artifactory_home}/data to ${BACKUP_DIR}/data"
      %__mv %{target_artifactory_home}/data "${BACKUP_DIR}/data" || exit $?
    else
      echo "PLEASE NOTE: Skipped creating a backup of the Artifactory data folder because source and target are not in the same drive [%{target_artifactory_home}/data, ${BACKUP_DIR}/data/]"
      %__cp -pr %{target_artifactory_home}/data ${BACKUP_DIR}/data
    fi
  fi

  if [ -e %{target_tomcat_home}/lib/mysql-connector-java*.jar ]; then
    echo "MySQL connector found"
    %__mv %{target_tomcat_home}/lib/mysql-connector-java* "${BACKUP_DIR}" || exit $?
  fi
  if [ -e %{target_artifactory_home}/backup ]; then
    %__mv %{target_artifactory_home}/backup "${BACKUP_DIR}/backup" || exit $?
  fi
fi

exit 0

%postun
if [ "$1" = "0" ]; then
  # It's an un-installation

  echo "Logging off user %{username}"
  %{_bindir}/pkill -KILL -u %{username}

  %__rm -rf %{target_artifactory_home}/work/* || exit $?

  # Ignoring user folders since the home dir is deleted already by the RPM spec
  echo "Removing user %{username}"
  %{_sbindir}/userdel -r %{username} 2>/dev/null || echo $?

  EXISTING_GROUP="`grep %{group_name} /etc/group | awk -F ':' '{ print $1 }' 2>/dev/null`"
  if [ "$EXISTING_GROUP" == "%{group_name}" ]; then
    echo "Removing group %{group_name}"
    %{_sbindir}/groupdel %{group_name}
  fi

  %__rm -rf %{target_artifactory_install} || exit $?
  %__rm -rf %{target_etc_dir} || exit $?
  %__rm -rf %{target_jfrog_doc} || exit $?

  # Do not remove /var/opt/jfrog always keep it
  # Then remove /etc/opt/jfrog /opt/jfrog/doc and /opt/jfrog if empty
  [ -z "`ls /etc%{target_jfrog_home}/`" ] && %__rm -r "/etc%{target_jfrog_home}"
  [ -z "`ls %{target_jfrog_home}/doc`" ] && %__rm -r "%{target_jfrog_home}/doc"
  [ -z "`ls %{target_jfrog_home}/`" ] && %__rm -r "%{target_jfrog_home}"
fi
exit 0

%files
%attr(775,root,root) %config %{_sysconfdir}/init.d/%{name}
%attr(775,root,root) %dir %{target_jfrog_home}
%attr(775,root,root) %config %{target_artifactory_install}/bin
%attr(775,root,root) %config %{target_artifactory_install}/misc
%attr(775,root,root) %config %{target_artifactory_install}/webapps
%attr(774,root,root) %{target_jfrog_doc}/Third-Parties-Usage-About-Box.html
%attr(774,root,root) %{target_jfrog_doc}/COPYING
%attr(774,root,root) %{target_jfrog_doc}/COPYING.LESSER
%attr(774,root,root) %{target_jfrog_doc}/README.txt
%attr(775,root,root) %dir %{target_tomcat_home}
%attr(775,root,root) %config %{target_tomcat_home}/bin
%attr(775,root,root) %config %{target_tomcat_home}/conf
%attr(775,root,root) %{target_tomcat_home}/lib
%attr(775,root,root) %{target_tomcat_home}/logs
%attr(775,root,root) %{target_tomcat_home}/temp
%attr(775,root,root) %{target_tomcat_home}/work
%attr(775,artifactory,artifactory) %{target_artifactory_home}
%attr(775,artifactory,artifactory) %{target_tomcat_home}/webapps
%attr(774,root,root) %{target_tomcat_home}/LICENSE
%attr(774,root,root) %{target_tomcat_home}/NOTICE
%attr(774,root,root) %{target_tomcat_home}/RELEASE-NOTES
%attr(774,root,root) %{target_tomcat_home}/RUNNING.txt

%defattr(770,%{username}, %{group_name}, -)
%{target_var_dir}/run
%dir %{target_etc_dir}
%config(noreplace) %{target_etc_dir}/artifactory.system.properties
%config(missingok) %{target_etc_dir}/artifactory.config.xml
%config %{target_etc_dir}/default
%config %{target_etc_dir}/logback.xml
%config %{target_etc_dir}/mimetypes.xml
#%config(noreplace) %{target_etc_dir}/db

%doc
