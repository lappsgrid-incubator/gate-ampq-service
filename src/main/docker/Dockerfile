FROM openjdk:8
RUN apt update && apt install -y netcat
RUN useradd -ms /bin/bash service
COPY gate-ampq-service.jar /home/service
COPY start.sh /home/service
RUN chown -R service:service /home/service
RUN chmod +x /home/service/start.sh

WORKDIR /home/service
USER service
CMD ./start.sh
#CMD ["--mailbox", "unknown", "--gate", "/gate", "--server", "localhost", "--username", "guest", "--password", "guest", "--exchange", "services", "--logdir", "/var/log" ]
#CMD ["java", "-Xmx1G", "-jar", "service.jar","--mailbox", "unknown", "--gate", "/gate", "--server", "host.docker.internal", "--username", "guest", "--password", "guest", "--exchange", "services", "--logdir", "/var/log" ]