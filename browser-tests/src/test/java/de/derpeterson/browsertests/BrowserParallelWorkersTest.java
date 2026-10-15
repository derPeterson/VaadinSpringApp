package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class BrowserParallelWorkersTest {
    @TempDir Path temporary;
    private ObjectNode complete() {
        return new ObjectMapper().createObjectNode().put("finishedAt", "2026-10-15T12:00:00Z")
                .put("setup", "passed").put("tests", "passed").put("cleanup", "passed").put("imageId", "sha256:example")
                .put("testsFound", 2).put("testsPassed", 2).put("testsFailed", 0).put("testsSkipped", 0);
    }

    @Test void onlyCompleteWorkerWithSameImageAndEvidenceCanPass() {
        assertTrue(BrowserParallelWorkers.successful(complete(), 0, "sha256:example", 2));
        assertFalse(BrowserParallelWorkers.successful(complete(), 1, "sha256:example", 2));
        assertFalse(BrowserParallelWorkers.successful(complete(), 0, "sha256:other", 2));
        assertFalse(BrowserParallelWorkers.successful(complete(), 0, "sha256:example", 1));
        assertFalse(BrowserParallelWorkers.successful(complete(), 0, "sha256:example", 3));
        for (String key : new String[]{"setup", "tests", "cleanup"})
            assertFalse(BrowserParallelWorkers.successful(complete().put(key, "failed"), 0, "sha256:example", 2));
        assertFalse(BrowserParallelWorkers.successful(complete().put("testsSkipped", 1), 0, "sha256:example", 2));
        assertFalse(BrowserParallelWorkers.successful(complete().put("testsFound", 0).put("testsPassed", 0), 0, "sha256:example", 0));
        var unfinished = complete(); unfinished.remove("finishedAt");
        assertFalse(BrowserParallelWorkers.successful(unfinished, 0, "sha256:example", 2));
    }

    @Test void evidenceIsMovedOnceAndKeepsAttachmentNames() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("worker"));
        Path target = Files.createDirectory(temporary.resolve("combined"));
        Files.writeString(source.resolve("case-result.json"), "result");
        Files.writeString(source.resolve("screenshot.png"), "image");
        Files.writeString(source.resolve("environment.properties"), "worker-only");
        BrowserParallelWorkers.collect(source, target);
        assertEquals("result", Files.readString(target.resolve("case-result.json")));
        assertEquals("image", Files.readString(target.resolve("screenshot.png")));
        assertFalse(Files.exists(source.resolve("screenshot.png")));
        assertFalse(Files.exists(target.resolve("environment.properties")));
    }

    @Test void duplicateEvidenceCannotOverwriteAnotherWorker() throws Exception {
        Path source = Files.createDirectory(temporary.resolve("worker"));
        Path target = Files.createDirectory(temporary.resolve("combined"));
        Files.writeString(source.resolve("same-result.json"), "second");
        Files.writeString(target.resolve("same-result.json"), "first");
        assertThrows(IOException.class, () -> BrowserParallelWorkers.collect(source, target));
        assertEquals("first", Files.readString(target.resolve("same-result.json")));
    }
    @Test void classMembershipMattersButDiscoveryOrderDoesNot() throws Exception {
        var expected = java.util.List.<Class<?>>of(
                de.derpeterson.browsertests.suites.AuthenticationBrowserTest.class,
                de.derpeterson.browsertests.suites.RegistrationBrowserTest.class);
        var data = new ObjectMapper().readTree("{\"testClasses\":[\"RegistrationBrowserTest\",\"AuthenticationBrowserTest\"]}");
        assertTrue(BrowserParallelWorkers.matchesClasses(data, expected));
        assertFalse(BrowserParallelWorkers.matchesClasses(new ObjectMapper().readTree(
                "{\"testClasses\":[\"RegistrationBrowserTest\",\"RegistrationBrowserTest\"]}"), expected));
        assertFalse(BrowserParallelWorkers.matchesClasses(new ObjectMapper().readTree(
                "{\"testClasses\":[\"RegistrationBrowserTest\"]}"), expected));
    }

}
