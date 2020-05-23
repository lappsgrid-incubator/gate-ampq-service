#!/bin/sh

cd /home/service

MAILBOX=${MAILBOX:-gate.ampq.service}
GATE=${GATE:-/gate}
XGAPP=${XGAPP:-application.xgapp}
RABBIT=${RABBIT:-host.docker.internal}
USER=${USER:-guest}
PASS=${PASS:-guest}
EXCHANGE=${EXCHANGE:-services}
LOGDIR=${LOGDIR:-.}
LOGFILE=${LOGFILE:-ampq-service}
THREADS=${THREADS:-2}

echo "Waiting for the RabbitMQ service to become available"
waiting=1
while [ $waiting -eq 1 ] ; do
    sleep 1
    nc -z $RABBIT 5672
    if [ $? = 0 ] ; then
        waiting=0
    fi
done

echo "RabbitMQ is online. Starting the AMPQ service for $MAILBOX"
java -jar gate-ampq-service.jar -g $GATE/$XGAPP -m $MAILBOX -s $RABBIT -u $USER -p $PASS -l $LOGDIR -f $LOGFILE -t $THREADS
