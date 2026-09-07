package com.xenoamess.lptv;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollingVideoGeneratorTest {

    private static final Path TEST_IMAGE = Path.of("src", "test", "resources", "vivhite.png");

    private final ScrollingVideoGenerator generator = new ScrollingVideoGenerator();

    @TempDir
    Path tempDir;

    @Test
    void generatesScrollingVideoWithDefaultWidthAndConfiguredHeight() throws Exception {
        Path output = tempDir.resolve("vivhite-scroll.mp4");

        GenerationResult result = generator.generate(new ScrollVideoConfig(
                TEST_IMAGE, output,
                null,   // 宽度默认取图片宽度
                360,    // 自定义高度
                4000.0, // 加速滚动，缩短测试耗时
                30));

        assertTrue(Files.isRegularFile(output));
        assertTrue(Files.size(output) > 0);

        // 用 ffprobe 等价的 JavaCV grabber 校验产物元数据
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(output.toFile())) {
            grabber.start();
            assertEquals(result.width(), grabber.getImageWidth());
            assertEquals(result.height(), grabber.getImageHeight());
            assertEquals(30, (int) Math.round(grabber.getFrameRate()));
            assertTrue(grabber.getLengthInVideoFrames() > 50,
                    "帧数过少: " + grabber.getLengthInVideoFrames());
            grabber.stop();
        }
    }

    @Test
    void derivesDefaultHeightFromSixteenToNineRatio(@TempDir Path dir) throws Exception {
        Path output = dir.resolve("out.mp4");

        GenerationResult result = generator.generate(new ScrollVideoConfig(
                TEST_IMAGE, output, 320, null, 8000.0, 30));

        // 320 * 9/16 = 180
        assertEquals(320, result.width());
        assertEquals(180, result.height());
        assertTrue(Files.isRegularFile(output));
    }

    @Test
    void derivesDefaultOutputPath() {
        Path input = Path.of("src", "test", "resources", "vivhite.png");
        Path derived = ScrollingVideoGenerator.deriveOutputPath(input);
        assertEquals("vivhite-scroll.mp4", derived.getFileName().toString());
    }

    @Test
    void rejectsMissingInput(@TempDir Path dir) {
        assertThrows(IllegalArgumentException.class, () ->
                generator.generate(new ScrollVideoConfig(
                        dir.resolve("nonexistent.png"), null, null, null, null, null)));
    }

    @Test
    void rejectsWindowTallerThanImage(@TempDir Path dir) {
        // 高度超过图片缩放后的高度时应报错
        assertThrows(IllegalArgumentException.class, () ->
                generator.generate(new ScrollVideoConfig(
                        TEST_IMAGE, dir.resolve("out.mp4"), 100, 999_999, null, null)));
    }
}
