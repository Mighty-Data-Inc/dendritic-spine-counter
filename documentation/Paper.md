# Dendritic Spine Counter

## Abstract
In the central nervous system, most excitatory synapses are represented as small protrusions called dendritic spines. The number of excitatory synapses on a neuron impacts overall excitability, and, on a larger scale, network activity. In diseases such as Alzheimer's Disease, Autism Spectrum Disorder, and schizophrenia, spine density is abnormal, resulting in improper brain function. Therefore, computing spine density is an important data-gathering step for many avenues of research into these disorders. Here, we have created a free ImageJ plugin, Dendritic Spine Counter, that will allow researchers and students to count spines in 2D. This plugin allows a user to submit a 2D image of stained neuronal tissue, mark a dendrite on that image for analysis, and receive an automated visually interactive list of structural features along that dendrite that are likely to be spines. The plugin operates “semi-automatically”, requiring minimal input from the user outside of specifying the dendrite to analyze. It also offers manual override options, permitting the user to directly add or remove spines. The machine vision algorithms underlying Dendritic Spine Counter were implemented using a heuristic-driven “expert system” approach, primarily employing basic image transformation functions and statistical analyses of pixel areas.

## Background
This plugin was developed in order to fill a need in the neuroscience research community. At the time of its authorship, smaller labs lacked a way to conveniently assign and perform the relatively simple but tedious task of identifying, labeling, and counting dendritic spines. Automated tools for this task certainly existed, but were generally bundled within large software suites whose price-points and licensing restrictions often put out of reach of smaller labs with tighter budgets and fewer staff. With *Dendritic Spine Counter* now available, such labs can perform this research purely with free-to-use open-source tools.

## Methods
Herein we describe *Dendritic Spine Counter*'s principles of operation and the methods employed in its development.




























