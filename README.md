# jme3-maze

A first-person, single-player game with a mouse-oriented GUI.

Explore a randomly-generated three-dimensional maze with a Pharaonic Egypt theme.

Uses jMonkeyEngine, tonegodGUI, and sgold's utilities.

This project was formerly hosted at https://code.google.com/p/jme3-maze/

The source files are in JDK 7 format with CRLF line endings.


## How to build the maze game

### JMonkeyEngine (JME3) Software Development Kit (SDK)

 1. Download the jMonkeyEngine SDK from http://hub.jmonkeyengine.org/downloads
 2. Install the SDK, which includes:
   + the engine itself,
   + an integrated development environment (IDE) based on NetBeans, and
   + the Blender 3D application.

The hardware and software requirements of the SDK are documented at 
http://hub.jmonkeyengine.org/wiki/doku.php/jme3:requirements

### Plugins

You will need the following plugins: tonegodGUI and SkyControl. These can be 
downloaded from the "jMonkeyPlatform User Contributions" update center.

To install plugins:
 1. Open the Plugins dialog in the IDE:
   + Menu bar -> "Tools" -> "Plugins"
 2. Click on the "Settings" tab.
 3. Make sure the box next to "JMonkeyEngine SDK User Contributions" is checked.
 4. Click on the "Available Plugins" tab.
 5. Click on the "Name" button in the header to sort by name.
 6. Check the "Install" box next to each plugin you wish to install.
 7. Click on the "Install" button.
 8. Follow the instructions of the Plugin Installer wizard, (reading and) 
    accepting all license agreements.

It's normal to get a validation warning telling you that the plugins are
unsigned. It's normal to restart the IDE after installing plugins.

### Source files

Clone the Maze Game source files using Git:
 1. Open the Clone wizard in the IDE:
   + Menu bar -> "Team" -> "Git" -> "Clone..."
 2. For "Repository URL:" specify 
    "https://github.com/stephengold/jme3-maze.git" (without the quotes).
 3. Clear the "User:" and "Password:" text boxes.
 4. Click on the "Next >" button.
 5. Make sure the "master" remote branch is checked.
 6. Click on the "Next >" button again.
 7. For "Parent Directory:" specify a writable folder on a local filesystem or 
    a non-existent subfolder of such a folder.
 8. Make sure the "Scan for NetBeans Projects after Checkout" box is checked.
 9. Click on the "Finish" button.
 9. When asked whether to open the project, click the "Open Project" button.

### Project configuration

 1. (optional) Rename the project:
  + Right-click on the jme3-maze project in the "Projects" window.
  + Select "Rename...".
  + Type a new name in the text box.
  + Click on the "Rename" button.
 2. Build the project's JARs:
  + Right-click on the new project in the "Projects" window.
  + Select "Clean and Build".
 3. (optional) Generate the project's javadoc:
  + Right-click on the new project in the "Projects" window.
  + Select "Generate Javadoc".

### Asset conversion

Before you run the game itself, you'll need to convert the Blender assets to 
native J3O format. To do this, run the AssetProcessor.java file in the jme3maze 
package.

Note: The source files are in JDK 7 format, but the IDE creates new JME3 
projects in JDK 5 format.
        
### Next steps


## Acknowledgments

Like most projects, the jme3-maze project builds upon the work of many who 
have gone before.

I therefore acknowledge the following software developers:
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

I am grateful to Google Code and Github for providing free hosting for the 
jme3-maze project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can 
correct the situation.