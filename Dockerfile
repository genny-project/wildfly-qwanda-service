FROM gennyproject/wildfly:latest 

USER root

RUN env

ENV PROJECT wildfly-qwanda-service
ADD gennyCredentials /root/.credentials/sheets.googleapis.com-java-quickstart/StoredCredential
ADD gennyCredentials /root/.credentials/genny/StoredCredential
ADD gennyC /root/.credentials/sheets.googleapis.com-java-quickstart/StoredCredential
ADD channel /root/.credentials/channel/StoredCredential
USER root

RUN mkdir /opt/realm
RUN mkdir /opt/jboss/wildfly/realm
RUN mkdir /realm

ADD realm /opt/realm
ADD google $JBOSS_HOME/google
ADD docker-entrypoint2.sh /opt/jboss/

#USER jboss
USER root
EXPOSE 8998
EXPOSE 8787


HEALTHCHECK --interval=10s --timeout=3s --retries=15 CMD curl -f / http://localhost:8080/version || exit 1 

ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
CMD ["-b", "0.0.0.0"]
RUN touch $JBOSS_HOME/standalone/deployments/$PROJECT.war.dodeploy
ADD target/$PROJECT $JBOSS_HOME/standalone/deployments/$PROJECT.war
#RUN chown -R root:root $JBOSS_HOME/standalone/deployments/$PROJECT.war
#RUN chmod -Rf 777 $JBOSS_HOME/standalone/deployments/$PROJECT.war
