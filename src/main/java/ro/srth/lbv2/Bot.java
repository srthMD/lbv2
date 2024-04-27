package ro.srth.lbv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.srth.lbv2.command.CommandManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Bot {
    private static JDA bot;
    public static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) {
        String token;

        try{
            token = getToken();
        } catch (FileNotFoundException e) {
            token = null;
            log.error("File not found exception getting token: " + e.getMessage());
            System.exit(-1);
        }

        JDABuilder builder = JDABuilder.createLight(token);
        builder.setLargeThreshold(100)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(true);
        bot = builder.build();

        try {
            bot.awaitReady();
        } catch (InterruptedException e) {
            log.warn("Await ready interrupted: " + e.getMessage());
        }

        CommandManager.registerCommands();
    }

    public static JDA getBot() {
        return bot;
    }

    private static String getToken() throws FileNotFoundException {
        File file = new File("token.txt");
        Scanner in = new Scanner(file);

        String token = in.nextLine();
        in.close();
        return token;
    }
}
