package ro.srth.lbv2.command.slash

import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand

@Suppress("unused")
class SayCommand(data: Data) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val str = event.getOption<String>("msg")!!
        val attachment = event.getOption("attachment", OptionMapping::getAsAttachment)

        val interaction = event.channel.sendMessage(str)

        val reply = if (attachment == null) "Sending message..." else "Downloading attachment..."
        event.reply(reply).setEphemeral(true).queue()

        attachment?.proxy?.download()?.get()?.use { download ->
            val rand = bot.rand().nextLong()
            interaction.addFiles(FileUpload.fromData(download, "attachment${rand}.${attachment.fileExtension}"))
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