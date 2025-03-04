# Leilabot Version 2
A modular bot with the ability to update commands at runtime.

### What's Different?
The first iteration of leilabot relied heavily on the reflections
library to register commands, which was not optimal for startup times
and also had
an [implementation to handle interactions](https://github.com/srthMD/leilabot/blob/3cab69958fbe74c88368232542da282f4cb1b610/src/main/java/ro/srth/leila/command/CmdMan.java#L90)
that constructed an object... every interaction? 
<br>
<br>
Command registration was also very jank at times, with each command class
containing a static block that would initialize info like name and options.
As seen by this [comment](https://github.com/srthMD/leilabot/blob/3cab69958fbe74c88368232542da282f4cb1b610/src/main/java/ro/srth/leila/command/CmdMan.java#L71)
I apparently had no idea why this was a bad choice as I completely forgot how
static variables in classes work.
<br>
<br>
LBV2 still holds on to some of the design choices of its predecessor, although
with major variations in its command handler. 
<br>
Each command is now registered via
a JSON file that holds its command data. 
On startup, it will read every JSON file
and parse it into a Java record, where the registrar will collect the data and
register the command, constructing a new command handler with the provided data.

JSON files can be generated with the [JSONGenerator](https://github.com/srthMD/lbv2/blob/master/src/main/java/ro/srth/lbv2/JSONGenerator.java),
and must be put into a folder called "cmds" in the root directory of the JAR.

### Building

To build just the JAR file, all you would need to do is run the shadowJar task,
ensuring you have a compatible JDK version.

```console
.\gradlew shadowJar
```

For creating a full build, run the deploy task.

```console
.\gradlew :deploy
```

### Running
Before running, there are some files that the JAR will need before running.

* A token.txt with your application token
* A cmds folder containing your commands as .json files. (see the commands directory)
* A ffmpeg installation in the same directory as the JAR (if you have a command that requires it)

By default, the bot does not automaticly register commands, to do so you need to run
with the --register argument:

```console
java -jar lbv2-vX.X.X-all.jar --register
```

> [!WARNING]
> Any commands that do not have a corresponding JSON file will not be registered.
> <br>
> The same is applied for any JSON files without their respective classes.
