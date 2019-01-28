# Jme3-maze Project

The Jme3-maze Project is a first-person, single-player game with a mouse-oriented GUI.

Explore a randomly-generated three-dimensional maze with a Pharaonic Egypt theme.

Uses jMonkeyEngine, tonegodGUI, and jme3-utilities.

## Contents of this document

 + [How to install the SDK and the Jme3-maze Project](#install)
 + [Acknowledgments](#acks)

<a name="install"/>

## How to install the SDK and the Jme3-maze Project

### jMonkeyEngine3 (jME3) Software Development Kit (SDK)

The Jme3-maze Project currently targets
Version 3.2.2 of jMonkeyEngine.  You are welcome to use the Engine
without also using the SDK, but I use the SDK, and the following
installation instructions assume you will too.

The hardware and software requirements of the SDK are documented at
https://jmonkeyengine.github.io/wiki/jme3/requirements.html

 1. Download a jMonkeyEngine 3.2 SDK from
    https://github.com/jMonkeyEngine/sdk/releases
 2. Install the SDK, which includes:
   + the engine itself,
   + an integrated development environment (IDE) based on NetBeans,
   + various plugins, and
   + the Blender 3D application.
 3. To open the Jme3-maze Project in the IDE (or NetBeans), you will need the
    `Gradle Support` plugin.  Download and install it before proceeding.
    If this plugin isn't shown in the IDE's "Plugins" tool,
    you can download it from
    [GitHub](https://github.com/kelemen/netbeans-gradle-project/releases).

### Source files

Clone the jme3-maze repository using Git:

 1. Open the Clone wizard in the IDE:
   + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    `https://github.com/stephengold/jme3-maze.git`
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    which doesn't already contain "jme3-maze".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Complete" dialog appears, click on the "Open Project..."
    button.

### Asset conversion

Before you run the game itself, you'll need to convert the Blender assets to
native J3O format. To do this, run the AssetProcessor.java file in the jme3maze
package:

 1. Right-click on the "jme3-maze" project in the "Projects" window.
 2. Select "Tasks" -> "runAssetProcessor"

### Next steps

To run the game:

 1. Right-click on the "jme3-maze" project in the "Projects" window.
 2. Select "Run"


<a name="acks"/>

## Acknowledgments

Like most projects, the Jme3-maze Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Cris (aka "t0neg0d") for creating tonegodGUI and adapting it to my needs
+ the creators of (and contributors to) the following software:
  + Adobe Photoshop Elements
  + the Blender 3D animation suite
  + the FindBugs source code analyzer
  + the Git and Subversion revision control systems
  + the Google Chrome web browser
  + the Java compiler, standard doclet, and runtime environment
  + jMonkeyEngine and the jME3 Software Development Kit
  + LWJGL, the Lightweight Java Game Library
  + the MakeHuman 3D character creation tool
  + Microsoft Windows
  + the NetBeans integrated development environment
  + the PMD source code analyzer
  + the WinMerge differencing and merging tool

I am grateful to JFrog, Google, and Github for providing free hosting for the
Jme3-maze Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation.
