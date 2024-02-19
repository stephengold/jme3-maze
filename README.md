<img height="150" src="https://i.imgur.com/MzrLrNm.png">

The [Jme3-maze Project][jme3maze] is creating a first-person, single-player game
with a mouse-oriented GUI.  Its logline is:
<blockquote>
Explore a randomly-generated three-dimensional maze with a Pharaonic Egypt theme.
</blockquote>

Complete source code (in Java) is provided under
[a 3-clause BSD license][license].

It uses [jMonkeyEngine][jme], [tonegodGUI], and [Acorus].


## How to build and run Jme3-maze from source

1. Install a [Java Development Kit (JDK)][adoptium],
   if you don't already have one.
2. Point the "JAVA_HOME" environment variable to your JDK installation.
   (The path might be something like "C:\Program Files\Java\jre1.8.0_301"
   or "/usr/lib/jvm/java-8-openjdk-amd64" or
   "/Library/Java/JavaVirtualMachines/liberica-jdk-17-full.jdk/Contents/Home" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using [Fish]: `set -g JAVA_HOME "` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the Jme3-maze source code from GitHub:
  + using [Git]:
    + `git clone https://github.com/stephengold/jme3-maze.git`
    + `cd jme3-maze`
  + using a web browser:
    + Browse to https://github.com/stephengold/jme3-maze/archive/refs/heads/master.zip
    + save the ZIP file
    + extract the contents of the saved ZIP file
    + `cd` to the extracted directory/folder
4. Run the [Gradle] wrapper:
  + using Bash or Fish or PowerShell or Zsh: `./gradlew build runAssetProcessor`
  + using Windows Command Prompt: `.\gradlew build runAssetProcessor`

You can run the local build using the Gradle wrapper:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew run`
+ using Windows Command Prompt: `.\gradlew run`

You can restore the project to a pristine state:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew clean`
+ using Windows Command Prompt: `.\gradlew clean`

[Jump to the table of contents](#toc)


## Acknowledgments

Like most projects, Jme3-maze builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Cris (aka "t0neg0d") for creating tonegodGUI and adapting it to my needs
+ the creators of (and contributors to) the following software:
    + [Adobe Photoshop Elements][elements]
    + the [Blender] 3-D animation suite
    + the [Checkstyle] tool
    + the [FindBugs] source-code analyzer
    + the [Firefox] and [Google Chrome][chrome] web browsers
    + the [Git] revision-control system and GitK commit viewer
    + the [GitKraken] client
    + the [Gradle] build tool
    + the [IntelliJ IDEA][idea] and [NetBeans] integrated development environments
    + the [Java] compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman] 3-D character creation tool
    + the [Markdown] document-conversion tool
    + the [Meld] visual merge tool
    + Microsoft Windows
    + the PMD source-code analyzer
    + the Subversion revision-control system
    + the [WinMerge] differencing and merging tool

I am grateful to [JFrog], Google, [Github], and [Imgur]
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know, so I can
correct the situation: sgold@sonic.net

[Jump to the table of contents](#toc)


[adoptium]: https://adoptium.net/releases.html "Adoptium Project"
[acorus]: https://stephengold.github.io/Acorus "Acorus Project"
[blender]: https://docs.blender.org "Blender Project"
[checkstyle]: https://checkstyle.org "Checkstyle"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: https://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: https://findbugs.sourceforge.net "FindBugs Project"
[firefox]: https://www.mozilla.org/en-US/firefox "Firefox"
[fish]: https://fishshell.com/ "Fish command-line shell"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gitkraken]: https://www.gitkraken.com "GitKraken client"
[gradle]: https://gradle.org "Gradle Project"
[idea]: https://www.jetbrains.com/idea/ "IntelliJ IDEA"
[imgur]: https://imgur.com/ "Imgur"
[java]: https://en.wikipedia.org/wiki/Java_(programming_language) "Java programming language"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org "jMonkeyEngine Project"
[jme3maze]: https://github.com/stephengold/jme3-maze "Jme3-maze Project"
[license]: https://github.com/stephengold/jme3-maze/blob/master/LICENSE "Jme3-maze license"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[meld]: https://meldmerge.org "Meld merge tool"
[mint]: https://linuxmint.com "Linux Mint Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[tonegodgui]: https://github.com/stephengold/tonegodgui "TonegodGUI Project"
[winmerge]: https://winmerge.org "WinMerge Project"
