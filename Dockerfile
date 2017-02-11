from java:8

RUN mkdir /usr/src/myapp

COPY application.properties /usr/src/myapp/
COPY target/media-organizer.jar /usr/src/myapp/

WORKDIR /usr/src/myapp

CMD ["java", "-jar", "media-organizer.jar"]
