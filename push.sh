#!/bin/bash
if [ -z "${1}" ]; then
   version="latest"
else
   version="${1}"
fi

sudo docker push gennyproject/wildfly-qwanda-service:"${version}"
sudo docker tag gennyproject/wildfly-qwanda-service:"${version}" gennyproject/wildfly-qwanda-service:latest
sudo docker push gennyproject/wildfly-qwanda-service:latest
