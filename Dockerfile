ARG CODE_VERSION=latest
FROM gennyproject/wildfly:${CODE_VERSION}
RUN apk update && apk add git
RUN mv /usr/glibc-compat/lib/ld-linux-x86-64.so.2 /usr/glibc-compat/lib/ld-linux-x86-64.so
RUN ln -s /usr/glibc-compat/lib/ld-linux-x86-64.so /usr/glibc-compat/lib/ld-linux-x86-64.so.2

USER root

ADD docker-entrypoint.sh /opt/jboss/docker-entrypoint.sh
ADD docker-entrypoint2.sh /opt/jboss/

EXPOSE 8080
#RUN mkdir /opt/realm
#RUN mkdir /opt/jboss/wildfly/realm
#RUN mkdir /realm

ENTRYPOINT [ "/opt/jboss/docker-entrypoint2.sh" ]
#ADD realm /opt/realm
ADD qwanda-service-ear/target/qwanda-service-ear.ear $JBOSS_HOME/standalone/deployments/qwanda-service-ear.ear
