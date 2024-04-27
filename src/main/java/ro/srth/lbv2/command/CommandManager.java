package ro.srth.lbv2.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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

public class CommandManager {

    public static void registerCommands() {
        JDA bot = Bot.getBot();

        List<LBCommand.Data> commandData;

        try {
            commandData = parseData();
        } catch (FileNotFoundException e) {
            Bot.log.error("FileNotFoundException while registering commands: " + e.getMessage());
            return;
        }

        for (LBCommand.Data data : commandData) {
            if(!data.shouldRegister()){
                continue;
            }

            LBCommand command = safeNewInstance(data.backendClass());

            if(command == null){
                Bot.log.warn("Skipping command " + data.name() + " because safeNewInstance returned null.");
                continue;
            }

            SlashCommandData cmdData = Commands.slash(data.name(), data.description());

            String guildId = data.guildId();
            OptionData[] options = data.options();
            Permission[] permissions = data.permissions();
            SubcommandData[] subCmds = data.subCommands();

            //i hate this
            if(options != null){
                cmdData.addOptions(options);
            }

            if(permissions != null){
                cmdData.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions));
            }

            if(subCmds != null){
                cmdData.addSubcommands(subCmds);
            }

            if(guildId == null){
                bot.upsertCommand(cmdData).queue(
                        (suc) -> {
                            bot.addEventListener(command);
                            Bot.log.info("Registered global command " + data.name());
                        },
                        (err) -> Bot.log.warn("Failed to register global command " + data.name() + ": " + err.getMessage())
                );
            } else{
                Guild g = bot.getGuildById(guildId);

                if(g == null){
                    Bot.log.error("Cannot upsert command " + data.name() + " beause the guild id is invalid");
                    continue;
                }

                g.upsertCommand(cmdData).queue(
                        (suc) -> {
                            bot.addEventListener(command);
                            Bot.log.info("Registered guild command " + data.name());
                        },
                        (err) -> Bot.log.warn("Failed to register guild command " + data.name() + ": " + err.getMessage())
                );
            }
        }
    }

    private static List<LBCommand.Data> parseData() throws FileNotFoundException {
        List<File> jsons = getJson();

        if(jsons == null){
            Bot.log.warn("getJson returned null");
            return null;
        }

        List<LBCommand.Data> dataList = new ArrayList<>(jsons.size());

        for (File json : jsons) {
            BufferedReader reader = new BufferedReader(new FileReader(json));

            //it works
            String raw = reader.lines().collect(Collectors.joining());

            try {
                reader.close();
            } catch (IOException e) {
                Bot.log.warn("IOException when closing reader parseData: " + e.getMessage());
            }

            try{
                JSONObject jsonObj = new JSONObject(raw);

                Class<? extends LBCommand> backend;

                try {
                    backend = getBackendClass(jsonObj);
                } catch (ClassNotFoundException e) {
                    Bot.log.error("Backend class not found for file " + json.getName());
                    continue;
                } catch (InvalidCommandClassException e) {
                    Bot.log.error("InvalidCommandClassException for classpath " + e.getClasspath() + ": " + e.getMessage());
                    continue;
                }

                //notnull stuff
                String name = jsonObj.getString("name");
                String desc = jsonObj.getString("description");
                boolean register = jsonObj.getBoolean("register");

                //nullable stuff
                String guildId = getGuildId(jsonObj);
                Permission[] permissions = getPermissions(jsonObj);
                OptionData[] optionData = getOptionData(jsonObj);

                LBCommand.Data data = new LBCommand.Data(name, desc, register, backend, guildId, optionData, permissions, null);

                dataList.add(data);

            } catch (JSONException e){
                Bot.log.error("Unknown JSONException registering command: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            }
        }
        return dataList;
    }

    @Nullable
    private static OptionData[] getOptionData(@NotNull JSONObject obj){
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

                OptionData dat = new OptionData(optionType, name, description, required);

                data[i] = dat;
            }catch (JSONException e){
                Bot.log.error("Unknown JSONException getting option data: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            }
        }

        return data;
    }

    @Nullable
    private static Permission[] getPermissions(@NotNull JSONObject obj){
        Objects.requireNonNull(obj);

        JSONArray permissions;

        try{
            permissions = obj.getJSONArray("options");
        } catch (JSONException e){
            return null;
        }

        Permission[] perms = new Permission[permissions.length()];

        for (int i = 0; i < permissions.length(); i++) {
            try{
                Permission p = Permission.getFromOffset(permissions.getInt(i));
                perms[i] = p;
            } catch (JSONException e){
                Bot.log.error("Unknown JSONException getting permissions: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
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
    private static LBCommand safeNewInstance(Class<? extends LBCommand> command){
        try {
            Constructor<? extends LBCommand> c = command.getConstructor();

            return c.newInstance();
        } catch (NoSuchMethodException e) {
            Bot.log.error("No such constructor for class " + command.getCanonicalName());
            return null;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Bot.log.error(e.getClass().getCanonicalName() + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private static List<File> getJson() throws FileNotFoundException {
        File jsonFolder = new File("cmds");

        if(!jsonFolder.exists()){
            Bot.log.warn("cmds folder does not exist, creating new folder...");
            boolean suc = jsonFolder.mkdir();
            if(!suc){
               Bot.log.warn("cmds folder creation unsuccessful");
            }
            throw new FileNotFoundException("Slash commands data folder does not exist!");
        }

        File[] files = jsonFolder.listFiles();

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
}
