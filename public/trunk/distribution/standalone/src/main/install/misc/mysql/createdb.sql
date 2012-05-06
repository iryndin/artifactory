create database artifactory character set utf8;
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER,INDEX on artifactory.* TO 'artifactory_user'@'localhost' IDENTIFIED BY 'password';
flush privileges;

