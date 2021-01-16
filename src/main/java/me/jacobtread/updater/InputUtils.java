package me.jacobtread.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class InputUtils {

    private static final Logger LOGGER = Logger.getLogger("Updater");
    private static final Predicate<String> YES = s -> s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
    private static final Predicate<String> NO = s -> s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false");


    public static boolean awaitBoolean(BufferedReader in) {
        String result = awaitInput(in, YES.or(NO), input -> '"' + input + "\" is not a valid response please use Y or N.");
        return YES.test(result);
    }

    public static String awaitInput(BufferedReader in, Predicate<String> validator, InputResponse errorResponse) {
        while (true) {
            try {
                String line = in.readLine();
                if (line != null) {
                    if (validator.test(line)) {
                        return line;
                    } else {
                        LOGGER.warning(errorResponse.handle(line));
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    public interface InputResponse {

        String handle(String input);

    }

}
