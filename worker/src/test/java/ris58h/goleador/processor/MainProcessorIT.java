package ris58h.goleador.processor;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MainProcessorIT {
    static final String FORMAT = "136"; // 720p

    static Path testDirPath = Paths.get("test");
    static MainProcessor mainProcessor = new MainProcessor();

    @BeforeAll
    static void beforeAll() throws Exception {
        mainProcessor.init();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mainProcessor.dispose();
    }

    @TestFactory
    Stream<DynamicTest> tests() {
        return MainProcessorTestData.DATA_BY_VIDEO.entrySet().stream()
                .map(e -> DynamicTest.dynamicTest(e.getKey(), () -> {
                    test(e.getKey(), e.getValue());
                }));
    }

    private void test(String videoId, List<String> expectedLines) throws Exception {
        Path inputPath = testDirPath.resolve("video").resolve(FORMAT).resolve(videoId + ".mp4");
        File inputFile = inputPath.toFile();
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file not found: " + inputFile);
        }
        String input = inputFile.getAbsolutePath();
        Path workingDirPath = testDirPath.resolve("work").resolve(FORMAT).resolve(videoId);
        File workingDirFile = workingDirPath.toFile();
        if (!workingDirFile.exists()) {
            workingDirFile.mkdirs();
        }
        String workingDir = workingDirFile.getAbsolutePath();
        List<String> lines = mainProcessor.process(input, workingDir).stream()
                .map(ScoreFrames::toString)
                .collect(Collectors.toList());
        Assertions.assertIterableEquals(expectedLines, lines);
    }
}
