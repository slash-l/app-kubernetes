FROM demo.jfrogchina.com/slash-docker-virtual/docker-framework:latest

LABEL MAINTAINER="Slash"

RUN rm -rf /home/exec/tomcat/webapps/*

ADD app/*.jar /home/exec/tomcat/webapps/ROOT.jar
CMD /bin/bash -c cd /home/exec; /home/exec/tomcat/bin/catalina.sh run
