Back to main [README](./../README.md)

-----

# Configuration parameters

kendo provides the following configuration properties:
* `BRANCH_NAME_ENV_VAR=BRANCH_NAME` -> Refer to the [Special properties](#Special-properties) section. Default: `git rev-parse --abbrev-ref HEAD`
* `BUILD_ID_ENV_VAR=BUILD_ID` -> Default: `NOT-SET`
* `BUILD_INITIATION_REASON_ENV_VAR=BUILD_REASON` -> Default: `NOT-SET`
* `PARALLEL_COUNT=2`  -> Parallel count for the test execution. Default: `PARALLEL=5`
* `PROJECT_NAME=karate tests`  -> The name of your project. Default: The current directory name will be used for the `PROJECT_NAME`.
* `TAGS=...` -> Subset of tests to run? Ex: `TAGS=confengine` will run all tests having the tag confengine
    * To run test with multiple tags specified, you can use a command like:

            TAGS=@demo,~@sanity TARGET_ENVIRONMENT=prod TEST_TYPE=workflow ./gradlew clean test`

    * To run tests having tags as @tags OR @sample:

            TEST_TYPE=api  TARGET_ENVIRONMENT=prod  TAGS=@tags,@sample ./gradlew clean test

    * To run tests having tags as @tags OR @sample AND exclude tags @demo2

            TEST_TYPE=api  TARGET_ENVIRONMENT=prod  TAGS=@tags,@sample:~@demo2 ./gradlew clean test

    * To run tests having tags as @tags OR @sample AND exclude tests having either of the following tags: @demo2, @e2e

            TEST_TYPE=api  TARGET_ENVIRONMENT=prod  TAGS=@tags,@sample:~@demo2:~@e2e ./gradlew clean test
* `TEST_DATA_FILE_NAME=./src/test/java/test_data.json` -> Test data file name. Default: `./src/test/java/test_data.json` 
* `TEST_TYPE=api`  -> `[api | workflow]` -> Type of test to run
* `TARGET_ENVIRONMENT=prod`  -> Run tests for specific environment. Data will be picked up accordingly from `TEST_DATA_FILE_NAME`. Default: `NOT-SET`
* `RP_ENABLE=true` -> Refer to [ReportPortal](ReportPortal.md) for more details. Default: false

### Special properties

* The following properties are different:

      BRANCH_NAME_ENV_VAR=BRANCH_NAME 
      
      BUILD_ID_ENV_VAR=BUILD_ID 
    
      BUILD_INITIATION_REASON_ENV_VAR=BUILD_REASON

  The value provided for these property names are respective environment variables where the real value will be seen.
  Example: In Azure DevOps, the BUILD_ID is available from environment variable: BUILDID. So we will provide the value for `BUILD_ID_ENV_VAR` as `BUILD_ID`

  IF `BRANCH_NAME_ENV_VAR` is not provided, then we will fetch the branch name from git using the command `git rev-parse --abbrev-ref HEAD` and use that value.

-----
Back to main [README](./../README.md)
