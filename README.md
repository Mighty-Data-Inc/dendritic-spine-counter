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

[**View the recording of the poster presentation**](http://todo-do-this)

## Build (via Maven)
This project is built from a 
[boilerplate example ImageJ command implementation](https://github.com/imagej/example-imagej2-command). 
Users should refer to that example project
to answer any questions about how to configure and build
this software in your IDE of choice.

The specification for the build process can be found in `pom.xml` at the root of the project, and can be modified to suit your needs.

## Installation
The output of the Maven build is a JAR file called `dendritic-spine-counter-jar-with-dependencies.jar`. Because it's intended to be fully self-contained and deployable, it might be rather large (approximately 130 MB). 

Copy this file into your ImageJ `plugins` folder. For example, on a Windows 10 computer, if you
installed ImageJ through the [Fiji](https://imagej.net/software/fiji/) package, then your
plugins folder is probably at `C:\"Program Files"\Fiji.app\plugins`.

After you copy (or drag-and-drop) this file into your ImageJ plugins folder, ImageJ will detect
this plugin automatically the next time you run ImageJ. You should see this plugin appear as
`Dendritic Spine Counter` under the `Plugins` menu.

## Usage
