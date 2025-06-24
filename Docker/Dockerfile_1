FROM alpine

RUN apk update && apk add openjdk17 wget tar bash

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN wget https://downloads.apache.org/tomcat/tomcat-11/v11.0.8/bin/apache-tomcat-11.0.8.tar.gz && \
    tar -xvzf apache-tomcat-11.0.8.tar.gz && \
    mv apache-tomcat-11.0.8 /opt/tomcat && \
    rm apache-tomcat-11.0.8.tar.gz

WORKDIR /opt/tomcat

# Corrected: direct WAR download and proper saving
RUN wget -O webapps/myapp.war https://github.com/lerndevops/code/raw/main/myapp.war

EXPOSE 8080

CMD ["/opt/tomcat/bin/catalina.sh", "run"]

