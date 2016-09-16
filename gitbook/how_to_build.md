# HOW TO BUILD

## REQUISITES


To build this project you will need the JDK 1.8 and maven 3.3. Thanks to maven all the code dependencies will be automatically downloaded.

## COMPILE THE CODE


Go to the same folder where the `pom.xml` file is and type:
```
mvn install
```
This command will download the dependencies, compile the code and create a war package, `orchestrator.war`, which will be put inside the `docker` folder.


## COMPILE THE DOCKER IMAGE


After the previous step, you can build the Docker image of the Orchestrator with the command:
```
docker build -t indigodatacloud/orchestrator docker/ 
```

