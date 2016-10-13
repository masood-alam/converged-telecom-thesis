# Mobicents GMLC

Updated at http://github.com/Restcomm/GMLC

version 1.0.0-SNAPSHOT for use in thesis

modified to work with two SBB, one for MAP RA, other for HTTP Servlet RA

#SETUP

Download restcomm jain-slee from https://github.com/RestComm/jain-slee/releases/download/2.8.0.FINAL/restcomm-slee-2.8.36.73.zip

extract in /opt  (unzip restcomm-slee-2.8.36.73.zip)

update ~/.bashrc

export JBOSS_HOME=/opt/restcomm-slee-2.8.36.73/jboss-5.1.0.GA

export PATH=$JBOSS_HOME:PATH

extract mobicents-gmlc   (tar xvzf mobicents-gmlc-1.0.0-SNAPSHOT.tar.gz)

install into TAS  (mvn install)

Copy xml configuration files for JSS7

cp mobicents-gmlc-1.0.0-SNAPSHOT/core/bootstrap/config/data/*.xml $JBOSS_HOME/server/default/data

start jboss  (run.sh -b 0.0.0.0)

