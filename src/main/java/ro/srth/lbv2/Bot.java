package ro.srth.lbv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
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
    private static Bot instance;

    private final ShardManager bot;
    private final CommandManager commandManager;
    private final OkHttpClient client = new OkHttpClient();
    private final Random rand = new Random(System.currentTimeMillis());
    private final FileCache fileCache;
    private final InputHandler inputHandler;

    public static final Logger log = LoggerFactory.getLogger(Bot.class);

    public Bot() throws IOException {
        String token;

        try{
            token = getToken();
        } catch (FileNotFoundException e) {
            token = null;
            log.error("File not found exception getting token: {}", e.getMessage());
            System.exit(-1);
        }

        var builder = DefaultShardManagerBuilder.createLight(token);
        builder.setLargeThreshold(100)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(true)
                .setMemberCachePolicy(MemberCachePolicy.ALL);
        bot = builder.build();

        this.fileCache = new FileCache();

        try {
            for (JDA shard : bot.getShards()) {
                shard.awaitReady();
            }
        } catch (InterruptedException e) {
            log.warn("Await ready interrupted: {}", e.getMessage());
        }

        this.inputHandler = new InputHandler();
        this.commandManager = new CommandManager();
        bot.addEventListener(commandManager);
    }

    public static void main(String[] args) throws IOException {
        boolean coldstart = false;

        if (args.length != 0) {
            if (args[0].equals("--register")) {
                coldstart = true;
            }
        }

        instance = new Bot();
        instance.inputHandler.initHandler();
        instance.commandManager.registerCommands(coldstart);
    }

    private static String getToken() throws FileNotFoundException {
        File file = new File("token.txt");
        Scanner in = new Scanner(file);

        String token = in.nextLine();
        in.close();
        return token;
    }

    public static Bot getInstance() {
        return instance;
    }

    public void shutdown() {
        inputHandler.getThread().interrupt();
        fileCache.flush();
        bot.shutdown();
    }

    public ShardManager getBot() {
        return bot;
    }

    public Random rand() {
        return rand;
    }

    public FileCache getFileCache() {
        return fileCache;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public OkHttpClient getClient() {
        return client;
    }
}
