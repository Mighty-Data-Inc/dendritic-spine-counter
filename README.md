# Dendritic Spine Counter
This Maven project builds an extension for [ImageJ](https://imagej.net/), 
the public-domain software for processing and analyzing scientific images.
Specifically, it creates a modular wizard to perform automation and 
data-entry operations related to the task of counting dendritic spines in 
microscope images. 

It is intended to be used by neuroscience researchers,
and is made available to the neuroscience community under the 
[GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.txt).

It was debuted to the neuroscience community online via a virtual poster
presentation on Nov 11, 2021, at the [2021 annual meeting of the Society 
for Neuroscience](https://www.sfn.org/meetings/neuroscience-2021/). 

[View the recording of the poster presentation](http://todo-do-this)

## Build and deploy
This project is built from a 
[boilerplate example ImageJ command implementation](https://github.com/imagej/example-imagej2-command). 
Users should refer to that example project
to answer any questions about how to configure and build
this software in your IDE of choice.

### Deployment
The output of the Maven build is a JAR file called `dendritic-spine-counter-jar-with-dependencies.jar`. Because it's intended to be fully self-contained and deployable, it might be rather large (approximately 130 MB). 

Copy this file into your ImageJ `plugins` folder. For example, on a Windows 10 computer, if you
installed ImageJ through the [Fiji](https://imagej.net/software/fiji/) package, then your
plugins folder is probably at `C:\"Program Files"\Fiji.app\plugins`.

After you copy (or drag-and-drop) this file into your ImageJ plugins folder, ImageJ will detect
this plugin automatically the next time you run ImageJ. You should see this plugin appear as
`Dendritic Spine Counter` under the `Plugins` menu.





It is intended as an ideal starting point to develop new ImageJ commands
in an IDE of your choice. You can even collaborate with developers using a
different IDE than you.

* In [Eclipse](http://eclipse.org), for example, it is as simple as
  _File &#8250; Import... &#8250; Existing Maven Project_.

* In [NetBeans](http://netbeans.org), it is even simpler:
  _File &#8250; Open Project_.

* The same works in [IntelliJ](http://jetbrains.net).

* If [jEdit](http://jedit.org) is your preferred IDE, you will need the
  [Maven Plugin](http://plugins.jedit.org/plugins/?MavenPlugin).

Die-hard command-line developers can use Maven directly by calling `mvn`
in the project root.

However you build the project, in the end you will have the `.jar` file
(called *artifact* in Maven speak) in the `target/` subdirectory.

To copy the artifact into the correct place, you can call
`mvn -Dscijava.app.directory="/path/to/ImageJ.app/"`.
This will not only copy your artifact, but also all the dependencies.
Restart your ImageJ or call *Help &#8250; Refresh Menus* to see your
plugin in the menus.

Developing plugins in an IDE is convenient, especially for debugging. To
that end, the plugin contains a `main` method which sets the `plugins.dir`
system property (so that the plugin is added to the Plugins menu), starts
ImageJ, loads an image and runs the plugin. See also
[this page](https://imagej.net/Debugging#Debugging_plugins_in_an_IDE_.28Netbeans.2C_IntelliJ.2C_Eclipse.2C_etc.29)
for information how ImageJ makes it easier to debug in IDEs.

Since this project is intended as a starting point for your own
developments, it is in the public domain.

How to use this project as a starting point
===========================================

1. Visit [this link](https://github.com/imagej/example-imagej-command/generate)
   to create a new repository in your space using this one as a template.

2. [Clone your new repository](https://help.github.com/en/articles/cloning-a-repository).

3. Edit the `pom.xml` file. Every entry should be pretty self-explanatory.
   In particular, change
    1. the *artifactId* (will be used for the JAR file name prefix)
    2. the *groupId*, ideally to a reverse domain name your organization owns
    3. the *version* (note that you typically want to use a version number
       ending in *-SNAPSHOT* to mark it as a work in progress rather than a
       final version)
    4. the *dependencies* (read how to specify the correct
       *groupId/artifactId/version* triplet
       [here](https://imagej.net/Maven#How_to_find_a_dependency.27s_groupId.2FartifactId.2Fversion_.28GAV.29.3F))
    5. the *developer* information
    6. the *scm* information

3. Remove the `GaussFiltering.java` file and add your own `.java` files
   to `src/main/java/<package>/` (if you need supporting files such as icons
   in the resulting `.jar` file, put them into `src/main/resources/`)

4. Replace the contents of `README.md` with information about your project.

5. Make your initial
   [commit](https://help.github.com/en/desktop/contributing-to-projects/committing-and-reviewing-changes-to-your-project) and
   [push the results](https://help.github.com/en/articles/pushing-commits-to-a-remote-repository)!

### Eclipse: To ensure that Maven copies the plugin to your ImageJ folder

1. Go to _Run Configurations..._
2. Choose _Maven Build_
3. Add the following parameter:
    - name: `scijava.app.directory`
    - value: `/path/to/ImageJ.app/`

This ensures that the final `.jar` file will also be copied to
your ImageJ plugins folder everytime you run the Maven build.
