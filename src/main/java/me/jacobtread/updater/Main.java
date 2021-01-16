package me.jacobtread.updater;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.jacobtread.updater.logging.LogFormatter;
import me.jacobtread.updater.logging.LogHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger("Updater");

    public static void main(String[] args) {
        ServerUpdater serverUpdater = new ServerUpdater(getServerArgs(args));
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        OptionSpec<Void> help = optionParser.accepts("updater-help", "Displays command/parameter information");
        OptionSpec<Void> verbose = optionParser.accepts("updater-verbose", "Verbose/Debug logging");
        OptionSpec<Void> quiet = optionParser.accepts("updater-quiet", "Run without any messages from ServerUpdater");
        OptionSpec<Void> offline = optionParser.accepts("updater-offline", "Force offline and attempt to use existing jar");
        OptionSpec<Void> noInput = optionParser.accepts("updater-no-input", "Do not ask the user for input and makes decisions on its own");
        OptionSpec<String> logFile = optionParser.accepts("updater-log", "Specify a output log file").withRequiredArg();
        OptionSet optionSet = optionParser.parse(args);
        if (optionSet.has(verbose)) {
            ServerUpdater.VERBOSE = true;
        }
        if (optionSet.has(offline)) {
            ServerUpdater.FORCE_OFFLINE = true;
        }
        if (optionSet.has(noInput)) {
            ServerUpdater.NO_INPUT = true;
        }
        LOGGER.setUseParentHandlers(false);
        if (optionSet.has(quiet)) {
            ServerUpdater.NO_INPUT = true;
        } else {
            LOGGER.addHandler(new LogHandler());
            System.out.printf("  _   _           _       _            \n" +
                    " | | | |_ __   __| | __ _| |_ ___ _ __ \n" +
                    " | | | | '_ \\ / _` |/ _` | __/ _ \\ '__|\n" +
                    " | |_| | |_) | (_| | (_| | ||  __/ |   \n" +
                    "  \\___/| .__/ \\__,_|\\__,_|\\__\\___|_|   \n" +
                    "       |_|                             \n" +
                    "\nVersion %s, Developed By Jacobtread, \u00A9 Copyright %d\n" +
                    "\n", ServerUpdater.VERSION, LocalDateTime.now().getYear());
            if (ServerUpdater.FORCE_OFFLINE) {
                LOGGER.warning("Running as " + LogHandler.RED + "OFFLINE" + LogHandler.RESET);
            }
        }
        if (optionSet.hasArgument(logFile)) {
            try {
                FileHandler fileHandler = new FileHandler(logFile.value(optionSet));
                fileHandler.setFormatter(new LogFormatter());
                LOGGER.addHandler(fileHandler);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to add log file handler", e);
            }
        }
        if (optionSet.has(help)) {
            try {
                optionParser.printHelpOn(System.out);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to print help to system out", e);
            }
            return;
        }
        serverUpdater.init();
    }

    private static List<String> getServerArgs(String[] args) {
        List<String> newArgs = new ArrayList<>();
        for (String arg : args) {
            if (!arg.startsWith("--updater-")) {
                newArgs.add(arg);
            }
        }
        return newArgs;
    }

}
