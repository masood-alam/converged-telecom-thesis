# telecom communications service provider setup
# on linux virtual machine running CentOS-6.2
 create "NAT" and "inet" network interface on eth0 and eth1
 configure "eth1 network interface with ip address 192.168.70.1
 Download dpklnx.Z from https://www.dialogic.com/files/DSI/developmentpackages/linux/dpklnx.Z
 extract in folder /opt/dsi  (tar -xzf dpklnx.Z)
 update /etc/ld.so.conf  with /opt/dsi/32
 execute ldconfig -v
 install lksctp-tools  (yum install lksctp-tools)

 copy system.txt, config.txt, mtr_hlr and mtr_msc into /opt/dsi
 update permissions for executable

 run  (./gctload -d )


