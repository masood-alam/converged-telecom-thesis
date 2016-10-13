# Telecom Application Server (TAS)
# JBOSS 5.1.0.GA

#SETUP

Virtual machine running CentOS 6.2

create NAT, inet and hostonly Network interfaces

configure inet ip=192.168.70.1, hostonly ip=192.168.56.101

Install jdk 1.8.0_101, Maven 3.3.3, Ant 1.9.4

Download restcomm jain-slee from https://github.com/RestComm/jain-slee/releases/download/2.8.0.FINAL/restcomm-slee-2.8.36.73.zip

extract in /opt  (unzip restcomm-slee-2.8.36.73.zip)

update ~/.bashrc

export JBOSS_HOME=/opt/restcomm-slee-2.8.36.73/jboss-5.1.0.GA

export PATH=$JBOSS_HOME:PATH

extract mobicents-gmlc   (tar xvzf mobicents-gmlc-1.0.0-SNAPSHOT.tar.gz)

start jboss  (run.sh -b 0.0.0.0)

