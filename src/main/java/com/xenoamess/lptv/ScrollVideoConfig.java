package com.xenoamess.lptv;

import java.nio.file.Path;

/**
 * 滚动视频生成配置。
 *
 * @param input  输入的超长图片路径（必填）
 * @param output 输出视频路径，null 时自动派生为 &lt;输入文件名&gt;-scroll.mp4
 * @param width  输出视频宽度（像素），null 时使用图片宽度
 * @param height 输出视频高度（像素），null 时按 16:9 由宽度推导（width * 9 / 16）
 * @param speed  滚动速度（像素/秒），null 时使用 {@link ScrollingVideoGenerator#DEFAULT_SPEED}
 * @param fps    视频帧率，null 时使用 {@link ScrollingVideoGenerator#DEFAULT_FPS}
 */
public record ScrollVideoConfig(
        Path input,
        Path output,
        Integer width,
        Integer height,
        Double speed,
        Integer fps
) {
}
