# telecom communications service provider setup
# using Dialogic DSI Protocol Stack

Virtual Machine running CentOS 6.2

create "NAT" and "inet" network interface on eth0 and eth1

configure internal interface eth1 ip=192.168.70.2

Download dpklnx.Z from https://www.dialogic.com/files/DSI/developmentpackages/linux/dpklnx.Z

extract in folder /opt/dsi  (tar -xzf dpklnx.Z)

create symbolic links  (ln -s /opt/dsi/32/libgctlib.so.1.55.0 /opt/dsi/32/libgctlib.so.1

update /etc/ld.so.conf  with /opt/dsi/32

execute ldconfig -v

install lksctp-tools  (yum install lksctp-tools)

copy system.txt, config.txt, mtr_hlr and mtr_msc into /opt/dsi

update permissions for executable

run  (./gctload -d )


