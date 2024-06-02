package ro.srth.lbv2.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import ro.srth.lbv2.Bot;

/**
 * The base class for all slash command handler classes.
 * Every command should extend this class and implement {@link #runSlashCommand(SlashCommandInteractionEvent)}.
 */
public abstract class LBCommand {
    private final Data data;

    protected final Bot bot = Bot.getInstance();

    public LBCommand(@NotNull Data data) {
        this.data = data;
    }

    public Data getData(){
        return data;
    }

    /**
     * Preforms some checks and necessary work before running a slash command.
     */
    public final void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try{
            if(event.isAcknowledged()){
                return;
            }

            if(event.getName().equals(this.data.name)){
                runSlashCommand(event);
            }
        } catch (Exception e){
            if(event.isAcknowledged()){
                event.getHook().sendMessage("An unknown error occurred.").queue();
            } else{
                event.reply("An unknown error occurred.").setEphemeral(true).queue();
            }

            Bot.log.error("{} running command {}: {} \n{}", e.getClass().getCanonicalName(), event.getFullCommandName(), e.getMessage(), e.getStackTrace());
        }
    }

    /**
     * The actual implementation of a subclass' slash command handler.
     */
    public abstract void runSlashCommand(@NotNull SlashCommandInteractionEvent event);

    /**
     * Holds most of the data needed to register a command to discord.
     * @param shouldRegister Dictates if the command appears in discord or not.
     * @param backendClass The class responsible for handling the interaction with the command.
     * @param guildId Optional guild id parameter if this command is a guild command.
     * @param options The options for this command. (optional)
     * @param permissions The required server {@link Permission permissions} for this command. (optional)
     * @param subCommands The sub commands of this command, with its options attached. (optional)
     * @param attachedData Any attached data that the command may use internally in its handler (optional)
     */
    public record Data(
            String name,
            String description,
            boolean shouldRegister,
            Class<? extends LBCommand> backendClass,
            @Nullable String guildId,
            @Nullable OptionData[] options,
            @Nullable Permission[] permissions,
            @Nullable SubcommandData[] subCommands,
            @Nullable JSONObject attachedData)
    {
        public SlashCommandData toSlashCommand(){
            SlashCommandData cmdData = Commands.slash(this.name(), this.description());

            //I hate this
            if(options != null){
                cmdData.addOptions(options);
            }

            if(permissions != null){
                cmdData.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions));
            }

            if(subCommands != null){
                cmdData.addSubcommands(subCommands);
            }

            return cmdData;
        }
    }
}
