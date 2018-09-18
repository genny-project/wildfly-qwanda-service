#!/bin/bash
cd qwanda-service-war
mvn clean install
cd ..
mvn clean  package 
mvn eclipse:eclipse
