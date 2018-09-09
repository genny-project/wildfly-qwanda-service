#FROM jboss/wildfly:13.0.0.Final
FROM gennyproject/wildfly:latest 


USER root

#RUN yum install net-tools -y

ADD docker-entrypoint.sh /opt/jboss/docker-entrypoint.sh
ADD docker-entrypoint2.sh /opt/jboss/

EXPOSE 8080
RUN mkdir /opt/realm
RUN mkdir /opt/jboss/wildfly/realm
RUN mkdir /realm


ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
ADD realm /opt/realm
ADD persistent-process-ear/target/persistent-process-ear.ear $JBOSS_HOME/standalone/deployments/persistent-process-ear.ear
