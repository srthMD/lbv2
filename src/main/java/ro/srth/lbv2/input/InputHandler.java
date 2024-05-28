package ro.srth.lbv2.input;

import ro.srth.lbv2.Bot;
import ro.srth.lbv2.command.CommandManager;

import java.util.Scanner;

/**
 * Very simple command handler.
 */
public class InputHandler {
    private static final Scanner in = new Scanner(System.in);

    public static void initHandler(){
        new Thread(() -> {
            var fileCache = Bot.getFileCache();
            while(true){
                var input = in.nextLine();

                if(input.equals("quit")){
                    Bot.shutdown();
                    System.exit(0);
                }

                switch (input) {
                    case "gc" -> System.gc();
                    case "info" -> {
                        var runtime = Runtime.getRuntime();
                        System.out.println("Total: " + runtime.totalMemory()/1024 + "KB");
                        System.out.println("Free: " + runtime.freeMemory()/1024 + "KB");
                        System.out.println("Processors: " + runtime.availableProcessors());
                        System.out.println("FileCache size: " + fileCache.size());
                    }
                    case "clrcache" -> fileCache.flush();
                    case "register" -> CommandManager.registerCommands(true);
                    case "upsert" -> {
                        String[] args = input.split(" ");
                        if (args.length != 2) {
                            System.out.println("Invalid input.");
                            continue;
                        }

                        String id = args[1];

                        CommandManager.upsertCommand(id);
                    }
                }
            }
        }).start();
    }
}
