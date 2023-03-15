<img height="150" src="https://i.imgur.com/MzrLrNm.png">

The [Jme3-maze Project][jme3maze] is creating a first-person, single-player game
with a mouse-oriented GUI.  Its logline is:
<blockquote>
Explore a randomly-generated three-dimensional maze with a Pharaonic Egypt theme.
</blockquote>

It uses [jMonkeyEngine][jme], [tonegodGUI][], and [jme3-utilities][utilities].


## How to build and run Jme3-maze from source

1. Install Git and a Java Development Kit (JDK),
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation.
  + using Bash:  `export JAVA_HOME="` *path to installation* `"`
  + using Windows Command Prompt:  `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the Jme3-maze source code using Git:
  + `git clone https://github.com/stephengold/jme3-maze.git
  + `cd jme3-maze`
4. Build the project from source:
  + using Bash or PowerShell: `./gradlew build runAssetProcessor`
  + using Windows Command Prompt: `.\gradlew build runAssetProcessor`

You can run the local build using the Gradle wrapper:
  + using Bash or PowerShell: `./gradlew run`
  + using Windows Command Prompt: `.\gradlew run`

You can restore the project to a pristine state:
 + using Bash or PowerShell: `./gradlew clean`
 + using Windows Command Prompt: `.\gradlew clean`

[Jump to table of contents](#toc)


## Acknowledgments

Like most projects, the Jme3-maze Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Cris (aka "t0neg0d") for creating tonegodGUI and adapting it to my needs
+ the creators of (and contributors to) the following software:
    + [Adobe Photoshop Elements][elements]
    + the [Blender][] 3-D animation suite
    + the [Checkstyle] tool
    + the [FindBugs][] source-code analyzer
    + the [Git][] revision-control system and GitK commit viewer
    + the [Google Chrome web browser][chrome]
    + the [Gradle][] build tool
    + the Java compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman][] 3-D character creation tool
    + the [Markdown][] document conversion tool
    + Microsoft Windows 7 Professional
    + the [NetBeans][] integrated development environment
    + the PMD source-code analyzer
    + the Subversion revision-control system
    + the [WinMerge][] differencing and merging tool

I am grateful to [JFrog][], Google, and [Github][] for providing free hosting for the
Jme3-maze Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)


[blender]: https://docs.blender.org "Blender Project"
[bsd3]: https://opensource.org/licenses/BSD-3-Clause "3-Clause BSD License"
[checkstyle]: https://checkstyle.org "Checkstyle"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: https://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: https://findbugs.sourceforge.net "FindBugs Project"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gradle]: https://gradle.org "Gradle Project"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org  "jMonkeyEngine Project"
[jme3maze]: https://github.com/stephengold/jme3-maze "Jme3-maze Project"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[tonegodgui]: https://github.com/stephengold/tonegodgui "TonegodGUI Project"
[utilities]: https://github.com/stephengold/jme3-utilities "jME3 Utilities Project"
[winmerge]: https://winmerge.org "WinMerge Project"
