package ro.srth.lbv2.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ro.srth.lbv2.Bot;
import ro.srth.lbv2.exception.InvalidCommandClassException;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages registering all commands from JSON data to discord.
 * <br>
 * After logon, the bot will usually call {@link #registerCommands(boolean) registerCommamnds} first.
 */
public class CommandManager {

    private static final JDA bot = Bot.getBot();

    private static final File CMDPATH = new File("cmds");

    /**
     * (Unimplemented)
     * <br>
     * Updates a single command in the bot, if the command is not registered in discord, an error will be logged (not thrown).
     * <br>
     * <br>
     * For changes to take effect, the JSON data file of the command must be replaced by its old one in the cmds directory.
     * The name of the JSON file must also be the same as the command name.
     * @param id The ID of the command represented in the discord client.
     */
    public static void upsertCommand(String id) {
        Command command = bot.retrieveCommandById(id).onErrorMap(throwable -> null).complete();

        if (command == null){
            Bot.log.error("Command id {} does not exist.", id);
            return;
        }

        File json = jsonFromName(command.getName());

        if(json == null){
            Bot.log.error("{} does not exist!", command.getName() + ".json");
            return;
        }

        String raw;

        try {
            raw = raw(json);
        } catch (FileNotFoundException e) {
            return;
        }

        LBCommand.Data data = fromJSON(new JSONObject(raw));

        SlashCommandData slashCmd = data.toSlashCommand();

        command.editCommand().apply(slashCmd).queue(
                (suc) -> {},
                (fail) -> Bot.log.error("Failed to edit command {}: {}", id, fail.getMessage())
        );
    }

    /**
     * The main entry point to register commands, updating or registering commands as needed and setting up event listeners.
     * If not starting from a cold start (determined by the --register argument), then only the event listeners will be set up.
     * @param coldstart Decides whether to register commands to discord or not.
     */
    public static void registerCommands(boolean coldstart) {
        JDA bot = Bot.getBot();

        if(coldstart){
            wipeUselessCommands();
        }

        List<LBCommand.Data> commandData;

        try {
            commandData = parseData();
        } catch (FileNotFoundException e) {
            Bot.log.error("FileNotFoundException while registering commands: {}", e.getMessage());
            return;
        }

        if(commandData == null){
            return;
        }


        for (LBCommand.Data data : commandData) {
            if(!data.shouldRegister()){
                continue;
            }

            LBCommand command = safeNewInstance(data.backendClass(), data);

            if(command == null){
                Bot.log.warn("Skipping command {} because safeNewInstance returned null.", data.name());
                continue;
            }

            SlashCommandData cmdData = data.toSlashCommand();

            String guildId = data.guildId();

            if(guildId == null){
                if(coldstart){
                    bot.upsertCommand(cmdData).queue(
                            (suc) -> {
                                bot.addEventListener(command);
                                Bot.log.info("Registered global command {}", data.name());
                            },
                            (err) -> Bot.log.warn("Failed to register global command {}: {}", data.name(), err.getMessage())
                    );
                } else{
                    bot.addEventListener(command);
                }
            } else{
                if(coldstart){
                    Guild g = bot.getGuildById(guildId);

                    if(g == null){
                        Bot.log.error("Cannot upsert command {} beause the guild id is invalid", data.name());
                        continue;
                    }

                    g.upsertCommand(cmdData).queue(
                            (suc) -> {
                                bot.addEventListener(command);
                                Bot.log.info("Registered guild command {}", data.name());
                            },
                            (err) -> Bot.log.warn("Failed to register guild command {}: {}", data.name(), err.getMessage())
                    );
                } else{
                    bot.addEventListener(command);
                }
            }
        }
    }


    private static void wipeUselessCommands(){
        for (Command command : bot.retrieveCommands().complete()) {
            File f = jsonFromName(command.getName());

            if(f == null){
                command.delete().queue();
            }
        }
    }


    private static List<LBCommand.Data> parseData() throws FileNotFoundException {
        List<File> jsons = getJsonFiles();

        if(jsons == null){
            Bot.log.warn("getJson returned null");
            return null;
        }

        List<LBCommand.Data> dataList = new ArrayList<>(jsons.size());

        for (File json : jsons) {
            String raw = raw(json);

            try{
                JSONObject jsonObj = new JSONObject(raw);

                LBCommand.Data data = fromJSON(jsonObj);
                if (data == null) continue;

                dataList.add(data);

            } catch (JSONException e){
                Bot.log.error("Unknown JSONException registering command: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        }
        return dataList;
    }


    @NotNull
    private static String raw(File json) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(json));

        //it works
        String raw = reader.lines().collect(Collectors.joining());

        try {
            reader.close();
        } catch (IOException e) {
            Bot.log.warn("IOException when closing reader parseData: {}", e.getMessage());
        }
        return raw;
    }


    @Nullable
    private static LBCommand.Data fromJSON(JSONObject jsonObj) {
        Class<? extends LBCommand> backend;

        try {
            backend = getBackendClass(jsonObj);
        } catch (ClassNotFoundException e) {
            Bot.log.error("Backend class not found, classpath: {}", jsonObj.getString("backendClass"));
            return null;
        } catch (InvalidCommandClassException e) {
            Bot.log.error("InvalidCommandClassException for classpath {}: {}", e.getClasspath(), e.getMessage());
            return null;
        }

        //notnull stuff
        String name = jsonObj.getString("name");
        String desc = jsonObj.getString("description");
        boolean register = jsonObj.getBoolean("register");

        //nullable stuff
        String guildId = getGuildId(jsonObj);
        Permission[] permissions = getPermissions(jsonObj);
        OptionData[] optionData = getOptionData(jsonObj);

        return new LBCommand.Data(name, desc, register, backend, guildId, optionData, permissions, null);
    }

    @Nullable
    private static OptionData[] getOptionData(@NotNull JSONObject obj) {
        Objects.requireNonNull(obj);

        JSONArray options;

        try{
            options = obj.getJSONArray("options");
        } catch (JSONException e){
            return null;
        }

        OptionData[] data = new OptionData[options.length()];

        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);

            try{
                String name = option.getString("name");
                String description = option.getString("description");
                boolean required = option.getBoolean("required");
                OptionType optionType = OptionType.fromKey(option.getInt("type"));

                JSONObject range = option.optJSONObject("range");
                JSONArray choices = option.optJSONArray("choices");


                OptionData dat = new OptionData(optionType, name, description, required);

                setRanges(range, dat);
                setChoices(choices, dat);

                data[i] = dat;
            }catch (JSONException e){
                Bot.log.error("Unknown JSONException getting option data: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        }

        return data;
    }

    //extracted
    private static void setRanges(JSONObject range, OptionData dat) {
        if(!(range == null)){
            switch (dat.getType()){
                case NUMBER -> {
                    long min = range.optInt("minInt", -1);
                    long max = range.optInt("maxInt", -1);

                    if(min != -1){
                        dat.setMinValue(min);
                    }

                    if(max != -1){
                        dat.setMaxValue(max);
                    }
                }
                case STRING -> {
                    int min = range.optInt("minLength", -1);
                    int max = range.optInt("maxLength", -1);

                    if(min != -1){
                        dat.setMinLength(min);
                    }

                    if(max != -1){
                        dat.setMaxLength(max);
                    }
                }
            }
        }
    }

    private static void setChoices(JSONArray choices, OptionData dat) {
        if(!(choices == null)){
            switch (dat.getType()){
                //i tried using Function idfk why it didnt work
                case STRING -> {
                    for(int i = 0; i < choices.length(); i++){
                        JSONObject choice = choices.getJSONObject(i);

                        String name = choice.getString("name");
                        String val = choice.getString("val");

                        dat.addChoice(name, val);
                    }
                }
                case INTEGER -> {
                    for(int i = 0; i < choices.length(); i++){
                        JSONObject choice = choices.getJSONObject(i);

                        String name = choice.getString("name");
                        int val = choice.getInt("val");

                        dat.addChoice(name, val);
                    }
                }
                case NUMBER -> {
                    for(int i = 0; i < choices.length(); i++){
                        JSONObject choice = choices.getJSONObject(i);

                        String name = choice.getString("name");
                        double val = choice.getDouble("val");

                        dat.addChoice(name, val);
                    }
                }
            }
        }
    }

    @Nullable
    private static Permission[] getPermissions(@NotNull JSONObject obj) {
        Objects.requireNonNull(obj);

        JSONArray permissions = obj.optJSONArray("permissions");

        if(permissions == null){
            return null;
        }

        Permission[] perms = new Permission[permissions.length()];

        for (int i = 0; i < permissions.length(); i++) {
            try{
                Permission p = Permission.getFromOffset(permissions.getInt(i));
                perms[i] = p;
            } catch (JSONException e){
                Bot.log.error("Unknown JSONException getting permissions: {}\n{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        }

        return perms;
    }


    @Nullable
    private static String getGuildId(@NotNull JSONObject obj) {
        Objects.requireNonNull(obj);

        try{
            JSONObject guildInfo = obj.getJSONObject("guildInfo");

            return guildInfo.getString("id");
        } catch (JSONException e){
            return null;
        }
    }


    @SuppressWarnings(value = "unchecked")
    private static Class<? extends LBCommand> getBackendClass(@NotNull JSONObject obj) throws ClassNotFoundException, InvalidCommandClassException {
        Objects.requireNonNull(obj);

        String classpath;
        
        try{
            classpath = obj.getString("backendClass");
        } catch (JSONException e){
            return null;
        }
        
        //if the json specifies a different path return null
        if(!classpath.startsWith("ro.srth.lbv2.command.slash")){
            throw new InvalidCommandClassException(classpath, "Improper classpath, must be child of slash package.");
        }
        
        Class<?> clazz = Class.forName(classpath);
        
        //if the class does not extend LBCommand, return null
        if(!clazz.getSuperclass().equals(LBCommand.class)){
            throw new InvalidCommandClassException(classpath, "Class does not extend LBCommand");
        }

        //should be fine when above if statement passes
        return (Class<? extends LBCommand>) clazz;
    }


    @Nullable
    private static LBCommand safeNewInstance(Class<? extends LBCommand> command, LBCommand.Data dat){
        try {
            Constructor<? extends LBCommand> c = command.getConstructor(LBCommand.Data.class);

            return c.newInstance(dat);
        } catch (NoSuchMethodException e) {
            Bot.log.error("No such constructor for class {}", command.getCanonicalName());
            return null;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Bot.log.error("{}: {}", e.getClass().getCanonicalName(), e.getMessage());
            return null;
        }
    }


    @Nullable
    private static List<File> getJsonFiles() {
        boolean b = doesCmdPathExist();

        if(!b){
            return null;
        }

        File[] files = CMDPATH.listFiles();

        if(files == null){
            return null;
        }

        List<File> jsons = new ArrayList<>(files.length);

        for (File file : files) {
            String fileName = file.getName();
            if (fileName.trim().substring(fileName.lastIndexOf('.')+1).equalsIgnoreCase("json")){
                jsons.add(file);
            }
        }

        return jsons;
    }


    @Nullable
    private static File jsonFromName(String name) {
        boolean b = doesCmdPathExist();

        if(!b){
            return null;
        }

        File json = new File(CMDPATH, name + ".json");

        if(!json.exists()){
            return null;
        } else {
            return json;
        }
    }


    private static boolean doesCmdPathExist(){
        if(!CMDPATH.exists()){
            Bot.log.warn("cmds folder does not exist, creating new folder...");
            boolean suc = CMDPATH.mkdir();
            if(!suc){
                Bot.log.warn("cmds folder creation unsuccessful");
            }
            return false;
        }
        return true;
    }
}
