package ro.srth.lbv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.srth.lbv2.cache.FileCache;
import ro.srth.lbv2.command.CommandManager;
import ro.srth.lbv2.handlers.InputHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;

public final class Bot {
    private static Bot instance;

    private final JDA bot;
    private final CommandManager commandManager;
    private final OkHttpClient client = new OkHttpClient();
    private final Random rand = new Random(System.currentTimeMillis());
    private final FileCache fileCache;
    private final InputHandler inputHandler;
    private boolean shutdown = false;

    public static final Logger log = LoggerFactory.getLogger(Bot.class);

    public Bot() throws InterruptedException {
        String token;

        try{
            token = getToken();
        } catch (FileNotFoundException e) {
            token = null;
            log.error("File not found exception getting token: {}", e.getMessage());
            System.exit(-1);
        }

        var builder = JDABuilder.createLight(token);
        builder.setLargeThreshold(100)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(true)
                .setMemberCachePolicy(MemberCachePolicy.ALL);

        bot = builder.build().awaitReady();

        this.fileCache = new FileCache();
        this.inputHandler = new InputHandler();
        this.commandManager = new CommandManager();
    }

    public static void main(String[] args) throws InterruptedException {
        boolean coldstart = false;

        if (args.length != 0) {
            if (args[0].equals("--register")) {
                coldstart = true;
            }
        }

        instance = new Bot();
        instance.inputHandler.initHandler();
        instance.commandManager.registerCommands(coldstart);
        instance.bot.addEventListener(instance.commandManager);
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
        this.shutdown = true;
        inputHandler.shutdown();
        commandManager.getExecutor().shutdown();
        fileCache.flush();
        bot.shutdown();
    }

    public JDA getBot() {
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

    public boolean isShutdown() {
        return shutdown;
    }
}
