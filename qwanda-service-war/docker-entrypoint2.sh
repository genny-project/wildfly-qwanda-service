#!/bin/bash
echo "Setting up Realms ****"

KEYCLOAK_JSON_DIR=/opt/jboss/wildfly/realm
KEYCLOAK_ORIGINAL_JSON_DIR=/opt/realm
# copy all the keycloak files so they may be modified
cp -rf ${KEYCLOAK_ORIGINAL_JSON_DIR}/* ${KEYCLOAK_JSON_DIR}/

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


for i in `ls ${KEYCLOAK_JSON_DIR}` ; do
if grep -r localhost ${KEYCLOAK_JSON_DIR}/${i} 
then
   OLD_LINE_KEY="auth-server-url"
   NEW_LINE="\"auth-server-url\": \"${KEYCLOAKURL}/auth\","
   change_line "\${OLD_LINE_KEY}" "\${NEW_LINE}" "\${KEYCLOAK_JSON_DIR}\/\${i}"
fi

done

#hack

#Set some ENV by extracting from keycloak.json file
#export KEYCLOAK_REALM=`jq '.realm' /realm/keycloak.json`
#export KEYCLOAK_URL=`jq '.["auth-server-url"]' /realm/keycloak.json`
#export KEYCLOAK_CLIENTID=`jq '.resource' /realm/keycloak.json`
#export KEYCLOAK_SECRET=`jq '.secret' /realm/keycloak.json`

#echo "KEYCLOAK REALM= ${KEYCLOAK_REALM}"
#echo "KEYCLOAK URL= ${KEYCLOAK_URL}"
#echo "KEYCLOAK CLIENTID= ${KEYCLOAK_CLIENTID}"

/opt/jboss/docker-entrypoint.sh -b 0.0.0.0

