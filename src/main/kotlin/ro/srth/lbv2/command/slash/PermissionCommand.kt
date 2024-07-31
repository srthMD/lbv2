package ro.srth.lbv2.command.slash

import dev.minn.jda.ktx.interactions.components.getOption
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import ro.srth.lbv2.command.LBCommand
import java.awt.Color

@Suppress("unused")
class PermissionCommand(data: Data) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "view" -> showPermissions(event)
        }
    }

    private fun showPermissions(event: SlashCommandInteractionEvent) {
        val input = event.getOption<String>("command")!!
        val command: LBCommand? = bot.commandManager.getCommandHandler(input)

        if (command == null) {
            event.reply("Command does not exist.").setEphemeral(true).queue()
            return
        }

        val data = command.data
        val perms = data.permissions

        val embed = EmbedBuilder()
            .setColor(Color.white)
            .setTitle("${data.name.replaceFirstChar(Char::uppercase)} permission(s).")
            .setFooter("Made by srth in Java and Kotlin.")

        val permsStr = StringBuilder()

        if (perms == null) {
            permsStr.append("None")
        } else {
            perms.forEachIndexed { index, it ->
                permsStr.append(
                    if (index == perms.size - 1) it!!.name else "${it!!.name}, ",
                )
            }
        }

        embed.addField("Permissions", permsStr.toString(), false)

        event.replyEmbeds(embed.build()).queue()
    }
}