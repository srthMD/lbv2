# Leilabot Version 2
A modular bot with the ability to update commands at runtime.

### Whats Different?
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

For creating a full build, run the deploy task **(this will call shadowJar)**.

```console
.\gradlew :deploy
```

### Running

> [!NOTE]
> If you are building from your own sources, you will need to run with the version you
> built with, otherwise if you are running the build from the Releases page, you need Java 21
> to run.

Before running, there are some files that the JAR will need before running.

* A token.txt with your application token
* A cmds folder containing your commands as .json files. (see the commands directory)
* A folder named ffmpeg with a ffmpeg installation (ffmpeg/bin/ffmpeg.exe)

> [!NOTE]
> An FFMPEG installation is not needed if
> [ShitifyCommand](https://github.com/srthMD/lbv2/blob/master/src/main/kotlin/ro/srth/lbv2/command/slash/ShitifyCommand.kt)
> does not exist, otherwise the command will not run.

After setup, run the bot using your Java installation like so, replace the X's with
the version number.

```console
java -jar lbv2-vX.X.X-all.jar
```

By default, this will not automatically register commands to Discord, to register commands run with
the --register argument:

```console
java -jar lbv2-vX.X.X-all.jar --register
```

> [!WARNING]
> Any commands that do not have a corresponding JSON file will not be registered.
> <br>
> The same is applied for any JSON files without their respective classes.
