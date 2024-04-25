package com.znsio.kendo;

import com.epam.reportportal.karate.KarateReportPortalRunner;
import com.intuit.karate.Results;
import com.jayway.jsonpath.JsonPath;
import com.znsio.kendo.cmd.CommandLineExecutor;
import com.znsio.kendo.cmd.CommandLineResponse;
import com.znsio.kendo.exceptions.HardGateFailedException;
import com.znsio.kendo.exceptions.InvalidTestDataException;
import com.znsio.kendo.exceptions.TestExecutionException;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.znsio.kendo.OverriddenVariable.getOverriddenStringValue;

public class RunTest {

    private final int parallelCount;
    private final String branchName;
    private final String buildId;
    private final String buildInitiationReason;
    private final String projectName;
    private final String javaVersion = System.getProperty("java.specification.version");
    private final String userName = System.getProperty("user.name");
    private final TEST_TYPE testType;
    private static final String DEFAULT_CUCUMBER_REPORTS_FILE_NAME = "cucumber-html-reports";
    private static final String DEFAULT_KARATE_REPORTS_DIR_NAME = "karate-reports";
    static final String DEFAULT_LOGBACK_XML_FILE_NAME = "/src/test/java/logback-test.xml";
    private static final String DEFAULT_PARALLEL_COUNT = "5";
    private static final String DEFAULT_TEST_DATA_FILENAME = "./src/test/java/test_data.json";

    public static final String OS_NAME = System.getProperty("os.name");
    public static final boolean IS_WINDOWS = OS_NAME.toLowerCase().startsWith("windows");
    public static final boolean IS_MAC = OS_NAME.toLowerCase().startsWith("mac");
    private static final String WORKING_DIR = System.getProperty("user.dir");
    private static final String CURRENT_DIR_NAME = Paths.get("").toAbsolutePath().getFileName().toString();
    private static final String NOT_SET = "NOT_SET";
    private static final String LOCAL_RUN = "LOCAL_RUN";
    private static final String NA = "N/A";
    private final String reportsDirectory;
    private static final String BASE_URL = "baseUrl";
    private final String LOG_DIR = "LOG_DIR";

    private static final HashMap testMetadata = new HashMap<>();
    private static List<Map.Entry<String, Integer>> sortedTestMetaDataKeys;
    private static Map<String, Object> envConfig;
    private static Map<String, Object> testDataConfig;
    private static Properties properties;
    private final String karateEnv;
    private final String customTags;
    private final List<String> tags;
    private final String testDataFile;
    private final String rpEnable;
    private final String DEFAULT_CONFIG_FILE_NAME = "./src/test/java/config.properties";
    private final boolean isSetHardGate;
    private final boolean isFailingTestSuite;

    enum Metadata {
        BRANCH_NAME_ENV_VAR,
        BUILD_ID_ENV_VAR,
        BUILD_INITIATION_REASON_ENV_VAR,
        PARALLEL_COUNT,
        PROJECT_NAME,
        RP_ENABLE,
        TAGS,
        TEST_DATA_FILE_NAME,
        TEST_TYPE,
        TARGET_ENVIRONMENT,
        SET_HARD_GATE,
        IS_FAILING_TEST_SUITE,
        FAIL_THE_BUILD,
        CONFIG_FILE
    }

    private enum TEST_TYPE {
        api,
        workflow
    }

    public RunTest() {
        System.setProperty("logback.configurationFile", WORKING_DIR + DEFAULT_LOGBACK_XML_FILE_NAME);
        System.out.println("Set system property: logback.configurationFile=" + WORKING_DIR + DEFAULT_LOGBACK_XML_FILE_NAME);

        reportsDirectory = getReportsDirectory();
        properties = loadProperties(getConfigFileName());
        branchName = getBranchName(Metadata.BRANCH_NAME_ENV_VAR, NOT_SET);
        buildId = getOverloadedValueFor(Metadata.BUILD_ID_ENV_VAR, NOT_SET);
        buildInitiationReason = getOverloadedValueFor(Metadata.BUILD_INITIATION_REASON_ENV_VAR, NOT_SET);
        parallelCount = Integer.parseInt(getOverloadedValueFromPropertiesFor(Metadata.PARALLEL_COUNT, DEFAULT_PARALLEL_COUNT));
        projectName = getOverloadedValueFromPropertiesFor(Metadata.PROJECT_NAME, CURRENT_DIR_NAME);
        rpEnable = getOverloadedValueFromPropertiesFor(Metadata.RP_ENABLE, String.valueOf(false));
        customTags = getOverloadedValueFromPropertiesFor(Metadata.TAGS, "");
        testDataFile = getTestDataFileName();
        testType = getTestType();
        isSetHardGate = Boolean.parseBoolean(getOverloadedValueFromPropertiesFor(Metadata.SET_HARD_GATE, String.valueOf(false)));
        isFailingTestSuite = Boolean.parseBoolean(getOverloadedValueFromPropertiesFor(Metadata.IS_FAILING_TEST_SUITE, String.valueOf(false)));
        karateEnv = getOverloadedValueFromPropertiesFor(Metadata.TARGET_ENVIRONMENT, NOT_SET);

        String testDataFileName = new File(testDataFile).getName();
        System.out.println("testDataFileName: " + testDataFileName);
        System.setProperty(Metadata.TEST_DATA_FILE_NAME.name(), testDataFileName);
        System.out.println("WORKING_DIR: " + WORKING_DIR);
        tags = getTags();

        loadEnvConfig();
        captureTestExecutionMetadata();
    }

    private TEST_TYPE getTestType() {
        String testType = getOverloadedValueFromPropertiesFor(Metadata.TEST_TYPE, NOT_SET).toLowerCase();
        try {
            return TEST_TYPE.valueOf(testType);
        } catch (IllegalArgumentException e) {
            throw new InvalidTestDataException("Invalid test type: '%s' provided. Supported values are: '%s'".formatted(testType, Arrays.toString(TEST_TYPE.values())));
        }
    }

    private String getConfigFileName() {
        String providedConfigFileName = System.getenv(Metadata.CONFIG_FILE.name());
        System.out.printf("Config file name: %s%n", providedConfigFileName);

        String configFileToUse = findFile("config", providedConfigFileName, DEFAULT_CONFIG_FILE_NAME);
        System.out.println("Config file to use: " + configFileToUse);
        return configFileToUse;
    }

    private String findFile(String fileType, String providedConfigFileName, String defaultFileName) {
        if (providedConfigFileName != null && new File(providedConfigFileName).exists()) {
            System.out.printf("\tUsing provided " + fileType + " file name '%s'%n", providedConfigFileName);
            return getCanonicalPath(providedConfigFileName);
        }

        String defaultConfigWithoutPathFileName = new File(defaultFileName).getName();
        System.out.printf("\tDefault " + fileType + " file name without path: %s%n", defaultConfigWithoutPathFileName);

        File defaultConfigWithoutPathFile = new File(defaultConfigWithoutPathFileName);
        if (defaultConfigWithoutPathFile.exists()) {
            System.out.printf("\tUsing default " + fileType + " file name '%s' in current directory '%s'%n", defaultConfigWithoutPathFileName, WORKING_DIR);
            return getCanonicalPath(defaultConfigWithoutPathFileName);
        }

        System.out.printf("\tNeither provided " + fileType + " file nor default config file exists. Checking if DEFAULT_" + fileType.toUpperCase() + "_FILE_NAME '%s' exists%n", defaultFileName);
        if (new File(defaultFileName).exists()) {
            System.out.println("\tUsing DEFAULT_" + fileType.toUpperCase() + "_FILE_NAME");
            return getCanonicalPath(defaultFileName);
        }

        throw new InvalidTestDataException("Config file not provided and DEFAULT_" + fileType.toUpperCase() + "_FILE_NAME does not exist");
    }

    private static String getCanonicalPath(String fileName) {
        try {
            return new File(fileName).getCanonicalPath();
        } catch (IOException e) {
            throw new InvalidTestDataException("Failed to get canonical path for config file: " + fileName, e);
        }
    }

    private String getTestDataFileName() {
        System.out.println("Get test data file name");
        String testDataFileName = getOverloadedValueFromPropertiesFor(Metadata.TEST_DATA_FILE_NAME, DEFAULT_TEST_DATA_FILENAME);
        String testDataFileToUse = findFile("Test_Data", testDataFileName, DEFAULT_TEST_DATA_FILENAME);
        System.out.println("Test data file to use: " + testDataFileToUse);
        return testDataFileToUse;
    }

    @NotNull
    private static Properties loadProperties(String configFile) {
        System.out.printf("Loading properties from %s%n", configFile);
        final Properties properties;
        try (InputStream input = new FileInputStream(configFile)) {
            properties = new Properties();
            properties.load(input);
        } catch (IOException ex) {
            throw new InvalidTestDataException("Config file not found, or unable to read it", ex);
        }
        return properties;
    }

    private static String getHostMachineName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.out.println("Error fetching machine name: " + e.getMessage());
            return NOT_SET;
        }
    }

    @Test
    void runKarateTests() {
        System.out.printf("Class: %s :: Test: runKarateTests%n", this.getClass().getSimpleName());
        System.setProperty("rp.launch", projectName + " " + testType.name() + " tests");
        System.setProperty("rp.description", "Running " + testType.name() + " tests for project: " + projectName);
        System.setProperty("rp.launch.uuid.print", String.valueOf(Boolean.TRUE));
        System.setProperty("rp.client.join", String.valueOf(Boolean.FALSE));
        System.setProperty("rp.attributes", getRpAttributes());
        String karateReportsDir = reportsDirectory + File.separator + DEFAULT_KARATE_REPORTS_DIR_NAME;
        Results results = KarateReportPortalRunner
                .path(getClasspath())
                .tags(tags)
                .karateEnv(karateEnv)
                .reportDir(karateReportsDir)
                .outputCucumberJson(true)
                .outputJunitXml(true)
                .outputHtmlReport(true)
                .parallel(getParallelCount());
        String reportFilePath = generateCucumberHtmlReport(results.getReportDir());
        logTestExecutionStatus result = getLogTestExecutionStatus(results, karateReportsDir, reportFilePath);
        checkHardGate(result.scenariosPassed(), result.scenariosFailed(), result.testExecutionSummaryMessage());
    }

    private logTestExecutionStatus getLogTestExecutionStatus(Results results, String karateReportsDir, String reportFilePath) {
        String testExecutionSummaryMessage = "\n\n" + "Test execution summary: ";
        testExecutionSummaryMessage += "\n\t" + "Tags: " + tags;
        testExecutionSummaryMessage += "\n\t" + "Environment: " + karateEnv;
        testExecutionSummaryMessage += "\n\t" + "Parallel count: " + getParallelCount();
        int scenariosFailed = results.getScenariosFailed();
        int scenariosPassed = results.getScenariosPassed();
        int scenariosTotal = results.getScenariosTotal();
        testExecutionSummaryMessage += "\n\t" + "Scenarios: Failed: " + scenariosFailed + ", Passed: " + scenariosPassed + ", Total: " + scenariosTotal;
        testExecutionSummaryMessage += "\n\t" + "Features : Failed: " + results.getFeaturesFailed() + ", Passed: " + results.getFeaturesPassed() + ", Total: " + results.getFeaturesTotal();
        testExecutionSummaryMessage += "\n\t" + "Karate Reports available here: file://" + karateReportsDir + File.separator + "karate-summary.html";
        testExecutionSummaryMessage += "\n\t" + "Cucumber Reports available here: file://" + reportFilePath;

        logTestExecutionStatus result = new logTestExecutionStatus(testExecutionSummaryMessage, scenariosFailed, scenariosPassed);
        return result;
    }

    private record logTestExecutionStatus(String testExecutionSummaryMessage, int scenariosFailed, int scenariosPassed) {
    }

    private void checkHardGate(int scenariosPassed, int scenariosFailed, String testExecutionSummaryMessage) {
        if (isSetHardGate) {
            if (isFailingTestSuite) {
                if (scenariosPassed == 0) {
                    // pass the build
                    System.out.printf("Hard Gate passed for Failing Tests build. %n%s%n", testExecutionSummaryMessage);
                } else {
                    // hard gate exception
                    System.setProperty(RunTest.Metadata.FAIL_THE_BUILD.name(), String.valueOf(true));
                    throw new HardGateFailedException("Hard Gate failed for Failing Tests build. %n%s".formatted(testExecutionSummaryMessage));
                }
            } else {
                if (scenariosFailed == 0) {
                    // pass the build
                    System.out.printf("Hard Gate passed for Passing Tests build. %n%s%n", testExecutionSummaryMessage);
                } else {
                    // hard gate exception
                    System.setProperty(RunTest.Metadata.FAIL_THE_BUILD.name(), String.valueOf(true));
                    throw new HardGateFailedException("Hard Gate failed for Passing Tests build. %n%s".formatted(testExecutionSummaryMessage));
                }
            }
        } else {
            if (scenariosFailed > 0) {
                System.out.println("Hard Gate not set. Test(s) failed.");
                throw new TestExecutionException(testExecutionSummaryMessage);
            } else {
                System.out.println("Hard Gate not set. Test(s) passed.");
                System.out.println(testExecutionSummaryMessage);
            }
        }
    }

    private String getRpAttributes() {
        StringBuilder rpAttributes = new StringBuilder();
        System.out.println("Adding following attributes to ReportPortal's Launch");
        for (Map.Entry<String, Integer> entry : sortedTestMetaDataKeys) {
            System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
            rpAttributes.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }

        System.out.println("rpAttributes: " + rpAttributes);
        return rpAttributes.toString();
    }

    private void captureTestExecutionMetadata() {
        testMetadata.put("RunByFatJarRunner", System.getProperty("IS_FATJAR_RUNNER", Boolean.FALSE.toString()));
        testMetadata.put("TargetEnvironment", karateEnv);
        testMetadata.put("Type", testType.name());
        testMetadata.put("ParallelCount", parallelCount);
        testMetadata.put("LoggedInUser", userName);
        testMetadata.put("JavaVersion", javaVersion);
        testMetadata.put("OS", OS_NAME);
        testMetadata.put("HostName", getHostMachineName());
        testMetadata.put("BranchName", branchName);
        testMetadata.put("BuildId", buildId);
        testMetadata.put("BuildInitiationReason", buildInitiationReason);
        testMetadata.put("BaseUrl", getBaseUrl());
        testMetadata.put("RunInCI", getIsRunInCI());
        testMetadata.put("Tags (custom)", customTags);
        testMetadata.put("Tags", tags);

        // Convert hashmap entries to a list
        sortedTestMetaDataKeys = new ArrayList<>(testMetadata.entrySet());

        // Sort the list by keys
        Collections.sort(sortedTestMetaDataKeys, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
    }

    private static String getStringValueFromPropertiesIfAvailable(String key, String defaultValue) {
        return properties.getProperty(key, String.valueOf(defaultValue));
    }

    private String getBranchName(Metadata metadata, String defaultValue) {
        String branchName = getOverriddenStringValue(Metadata.BRANCH_NAME_ENV_VAR.name(), getStringValueFromPropertiesIfAvailable(Metadata.BRANCH_NAME_ENV_VAR.name(), NOT_SET));
        branchName = getOverriddenStringValue(branchName, getBranchNameUsingGitCommand());
        return branchName;
    }

    private static String getBranchNameUsingGitCommand() {
        String[] getBranchNameCommand = new String[]{"git rev-parse --abbrev-ref HEAD"};
        CommandLineResponse response = CommandLineExecutor.execCommand(getBranchNameCommand);
        String branchName = response.getStdOut();
        System.out.println(String.format("\tBranch name from git command: '%s': '%s'", Arrays.toString(getBranchNameCommand), branchName));
        return branchName;
    }

    private String getOverloadedValueFor(Metadata propertyName, String defaultValue) {
        String envVar = getOverriddenStringValue(propertyName.name(), getStringValueFromPropertiesIfAvailable(propertyName.name(), defaultValue));
        return getOverriddenStringValue(envVar, NOT_SET);
    }

    private String getOverloadedValueFromPropertiesFor(Metadata propertyName, String defaultValue) {
        return getOverriddenStringValue(propertyName.name(), getStringValueFromPropertiesIfAvailable(propertyName.name(), defaultValue));
    }

    private boolean getIsRunInCI() {
        return (!buildInitiationReason.equalsIgnoreCase(LOCAL_RUN));
    }

    private String generateCucumberHtmlReport(String karateOutputPath) {
        System.out.println("================================================================================================");
        System.out.println("===================================  Generating reports  =======================================");
        System.out.println("================================================================================================");
        java.util.Collection<java.io.File> jsonFiles = org.apache.commons.io.FileUtils.listFiles(new java.io.File(karateOutputPath), new String[]{"json"}, true);
        String reportFilePath = null;
        if (jsonFiles.size() == 0) {
            throw new InvalidTestDataException("Reports NOT generated. Have you provided the correct configuration/tags for execution?");
        } else {
            java.util.List<String> jsonPaths = new java.util.ArrayList<>(jsonFiles.size());
            jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
            net.masterthought.cucumber.Configuration config = new net.masterthought.cucumber.Configuration(new File(reportsDirectory), projectName);

            int count = 0;
            String[] envList = new String[testDataConfig.keySet().size()];
            for (Map.Entry<String, Object> stringObjectEntry : testDataConfig.entrySet()) {
                envList[count] = "@" + stringObjectEntry.getKey();
                count++;
            }

            config.setTagsToExcludeFromChart(envList);
            addClassifications(config);
            System.out.println("Excluded tags from Cucumber-html tag report: " + config.getTagsToExcludeFromChart());

            net.masterthought.cucumber.ReportBuilder reportBuilder = new net.masterthought.cucumber.ReportBuilder(jsonPaths, config);
            reportBuilder.generateReports();
            reportFilePath = config.getReportDirectory().getAbsolutePath() + File.separator + DEFAULT_CUCUMBER_REPORTS_FILE_NAME + File.separator + "overview-features.html";
        }
        System.out.println("================================================================================================");
        return reportFilePath;
    }

    private String getPath(String rootDirectory, String pathSuffix) {
        String[] testFilePaths = pathSuffix.split("/");
        for (int eachPath = 0; eachPath < testFilePaths.length; eachPath++) {
            rootDirectory += File.separator + testFilePaths[eachPath];
        }
        System.out.printf("Path for: '%s' is: '%s'%n", pathSuffix, rootDirectory);
        return rootDirectory;
    }

    private void addClassifications(net.masterthought.cucumber.Configuration config) {
        System.out.println("Adding classifications in cucumber-html-reports");
        for (Map.Entry<String, Integer> entry : sortedTestMetaDataKeys) {
            System.out.println("\t" + entry.getKey() + " : " + entry.getValue());
            config.addClassifications(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private String getBaseUrl() {
        return envConfig.get(BASE_URL).toString();
    }

    private void loadEnvConfig() {
        System.out.println("Test Data file path: " + testDataFile);
        try {
            testDataConfig = JsonPath.parse(new File(testDataFile)).read("$", Map.class);
            envConfig = JsonPath.parse(testDataConfig).read("$." + karateEnv + ".env", Map.class);
        } catch (IOException e) {
            throw new InvalidTestDataException(String.format("Unable to load test data file: '%s'", testDataFile), e);
        }
    }

    private static String getCurrentDatestamp(Date today) {
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        return df.format(today);
    }

    private static String getMonth(Date today) {
        SimpleDateFormat df = new SimpleDateFormat("MMM-yyyy");
        return df.format(today);
    }

    private static String getCurrentTimestamp(Date today) {
        SimpleDateFormat df = new SimpleDateFormat("HH-mm-ss");
        return df.format(today);
    }

    private String getReportsDirectory() {
        String reportsDirPath = NOT_SET;
        if (null == System.getenv(LOG_DIR)) {
            Date today = new Date();
            reportsDirPath = getPath(WORKING_DIR,
                    "target" + File.separator
                            + getMonth(today) + File.separator
                            + getCurrentDatestamp(today) + File.separator
                            + getCurrentTimestamp(today));
        } else {
            reportsDirPath = System.getenv(LOG_DIR);
        }
        System.out.println("Reports directory: " + reportsDirPath);
        return reportsDirPath;
    }

    private String getTargetEnvironment() {
        String karateEnv = OverriddenVariable.getOverriddenStringValue(Metadata.TARGET_ENVIRONMENT.name(), properties.getProperty(Metadata.TARGET_ENVIRONMENT.name()));
        if ((null == karateEnv) || (karateEnv.isBlank())) {
            String message = "TARGET_ENVIRONMENT is not specified as an environment variable";
            System.out.println(message);
            throw new InvalidTestDataException(message);
        }
        System.setProperty("karate.env", karateEnv);
        return karateEnv;
    }

    private List<String> getTags() {
        System.out.println("In " + this.getClass().getSimpleName() + " :: getTags");
        java.util.List<String> tagsToRun = new java.util.ArrayList<>();
        if ((null != customTags) && (!customTags.trim().isEmpty())) {
            String[] customTags = this.customTags.trim().split(":");
            for (String customTag : customTags) {
                tagsToRun.addAll(List.of(customTag));
            }
        }
        tagsToRun.add(getEnvTag());
        tagsToRun.add("~@ignore");
        tagsToRun.add("~@wip");
        tagsToRun.add("~@template");
        tagsToRun.add("~@data");
        if (isSetHardGate) {
            if (isFailingTestSuite) {
                tagsToRun.add("@failing");
            } else {
                tagsToRun.add("~@failing");
            }
        }

        System.out.println("Run tests with tags: " + tagsToRun);
        return tagsToRun;
    }

    private String getEnvTag() {
        String env = karateEnv;
        String envTag = ((null != env) && (!env.trim().isEmpty())) ? env.toLowerCase() : "@prod";
        if (!envTag.startsWith("@")) {
            envTag = "@" + envTag;
        }
        System.out.println("Run tests on environment: " + envTag);
        return envTag;
    }

    private String getClasspath() {
        System.out.println("In " + this.getClass().getSimpleName() + " :: getClassPath");
        String classPath = "classpath:com/znsio/" + testType.name();
        System.out.printf("Running %s tests%n", classPath);
        return classPath;
    }

    private int getParallelCount() {
        String parallel = OverriddenVariable.getOverriddenStringValue(Metadata.PARALLEL_COUNT.name(), properties.getProperty(Metadata.PARALLEL_COUNT.name()));
        int parallelCount = (null == parallel || parallel.isEmpty()) ? 5 : Integer.parseInt(parallel);
        System.out.printf("Parallel count: %d %n", parallelCount);
        return parallelCount;
    }
}
