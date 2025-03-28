package ro.srth.lbv2.command.slash

import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

//فكرة إنتلييج

@Suppress("unused")
class SayCommand(data: Data) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val str = event.getOption<String>("msg")!!
        val attachment = event.getOption("attachment", OptionMapping::getAsAttachment)

        val interaction = event.channel.sendMessage(str)

        if (attachment != null) {
            event.reply("Downloading attachment...").setEphemeral(true).queue()

            val file: InputStream
            try {
                file = attachment.proxy.download().get(5, TimeUnit.SECONDS);
            } catch (e: TimeoutException) {
                event.hook.editOriginal("Timed out while downloading attachment.").queue()
                return
            }

            interaction.addFiles(FileUpload.fromData(file, attachment.fileName))
        } else {
            event.reply("Sending message...").setEphemeral(true).queue()
        }

        @Suppress("InconsistentCommentForJavaParameter")
        interaction.queue(
            /* success = */ {
                event.hook.editOriginal("done").queue()
            },
            /* failure = */ { err ->
                event.hook.editOriginal("An error occurred while sending").queue()
                Bot.log.error(err.javaClass.canonicalName + " while using /say: ${err.message}")
            }
        )
    }
}