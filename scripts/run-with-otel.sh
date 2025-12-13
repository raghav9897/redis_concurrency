 java \
 -javaagent:../opentelemetry-javaagent.jar \
 -Dserver.port=9191 \
 -Dotel.service.name=my-spring-service \
 -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
 -Dotel.exporter.otlp.protocol=grpc \
 -jar ../build/libs/*SNAPSHOT.jar

