package ro.srth.lbv2.input;

import ro.srth.lbv2.Bot;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

/**
 * Very simple command handler that allows the user to run actions
 * via the console.
 */
@SuppressWarnings(value = "unused")
public class InputHandler {
    private static final Bot bot = Bot.getInstance();

    private static final Scanner in = new Scanner(System.in);

    private final Thread thread;

    public InputHandler() {
        this.thread = initHandler();
    }

    private static void quit(String[] ignoredArgs) {
        Bot.log.warn("Shutting down...");
        in.close();
        Bot.getInstance().shutdown();
        System.exit(0);
    }

    private static void info(String[] ignoredArgs) {
        var runtime = Runtime.getRuntime();
        System.out.println("Total: " + runtime.totalMemory() / 1024 + "KB");
        System.out.println("Free: " + runtime.freeMemory() / 1024 + "KB");
        System.out.println("FileCache size: " + bot.getFileCache().size());
    }

    private static void clrcache(String[] ignoredArgs) {
        System.out.println("Clearing " + bot.getFileCache().size() + " elements.");
        bot.getFileCache().flush();
    }

    private static void gc(String[] ignoredArgs) {
        long memBefore = Runtime.getRuntime().freeMemory();
        System.gc();
        long memAfter = Runtime.getRuntime().freeMemory();
        System.out.println((memAfter - memBefore) / 1024 + "KB freed since gc.");
    }

    private static void register(String[] ignoredArgs) {
        Bot.getInstance().getCommandManager().registerCommands(true);
    }

    private static void upsert(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid input.");
            return;
        }

        String id = args[1];

        Bot.getInstance().getCommandManager().upsertCommand(id);
    }

    public Thread initHandler() {
        Thread t = new Thread(() -> {
            while(true){
                var input = in.nextLine().split(" ");

                try {
                    var method = InputHandler.class.getDeclaredMethod(input[0], String[].class);

                    method.invoke(null, (Object) input);
                } catch (NoSuchMethodException e) {
                    System.out.println("Command does not exist.");
                } catch (InvocationTargetException | IllegalAccessException e) {
                    System.out.println("Something went wrong running the command.");
                }
            }
        });
        t.setDaemon(true);
        return t;
    }

    public Thread getThread() {
        return thread;
    }
}
