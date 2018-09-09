#!/bin/bash
myip=
while IFS=$': \t' read -a line ;do
    [ -z "${line%inet}" ] && ip=${line[${#line[1]}>4?1:2]} &&
        [ "${ip#127.0.0.1}" ] && myip=$ip
  done< <(LANG=C /sbin/ifconfig eth0)

export IPMY=$myip


if [ $ADMIN_USERNAME ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/wildfly/bin/add-user.sh  $ADMIN_USERNAME  $ADMIN_PASSWORD
fi

if [ $XMX ] ; then
  echo "JVM XMX = "$XMX
  sed -i 's,-Xmx512m,-Xmx'"$XMX"',g' /opt/jboss/wildfly/bin/standalone.conf
fi

    /opt/jboss/wildfly/bin/add-user.sh  jmsuser jmspassword1

if [[ $DEBUG == "TRUE" ]]; then
   echo "Remote Debug on port 8787 True";
   export JAVA_OPTS="${JAVA_OPTS}   -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=${DEBUG_SUSPEND:=n}  -Drebel.remoting_plugin=true "
   /opt/jboss/wildfly/bin/standalone.sh --debug  -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -DHIBERNATE_SHOW_SQL=${HIBERNATE_SHOW_SQL:=false} -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''  -b 0.0.0.0
#--server-config=standalone-full-ha.xml
else
   echo "Debug is False";
   /opt/jboss/wildfly/bin/standalone.sh -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}
 #  -DHIBERNATE_SHOW_SQL=$HIBERNATE_SHOW_SQL -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''
fi
