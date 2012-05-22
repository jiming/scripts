#!/bin/bash

echo --------------------------------------
echo Updating                    [$tomcat]
echo Keeping previous version in [$backup]
echo --------------------------------------

echo  ========== [1] - Downloading ==========
echo   "##teamcity[progressMessage 'Downloading']"

rm -rf teamcity
mkdir  teamcity
cd     teamcity
wget   -nv ftp://ftp.intellij.net/pub/.teamcity/nightly/*.war

tcBuild=`ls *.war | cut -f 1 -d '.'`

echo  "##teamcity[progressMessage 'Updating to |[$tcBuild|]']"
echo  ========== [2] - Unpacking ==========

unzip *.war
rm    *.war
cd    ..

echo  ========== [3] - Stopping Tomcat ==========

$tomcat/bin/shutdown.sh
sleep 20
rm -rf $tomcat/logs $tomcat/temp $tomcat/work 
mkdir  $tomcat/logs $tomcat/temp $tomcat/work 

echo  ========== [4] - Moving TeamCity ==========

rm    -rf                   $backup/teamcity
mkdir -p                    $backup
mv $tomcat/webapps/teamcity $backup
mv teamcity                 $tomcat/webapps

echo  ========== [5] - Killing remaining Tomcat process ==========

echo  [`ps -AF | grep java | grep org.apache.catalina.startup.Bootstrap`]
kill   `ps -AF | grep java | grep org.apache.catalina.startup.Bootstrap | awk '{print $2}'`

echo  ========== [6] - Starting Tomcat ==========

$tomcat/bin/startup.sh

echo  ========== [6] - Tomcat started, sleeping for 2 minutes ==========

sleep 120

echo  ========== [7] - Listing Tomcat log file ==========

cat $tomcat/logs/catalina.out

echo  ========== [8] - Listing memory and disk usage ==========

echo -------
echo Memory:
echo -------

free -lt

echo -----
echo Disk:
echo -----

df -hl

echo  ========== [9] - Done! ==========
echo  "##teamcity[buildStatus status='SUCCESS' text='Updated to |[$tcBuild|]']"
