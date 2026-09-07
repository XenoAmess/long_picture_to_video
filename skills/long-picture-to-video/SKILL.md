---
name: long-picture-to-video
description: 将超长图片（如长截图、条漫、vivhite.png）转换为垂直滚动的 MP4 视频。当用户要求"长图转视频 / 图片滚动视频 / scrolling video"时使用。操作本仓库（com.xenoamess/long_picture_to_video，Java + Quarkus CLI，JavaCV/FFmpeg 编码 H.264）。
---

# long-picture-to-video

本仓库根目录即项目：`/home/xenoamess/workspace/long_picture_to_video`（若被复制到别处，以实际仓库根目录为准）。

## 构建

```bash
mvn -B -ntp package -DskipTests
```

产物为 fast-jar：`target/quarkus-app/quarkus-run.jar`。

## 生成视频（基本用法）

```bash
java -jar target/quarkus-app/quarkus-run.jar <输入图片> [选项]
```

最常用示例（使用全部默认值）：

```bash
java -jar target/quarkus-app/quarkus-run.jar src/test/resources/vivhite.png -o vivhite-scroll.mp4
```

## 参数

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `<input>`（位置参数） | 输入超长图片路径 | 必填 |
| `-o, --output <file>` | 输出视频路径 | `<输入文件名>-scroll.mp4`（与输入同目录） |
| `--width <px>` | 视频宽度 | 图片宽度（奇数时减 1 取偶） |
| `--height <px>` | 视频高度（控制画面比例） | `width * 9 / 16`（即 16:9） |
| `--speed <px/s>` | 滚动速度 | 300 |
| `--fps <n>` | 帧率 | 30 |

行为说明：

- 视频从图片**顶部滚动到底部**；时长 = (图片缩放后高度 - 视频高度) / speed。
- `--width` 与图片宽度不同时，图片先等比缩放到该宽度再滚动。
- 若缩放后图片高度 < 视频高度，命令会报错退出（提示降低 `--height` 或 `--width`）。
- 输出为 H.264 / yuv420p 的 MP4；码率按分辨率与帧率自动估算（0.1 bit/像素/帧，限幅 2~20 Mbps）。

示例：

```bash
# 9:16 竖屏、快速滚动
java -jar target/quarkus-app/quarkus-run.jar input.png --width 720 --height 1280 --speed 600

# 60fps 慢速高清
java -jar target/quarkus-app/quarkus-run.jar input.png --fps 60 --speed 150 -o out.mp4
```

## 验证产物

```bash
ffprobe -v error -show_entries format=duration:stream=width,height,nb_frames,r_frame_rate \
  -of default=noprint_wrappers=1 <输出文件>
```

## 自测 / 测试

仓库自带超长测试图 `src/test/resources/vivhite.png`（1547×23491）：

```bash
mvn -B -ntp test   # 单元测试会用该图生成视频并用 JavaCV 校验元数据
```

## 故障排查

- 构建首次需下载约 100MB+ 的 JavaCV/FFmpeg 原生依赖。
- 仅内置 linux-x86_64 原生库；其他平台需在 `pom.xml` 中更换/追加 classifier。
- 运行时出现 `Restricted methods ... native access` 警告属正常（JavaCPP 在 JDK 21+ 的提示），不影响功能。
