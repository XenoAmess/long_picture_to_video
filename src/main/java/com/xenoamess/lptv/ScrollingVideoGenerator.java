package com.xenoamess.lptv;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将一张很高的图片转换为从上到下垂直滚动的视频（H.264 / MP4）。
 */
public class ScrollingVideoGenerator {

    /** 默认滚动速度：300 像素/秒。 */
    public static final double DEFAULT_SPEED = 300.0;

    /** 默认帧率。 */
    public static final int DEFAULT_FPS = 30;

    /** 默认宽高比（16:9）中高度相对宽度的系数。 */
    public static final double DEFAULT_HEIGHT_RATIO = 9.0 / 16.0;

    public GenerationResult generate(ScrollVideoConfig config) throws IOException {
        validate(config);

        BufferedImage source = ImageIO.read(config.input().toFile());
        if (source == null) {
            throw new IllegalArgumentException("无法读取图片（格式不支持？）: " + config.input());
        }

        int outWidth = toEven(config.width() != null ? config.width() : source.getWidth());
        int outHeight = toEven(config.height() != null
                ? config.height()
                : (int) Math.round(outWidth * DEFAULT_HEIGHT_RATIO));
        double speed = config.speed() != null ? config.speed() : DEFAULT_SPEED;
        int fps = config.fps() != null ? config.fps() : DEFAULT_FPS;

        // 将原图等比缩放到输出视频宽度，随后在其上滑动窗口取景。
        double scale = (double) outWidth / source.getWidth();
        int scaledHeight = (int) Math.round(source.getHeight() * scale);
        if (scaledHeight < outHeight) {
            throw new IllegalArgumentException(
                    "缩放后图片高度 (" + scaledHeight + "px) 小于视频高度 (" + outHeight
                            + "px)，无法滚动。请降低 --height 或 --width。");
        }

        Path output = config.output() != null ? config.output() : deriveOutputPath(config.input());
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        BufferedImage scaled = scaleImage(source, outWidth, scaledHeight);

        int maxOffset = scaledHeight - outHeight;
        double durationSeconds = maxOffset / speed;
        int frameCount = Math.max(1, (int) Math.round(durationSeconds * fps) + 1);

        System.out.printf("输入图片: %s (%dx%d)%n", config.input(), source.getWidth(), source.getHeight());
        System.out.printf("输出视频: %s (%dx%d @ %dfps, %.1fs, %d 帧, 速度 %.0f px/s)%n",
                output, outWidth, outHeight, fps, durationSeconds, frameCount, speed);

        Java2DFrameConverter converter = new Java2DFrameConverter();
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output.toFile(), outWidth, outHeight)) {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(fps);
            // 内置编码器为 OpenH264，其默认码率过低（约 400kb/s），按分辨率/帧率估算合理码率
            recorder.setVideoBitrate(estimateBitrate(outWidth, outHeight, fps));
            // 以下选项仅对 libx264 生效，OpenH264 会忽略
            recorder.setVideoOption("preset", "medium");
            recorder.setVideoOption("crf", "20");
            recorder.start();

            BufferedImage window = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = window.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            try {
                for (int i = 0; i < frameCount; i++) {
                    int offset = (int) Math.min(maxOffset, Math.round(i * speed / fps));
                    g.drawImage(scaled, 0, 0, outWidth, outHeight,
                            0, offset, outWidth, offset + outHeight, null);
                    recorder.record(converter.convert(window));
                }
            } finally {
                g.dispose();
            }
        }

        System.out.println("完成: " + output);
        return new GenerationResult(output, outWidth, outHeight, fps, frameCount, durationSeconds, speed);
    }

    private static void validate(ScrollVideoConfig config) {
        if (config.input() == null) {
            throw new IllegalArgumentException("必须指定输入图片路径");
        }
        if (!Files.isRegularFile(config.input())) {
            throw new IllegalArgumentException("输入图片不存在: " + config.input());
        }
        if (config.width() != null && config.width() < 2) {
            throw new IllegalArgumentException("--width 必须 >= 2");
        }
        if (config.height() != null && config.height() < 2) {
            throw new IllegalArgumentException("--height 必须 >= 2");
        }
        if (config.speed() != null && config.speed() <= 0) {
            throw new IllegalArgumentException("--speed 必须 > 0");
        }
        if (config.fps() != null && config.fps() <= 0) {
            throw new IllegalArgumentException("--fps 必须 > 0");
        }
    }

    static Path deriveOutputPath(Path input) {
        String fileName = input.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        Path parent = input.toAbsolutePath().getParent();
        return parent.resolve(base + "-scroll.mp4");
    }

    private static int toEven(int value) {
        return value % 2 == 0 ? value : value - 1;
    }

    /** 按 0.1 bit/像素/帧 估算码率，限制在 [2Mbps, 20Mbps]。 */
    static int estimateBitrate(int width, int height, int fps) {
        long estimate = Math.round(width * (double) height * fps * 0.1);
        return (int) Math.min(20_000_000L, Math.max(2_000_000L, estimate));
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }
}
