CREATE DATABASE artdb CHARACTER SET utf8;
GRANT ALL ON artdb.* TO 'artifactory'@'localhost' IDENTIFIED BY 'password';
FLUSH PRIVILEGES;