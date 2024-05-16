package ro.srth.lbv2.command.slash;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import ro.srth.lbv2.Bot;
import ro.srth.lbv2.command.LBCommand;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class AsciifyCommand extends LBCommand {
    private static final String CHARS = " .:-=+*#%@";
    private static final String REVERSED = new StringBuilder(CHARS).reverse().toString();
    private static final int LENGTH = CHARS.length();

    private static final int DEFAULTWIDTH = 100;
    private static final int DEFAULTHEIGHT = 50;
    
    private static Color[] palette;

    static {
        palette = new Color[CHARS.length()];
        
        int color = 0;
        for (int i = 0; i < CHARS.length(); i++) {
            palette[i] = new Color(color, color, color);
            color += 255/CHARS.length();
        }
    }

    @Override
    public void runSlashCommand(@NotNull SlashCommandInteractionEvent event) {
        var mapping = event.getOption("image");

        int argWidth = event.getOption("width", DEFAULTWIDTH, OptionMapping::getAsInt);
        int argHeight = event.getOption("height", DEFAULTHEIGHT, OptionMapping::getAsInt);

        boolean reversed = event.getOption("reversed", true, OptionMapping::getAsBoolean);

        if(mapping == null){
            event.reply("An error occurred getting the image.").setEphemeral(true).queue();
            return;
        }

        var attachment = mapping.getAsAttachment();

        if(!attachment.isImage()){
            event.reply("File is not an image.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).setEphemeral(true).queue();


        try(InputStream imageStream = attachment.getProxy().download().get()) {
            var image = downscale(ImageIO.read(imageStream), argWidth, argHeight);


            File outputfile = new File("saved.png");
            ImageIO.write(image, "png", outputfile);

            int width = image.getWidth();
            int height = image.getHeight();

            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color = new Color(image.getRGB(x, y));
                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();

                    int avg = (r+g+b)/3; // we dont need precision

                    sb.append(grayscaleToChar(avg, reversed));
                }
                sb.append("\n");
            }

            image.flush();
            imageStream.close();

            String raw = sb.toString();

            var hook = event.getHook();

            try(FileUpload upload = FileUpload.fromData(toByteArray(raw), "ascii.txt")) {
                hook.sendFiles(upload).queue();
            }

        } catch (IOException | ExecutionException | InterruptedException e) {
            event.reply("Something went wrong while executing the command.").setEphemeral(true).queue();
            Bot.log.error("Error running AsciifyCommand from user: {}\n Message: {},\n Stacktrace: \n{}", event.getUser().getId(), e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    private char grayscaleToChar(int avg, boolean reversed){
        if(reversed){
            return REVERSED.charAt((LENGTH - 1) * avg / 255);
        } else{
            return CHARS.charAt((LENGTH - 1) * avg / 255);
        }
    }

    private BufferedImage downscale(BufferedImage originalImage, int width, int height) {
        Image resultingImage = originalImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

//        for (int y = 0; y < outputImage.getHeight(); y++) {
//            for (int x = 0; x < outputImage.getWidth(); x++) {
//                Color old = new Color(originalImage.getRGB(x, y));
//
//                Color newColor = getClosestColor(old);
//
//                outputImage.setRGB(x, y, newColor.getRGB());
//
//                int avg = avg(newColor) - avg(old);
//                Color err = new Color(avg, avg, avg);
//
//                applyDith(outputImage, x+1, y, err, 7);
//                applyDith(outputImage, x-1, y+1, err, 3);
//                applyDith(outputImage, x, y+1, err, 5);
//                applyDith(outputImage, x+1, y+1, err, 1);
//            }
//        }
        return outputImage;
    }

    private void applyDith(BufferedImage img, int x, int y, Color err, int val){
        try{
            Color c = new Color(img.getRGB(x, y));
            img.setRGB(x, y, add(c, mul(err, (double) val / 16)).getRGB());
        } catch (ArrayIndexOutOfBoundsException ignored){}
    }

    private Color getClosestColor(Color color){
        Color closest = palette[0];

        for (Color p : palette) {
            if(diff(p, color) < diff(p, closest)){
                closest = color;
            }
        }

        return closest;
    }

    private int avg(Color c){
        return (c.getRed() + c.getBlue() + c.getGreen())/3;
    }

    private int diff(Color c, Color other) {
        return Math.abs(other.getRed() - c.getRed()) +  Math.abs(other.getGreen() - c.getGreen()) +  Math.abs(other.getBlue() - c.getBlue());
    }

    private Color add(Color c, Color other){
        return new Color(c.getRed() + other.getRed(), c.getGreen() + other.getGreen(), c.getBlue() + other.getBlue());
    }

    private Color mul(Color c, double d){
        return new Color((int) (c.getRed()*d), (int) (c.getGreen()*d), (int) (c.getBlue()*d));
    }

    private byte[] toByteArray(String raw){
        byte[] bytes = new byte[raw.length()];
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            bytes[i] = (byte) c;
        }

        return bytes;
    }
}
