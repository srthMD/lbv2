package ro.srth.lbv2;

import net.bramp.ffmpeg.FFmpeg;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.srth.lbv2.cache.FileCache;
import ro.srth.lbv2.command.CommandManager;
import ro.srth.lbv2.input.InputHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public final class Bot {
    private static JDA bot;
    private static FFmpeg FFMPEG;
    private static final Random rand = new Random(System.currentTimeMillis());
    private static final FileCache fileCache = new FileCache();

    public static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws IOException {
        boolean coldstart = false;

        if(args.length != 0){
            if(args[0].equals("--register")){
                coldstart = true;
            }
        }

        String token;

        try{
            token = getToken();
        } catch (FileNotFoundException e) {
            token = null;
            log.error("File not found exception getting token: {}", e.getMessage());
            System.exit(-1);
        }

        FFMPEG = new FFmpeg("ffmpeg/bin/ffmpeg.exe");

        JDABuilder builder = JDABuilder.createLight(token);
        builder.setLargeThreshold(100)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(true)
                .setMemberCachePolicy(MemberCachePolicy.ALL);
        bot = builder.build();

        try {
            bot.awaitReady();
        } catch (InterruptedException e) {
            log.warn("Await ready interrupted: {}", e.getMessage());
        }

        CommandManager.registerCommands(coldstart);
        InputHandler.initHandler();
    }

    private static String getToken() throws FileNotFoundException {
        File file = new File("token.txt");
        Scanner in = new Scanner(file);

        String token = in.nextLine();
        in.close();
        return token;
    }

    public static void shutdown() {
        fileCache.flush();
        bot.shutdown();
    }

    public static JDA getBot() {
        return bot;
    }

    public static FFmpeg getFFMPEG() {
        return FFMPEG;
    }

    public static Random rand(){
        return rand;
    }

    public static FileCache getFileCache() {
        return fileCache;
    }
}

//Adam You Are A Fat Cunt
