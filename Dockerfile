FROM gennyproject/wildfly:latest

RUN env

ENV PROJECT wildfly-qwanda-service
ADD target/$PROJECT $JBOSS_HOME/standalone/deployments/$PROJECT.war
#ADD gennyCredentials /root/.credentials/sheets.googleapis.com-java-quickstart/StoredCredential
ADD gennyCredentials /root/.credentials/genny/StoredCredential
RUN touch $JBOSS_HOME/standalone/deployments/$PROJECT.war.dodeploy
USER root
RUN chown -R jboss:jboss $JBOSS_HOME/standalone/deployments/$PROJECT.war
RUN chmod -Rf 777 $JBOSS_HOME/standalone/deployments/$PROJECT.war

ADD realm $JBOSS_HOME/realm
ADD google $JBOSS_HOME/google

USER jboss
USER root
EXPOSE 8998

