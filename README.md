# *Dendritic Spine Counter*
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

[**View the recording of the Neuroscience 2021 poster presentation**](http://todo-do-this)

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

Dendritic Spine Counter is designed to be intuitive, self-documenting, and naturally interoperative
with other ImageJ components. However, most users still appreciate a thorough How-To guide, so here
we will document *Dendritic Spine Counter*'s intended usage patterns.

### Preparation

*Dendritic Spine Counter* expects ImageJ's user to start the plugin while an active image, or
"dataset" in ImageJ parlance, has already been selected. If the user attempts to start
*Dendritic Spine Counter* without an active image, the ImageJ framework will complain.

1. Start ImageJ. (In these screenshots, ImageJ is being run on a Windows 10 PC through the Fiji package.) ![Launch ImageJ.](/documentation/images/01-01--Launch-ImageJ.jpg)
1. Use `File>Open...` to open an image file. ![Open an image file.](/documentation/images/01-02-a--Open-an-image.jpg)
1. ImageJ may require you to provide additional configuration information to process the image file. If a stack of images is opened, then ImageJ may provide options by which to consolidate them into a single 2D image, such as minimum intensity projection (MinIP) stacking. Experienced users of ImageJ are likely to already be thoroughly familiar with these techniques. ![Produce a MinIP 2D image.](/documentation/images/01-02-b--Open-an-image.jpg)
1. Select `Dendritic Spine Counter` from the `Plugins` dropdown. ![Activate *Dendritic Spine Counter*.](/documentation/images/01-03-a--Activate-Dendritic-Spine-Counter.jpg)
1. 












