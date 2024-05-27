package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.ExecutionException
import javax.imageio.ImageIO

class AsciifyCommand(data: Data?) : LBCommand(data) {

    companion object {
        const val CHARS = " .:-=+*#%@"
        const val REVERSED = "@%#*+=-:. "
        const val LENGTH = CHARS.length

        const val DEFAULTWIDTH = 100
        const val DEFAULTHEIGHT = 50
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val mapping = event.getOption("image")

        val argWidth = event.getOption("width", DEFAULTWIDTH, OptionMapping::getAsInt)
        val argHeight = event.getOption("height", DEFAULTHEIGHT, OptionMapping::getAsInt)
        val reversed = event.getOption("reversed", false, OptionMapping::getAsBoolean)

        if (mapping == null) {
            event.reply("An error occurred getting the image.").setEphemeral(true).queue()
            return
        }

        val attachment = mapping.asAttachment

        if (!attachment.isImage) {
            event.reply("File is not an image.").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()
        
        try {
            attachment.proxy.download().get().use { imageStream ->
                val image: BufferedImage = downscale(ImageIO.read(imageStream), argWidth, argHeight)

                val width = image.width
                val height = image.height

                val sb = StringBuilder()
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val color = Color(image.getRGB(x, y))
                        val r = color.red
                        val g = color.green
                        val b = color.blue

                        val avg = (r + g + b) / 3

                        sb.append(grayscaleToChar(avg, reversed))
                    }
                    sb.append("\n")
                }

                image.flush()
                imageStream.close()

                val raw = sb.toString()

                val hook = event.hook
                FileUpload.fromData(toByteArray(raw), "ascii.txt").use { upload ->
                    hook.sendFiles(upload).queue()
                }
            }
        } catch (e: Exception) {
            when (e) {
                is IOException, is ExecutionException, is InterruptedException -> {
                    event.reply("Something went wrong while executing the command.").setEphemeral(true).queue()
                    Bot.log.error(
                        "Error running AsciifyCommand from user: {}\n Message: {},\n Stacktrace: \n{}",
                        event.user.id,
                        e.message,
                        e.stackTrace.contentToString()
                    )
                }
                else -> throw e
            }
        }
    }

    private fun grayscaleToChar(avg: Int, reversed: Boolean): Char {
        return if (reversed) {
            REVERSED[(LENGTH - 1) * avg / 255]
        } else {
            CHARS[(LENGTH - 1) * avg / 255]
        }
    }

    private fun downscale(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
        val resultingImage = originalImage.getScaledInstance(width, height, Image.SCALE_DEFAULT)
        val outputImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        outputImage.graphics.drawImage(resultingImage, 0, 0, null)

        return outputImage
    }

    private fun toByteArray(raw: String): ByteArray {
        val bytes = ByteArray(raw.length)
        for (i in raw.indices) {
            val c = raw[i]

            bytes[i] = c.code.toByte()
        }

        return bytes
    }
}