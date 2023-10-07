package org.odinware;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;


public class TestRunner {

    public static void main(String[] args) {
        Class[] testClasses = {
                TextHelperTest.class,
                OpenAIRequestBuilderTest.class,
                ContextTest.class,
                GptOpsHelperTest.class
        };
        Result result = JUnitCore.runClasses(testClasses);

        if (result.wasSuccessful()) {
            System.out.println("All tests passed!");
        } else {
            System.out.println("Test failure(s):");
            for (Failure failure : result.getFailures()) {
                System.out.println(failure.toString());
            }
        }
    }
}