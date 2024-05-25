package ro.srth.lbv2.command.slash

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.File

class ShitifyCommand(data: Data?) : LBCommand(data) {

    companion object {
        val ffmpeg: FFmpeg = Bot.getFFMPEG()
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        handleVideo(event)
    }

    private fun handleVideo(event: SlashCommandInteractionEvent) {
        val video = event.getOption("video") { obj: OptionMapping -> obj.asAttachment }

        var width = event.getOption("width", null) { obj: OptionMapping -> obj.asInt }
        var height = event.getOption("height", null) { obj: OptionMapping -> obj.asInt }
        val bitrate = event.getOption("bitrate", 8000) { obj: OptionMapping -> obj.asInt }
        val fps = event.getOption("fps", 5) { obj: OptionMapping -> obj.asInt }

        event.deferReply().queue()

        if (!video!!.isVideo) {
            event.reply("Attachment provided is not a video file").setEphemeral(true).queue()
            return
        }

        val vidFile = video.proxy.downloadToFile(File.createTempFile("lbvid", ".${video.fileExtension}")).join()

        if (width == null) {
            width = video.width
        }

        if (height == null) {
            height = video.height
        }

        val compressed = compressVideo(vidFile, video.fileExtension!!, width, height, bitrate, fps)

        event.hook.sendFiles(FileUpload.fromData(compressed)).complete()

        compressed.deleteOnExit()
        vidFile.deleteOnExit()
    }

    private fun compressVideo(vid: File, extension: String, width: Int, height: Int, bitrate: Int, fps: Int): File {
        val builder = FFmpegBuilder()

        val out = File.createTempFile("lbvidjob", ".$extension")

        builder.setInput(vid.path)
            .overrideOutputFiles(true)
            .addOutput(out.path)
            .setVideoBitRate(bitrate.toLong())
            .setAudioBitRate(16000)
            .setVideoFrameRate(fps.toDouble())
            .setVideoWidth(width)
            .setVideoHeight(height)
            .setAudioFilter("volume=10")

        val exec = FFmpegExecutor(ffmpeg)

        exec.createJob(builder).run()

        return out
    }
}