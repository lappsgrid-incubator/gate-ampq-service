FROM openjdk:8
RUN apt update && apt install -y netcat sudo emacs
RUN useradd -ms /bin/bash gate
COPY gate-ampq-service.jar /home/gate
COPY start.sh /home/gate
COPY java.sh /etc/profile.d/
RUN chown -R gate:gate /home/gate
RUN chmod +x /home/gate/start.sh
RUN mkdir /gate

WORKDIR /home/gate
CMD /home/gate/start.sh
#CMD ["--mailbox", "unknown", "--gate", "/gate", "--server", "localhost", "--username", "guest", "--password", "guest", "--exchange", "services", "--logdir", "/var/log" ]
#CMD ["java", "-Xmx1G", "-jar", "service.jar","--mailbox", "unknown", "--gate", "/gate", "--server", "host.docker.internal", "--username", "guest", "--password", "guest", "--exchange", "services", "--logdir", "/var/log" ]