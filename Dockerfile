FROM gennyproject/wildfly:latest 

USER root
RUN set -x \
    && apt-get update --quiet \
    && apt-get install --quiet --yes --no-install-recommends jq sed  iputils-ping vim sed \
    && apt-get clean

RUN ln -s /bin/sed /usr/bin/sed
RUN chmod a+x /usr/bin/sed

RUN env

ENV PROJECT wildfly-qwanda-service
ADD target/$PROJECT $JBOSS_HOME/standalone/deployments/$PROJECT.war
#ADD gennyCredentials /root/.credentials/sheets.googleapis.com-java-quickstart/StoredCredential
ADD gennyCredentials /root/.credentials/genny/StoredCredential
ADD channel /root/.credentials/channel/StoredCredential
RUN touch $JBOSS_HOME/standalone/deployments/$PROJECT.war.dodeploy
USER root
RUN chown -R jboss:jboss $JBOSS_HOME/standalone/deployments/$PROJECT.war
RUN chmod -Rf 777 $JBOSS_HOME/standalone/deployments/$PROJECT.war

RUN mkdir /opt/realm
RUN mkdir /opt/jboss/wildfly/realm
RUN mkdir /realm

ADD realm /opt/realm
ADD google $JBOSS_HOME/google
ADD docker-entrypoint2.sh /opt/jboss/

USER jboss
USER root
EXPOSE 8998

ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
CMD ["-b", "0.0.0.0"]
