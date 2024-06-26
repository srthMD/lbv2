package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand

@Suppress("unused")
class SayCommand(data: Data) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val str = event.getOption("msg", "", OptionMapping::getAsString)
        val attachment = event.getOption("attachment", OptionMapping::getAsAttachment)

        val interaction = event.channel.sendMessage(str)

        val reply = if (attachment == null) "Sending message..." else "Downloading attachment..."
        event.reply(reply).setEphemeral(true).queue()


        if (attachment != null) {
            attachment.proxy.download().get().use { download ->
                val rand = bot.rand().nextLong()
                interaction.addFiles(FileUpload.fromData(download, "attachment${rand}.${attachment.fileExtension}"))
            }
        } else if (str.isEmpty()) {
            event.reply("You must send at least a message or attachment.").setEphemeral(true).queue()
        }

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