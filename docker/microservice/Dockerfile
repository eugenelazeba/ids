FROM @docker.registry@/generic-spring-boot-microservice
LABEL fat.jar.name=@fat-jar.name@.jar
LABEL project.groupId=@project.groupId@
LABEL project.artifactId=@project.artifactId@
LABEL project.version=@project.version@
ADD @fat-jar.name@.jar app.jar
RUN sh -c 'touch /app.jar'