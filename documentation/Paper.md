# *Dendritic Spine Counter* plugin for ImageJ

## Abstract
In the central nervous system, most excitatory synapses are represented as small protrusions called dendritic spines. The number of excitatory synapses on a neuron impacts overall excitability, and, on a larger scale, network activity. In diseases such as Alzheimer's Disease, Autism Spectrum Disorder, and schizophrenia, spine density is abnormal, resulting in improper brain function. Therefore, computing spine density is an important data-gathering step for many avenues of research into these disorders. Here, we have created a free ImageJ plugin, *Dendritic Spine Counter*, that will allow researchers and students to count spines in 2D. This plugin allows a user to submit a 2D image of stained neuronal tissue, mark a dendrite on that image for analysis, and receive an automated visually interactive list of structural features along that dendrite that are likely to be spines. The plugin operates “semi-automatically”, requiring minimal input from the user outside of specifying the dendrite to analyze. It also offers manual override options, permitting the user to directly add or remove spines. The machine vision algorithms underlying Dendritic Spine Counter were implemented using a heuristic-driven “expert system” approach, primarily employing basic image transformation functions and statistical analyses of pixel areas.

## Background and motivation
This plugin was developed in order to fill a need in the neuroscience research community. This need largely consists of reducing the barrier to entry for smaller labs to perform investigations of potential links between dendritic spine densities and a lab's larger research focus. In the process, *Dendritic Spine Counter* was built with an aim toward creating objective, explainable criteria for its identification process; this involved eschewing more modern popular computer vision techniques that revolve around machine learning, in favor of more transparent heuristic approaches.

### Democratizing access to avenues of investigation
At the time of this app's development, smaller labs lack a way to conveniently assign and perform the relatively simple but tedious task of identifying, labeling, and counting dendritic spines. Automated tools for this task certainly exist, but are generally bundled within large software suites with high price-points and rigid licensing restrictions, thus often putting them out of reach of smaller labs with tighter budgets. Small labs can still perform dendritic spine counts manually, but this entails allocating time and effort on the part of a skilled staff member with sufficient expertise to perform spine recognition on neuronal slices on their own. With a large set of images or a particularly high density of spines, this could be a quite labor-intensive process[1].

As such, small labs often find themselves squeezed between the proverbial "rock" of budgetary constraints and the "hard place" of labor availability, rendering this fairly straightforward task to be counterintuitively cost-prohibitive. This makes it impossible for them to perform exploratory investigation of any potential connections between their research topics and dendritic spine densities. That is, supposing a researcher's curiosity is piqued by the potential hypothesis that some disease, drug, behavioral phenomenon, etc., might affect (or be affected by) unusual dendritic spine densities, that researcher needs to be ready to commit a nontrivial investment of time, money, or both in order to test whether or not the hypothesis has merit. As such, this investigation simply is left undone, and the hypothesis left unexplored.

Fortunately, tasks that are cost-prohibitive due to tediousness are prime candidates for computer-assisted automation. With *Dendritic Spine Counter* now available, such labs can perform this research purely with free-to-use open-source tools.

### Aiding replication
The identification of dendritic spines is not an entirely objective process. Though hard criteria for detection and classification of spines have existed for decades[2], the exact procedures involved in applying those criteria to real-world 2D images remains a source of a great deal of debate and controversy[6]. 

Having a single common quantifiable de facto approach to this problem would facilitate more reliable replication of experimental results. Being an automated tool, *Dendritic Spine Counter* cannot exhibit differences in judgment, confirmation bias, and other modalities of human error[7]. This is not to say that *Dendritic Spine Counter* is categorically immune to error, of course, but rather to say that its error modalities will be systemic rather than situational, and can at least be relied upon to be consistent across multiple executions[8].

## Prior art
Efforts at producing low-cost or free software to perform computer-assisted spine identification are not new[3]. However, these efforts tend to focus on spine morphology in three dimensions[3], or even four with the inclusion of a temporal component[4]. These types of analyses are not always suitable for lower-budget labs, who typically don't have reliable access to the kind of equipment, such as electron microscopy[5], that can produce three-dimensional images of sufficient quality to be usable with some of these automated approaches. 



## Methods
Herein we describe *Dendritic Spine Counter*'s principles of operation and the methods employed in its development.







## References
1. Rapid Golgi Analysis Method for Efficient and Unbiased Classification of Dendritic Spines
Risher WC, Ustunkaya T, Singh Alvarado J, Eroglu C (2014) Rapid Golgi Analysis Method for Efficient and Unbiased Classification of Dendritic Spines. PLOS ONE 9(9): e107591. https://doi.org/10.1371/journal.pone.0107591
2. Harris KM, Jensen FE, Tsao B. Three-dimensional structure of dendritic spines and synapses in rat hippocampus (CA1) at postnatal day 15 and adult ages: implications for the maturation of synaptic physiology and long-term potentiation. J Neurosci. 1992 Jul;12(7):2685-705. doi: 10.1523/JNEUROSCI.12-07-02685.1992. Erratum in: J Neurosci 1992 Aug;12(8):followi. PMID: 1613552; PMCID: PMC6575840. https://www.jneurosci.org/content/jneuro/12/7/2685.full.pdf
3. Rodriguez A, Ehlenberger DB, Dickstein DL, Hof PR, Wearne SL. Automated three-dimensional detection and shape classification of dendritic spines from fluorescence microscopy images. PLoS One. 2008 Apr 23;3(4):e1997. doi: 10.1371/journal.pone.0001997. PMID: 18431482; PMCID: PMC2292261.
4. Swanger, S.A., Yao, X., Gross, C. et al. Automated 4D analysis of dendritic spine morphology: applications to stimulus-induced spine remodeling and pharmacological rescue in a disease model. Mol Brain 4, 38 (2011). https://doi.org/10.1186/1756-6606-4-38
5. Kashiwagi, Y., Higashi, T., Obashi, K. et al. Computational geometry analysis of dendritic spines by structured illumination microscopy. Nat Commun 10, 1285 (2019). https://doi.org/10.1038/s41467-019-09337-0
6. Pchitskaya E, Bezprozvanny I. Dendritic Spines Shape Analysis-Classification or Clusterization? Perspective. Front Synaptic Neurosci. 2020 Sep 30;12:31. doi: 10.3389/fnsyn.2020.00031. PMID: 33117142; PMCID: PMC7561369.
7. Cameron, James (1984). The Terminator. Orion Pictures. p.121. https://www.scriptslug.com/assets/scripts/the-terminator-1984.pdf
8. Gonick, L. (1993). The Cartoon Guide to Statistics. HarperPerennial. https://www.amazon.com/Cartoon-Guide-Statistics-Larry-Gonick/dp/0062731025/







