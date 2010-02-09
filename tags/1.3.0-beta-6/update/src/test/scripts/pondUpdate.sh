#!/bin/sh

cd /opt/artifactory

version="1.3.0-beta-6"

rm -rf artifactory-$version
rm -rf artifactory-cli-$version
unzip ~bartender/work/artifactory/1.3.0-final/standalone/target/artifactory-1.3.0-beta-6.zip
unzip ~bartender/work/artifactory/1.3.0-final/cli/target/artifactory-cli-1.3.0-beta-6.zip

rm -f cli
rm -f current

ln -s artifactory-$version/ current
ln -s artifactory-cli-$version/ cli

cd current

mkdir work
chown artifactory.apache -R .

rm -rf logs
mv etc etc.orig
ln -s /var/log/artifactory/ logs
ln -s /backup/artifactory/ backup
ln -s /srv/artifactory/data/ data
ln -s /etc/artifactory/ etc

#rm -rf /srv/artifactory/data/*
rm -rf /opt/tomcat/artifactory/webapps/artifactory /opt/tomcat/artifactory/work/Catalina/localhost/artifactory

service artifactory start

tail -200f /var/log/artifactory/catalina.out

