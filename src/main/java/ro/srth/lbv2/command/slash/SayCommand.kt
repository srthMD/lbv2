package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import ro.srth.lbv2.Bot
import ro.srth.lbv2.command.LBCommand
import java.util.concurrent.ThreadLocalRandom

class SayCommand(data: Data?) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        val str = event.getOption("msg", "", OptionMapping::getAsString)
        val attachment = event.getOption("attachment", OptionMapping::getAsAttachment)

        val interaction = event.channel.sendMessage(str)
        event.reply("Sending message...").setEphemeral(true).queue()


        if (attachment != null) {
            val stream = attachment.proxy.download().get()
            val rand = ThreadLocalRandom.current().nextLong()
            interaction.addFiles(FileUpload.fromData(stream, "attachment${rand}.${attachment.fileExtension}"))
        } else{
            if(str.isEmpty()){
                event.reply("You must send atleast a message or atatchment").setEphemeral(true).queue()
            }
        }

        interaction.queue(
            {
                event.hook.editOriginal("done").queue()
            },
            { err ->
                event.hook.editOriginal("An error occured while sending").queue()
                Bot.log.error(err.javaClass.canonicalName + " while using /say: ${err.message}")
            })
    }
}