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

        var width = event.getOption("width", null, OptionMapping::getAsInt)
        var height = event.getOption("height", null, OptionMapping::getAsInt)
        val bitrate = event.getOption("bitrate", 8000, OptionMapping::getAsInt)
        val audioBitrate = event.getOption("audiobitrate", 16000, OptionMapping::getAsInt)
        val fps = event.getOption("fps", 5, OptionMapping::getAsInt)

        val vf = event.getOption("vf", "", OptionMapping::getAsString)
        val af = event.getOption("af", "",  OptionMapping::getAsString)

        if (!video!!.isVideo) {
            event.reply("Attachment provided is not a valid video file").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val vidFile = video.proxy.downloadToFile(File.createTempFile("lbvid", ".${video.fileExtension}")).join()

        if (width == null) {
            width = video.width
        }

        if (height == null) {
            height = video.height
        }

        val compressed = compressVideo(vidFile, video.fileExtension!!, width, height, bitrate, fps, audioBitrate, vf, af)

        event.hook.sendFiles(FileUpload.fromData(compressed)).complete()

        compressed.deleteOnExit()
        vidFile.deleteOnExit()
    }

    private fun compressVideo(vid: File, extension: String, width: Int, height: Int, bitrate: Int, fps: Int, audioBitrate: Int, vf: String, af: String): File {
        val builder = FFmpegBuilder()

        val out = File.createTempFile("lbvidjob", ".$extension")

        builder.setInput(vid.path)
            .overrideOutputFiles(true)
            .addOutput(out.path)
            .setVideoBitRate(bitrate.toLong())
            .setAudioBitRate(audioBitrate.toLong())
            .setVideoFrameRate(fps.toDouble())
            .setVideoResolution(width, height)

        if(af.isNotEmpty()){
            builder.setAudioFilter(af)
        }

        if(vf.isNotEmpty()){
            builder.setVideoFilter(vf)
        }

        val exec = FFmpegExecutor(ffmpeg)

        exec.createJob(builder).run()

        return out
    }
}