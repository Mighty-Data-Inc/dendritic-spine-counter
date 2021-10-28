# *Dendritic Spine Counter* plugin for ImageJ

## Abstract
In the central nervous system, most excitatory synapses are represented as small protrusions called dendritic spines. The number of excitatory synapses on a neuron impacts overall excitability, and, on a larger scale, network activity. In diseases such as Alzheimer's Disease, Autism Spectrum Disorder, and schizophrenia, spine density is abnormal, resulting in improper brain function. Therefore, computing spine density is an important data-gathering step for many avenues of research into these disorders. Here, we have created a free ImageJ plugin, *Dendritic Spine Counter*, that will allow researchers and students to count spines in 2D. This plugin allows a user to submit a 2D image of stained neuronal tissue, mark a dendrite on that image for analysis, and receive an automated visually interactive list of structural features along that dendrite that are likely to be spines. The plugin operates “semi-automatically”, requiring minimal input from the user outside of specifying the dendrite to analyze. It also offers manual override options, permitting the user to directly add or remove spines. The machine vision algorithms underlying Dendritic Spine Counter were implemented using a heuristic-driven “expert system” approach, primarily employing basic image transformation functions and statistical analyses of pixel areas.

## Background and motivation
This plugin was developed in order to fill a need in the neuroscience research community. This need largely consists of reducing the barrier to entry for smaller labs to perform investigations of potential links between dendritic spine densities and a lab's larger research focus. In the process, *Dendritic Spine Counter* was built with an aim toward creating objective, explainable criteria for its identification process; this involved eschewing more modern popular computer vision techniques that revolve around machine learning, in favor of more transparent heuristic approaches.

### Democratizing access to avenues of investigation
At the time of this app's development, smaller labs lack a way to conveniently assign and perform the relatively simple but tedious task of identifying, labeling, and counting dendritic spines. Automated tools for this task certainly exist, but are generally bundled within large software suites with high price-points and rigid licensing restrictions, thus often putting them out of reach of smaller labs with tighter budgets. 

Without access to such automated tools, small labs that wish to perform dendritic spine counts still have the ability to do so manually, but this entails allocating time and effort on the part of a skilled staff member with sufficient expertise to perform spine recognition on neuronal slices on their own. With a large set of images or a particularly high density of spines, this could be a quite labor-intensive process. 

As such, small labs often find themselves squeezed between the proverbial "rock" of budgetary constraints and the "hard place" of labor availability, rendering this fairly straightforward task to be counterintuitively cost-prohibitive. This makes it impossible for them to perform exploratory investigation of any potential connections between their research topics and dendritic spine densities. That is, supposing a researcher's curiosity is piqued by the potential hypothesis that some disease, drug, behavioral phenomenon, etc., might affect (or be affected by) unusual dendritic spine densities, that researcher needs to be ready to commit a nontrivial investment of time, money, or both in order to test whether or not the hypothesis has merit. As such, this investigation simply is left undone, and the hypothesis left unexplored.

Fortunately, tasks that are cost-prohibitive due to tediousness are prime candidates for computer-assisted automation. With *Dendritic Spine Counter* now available, such labs can perform this research purely with free-to-use open-source tools.

### Improving Replication






## Methods
Herein we describe *Dendritic Spine Counter*'s principles of operation and the methods employed in its development.




























