package ro.srth.lbv2.command.slash

import club.minnced.jda.reactor.toMono
import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import reactor.core.scheduler.Schedulers
import ro.srth.lbv2.command.LBCommand
import ro.srth.lbv2.util.toImage
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


@Suppress("unused")
class AsciifyCommand(data: Data) : LBCommand(data) {
    private val charSet: String
    private val defaultWidth: Int
    private val defaultHeight: Int

    init {
        val obj = data.attachedData!!
        this.charSet = obj.getString("charSet")
        this.defaultWidth = obj.getInt("defaultWidth")
        this.defaultHeight = obj.getInt("defaultHeight")
    }

    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val attachment = event.getOption<Message.Attachment>("image")!!

        val argWidth = event.getOption("width", defaultWidth, OptionMapping::getAsInt)
        val argHeight = event.getOption("height", defaultHeight, OptionMapping::getAsInt)
        val reversed = event.getOption("reversed", false, OptionMapping::getAsBoolean)

        if (!attachment.isImage) {
            event.reply("Attachment is not an image.").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue { interaction ->
            val result = attachment.toMono()
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull { it.toImage() }
                .map { asciify(it, argWidth, argHeight, reversed) }
                .block(10.seconds.toJavaDuration())

            FileUpload.fromData(toByteArray(result!!), "ascii.txt").use {
                interaction.sendFiles(it).queue()
            }
        }
    }

    private fun asciify(
        img: BufferedImage?,
        argWidth: Int,
        argHeight: Int,
        reversed: Boolean
    ): String {
        val image = downscale(img!!, argWidth, argHeight)

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

        val raw = sb.toString()
        return raw
    }

    private fun grayscaleToChar(avg: Int, reversed: Boolean): Char {
        return if (reversed) {
            charSet.reversed()[(charSet.length - 1) * avg / 255]
        } else {
            charSet[(charSet.length - 1) * avg / 255]
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