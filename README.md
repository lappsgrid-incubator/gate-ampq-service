# GATE AMPQ Service

A generic framework to expose GATE controllers as services.  This application is inteded to be used with the [gate-ampq-rest-service](https://github.com/lappsgrid-incubator/gate-ampq-rest-service). Although it can be used with any program that can send it messages.

The `gate-ampq-service.jar` can be run from the command line, run from the Docker image `docker.lappsgrid.org/gate/ampq-service`, or included with the GATE controller inside a Docker container.

## Command Line Usage

``` 
$> java -jar gate-ampq-service.jar --mailbox my-service --gate /path/to/application.xgapp
```

This will load the GATE controller from `/path/to/application.xgapp` and create a mailbox (task queue) on the RabbitMQ server named `my-service`.  By default the application will try to connect to a RabbitMQ server on *localhost* using the username *guest* and password *guest*.  The server, username, and password can be changed with the `-s [--server]`, `-u [--username]`, and `-p [--pasword]` options respectively.

``` 
$> java -jar gate-ampq-service.jar -s rabbit.lappsgrid.org -u john -p secret ...
```

### Other Options

- **`-f FILE, --logfile=FILE`**<br/>the name of the log file to be created.  Do not include a .log extention as that will be added automatically and will interfere with the log file roll over if specified (acutally you will end up with log files that have names like *service.log-2020-05-14.log*).
- **`-l DIR, --logdir DIR`**<br/>directory where log files are saved.  By default log files are written to the current directory.
- **`-t N, --threads=N`**<br/>the number of worker threads that will be spawned.
- **`-x EXCH, --exchange=EXCH`**<br/>the RabbitMQ exchange to be used.  By default all AMPQ services will use an exchange named *service*.
- **`-h, --help`**<br/>display a short help and usage message.
- **`-v, --version`**<br/>display the current application version

## Using The Docker Image

The Docker image takes many of the same parameters, except they are passed as environment variables to the container.  At a minimum that `MAILBOX` name must be specified and the directory containing the GATE `.xgapp` file should be mounted (-v) as `/gate` in the container.

``` 
$> docker run -d --name my-service -e MAILBOX=my-service -v /directory/containing/xgapp-file:/gate docker.lappsgrid.org/gate/ampq-service
```

This will start a container named `my-service` from the Docker image `docker.lappsgrid.org/gate/ampq-service`.  By default, the Docker image expects to load the GATE controller from `/gate/application.xgapp`.  The location of the `application.xgapp` file can be changed by setting the `GATE` environment variable for the container.

``` 
$> docker run -d -e GATE=/usr/local/gate -v /path/to/directory:/usr/local/gate ...
```

If the `.xgapp` file is named something other that `application.xgapp` use the `XGAPP` environment variable to specify the name.

``` 
$> docker run -d -e XGAPP=my-service.xgapp -v /usr/local/gate:/gate ...
```

The above will load the GATE controller from `/usr/local/gate/my-service.xgapp`

**NOTE** GATE expects to be able to load resources relative to the location of the `.xgapp` file.

The complete set of environment variables that can be set is:

- **`MAILBOX`**<br/>the name of the mailbox (task queue) to connect to on the RabbitMQ server. If the task queue does not exist it will be created
- **`GATE`**<br/>the directory containing the `application.xgapp` file and related resources
- **`XGAPP`**<br/>the name of the `.xgapp` file if is is not named `application.xgapp`
- **`RABBIT`**<br/>the address of the RabbitMQ server. By default the service will try to connect to `localhost`
- **`USER`**<br/>the user name to use when connecting to the RabbitMQ server. The default user name is `guest`
- **`PASS`**<br/>the password for the above user. The default password is `guest`
- **`EXCHANGE`**<br/>the message exchange for the task queue. Defaults to `services`
- **`LOGDIR`**<br/>the directory where log files will be created. Defaults to the current directory.
- **`LOGFILE`**<br/>the name of the log file without the `.log` extension.
- **`THREADS`**<br/>the number of worker threads to create. Default to 2.

## Creating a Docker Image

To create a Docker image with the GATE controller included use the `docker.lappsgrid.org/gate/ampq-service` as the base image, copy the GATE controller to the `/gate` directory in the container, and set any environment variables needed to configure the service.  All the environment variables listed above can be set in the `Dockerfile` or on the command line when starting the Docker container.

``` 
FROM docker.lappsgrid.org/gate/ampq-service
COPY my-service/ /gate
ENV MAILBOX=my-service 
```

### Creating A GATE Corpus Controller

Any GATE corpus controller can be used with the `gate-ampq` service.

1. Create a GATE Corpus Controller in GATE as normal.
1. Right click on the controller and select "Export for GATE Cloud"
1. Extract the generated .zip file to your Docker build directory, that is, the directory containing the Dockerfile.

