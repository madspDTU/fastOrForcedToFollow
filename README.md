# Fast or Forced to Follow - A mesoscopic simulation framework for handling speed heterogeneous multi-lane bicycle traffic :bicyclist:
Fast or Forced to Follow (FFF) is a simulation framework for MATSim [(Horni et al., 2016)](#MATSim).

The framework was developed during the PhD project *Mesoscopic Simulation of Multi-Modal Urban Traffic* at Technical University of Denmark [(Paulsen, 2020)](#PhD)



## Table of contents
- [Methodology](#methodology)
  * [Bicycle simulation using speed heterogeneous agents](#bicycle-simulation-using-speed-heterogeneous-agents)
  * [Bicycle traffic assignment](#bicycle-traffic-assignment)
  * [Multi-modal right-of-way](#multi-modal-right-of-way)
- [Technical details about the Java code](#technical-details-about-the-java-code)
  * [How to use it](#how-to-use-it)
  * [Network simulation](#network-simulation)
    + [Architecture](#architecture)
      - [Simulating bicycle traffic along links](#simulating-bicycle-traffic-along-links)
      - [Dealing with right-of-way at intersections](#dealing-with-right-of-way-at-intersections)
  * [Scoring and plan selection](#scoring-and-plan-selection)
- [Relation to the bicycle contrib](#relation-to-the-bicycle-contrib)
- [References](#references)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


## Methodology

### Bicycle simulation using speed heterogeneous agents

Allows simulation of cyclists with different desired speeds and preferences for headway distances. 
The simulation model was developed and is explained in detail in [Paulsen et al. (2019)](#PaulsenFFF).
The paper also includes a detailed description of how the parameters of the model was estimated.
Results of small-scale experiments are also found in the paper, and are validated with real-life observations from Queen Louise's Bridge in Central Copenhagen.

The reader is guided towards [Paulsen et al. (2019)](#PaulsenFFF) for further details.

### Bicycle traffic assignment 

The methodology of  [Paulsen et al. (2019)](#PaulsenFFF) was further extended in [Paulsen & Nagel (2019)](#PaulsenNagel)  for application in large-scale bicycle traffic assignment. 
The bicycle simulation model was implemented in MATSim [(Horni et al, 2016)](#MATSim), and appropriate corrections to the routing and scoring was conducted.

Results of large scale-experiments from Metropolitan Copenhagen and further details can be found in [Paulsen & Nagel (2019)](#PaulsenNagel).

### Multi-modal right-of-way

The methodology was further extended in [Paulsen et al. (2021)](#PaulsenRoW), where delays caused by right-of-way at (multi-modal) intersections were also included.
Nodes of the network are categorised into different node types, and at each of such nodes conflicts between different moves are determined before the simulation.
During the simulation, the model ensures that no conflicting moves can take place simultaneously in any of the nodes of the network. 

For further details, the reader is guided towards [Paulsen et al. (2021)](#PaulsenRoW) which also contains results of large-scale experiments from Metroplitan Copenhagen. 


## Technical details about the Java code
_DISCLAIMER_

Unfortunately, it has not yet been possible to integrate the entire code properly in MATSim. 
This would require a lot of core classes in MATSim to be changed - either by not making them final, or by changing the visibility of fields within classes, etc. 
Delegation alone could not deal with these issues. 
Such changes require close collaboration with the MATSim core devlopment team or my experience with how to ensure that changes of the core are kept to a minimum. 

To circumvent such changes, instead a lot of the classes used in the repository are small alterations to existing core files of MATSim such as new versions of
`QueueWithBuffer` with only very few changes. 


### How to use it

Any MATSim scenario can be setup to be valid for FFF simulation by running `FFFUtils.prepareScenarioForFFF(scenario)`. In theory, this should work with any scenario.

Likewise, a controler can be enriched to work with FFF by running `FFFUtils.prepareControlerForFFF(controler)`. How well this works with other controlers is an open question, but should in theory be possible (but has not been tested). 

### Network simulation

The framework comes with three possible setups for simulating traffic:

* FFF with congestion and the standard MATSim node model (`prepareControlerForFFF(controler)`)
* FFF without and with the standard MATSim node model (`prepareControlerForFFFWithoutCongestion(controler)`)
* FFF with congestion and a newly proposed right-of-way node model (`prepareControlerForFFFWithRoW(controler)`), see [Paulsen et al. (2021)](#PaulsenRoW)

In all cases, the network is simulated according to the default configurations of those setups. 
These configurations can be adjusted in the config groups, `FFFConfigGroup`, `FFFNodeConfigGroup`.

#### Architecture

This section contains further technical details.

##### Simulating bicycle traffic along links

The FFF simulation model overrules the standard way of simulating vehicles across links.
In order to potentially allow cars to be simulated normally alongside, separate links are created to handle the bicycle traffic. 
For these links, the buffers are changed so that the simulation happens in the `sublink`-class instead of the traditional `QueueWithBuffer`. Because the methodology is sensitive to the link length, links longer than some distance (default 60m) are split into multiple `sublink`s.

Furthermore, as each cyclist has unique parameters that determine the desired speed and headway preferences of that cyclist, the traditional `QVehicle` is not sufficient, why the extended `QCycle` is used instead.
A `QCycle` is a `QVehicle` but further has a `Cyclist` that holds the parameters of the agent. 


The FFF simulation model is installed by replacing the `QNetworkFactory` and the `QVehicleFactory` with an implementation of `AbstractFFFQNetworkFactory` and the `FFFQVehicleFactory`:

```
controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( DefaultFFFQNetworkFactory.class );
				this.bind( QVehicleFactory.class ).to( FFFQVehicleFactory.class ) ;
			}
		});
```

Also included in the code is a method `prepareScenarioForFFF(scenario)` which samples `Cyclist` attributes to each agent according to the distributions estimated in [Paulsen et al. (2019)](#PaulsenFFF). 
Parameters can be adjusted for other populations with for instance lower average speeds if needed, by changing the parameters of the `FFFConfigGroup`.
Implementing parameters being sampled from other distributions is not included in the code, but can be implemented more or less straightforwardly. 




##### Dealing with right-of-way at intersections

For adding right-of-way to MATSim it is no longer sufficient to only replace how vehicles are simulated along links. 
Instead, it is also needed to overrule how vehicles are simulated across nodes.
This is done by building `QFFFNodes` instead of the standard `QNodeImpl`, which is normally used as the extension for `AbstractQNode`.
However, the installment is the same as for simulating traffic without right-of-way, but with using the `FFFQNetworkFactoryWithQFFFNodes` instead of the `DefaultQNetworkFactory`.

Some important classes for the extensions are:

* `QVehicleAndMoveType`: Expanding QVehicle to also include a `moveType` (used for determining how the vehicle is processed).
* `QFFFAbstractNode`:  A class contained as a field in the `QFFFNodes`. Different extensions exists for different nodetypes, see [Paulsen et al., (2021)](#PaulsenRoW). For all of such nodes a double matrices for motorised traffic and bicycle traffic - carTimeouts and bicycleTimeouts, respectively - that hold elements representing the time until which the move from bundle i to bundle j is blocked. 
* `ConflictingMovesData`: A class containing for any move across the node, the set of conflicting moves with lower or equal prioritisation, and whether that conflicting move is of equal or lower prioritisation. 

In order for the right-of-way modelling to find the best solutions during routing, it is highly recommended that linkToLinkRouting is enabled. 
When this is the case the `addGeneralStuffToControler(controler)` will automatically enable the adjustments to the link to link routing mentioned in [Paulsen et al., (2021)](#PaulsenRoW).






### Scoring and plan selection

New plan selectors (`FFFPlanSelectors`) for selecting between plans are also proposed. 
Three different strategies are possible (introduced in the list further down).
Common for all of the strategies, is that they are bounded. 
This means, that after all iterations, if a plan has a score that is worse than  the score of the best plan times `threshold`, that plan is removed.
Likewise, all plans that have not been used for the past `maximumMemory` iterations, are also removed. 
See also [Paulsen et al. (2021)](#PaulsenRoW).

The three different strategies are: 

* `FFFBoundedLogitPlanSelector`
* `FFFGradualBoundedLogitPlanSelector`
* `FFFBestBoundedPlanSelector`

The bounded logit uses a fixed beta throughout all iterations, why all plans have a non-zero probability of being chosen in each iteration.
Probabilities are calculated according to the bounded logit model, where the probability mass is squeezed into the range \[bestScore, threshold\*bestScore\],
with a plan of score threshold\*bestscore having a probability of 0. 
For more information on bounded choice models, see [Watling et al., (2018)](#Watling2018).

The gradual bounded logit increases the value of beta throughout the iterations, so that the best plan is eventually guaranteed to be chosen.

The best bounded always chooses the plan that has the best score. 




## Relation to the bicycle contrib

This framework is not a part of the bicycle contrib of MATSim (https://github.com/matsim-org/matsim-libs/tree/master/contribs/bicycle/src/main/java/org/matsim/contrib/bicycle). 
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
Paulsen, M., Rasmussen, T. K., & Nielsen, O. A. (2022). 
*Including Right-of-Way in a Joint Large-Scale Agent-Based Dynamic Traffic Assignment Model for Cars and Bicycles.* 
Networks and Spatial Economics, 22(4), 915-957.
URL: https://doi.org/10.1007/s11067-022-09573-w

<a id="Watling2018">:newspaper:</a>
Watling, D. P., Rasmussen, T. K., Prato, C. G., & Nielsen, O. A. (2018). 
Stochastic user equilibrium with a bounded choice model. 
Transportation Research. Part B: Methodological, 114, 254–280.
URL: https://doi.org/10.1016/j.trb.2018.05.004 



