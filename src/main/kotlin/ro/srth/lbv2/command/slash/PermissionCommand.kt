package ro.srth.lbv2.command.slash

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import ro.srth.lbv2.command.CommandManager
import ro.srth.lbv2.command.LBCommand
import java.awt.Color

@Suppress("unused")
class PermissionCommand(data: Data) : LBCommand(data) {
    override fun runSlashCommand(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "view" -> showPermissions(event)
            "override" -> overridePermissions(event)
        }
    }

    private fun showPermissions(event: SlashCommandInteractionEvent) {
        val input = event.getOption("command", null, OptionMapping::getAsString)
            ?: throw NullPointerException("Input was null.")

        val command: LBCommand? = CommandManager.getCommandHandler(input)

        if (command == null) {
            event.reply("Command does not exist.").setEphemeral(true).queue()
            return
        }

        val data = command.data
        val perms = data.permissions

        val embed = EmbedBuilder()
            .setColor(Color.white)
            .setTitle("${data.name.replaceFirstChar { it.uppercase() }} permission(s).")
            .setFooter("Made by srth in Java and Kotlin.")

        var permsStr = ""

        if (perms == null) {
            permsStr = "None"
        } else {
            perms.forEachIndexed { index, it ->
                permsStr += if (index == perms.size - 1) {
                    it.name
                } else {
                    "${it.name}, "
                }
            }
        }
        embed.addField("Permissions", permsStr, false)

        event.replyEmbeds(embed.build()).queue()
    }

    private fun overridePermissions(event: SlashCommandInteractionEvent) {
        TODO("Not yet implemented")
    }
}