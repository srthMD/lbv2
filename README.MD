# The second coming of Leila

New and hopefully not going to be abandoned version
of leilabot, revamped from the past mistakes of my last
attempt at a bot (static blocks). 


### Whats Different?
The first iteration of leilabot relied heavily on the reflections
library to register commands, which was not optimal for startup times
and for some reason had its own custom [implementation to handle interactions](https://github.com/srthMD/leilabot/blob/3cab69958fbe74c88368232542da282f4cb1b610/src/main/java/ro/srth/leila/command/CmdMan.java#L90)
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

### Building and Running
Although LBV2 is developed with Java 21, it will most likely be backwards compatible
up to Java 17 (this may change later and I will forget to edit this).
<br>
To build all you need to do is run shadowJar.
<br>
<br>
Before running for the first time three things will need to be set up.
* A token.txt file containing the discord token.
* A folder called cmds.
* A folder called ffmpeg with an FFMPEG installation.
<br>
<br>


**Note that any commands without their respective JSON files will not be registered.**

### Dependencies
As of 27.05.2024 leilabot depends on:
* [JDA](https://github.com/discord-jda/JDA)
* [Kotlin](https://kotlinlang.org/)
* [org.json](https://mvnrepository.com/artifact/org.json/json)
* [Logback Classic](https://github.com/qos-ch/logback)
* [Bramp's FFMPEG wrapper](https://github.com/bramp/ffmpeg-cli-wrapper)

