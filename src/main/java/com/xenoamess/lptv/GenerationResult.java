package com.xenoamess.lptv;

import java.nio.file.Path;

/**
 * 一次滚动视频生成的结果信息。
 *
 * @param output        实际输出文件路径
 * @param width         视频宽度（像素）
 * @param height        视频高度（像素）
 * @param fps           帧率
 * @param frameCount    总帧数
 * @param durationSeconds 视频时长（秒）
 * @param speed         滚动速度（像素/秒）
 */
public record GenerationResult(
        Path output,
        int width,
        int height,
        int fps,
        int frameCount,
        double durationSeconds,
        double speed
) {
}
