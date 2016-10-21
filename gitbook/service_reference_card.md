# Service Reference Card

* Daemons running
  * `/usr/lib/jvm/java/bin/java` (inside the docker container)
* Init scripts and options
  * Started as a docker container (`docker run`)
* Configuration files location with example or template
  * The configuration parameters are provided through environment variables of the docker run command
* Logfile locations (and management) and other useful audit information
  * inside the docker container
    * `/opt/jboss/wildfly/standalone/log/server.log` (logs are rotated every day)
  * outside the docker container
    * logs can be retrieved with the `docker logs` command
* Open ports
  * `8080` HTTP port
* Possible unit test of the service
    * Continuous unit tests are conducted during development via [Jenkins](https://jenkins.indigo-datacloud.eu:8080/me/my-views/view/Orchestrator/job/orchestrator-unittest/).
* Where is service state held (and can it be rebuilt)
  * The service state is held in two attached DBs; it can be rebuilt from SQL dumps
* Cron jobs
  * **NONE**
* Security information
  * Access control Mechanism description (authentication & authorization)
    * IAM OAuth2 bearer token (`RFC 6750`)
  * Firewall configuration
    * Outbound traffic must be allowed to every port and addresses
    * Inbound traffic must be allowed to HTTP port from any addresses
  * Security recommendations
    * It's advised to put the orchestrator behind a reverse proxy with HTTPS enabled