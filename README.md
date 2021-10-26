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

**A note on interactivity**: The authors of this plugin have striven to make its user interface as
responsive as possible, but we may find ourselves limited by the restrictions of the Java Swing framework
and other constraints. As such, the user may occasionally find that the UI doesn't always reflect 
recent changes to the underlying image, inputs, or data. Typically, this can be resolved by clicking
away from the *Dendritic Spine Counter* wizard dialog, and then back into/onto it. The 
*Dendritic Spine Counter* is programmed to update its user interface whenever it gains window focus,
so most "lagged representation" problems can be resolved in this manner. We hope this won't be
too much of an inconvenience to most users, and we will seek ways to ameliorate this issue
(elegantly, i.e. without the introduction of prohibitive computational overhead) in the future.

### Preparation
*Dendritic Spine Counter* expects ImageJ's user to start the plugin while an active image, or
"dataset" in ImageJ parlance, has already been selected. If the user attempts to start
*Dendritic Spine Counter* without an active image, the ImageJ framework will complain.

1. Start ImageJ. (In these screenshots, ImageJ is being run on a Windows 10 PC through the Fiji package.)\
![Launch ImageJ.](/documentation/images/01-01--Launch-ImageJ.jpg)
1. Use `File>Open...` to open an image file.\
![Open an image file.](/documentation/images/01-02-a--Open-an-image.jpg)
1. ImageJ may require you to provide additional configuration information to process the image file. If a stack of images is opened, then ImageJ may provide options by which to consolidate them into a single 2D image, such as minimum intensity projection (MinIP) stacking. Experienced users of ImageJ are likely to already be thoroughly familiar with these techniques.\
![Produce a MinIP 2D image.](/documentation/images/01-02-b--Open-an-image.jpg)
1. Select `Dendritic Spine Counter` from the `Plugins` dropdown.\
![Activate *Dendritic Spine Counter*.](/documentation/images/01-03-a--Activate-Dendritic-Spine-Counter.jpg)
1. If you attempt to select `Dendritic Spine Counter` from the `Plugins` dropdown *without* first loading an active image per the steps above, then the ImageJ framework will complain with a message saying: `A Dataset is required but none exist.`\
![ImageJ complains if *Dendritic Spine Counter* is activated without an active image.](/documentation/images/01-03-b--Dendritic-Spine-Counter-complains-if-activated-without-an-image.jpg)
1. If all goes according to plan, then *Dendrite Spine Counter* will display its wizard dialog, opened to the first tab, the `Set feature size` tab. It will also create a "working image", which is a grayscale, contrast-maximized copy of the active image. *Dendritic Spine Counter* will do all of its work on this "working image". We choose this approach because, under the hood, *Dendritic Spine Counter* only reads image brightness data and ignores color, so working with a grayscale copy allows the user to proverbially see the image through the plugin's metaphorical eyes, and thereby take advantage of (and compensate for) features that might appear differently in grayscale than they do in color.\
![*Dendritic Spine Counter* opens a grayscale copy "working image".](/documentation/images/02-01--Scale-and-Color-Calibration.jpg)

### Tab 1: "Set feature size"
*Dendritic Spine Counter* needs to know how big of a "feature window" to use when performing operations such as determining a dendrite's width or identifying a spine. (The concept of a "feature window" is a very old and well-established principle in computer vision. It is similar, but not identical, to a "kernel" or "convolution window", which is used more commonly in modern work in automated image processing. These topics are discussed in more detail in the **Methods** section below.)

Setting the feature window too large will cause this plugin to miss smaller features (Type II errors). Setting this feature window too small will cause it to incorrectly identify noise as features (Type I errors), and will also increase computational time (which will be noticeable if you perform this operation on slower computers). It's worth remembering that this plugin allows the user full control to veto or augment the machine's inferences, so errors of any kind can be minimized with the help of human intervention.

The plugin ultimately needs its feature size set in units of pixels. However, it allows the researcher to express the feature size in physical units, and performs the calculation automatically. The plugin's feature size units will default to pixels if no physical unit scale is provided.

The smallest permissible feature window size is seven (7) pixels. Because this plugin uses circular feature windows, a feature window size of 7 pixels in diameter covers an area of 28 pixels. Window diameters must be odd integer values, because pixels by definition are discrete units of the image, and every feature window must include its center pixel. (This plugin permits users to enter even and non-integer pixel feature window sizes, but they're rounded down to the nearest odd value above 7.) As such, the next possible smallest window size, 5, would offer a pixel window size of only 19 pixels; at so few pixels, a lone statistical brightness value outlier (e.g. a single black pixel, 0% brightness, in an otherwise white field, 100% brightness, or vice versa) cannot be ruled out as "insignificant" with >95% confidence, and as such even a single pixel of noise could perturb the entire analysis.

1. Click on "Set Scale..." to bring up ImageJ's "Set Scale..." dialog box. This is an existing ImageJ function that allows the user to specify how ImageJ should convert between pixels and physical units, and/or vice versa. Some image formats or importation techniques provide for the possibility of this data being bundled directly with the image, and as such would permit this step to be unnecessary. Alternatively, the user may already know the pixel diameter of features to scan for. However, in many cases, the user will wish to set the image's scale at this juncture. This plugin provides the "Set Scale..." button for the user to readily do so at this point, where it's convenient.\
![Use "Set Scale..." to set a conversion between pixels and physical units.](/documentation/images/02-02--Define-scale-units-with-Set-Scale.jpg)
1. Upon having set the image's scale in physical units, the user will be able to enter the feature size in physical units, whereas before they could only do so in pixels. Again, if the user *already knows* the desired feature window size in pixels, then this entire process might be unnecessary; but it is provided nonetheless for researchers who prefer to work in physical units.\
![Enter the feature window size in physical units.](/documentation/images/02-03--Define-feature-size-with-physical-units.jpg)
1. *Dendritic Spine Counter* assumes that you are working with images taken with bright field microscopy (BF), and that z-stacked slices are combined into a unified 2D image using MinIP. In short, this plugin assumes that features are represented as dark pixels upon a light background. If the opposite is true, then *Dendritic Spine Counter* provides a convenient button to invert the image. (Sample screenshot uses an image from ["High-throughput synapse-resolving two-photon fluorescence microendoscopy for deep-brain volumetric imaging in vivo", Meng et. al., University of California, _eLife_, Jan 3 2019.](https://elifesciences.org/articles/40805))\
![If using fluorescence or other dark-field techniques, click on "Invert image brightness levels".](/documentation/images/02-04--Invert-working-image-if-MaxIP.jpg)

















