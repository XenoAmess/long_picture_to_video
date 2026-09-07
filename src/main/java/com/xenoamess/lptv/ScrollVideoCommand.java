package com.xenoamess.lptv;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * CLI 入口：将超长图片转换为垂直滚动视频。
 */
@TopCommand
@CommandLine.Command(
        name = "long-picture-to-video",
        mixinStandardHelpOptions = true,
        version = "1.0-SNAPSHOT",
        description = "将一张很高的图片转换为从上到下垂直滚动的 MP4 视频。"
)
public class ScrollVideoCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "<input>",
            description = "输入的超长图片路径（如 vivhite.png）"
    )
    Path input;

    @CommandLine.Option(
            names = {"-o", "--output"},
            paramLabel = "<file>",
            description = "输出视频路径，默认: <输入文件名>-scroll.mp4"
    )
    Path output;

    @CommandLine.Option(
            names = {"--width"},
            paramLabel = "<px>",
            description = "输出视频宽度（像素），默认: 图片宽度"
    )
    Integer width;

    @CommandLine.Option(
            names = {"--height"},
            paramLabel = "<px>",
            description = "输出视频高度（像素），默认: 按 16:9 由宽度推导 (width * 9 / 16)"
    )
    Integer height;

    @CommandLine.Option(
            names = {"--speed"},
            paramLabel = "<px/s>",
            description = "滚动速度（像素/秒），默认: " + (int) ScrollingVideoGenerator.DEFAULT_SPEED
    )
    Double speed;

    @CommandLine.Option(
            names = {"--fps"},
            paramLabel = "<n>",
            description = "视频帧率，默认: " + ScrollingVideoGenerator.DEFAULT_FPS
    )
    Integer fps;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        try {
            new ScrollingVideoGenerator().generate(
                    new ScrollVideoConfig(input, output, width, height, speed, fps));
        } catch (IllegalArgumentException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("生成失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
