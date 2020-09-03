#!/bin/bash
echo "Setting up Realms ****"

unset JAVA_OPTS

# change the package.json file
function escape_slashes {
    /usr/bin/sed 's/\//\\\//g'
}

function change_line {
    eval OLD_LINE_PATTERN="$1"
    eval NEW_LINE="$2"
    eval FILE="$3"

    local NEW=$(echo "${NEW_LINE}" | escape_slashes)
    /usr/bin/sed -i  '/'"${OLD_LINE_PATTERN}"'/s/.*/'"${NEW}"'/' "${FILE}"
}


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

if [ $XMS ] ; then
    echo "JVM XMS = "$XMS
    sed -i 's,-Xms64m,-Xms'"$XMS"',g' /opt/jboss/wildfly/bin/standalone.conf
fi

sed -i 's,MaxMetaspaceSize=256m,MaxMetaspaceSize=2048m,g' /opt/jboss/wildfly/bin/standalone.conf

rm -Rf /opt/jboss/wildfly/data/*


if [[ $DEBUG == "TRUE" ]]; then
    echo "Remote Debug on port 8787 True";
    /opt/jboss/wildfly/bin/standalone.sh --debug 8787 -Drebel.remoting_plugin=true -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -DHIBERNATE_SHOW_SQL=${HIBERNATE_SHOW_SQL:=false} -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''  -b 0.0.0.0 -Dresteasy.preferJacksonOverJsonB -Djboss.tx.node.id=${hostname} -Djboss.as.management.blocking.timeout=10000 --server-config=standalone-full-ha.xml
else
    echo "Debug is False";
    /opt/jboss/wildfly/bin/standalone.sh -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -Dresteasy.preferJacksonOverJsonB  -Djboss.tx.node.id=${hostname} -Djboss.as.management.blocking.timeout=10000  --server-config=standalone-full-ha.xml
fi
