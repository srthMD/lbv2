package ro.srth.lbv2.exception;

/**
 * Mainly thrown by {@link ro.srth.lbv2.command.CommandManager CommandManager} when a classpath in
 * a command file is invalid.
 */
public class InvalidCommandClassException extends Exception {
    private final String classpath;

    public InvalidCommandClassException(String classpath, String reason) {
        super(reason);
        this.classpath = classpath;
    }

    public String getClasspath() {
        return classpath;
    }
}
