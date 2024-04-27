package ro.srth.lbv2.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public abstract class LBCommand extends ListenerAdapter {
    protected Data data;

    public Data getData(){
        return data;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        runSlashCommand(event);
    }

    public abstract void runSlashCommand(@NotNull SlashCommandInteractionEvent event);



    /**
     * Record that holds information about a command, mainly holding data
     * needed to register a slash command, like the description and options.
     * @param name The name of the command.
     * @param description The description of the command.
     * @param shouldRegister Dictates if the command is active or not.
     * @param backendClass The class responsible for handling the interaction with the command.
     * @param guildId Optional guild id parameter if this command is a guild command.
     * @param options The options for this command. (optional)
     * @param permissions The required server {@link Permission permissions} for this command. (optional)
     * @param subCommands The sub commands of this command, with its options attached. (optional)
     */
    public record Data(
            String name,
            String description,
            boolean shouldRegister,
            Class<? extends LBCommand> backendClass,
            @Nullable String guildId,
            @Nullable OptionData[] options,
            @Nullable Permission[] permissions,
            @Nullable SubcommandData[] subCommands)
    {}
}
