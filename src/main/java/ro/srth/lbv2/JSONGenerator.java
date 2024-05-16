package ro.srth.lbv2;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.json.JSONArray;
import org.json.JSONObject;
import ro.srth.lbv2.command.slash.AsciifyCommand;

import javax.swing.filechooser.FileSystemView;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class JSONGenerator {
    public static void main(String[] args) {
        String name = "asciify";
        String description = "Turns an image into a bunch of ascii characters.";
        String classPath = AsciifyCommand.class.getName();
        boolean register = true;

        String guildId = null;
        Permission[] perms = null;
        OptionData[] options = {
                new OptionData(OptionType.ATTACHMENT, "image", "The image to asciify").setRequired(true),
                new OptionData(OptionType.INTEGER, "width", "The width in characters of the ascii result. Default is `100`.")
                        .setRequired(false)
                        .setMinValue(20)
                        .setMaxValue(145),
                new OptionData(OptionType.INTEGER, "height", "The height in characters of the ascii result. Default is 50.")
                        .setRequired(false)
                        .setMinValue(20)
                        .setMaxValue(50),
                new OptionData(OptionType.BOOLEAN, "inverted", "Decides if the character set is reversed (darker = denser character).")
                        .setRequired(false)
        };
        SubcommandData[] subCmds = null;

        JSONObject obj = new JSONObject();

        obj.put("name", name)
                .put("description", description)
                .put("register", register)
                .put("backendClass", classPath);


        if(guildId != null){
            JSONObject guildObj = new JSONObject();
            guildObj.put("id", guildId);

            obj.put("guildInfo", guildObj);
        }

        if(perms != null){
            JSONArray permsObj = new JSONArray();
            for (Permission perm : perms) {
                permsObj.put(perm.getOffset());
            }

            obj.put("permissions", permsObj);
        }

        if(options != null){
            obj.put("options", generateOptions(List.of(options)));
        }

        if(subCmds != null){
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

        System.out.println("Creating new file in project directory " + name + ".json");
        File file = new File(FileSystemView.getFileSystemView().getHomeDirectory(), name + ".json");
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(file.getAbsolutePath());

        try (BufferedWriter fw = new BufferedWriter(new FileWriter(file))){
            obj.write(fw, 4, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JSONArray generateOptions(Collection<OptionData> data){
        JSONArray optionArr = new JSONArray();

        for (OptionData option : data) {
            JSONObject optionObj = new JSONObject();

            optionObj.put("name", option.getName())
                    .put("description", option.getDescription())
                    .put("type", option.getType().getKey())
                    .put("required", option.isRequired());


            switch (option.getType()){
                case STRING -> {
                    var ranges = new JSONObject();

                    if(option.getMinLength() != null){
                        ranges.put("minLength", option.getMinLength());
                    }

                    if(option.getMaxLength() != null){
                        ranges.put("maxLength", option.getMaxLength());
                    }

                    optionObj.put("ranges", ranges);
                }
                case INTEGER -> {
                    var ranges = new JSONObject();

                    if(option.getMinValue() != null){
                        ranges.put("minInt", option.getMinValue());
                    }

                    if(option.getMaxValue() != null){
                        ranges.put("maxInt", option.getMaxValue());
                    }

                    optionObj.put("ranges", ranges);
                }
            }

            optionArr.put(optionObj);
        }

        return optionArr;
    }
}
