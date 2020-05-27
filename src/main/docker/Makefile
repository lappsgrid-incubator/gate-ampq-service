JAR=gate-ampq-service.jar
TARGET=../../../target/$(JAR)
REPO=docker.lappsgrid.org
GROUP=gate
NAME=ampq-service
IMAGE=$(REPO)/$(GROUP)/$(NAME)
VERSION=$(shell cat VERSION)

docker:
	if [[ $(TARGET) -nt $(JAR) ]] ; then cp $(TARGET) . ; fi
	docker build -t $(IMAGE) .

start:
	docker run -d --name $(NAME) $(IMAGE)

stop:
	docker rm -f $(NAME)

run:
	docker run -it --rm $(IMAGE) /bin/bash

push:
	docker tag $(IMAGE) $(IMAGE)
	docker push $(IMAGE)

tag:
	docker tag $(IMAGE) $(IMAGE):$(VERSION)
	docker push $(IMAGE):$(VERSION)

