# Usage Guide
*Dendritic Spine Counter* is designed to be intuitive, self-documenting, and naturally interoperative
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
"dataset" in ImageJ parlance, has already been selected. If the user starts
*Dendritic Spine Counter* without an active image, then the plugin will automatically
launch a "File Open..." dialog and prompt the user to select an image file.

If all goes according to plan, then *Dendrite Spine Counter* will display its wizard dialog, opened to the first tab, the `Set feature size` tab. It will also create a "working image", which is a grayscale, contrast-maximized copy of the active image. *Dendritic Spine Counter* will do all of its work on this "working image". We choose this approach because, under the hood, *Dendritic Spine Counter* only reads image brightness data and ignores color, so working with a grayscale copy allows the user to proverbially see the image through the plugin's metaphorical eyes, and thereby take advantage of (and compensate for) features that might appear differently in grayscale than they do in color.

![Dendritic Spine Counter opens a grayscale copy "working image".](/documentation/images/02-01--Scale-and-Color-Calibration.jpg)

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

### Tab 2: Trace dendrites
Users will probably spend most of their time in the `Trace dendrites` tab and the subsequent one, `Mark spines`. In `Trace dendrites`, users can employ the Polyline Tool to trace the approximate centerlines of dendritic segments. The plugin will detect the thicknesses of the dendrite segment at various regions described by the traced centerline, and users will be given the opportunity to refine the plugin's estimation of the dendrite's contours.

1. Activate the Polyline Tool. *Dendritic Spine Counter* initially identifies dendrites through coarse Polyline Tool tracings. You can, of course, use ImageJ's tool menu to select the Polyline Tool, but the plugin provides a convenient button for doing so within the wizard dialog.\
![Activate the Polyline Tool.](/documentation/images/03-01--Trace-Dendrite-tab.jpg)
1. Using the Polyline Tool, trace the centerline of a dendrite segment. Your tracing can be approximate; the plugin will try to follow dark regions to connect each subsequent point of your polyline when determining the dendrite segment's contours.\
![Trace a dendrite segment with the Polyline Tool.](/documentation/images/03-02--Trace-a-dendrite-segment-with-Polyline-Tool.jpg)
1. When you're done with a polyline trace, return to the wizard dialog and click on "Use existing polyline path to trace a dendrite". You will see the *Dendritic Spine Counter* mark the dendrite path with a blue overlay.\
![Trace a dendrite segment with the Polyline Tool.](/documentation/images/03-03-a--Click-Use-existing-polyline-to-add-dendrite-segment-to-list.jpg)
1. You can mark multiple dendrite segments at a time. They will show up in the listbox under "Dendrite Branches".\
![Add multiple dendrite segments at a time.](/documentation/images/03-03-b--Add-multiple-dendrite-segments.jpg)
1. You can select dendrite segments to modify. The *Dendritic Spine Counter* uses pixel brightness levels to determine a dendrite's likeliest structure. If you, using your human judgment and visual recognition abilities, disagree with the plugin's assessment, then you can select a segment to modify. The selected branch will appear in green.\
![Select a segment to modify.](/documentation/images/03-04--Select-a-dendrite-segment-to-modify.jpg)
1. The selected branch will contain a selected region, which will appear as a bright green circle. You can move this region forward and back along the length of the selected branch using the `Region Fwd` and `Region Back` buttons.\
![Move the selected region forward and back along the dendrite segment.](/documentation/images/03-05--Navigate-the-cursor-circle-fwd-and-back-along-the-segment.jpg)
1. You can modify the selected region of the selected branch. You can make it thicker or thinner, and shift it left or right (orthogonally relative to the flow of the dendrite branch.)\
![Modify the selected region.](/documentation/images/03-06--Make-circled-region-thicker-or-thinner-or-move-it-left-or-right.jpg)

### Tab 3: Mark spines
The user can activate the Mutli-point Tool, and click on places in the image that correspond to where the user sees that there are spines, with each point placed by the Multi-point Tool corresponding to one spine. The Multi-point Tool is of course accessible through the ImageJ toolbar, but the plug-in provides an additional button to activate this tool directly from the wizard dialog.
![Mark spines with Multi-point Tool.](/documentation/images/04-01--Mark-spines-with-Multi-point-tool.jpg)

Marked spines are considered "ephemeral" -- that is, they exist only as a current selection of the Multi-point Tool. They can be cleared easily if the user picks a different selection tool. The user should take care to only mark spines when no other annotation operations need to take place.

The user will have the opportunity to tabulate marked spines in the next tab, `Report results`.

#### Automatically detect spines
This plugin includes the option to use simple statistics-based image analysis techniques to find spines along the edges of the traced dendrite segments. This can be performed by simply clicking on the button marked `Automatically detect spines on traced dendrites`.

The user can adjust the sensitivity of the automatic detection process. At full sensitivity, any outcropping from the edge of the dendrite that is even slightly darker than its surroundings is marked as a spine. At the lowest sensitivity level, an outcropping has to be much darker than adjacent features in order to be considered a spine.

This operation will clear any existing markings and replace them with the automated results. If the user wishes to perform automated detection, they should do so *first*, and then adjust the output manually if desired.

When using the automatic detection feature, it's important to remember that the results of the automated process are simply points placed upon the image with the Multi-point Tool. The user is free to make any adjustments to the detected spines that they see fit, using the standard interface and control scheme that they would normally use when operating the Multi-point Tool in any other context. They can add more points to identify spines that the automation might have missed; remove points (through Alt-click) that the automation might have identified as spines erroneously; and move points that don't quite topologically correspond to the feature that the human recognizes as a spine.

### Tab 4: Report results
In the `Report results` tab, with points marked on the image using the Multi-point Tool to denote spines, the user can click on the button `Count spines near dendrite segments` to tabulate spine densities for every dendrite segment that they've marked. The plugin will automatically associate each spine with its nearest dendrite segment by Euclidean distance. The plugin will then populate a table containing the following information (with distances provided in physical units if the user had set the image's scale, or pixels if not):
* The name or identifier of the dendrite segment
* The length of the dendrite segment
* The average (mean) width of the dendrite segment
* The count of marked spines along the dendrite segment
* The density of spines along the segment, expressed as spines per unit distance

![Count spines along each dendrite segment.](/documentation/images/05-01--Count-results.jpg)

#### Copy results to your clipboard and paste to a spreadsheet
The button `Copy table data to clipboard` will automatically copy the contents of the table to your computer's clipboard. It will preserve row and column information using tab and newline delimiters, which are recognized by all major spreadsheet applications such as 
[Microsoft Excel](https://www.microsoft.com/en-us/microsoft-365/excel), 
[Google Sheets](https://www.google.com/sheets/about/), or 
[Apache OpenOffice Calc](https://www.openoffice.org/product/calc.html). As such, you can paste the table data directly into a spreadsheet. You can choose to copy the header labels when doing so (such as when first starting a new spreadsheet), or go without them (such as when adding subsequent information to an existing sheet).

![Copy results table, paste into Excel.](/documentation/images/05-02--Copy-results-table-paste-into-Excel.jpg)

**Optional columns.** In order to help the researcher (or team of researchers) consolidate data from many different dendrites across many different images, *Dendritic Spine Counter* provides the ability to designate a handful of optional columns. If the user chooses to fill values into these optional column fields, then this value will be copied down across all rows in the table data when the `Copy table data to clipboard` button is clicked. This additional column *will not appear* in the table shown in the dialog, but it will be stored on the clipboard and subsequently pasted into a spreadsheet. Using this option, a team of researchers can easily use the same spreadsheet to perform multiple copy-paste operations, and keep track of metadata such as which researcher was operating the software, which file they were examining, and what specific kinds of spine features they were looking for.

![Optional columns.](/documentation/images/05-03--Optional-Columns.jpg)

### Tab 5: Save/Load
*Dendritic Spine Counter* is intended to typically be used in one sitting at a time, with a workflow consisting of loading an image, tracing the dendrites, marking the spines, copying to a spreadsheet, and closing the application. However, we recognize that this isn't always possible or even necessarily desirable. For example, we recognize that dendrite tracings might need to be saved for subsequent re-examination, such as in a case where the task of operating this software is delegated to a less-experienced researcher and a more senior staff member might want the opportunity to later review their work. This plugin therefore provides the ability to save and load the dendrite segment tracing information to/from a file.

![Save and load dendrite segment tracing.](/documentation/images/06-02--Save-Load-saves-to-JSON-file.jpg)

This plugin's save files are written in JSON format. Though bulkier than a binary format, JSON was chosen for the sake of an open-source, full-transparency approach. JSON has the advantage of being text-readable and self-documenting (and in fact even text-editable), allowing a researcher to examine or even modify the contents of the file directly if they wish to do so. Though *Dendritic Spine Counter* isn't intended to be used in this manner (i.e. manual editing of save files), we nonetheless provide the potential capability.

![Dendrite tracing information saved as JSON.](/documentation/images/06-03--JSON-format-viewable.jpg)

## Background

This plugin was developed in order to fill a need in the neuroscience research community. At the time of its authorship, smaller labs lacked a way to conveniently assign and perform the relatively simple but tedious task of identifying, labeling, and counting dendritic spines. Automated tools for this task certainly existed, but were generally bundled within large software suites whose price-points and licensing restrictions often put out of reach of smaller labs with tighter budgets and fewer staff. With *Dendritic Spine Counter* now available, such labs can perform this research purely with free-to-use open-source tools.

### Abstract

In the central nervous system, most excitatory synapses are represented as small protrusions called dendritic spines. The number of excitatory synapses on a neuron impacts overall excitability, and, on a larger scale, network activity. In diseases such as Alzheimer's Disease, Autism Spectrum Disorder, and schizophrenia, spine density is abnormal, resulting in improper brain function. Therefore, computing spine density is an important data-gathering step for many avenues of research into these disorders. Here, we have created a free ImageJ plugin, Dendritic Spine Counter, that will allow researchers and students to count spines in 2D. This plugin allows a user to submit a 2D image of stained neuronal tissue, mark a dendrite on that image for analysis, and receive an automated visually interactive list of structural features along that dendrite that are likely to be spines. The plugin operates “semi-automatically”, requiring minimal input from the user outside of specifying the dendrite to analyze. It also offers manual override options, permitting the user to directly add or remove spines. The machine vision algorithms underlying Dendritic Spine Counter were implemented using a heuristic-driven “expert system” approach, primarily employing basic image transformation functions and statistical analyses of pixel areas.




























