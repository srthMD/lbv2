package ro.srth.lbv2.input;

import ro.srth.lbv2.Bot;
import ro.srth.lbv2.cache.FileCache;
import ro.srth.lbv2.command.CommandManager;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

/**
 * Very simple command handler that allows the user to run actions
 * via the console.
 */
@SuppressWarnings(value = "unused")
public class InputHandler {
    private static final Scanner in = new Scanner(System.in);

    private static final FileCache fileCache = Bot.getFileCache();

    public static Thread initHandler() {
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

    private static void quit(String[] ignoredArgs) {
        Bot.log.warn("Shutting down...");
        in.close();
        Bot.shutdown();
        System.exit(0);
    }

    private static void gc(String[] ignoredArgs) {
        long memBefore = Runtime.getRuntime().freeMemory();
        System.gc();
        long memAfter = Runtime.getRuntime().freeMemory();
        System.out.println((memAfter - memBefore) / 1024 + "KB freed since gc.");
    }

    private static void info(String[] ignoredArgs) {
        var runtime = Runtime.getRuntime();
        System.out.println("Total: " + runtime.totalMemory() / 1024 + "KB");
        System.out.println("Free: " + runtime.freeMemory() / 1024 + "KB");
        System.out.println("FileCache size: " + fileCache.size());
    }

    private static void clrcache(String[] ignoredArgs) {
        System.out.println("Clearing " + fileCache.size() + " elements.");
        fileCache.flush();
    }

    private static void register(String[] ignoredArgs) {
        CommandManager.registerCommands(true);
    }

    private static void upsert(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid input.");
            return;
        }

        String id = args[1];

        CommandManager.upsertCommand(id);
    }
}
