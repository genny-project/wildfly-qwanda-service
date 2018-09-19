#!/bin/bash
#cd qwanda-service-war this it is not need the parent pom will do it 
#mvn clean install we only need the package so no need to install on the local repository
# even for jenkins
#mvn eclipse:eclipse the maven eclipse plugin it is already retired http://maven.apache.org/plugins/maven-eclipse-plugin/eclipse-mojo.html
# this maven eclipse:eclipse it is causing some conflicts for running the build.sh (Sudan Jay and Alwy)
mvn clean  package 
