FROM eclipse-temurin:17-jdk-alpine 
VOLUME /tmp 
COPY  demo/build/libs/demo-0.0.1-SNAPSHOT.jar app.jar 
ENTRYPOINT [ "java", "-jar", "/app.jar" ] 
EXPOSE 8080