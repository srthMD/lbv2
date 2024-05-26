package ro.srth.lbv2;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.srth.lbv2.command.slash.SayCommand;


import javax.swing.filechooser.FileSystemView;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@SuppressWarnings(value = {"unused", "ConstantValue", "UnreachableCode"})
public class JSONGenerator {
    public static void main(String[] args) {
        String name = "say";
        String description = "Makes the bot say something";
        String classPath = SayCommand.class.getName();
        boolean register = true;

        String guildId = null;
        Permission[] perms = {
                Permission.ADMINISTRATOR
        };
        OptionData[] options = {
                new OptionData(OptionType.STRING, "msg", "whatever you want the bot to say, put a space if you want just an attachment").setRequired(true),
                new OptionData(OptionType.ATTACHMENT, "attachment", "the attachment you want to send"),
        };
        SubcommandData[] subCmds = null;

        JSONObject obj = new JSONObject();

        obj.put("name", name)
                .put("description", description)
                .put("register", register)
                .put("backendClass", classPath);


        if(guildId != null) {
            JSONObject guildObj = new JSONObject();
            guildObj.put("id", guildId);

            obj.put("guildInfo", guildObj);
        }

        if(perms != null) {
            JSONArray permsObj = new JSONArray();
            for (Permission perm : perms) {
                permsObj.put(perm.getOffset());
            }

            obj.put("permissions", permsObj);
        }

        if(options != null) {
            obj.put("options", generateOptions(List.of(options)));
        }

        if(subCmds != null) {
            JSONArray subArr = new JSONArray();

            for (SubcommandData subCmd : subCmds) {
                JSONObject optionObj = new JSONObject();

                optionObj.put("name", subCmd.getName())
                        .put("description", subCmd.getDescription());

                JSONArray optionArr = generateOptions(subCmd.getOptions());

                optionObj.put("options", optionArr);

                subArr.put(optionObj);
            }

            obj.put("subCmds", subArr);
        }

        System.out.println("Creating new file at user home: " + name + ".json");
        File file = new File(FileSystemView.getFileSystemView().getHomeDirectory(), name + ".json");

        if(!file.exists()) {
            try {
                boolean suc = file.createNewFile();

                if(!suc){
                    System.out.println("Could not create file: " + name + ".json");
                }
            } catch (IOException e) {
                System.err.println("Could not create file: " + name + ".json" + e.getMessage());
            }
        }

        try (BufferedWriter fw = new BufferedWriter(new FileWriter(file))) {
            obj.write(fw, 4, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JSONArray generateOptions(Collection<OptionData> data) {
        JSONArray optionArr = new JSONArray();

        for (OptionData option : data) {
            JSONObject optionObj = new JSONObject();

            optionObj.put("name", option.getName())
                    .put("description", option.getDescription())
                    .put("type", option.getType().getKey())
                    .put("required", option.isRequired());


            attachRanges(option, optionObj);
            attachChoices(option, optionObj);

            optionArr.put(optionObj);
        }


        return optionArr;
    }

    private static void attachRanges(OptionData option, JSONObject optionObj) {
        var ranges = new JSONObject();

        switch (option.getType()){
            case STRING -> {
                if(option.getMinLength() != null){
                    ranges.put("minLength", option.getMinLength());
                }

                if(option.getMaxLength() != null){
                    ranges.put("maxLength", option.getMaxLength());
                }

                if(!ranges.isEmpty()){
                    optionObj.put("ranges", ranges);
                }
            }
            case INTEGER -> {
                if(option.getMinValue() != null){
                    ranges.put("minInt", option.getMinValue());
                }

                if(option.getMaxValue() != null){
                    ranges.put("maxInt", option.getMaxValue());
                }

                if(!ranges.isEmpty()){
                    optionObj.put("ranges", ranges);
                }
            }
        }
    }

    private static void attachChoices(OptionData option, JSONObject optionObj){
        var choices = new JSONArray();
        for (Command.Choice choice : option.getChoices()) {

            var block = new JSONObject();

            block.put("name", choice.getName());
            switch (choice.getType()){
                case STRING -> block.put("val", choice.getAsString());
                case INTEGER -> block.put("val", choice.getAsLong());
                case NUMBER -> block.put("val", choice.getAsDouble());
            }

            choices.put(block);
        }

        if(!choices.isEmpty()) {
            optionObj.put("choices", choices);
        }
    }
}
