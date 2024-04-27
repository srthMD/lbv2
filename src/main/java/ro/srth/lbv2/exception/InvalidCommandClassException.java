package ro.srth.lbv2.exception;

public class InvalidCommandClassException extends Exception {
    private final String classpath;

    public InvalidCommandClassException(String classpath, String reason) {
        super(reason);
        this.classpath = classpath;
    }

    public InvalidCommandClassException(String classpath) {
        super();
        this.classpath = classpath;
    }

    public String getClasspath() {
        return classpath;
    }
}
