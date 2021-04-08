# Fast or Forced to Follow - A mesoscopic simulation framework for handling speed heterogeneous multi-lane bicycle traffic :bicyclist:
Fast or Forced to Follow (FFF) is a simulation framework for MATSim [(Horni et al., 2016)](#MATSim).

The theoretic foundation for the contribution is documented in  [Paulsen et al. (2019)](#PaulsenFFF); [Paulsen & Nagel (2019)](#PaulsenNagel); [Paulsen et al. (2021)](#PaulsenRoW). 

The framework was developed during the PhD project *Mesoscopic Simulation of Multi-Modal Urban Traffic* at Technical University of Denmark [(Paulsen, 2020)](#PhD)


## Methodology

### Bicycle simulation using speed heterogeneous agents

For now, see [Paulsen et al. (2019)](#PaulsenFFF)

### Bicycle traffic assignment 
For now, see [Paulsen & Nagel (2019)](#PaulsenNagel)

### Multi-modal right-of-way
For now, see [Paulsen et al. (2021)](#PaulsenRoW)


## How to use it

### Disclaimer 

Unfortunately, it has not yet been possible to integrate the entire code properly in MATSim. 
This would require a lot of core classes in MATSim to be changed - either by not making them final, or by changing the visibility of fields within classes, etc. 
Delegation alone could not deal with these issues. 
Such changes require close collaboration with the MATSim core devlopment team or my experience with how to ensure that changes of the core are kept to a minimum. 

To circumvent such changes, instead a lot of the classes used in the repository are small alterations to existing core files of MATSim such as new versions of
`QueueWithBuffer` with only very few changes. 


### How to use the different parts of the framework

#### Bicycle simulation using speed heterogeneous agents & Bicycle traffic assignment 

#### Multi-modal right-of-way




## The bicycle contrib

This framework is not a plan of the bicycle contrib of MATSim (https://github.com/matsim-org/matsim-libs/tree/master/contribs/bicycle/src/main/java/org/matsim/contrib/bicycle). 
I have discussed migrating the entire project or some parts of it to the bicycle contrib with their maintainers, but decided that most of the methodology of this project
is too specific for inclusion in that contrib. 
For most purposes it will be fine to ignore bicycle congestion or to model it in a simpler way that does not require simulating 100% of the population. 


## References
<a id="MATSim">:newspaper:</a>
Horni, A., Nagel, K., & Axhausen, K. W. (Eds.). (2016). 
*The Multi-Agent Transport Simulation MATSim.* 
London: Ubiquity Press. 
DOI: https://doi.org/10.5334/baw 

<a id="PhD">:newspaper:</a>
Paulsen, M. (2020).
*Mesoscopic Simulation of Multi-Modal Urban Traffic.*
PhD Thesis. 
URL: https://orbit.dtu.dk/en/publications/mesoscopic-simulation-of-multi-modal-urban-traffic

<a id="PaulsenNagel">:newspaper:</a>
Paulsen, M., & Nagel, K. (2019). 
*Large-Scale Assignment of Congested Bicycle Traffic Using Speed Heterogeneous Agents.*
Procedia Computer Science, 151, 820–825.
URL: https://doi.org/10.1016/j.procs.2019.04.112 

<a id="PaulsenFFF">:newspaper:</a>
Paulsen, M., Rasmussen, T. K., & Nielsen, O. A. (2019). 
*Fast or forced to follow: A speed heterogeneous approach to congested multi-lane bicycle traffic simulation.* 
Transportation Research Part B: Methodological, 127, 72–98. 
URL: https://doi.org/10.1016/j.trb.2019.07.002 

<a id="PaulsenRoW">:newspaper:</a>
Paulsen, M., Rasmussen, T. K., & Nielsen, O. A. (2021). 
*Including Right-of-Way in a Joint Large-Scale Agent-Based Dynamic Traffic Assignment Model for Cars and Bicycles.* 
Networks and Spatial Economics. 
Revised manuscript submitted after first round of review. 





