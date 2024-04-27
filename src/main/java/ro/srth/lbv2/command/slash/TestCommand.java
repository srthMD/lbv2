package ro.srth.lbv2.command.slash;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import ro.srth.lbv2.command.LBCommand;

public class TestCommand extends LBCommand {

    @Override
    public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
        event.reply("Hello").queue();
    }
}
