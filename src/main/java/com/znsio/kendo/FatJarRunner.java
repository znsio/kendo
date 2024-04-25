package com.znsio.kendo;

import com.znsio.kendo.exceptions.HardGateFailedException;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import java.io.PrintWriter;

import static com.intuit.karate.FileUtils.WORKING_DIR;
import static com.znsio.kendo.RunTest.DEFAULT_LOGBACK_XML_FILE_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class FatJarRunner {

    static final String IS_FATJAR_RUNNER = "IS_FATJAR_RUNNER";
    SummaryGeneratingListener listener = new SummaryGeneratingListener();

    public void runAll() {
        System.out.println("In " + FatJarRunner.class.getSimpleName() + " :: runAll");

        System.setProperty(IS_FATJAR_RUNNER, "true");
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(RunTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
    }

    public static void main(String[] args) {
        System.out.println("Started " + FatJarRunner.class.getSimpleName());

        FatJarRunner runner = new FatJarRunner();
        runner.runAll();

        boolean isFailTheBuild = Boolean.parseBoolean(System.getProperty(RunTest.Metadata.FAIL_THE_BUILD.name(), String.valueOf(false)));

        TestExecutionSummary summary = runner.listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        boolean haveTestsFailed = summary.getTestsAbortedCount() > 0 || summary.getTestsFailedCount() > 0;
        String testFailureSummary = "";
        if (haveTestsFailed) {
            for (Failure failure : summary.getFailures()) {
                testFailureSummary += failure.getTestIdentifier()
                        .getDisplayName() + "->\n" + failure.getException()
                        .toString() + "\n";
            }
        }

        if (isFailTheBuild) {
            System.out.println("FatJarRunner: Hard gate failure - throwing exception");
            throw new HardGateFailedException("Hard gate failure" + testFailureSummary);
        } else {
            if (haveTestsFailed) {
                throw new RuntimeException("Tests failed" + testFailureSummary);
            }
        }
    }
}
