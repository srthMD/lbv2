package ro.srth.lbv2.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LBCommand extends ListenerAdapter {
    protected Data attachment;

    public Data getData(){
        return attachment;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        runSlashCommand(event);
    }

    public abstract void runSlashCommand(@NotNull SlashCommandInteractionEvent event);

    /**
     * Holds most of the data needed
     * @param shouldRegister Dictates if the command appears in discord or not.
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
