package ro.srth.lbv2.command.slash

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.File
import java.io.IOException

@Suppress("unused")
class ShitifyCommand(data: Data) : LBCommand(data) {
    private val defaultBitrate: Int
    private val defaultAudioBitrate: Int
    private val defaultSamplingRate: Int
    private val defaultFps: Int

    private var ffmpeg: FFmpeg?

    private val fileCache = bot.fileCache

    init {
        try {
            this.ffmpeg = FFmpeg("ffmpeg/bin/ffmpeg.exe")
        } catch (e: IOException) {
            Bot.log.error("ShitifyCommand cant run because ffmpeg does not exist")
            this.ffmpeg = null
        }

        val obj = data.attachedData
        this.defaultBitrate = obj!!.getInt("defaultBitrate")
        this.defaultAudioBitrate = obj.getInt("defaultAudioBitrate")
        this.defaultSamplingRate = obj.getInt("defaultSamplingRate")
        this.defaultFps = obj.getInt("defaultFps")
    }


    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        if (ffmpeg == null) {
            event.reply("Command can not be run at this time.").setEphemeral(true).queue()
            return
        }

        when (event.subcommandName) {
            "video" -> handleVideo(event)
            "audio" -> handleAudio(event)
        }
    }

    private fun handleAudio(event: SlashCommandInteractionEvent) {
        val audio = event.getOption("attachment") { obj: OptionMapping -> obj.asAttachment }

        val audioBitrate = event.getOption("audiobitrate", defaultAudioBitrate, OptionMapping::getAsInt)
        val audioSampleRate = event.getOption("audiosamplerate", defaultSamplingRate, OptionMapping::getAsInt)
        val volume = event.getOption("volume", 1, OptionMapping::getAsInt)
        val speed = event.getOption("speed", 1.0, OptionMapping::getAsDouble)
        val pitch = event.getOption("pitch", 1.0, OptionMapping::getAsDouble)
        val af = event.getOption("af", "", OptionMapping::getAsString)

        if (!audio!!.contentType!!.contains("audio")) {
            event.reply("Attachment provided is not a valid audio file").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val audioFile = getFile(audio)

        if (audioFile == null) {
            event.hook.sendMessage("Something went wrong getting the attachment.").setEphemeral(true).queue()
            return
        }

        val compressed =
            compressAudio(audioFile, audio.fileExtension!!, audioBitrate, audioSampleRate, volume, speed, pitch, af)

        event.hook.sendFiles(FileUpload.fromData(compressed)).complete()

        compressed.deleteOnExit()
        audioFile.deleteOnExit()
    }

    private fun handleVideo(event: SlashCommandInteractionEvent) {
        val video = event.getOption("attachment") { obj: OptionMapping -> obj.asAttachment }

        var width = event.getOption("width", null, OptionMapping::getAsInt)
        var height = event.getOption("height", null, OptionMapping::getAsInt)
        val bitrate = event.getOption("bitrate", defaultBitrate, OptionMapping::getAsInt)
        val audioBitrate = event.getOption("audiobitrate", defaultAudioBitrate, OptionMapping::getAsInt)
        val fps = event.getOption("fps", defaultFps, OptionMapping::getAsInt)

        val vf = event.getOption("vf", "", OptionMapping::getAsString)
        val af = event.getOption("af", "",  OptionMapping::getAsString)

        if (!video!!.isVideo) {
            event.reply("Attachment provided is not a valid video file").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val vidFile = getFile(video)

        if (vidFile == null) {
            event.hook.sendMessage("Something went wrong getting the attachment.").setEphemeral(true).queue()
            return
        }

        if (width == null) {
            width = video.width
        }

        if (height == null) {
            height = video.height
        }

        val compressed =
            compressVideo(vidFile, video.fileExtension!!, width, height, bitrate, fps, audioBitrate, vf, af)

        event.hook.sendFiles(FileUpload.fromData(compressed)).complete()

        compressed.deleteOnExit()
        vidFile.deleteOnExit()
    }

    private fun getFile(att: Attachment): File? {
        val file: File?

        val possibleIdentifier = att.fileName + att.size
        if (fileCache.exists(possibleIdentifier)) {
            file = fileCache.get(possibleIdentifier)

            if (file == null) {
                return null
            }
        } else {
            try {
                file = att.proxy.downloadToFile(File.createTempFile("lbshitify", ".${att.fileExtension}")).get()
                fileCache.put(possibleIdentifier, file)
            } catch (e: Exception) {
                Bot.log.error("Error downloading file: $e")
                return null
            }
        }

        return file
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

        FFmpegExecutor(ffmpeg).also {
            it.createJob(builder).run()
        }

        return out
    }

    private fun compressAudio(
        audio: File,
        extension: String,
        bitrate: Int,
        sampling: Int,
        volume: Int,
        speed: Double,
        pitch: Double,
        af: String
    ): File {
        val builder = FFmpegBuilder()

        val out = File.createTempFile("lbaudiojob", ".$extension")

        builder.setInput(audio.path)
            .overrideOutputFiles(true)
            .addOutput(out.path)
            .setAudioBitRate(bitrate.toLong())
            .setAudioSampleRate(sampling)

        builder.setAudioFilter(
            af.ifEmpty {
                "volume=$volume,atempo=$speed,rubberband=pitch=$pitch"
            }
        )

        FFmpegExecutor(ffmpeg).also {
            it.createJob(builder).run()
        }

        return out
    }
}