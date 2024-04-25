Back to main [README](./../README.md)

-----

# ReportPortal
## Setup
[Install and setup ReportPortal](https://reportportal.io/installation) as a central server in your organisation

## Configure test framework
* Update [reportportal.properties](./src/test/resources/reportportal.properties) with the appropriate reportportal server and team information

## Enable sending reports to reportportal
You can enable sending reports to reportportal in 2 ways:
1. In [reportportal.properties](./src/test/resources/reportportal.properties), set `rp.enable=true`, or
2. Set environment variable `RP_ENABLE=true` and then run the tests [Running the tests with gradle command]
   Example
   > RP_ENABLE=true TARGET_ENVIRONMENT=prod TEST_TYPE=workflow ./runAPIWorkflowTests.sh
   > RP_ENABLE=true TARGET_ENVIRONMENT=prod TEST_TYPE=workflow ./gradlew clean test

### reportportal.properties

To enable logging test results to reportportal, you can:

* Change the reportportal.properties file

      rp.enable=true

* Enable it from command line:

      RP_ENABLE=true ./gradlew run

* Enable it from [config.properties](./../src/test/java/config.properties) file:

      RP_ENABLE=true ./gradlew run

-----

Back to main [README](./../README.md)
