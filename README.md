# ServerUpdater
By Jacobtread

This is an automatic updater for your Minecraft server. 
This auto updater is made from scratch and does not use any code from my original one made
for songoda. This automatic updater solves the bug that the ServerJars 
updater has with paper on Java 11 along with other features. Should support Java versions 8 and above.

Tested and working on Java 11.

All the server files will be stored in the /server directory next to the jar.

The updater config file is stored next to the jar named config.properties.
The config is formatted like this:
```properties
# This is the type of jar you want
type=paper
# This is the version you want
version=latest
# This is a stored hash for paper DO NOT CHANGE THIS it will be modified by the updater for you
hash=null
# Anything you put here e.g -Xmx1G will be used when executing the server.
# Setting this to jvm_args=-Xmx1G will change the command to "java -Xmx1G -jar server.jar"
jvm_args=null
```
(Configs can created with the guide upon startup or with the arg --updater-guide)

Make sure you specify any JVM arguments in the config instead of your java command.

### Note for Songoda
If you are affiliated with, work under or intend to use this code to improve the official Songoda updater code
a DCMA take-down will be issued. This is also stated in the LICENSE.txt I do not appreciate what your Company has done
to me, I will not allow you to use my code any longer.

### To run the updater use the following command:
Any arguments you pass to the updater will also be passed to the server .
(Note %JAR% must be replaced with the name of the jar)

```shell
java -jar %JAR%
```

### Specifying additional Java VM args
If you want to change the amount of ram your server is using or other arguments such as -Xmx or -Xms
you can specify them in the config.properties on the jvm_args line
```properties
jvm_args=-Xms1G -Xmx2G
```

### Updater Arguments

| Name      | Alias               | Description                                                  |
|-----------|---------------------|--------------------------------------------------------------|
| help      | --updater-help      | Displays command/parameter information                       |
| log       | --updater-log       | Specify a output log file                                    |
| no-input  | --updater-no-input  | Do not ask the user for input and makes decisions on its own |
| offline   | --updater-offline   | Force offline and attempt to use existing jar                |
| quiet     | --updater-quiet     | Run without any logging messages from ServerUpdater          |
| verbose   | --updater-verbose   | Verbose/Debug logging                                        |

Developed By Jacobtread