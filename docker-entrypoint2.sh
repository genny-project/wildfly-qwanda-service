#!/bin/bash
echo "Setting up Realms ****"

#KEYCLOAK_JSON_DIR=/opt/jboss/wildfly/realm
#KEYCLOAK_ORIGINAL_JSON_DIR=/opt/realm
# copy all the keycloak files so they may be modified
#cp -rf ${KEYCLOAK_ORIGINAL_JSON_DIR}/* ${KEYCLOAK_JSON_DIR}/

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


#for i in `ls ${KEYCLOAK_JSON_DIR}` ; do
#if grep -r localhost ${KEYCLOAK_JSON_DIR}/${i}
#then
#   OLD_LINE_KEY="auth-server-url"
#   NEW_LINE="\"auth-server-url\": \"${KEYCLOAKURL}/auth\","
#   change_line "\${OLD_LINE_KEY}" "\${NEW_LINE}" "\${KEYCLOAK_JSON_DIR}\/\${i}"
#fi

#done
myip=
while IFS=$': \t' read -a line ;do
    [ -z "${line%inet}" ] && ip=${line[${#line[1]}>4?1:2]} &&
        [ "${ip#127.0.0.1}" ] && myip=$ip
  done< <(LANG=C /sbin/ifconfig eth0)

export IPMY=$myip


#/opt/jboss/wildfly/bin/jboss-cli.sh -c --file=/opt/jboss/wildfly/bin/adapter-elytron-install-offline.cli

if [ $ADMIN_USERNAME ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/wildfly/bin/add-user.sh  $ADMIN_USERNAME  $ADMIN_PASSWORD
fi

if [ $XMX ] ; then
  echo "JVM XMX = "$XMX
  sed -i 's,-Xmx512m,-Xmx'"$XMX"',g' /opt/jboss/wildfly/bin/standalone.conf
  export JAVA_OPTS="${JAVA_OPTS} -Xmx$XMX "
fi

    /opt/jboss/wildfly/bin/add-user.sh  jmsuser jmspassword1

rm -Rf /opt/jboss/wildfly/data/*

#/subsystem=transactions:write-attribute(name=node-identifier,value=MyNode)

if [[ $DEBUG == "TRUE" ]]; then
   echo "Remote Debug on port 8787 True";
   export JAVA_OPTS="${JAVA_OPTS}   -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=${DEBUG_SUSPEND:=n}  -Drebel.remoting_plugin=true "
   /opt/jboss/wildfly/bin/standalone.sh --debug  -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -DHIBERNATE_SHOW_SQL=${HIBERNATE_SHOW_SQL:=false} -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''  -b 0.0.0.0 -Dresteasy.preferJacksonOverJsonB -Djboss.tx.node.id=${hostname} -Djboss.as.management.blocking.timeout=10000 --server-config=standalone-full-ha.xml
else
   echo "Debug is False";
   /opt/jboss/wildfly/bin/standalone.sh -Djboss.bind.address.private=$myip  -bmanagement=0.0.0.0 -b 0.0.0.0 -Dadmin.username=${ADMIN_USERNAME} -Dadmin.password=${ADMIN_PASSWORD} -Dpublic.host=${myip}  -Dresteasy.preferJacksonOverJsonB  -Djboss.tx.node.id=${hostname} -Djboss.as.management.blocking.timeout=10000  --server-config=standalone-full-ha.xml
 #  -DHIBERNATE_SHOW_SQL=$HIBERNATE_SHOW_SQL -DHIBERNATE_HBM2DDL=$HIBERNATE_HBM2DDL -DMYSQL_USER=$MYSQL_USER -DMYSQL_PASSWORD=$MYSQL_PASSWORD -Djava.security.auth.login.config=''
fi
