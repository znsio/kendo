# Kendo
This project provides the ability to easily run API and API workflow tests using Karate.

The sample tests under `./src/test` use `kendo` to manage the test setup, execution and reporting in a standard way.

Results are published in junit, html and cucumber-reporting format. Results are also published to [ReportPortal](./docs/ReportPortal.md)

## Running the tests

### Running the tests with gradle command
There are multiple ways to run the tests under `./src/test` folder

Example
    
    TARGET_ENVIRONMENT=prod TEST_TYPE=workflow ./gradlew clean test

## Build the uber jar

- Build an uber jar:

  `./gradlew build`

- The above command will create a jar in the upload folder with the name like: `upload/getting-started-with-karate-0.0.1.jar`

### Running the tests with a shell script:

    cd upload 
    PARALLEL=3 TARGET_ENVIRONMENT=prod TEST_TYPE=workflow ./runAPIWorkflowTests.sh

### Running the tests with the uber jar:

    cd upload 
    PARALLEL=3 TARGET_ENVIRONMENT=prod TEST_TYPE=workflow java -jar getting-started-with-karate-0.0.1.jar

## [Configuration parameters](./docs/ConfigurationParameters.md)

## [Project specific customisations](./docs/ProjectSpecificCustomizations.md)

## [ReportPortal](./docs/ReportPortal.md)

## [Machine setup](./docs/MachineSetup.md)

## Guidelines 
Read the [CODING GUIDLINES FOR AUTOMATION USING KARATE](docs/READMEGuideline.md) for writing tests in this framework

# Contact
Contact [@BagmarAnand](https://twitter.com/BagmarAnand) for help / information / feedback on this repo
