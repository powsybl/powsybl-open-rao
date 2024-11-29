# Tutorial

In this tutorial, we will see how to run a simple RAO from a network file and a CRAC. The CRAC will be created from
scratch using the Java API so there is no need to import a CRAC file.

## Set-up

For this tutorial, we'll need Java 17, and we'll create a new project called `org.example` and work on its `Main` class.
For everything to work properly, you also need to install the latest versions
of [PowSyBl core](https://github.com/powsybl/powsybl-core),
[PowSyBl Open Rao](https://github.com/powsybl/powsybl-open-rao) and
[PowSyBl Open Load Flow (OLF)](https://github.com/powsybl/powsybl-open-loadflow).

Start by creating a Maven `pom.xml` file and add the following dependencies:

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-starter</artifactId>
    <version>2023.4.0</version>
</dependency>
```

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>open-rao-crac-impl</artifactId>
    <version>5.0.1</version>
</dependency>
```

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
```

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-ucte-converter</artifactId>
    <version>6.1.0</version>
    <scope>runtime</scope>
</dependency>
```

## Import network file

The first step is to import a network for the simulation. As an example, we will consider the following 12-nodes UCTE
network that is made of 18 lines including 1 PST spread over 4 countries. All the production (1000 MW) is located in the
Netherlands (node _NNL1AA1_) and the consumption (1000 MW) is in France (node _FFR1AA1_).

![Basecase network](/_static/img/tutorial/basecase.svg){.forced-white-background}

We will create a UCTE file to model this network, so it can be processed and imported for the RAO. Copy and paste the
network data in a file named `12Nodes.uct` that you shall store in the resources directory of the project.

```
##C 2007.05.01
##N
##ZBE
BBE1AA1  BE1          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
BBE2AA1  BE2          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
BBE3AA1  BE3          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
##ZDE
DDE1AA1  DE1          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
DDE2AA1  DE2          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
DDE3AA1  DE3          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
##ZFR
FFR1AA1  FR1          0 2 400.00 1000.00 0.00000 00000.0 0.00000 9000.00 -9000.0 9000.00 -9000.0
FFR2AA1  FR2          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
FFR3AA1  FR3          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
##ZNL
NNL1AA1  NL1          0 2 400.00 0000.00 0.00000 -1000.0 0.00000 9000.00 -9000.0 9000.00 -9000.0
NNL2AA1  NL2          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
NNL3AA1  NL3          0 2 400.00 0.00000 0.00000 0.00000 0.00000 9000.00 -9000.0 9000.00 -9000.0
##L
BBE1AA1  BBE2AA1  1 0 0.0000 10.000 0.000000   5000
BBE1AA1  BBE3AA1  1 0 0.0000 10.000 0.000000   5000
FFR1AA1  FFR2AA1  1 0 0.0000 10.000 0.000000   5000
FFR1AA1  FFR3AA1  1 0 0.0000 10.000 0.000000   5000
FFR2AA1  FFR3AA1  1 0 0.0000 10.000 0.000000   5000
DDE1AA1  DDE2AA1  1 0 0.0000 10.000 0.000000   5000
DDE1AA1  DDE3AA1  1 0 0.0000 10.000 0.000000   5000
DDE2AA1  DDE3AA1  1 0 0.0000 10.000 0.000000   5000
NNL1AA1  NNL2AA1  1 0 0.0000 10.000 0.000000   5000
NNL1AA1  NNL3AA1  1 0 0.0000 10.000 0.000000   5000
NNL2AA1  NNL3AA1  1 0 0.0000 10.000 0.000000   5000
FFR2AA1  DDE3AA1  1 0 0.0000 10.000 0.000000   5000
DDE2AA1  NNL3AA1  1 0 0.0000 10.000 0.000000   5000
NNL2AA1  BBE3AA1  1 0 0.0000 10.000 0.000000   410
NNL2AA1  BBE3AA1  2 8 0.0000 10.000 0.000000   410
NNL2AA1  BBE3AA1  3 8 0.0000 10.000 0.000000   410
BBE2AA1  FFR3AA1  1 0 0.0000 10.000 0.000000   5000
##T
BBE2AA1  BBE3AA1  1 0 400.0 400.0 1000. 0.0000 10.000 0.000000 0.0	     5000 PST
##R
BBE2AA1  BBE3AA1  1                    -0.68 90.00 16 0         SYMM

```

The network can be imported using [PowSyBl](https://www.powsybl.org/index.html):

```java
String networkFilename = "12Nodes.uct";
Network network = Network.read(networkFilename, Main.class.getResourceAsStream("/%s".formatted(networkFilename)));
```

For this tutorial, we will simulate the loss of line _NNL3AA1 DDE2AA1 1_. This loss will divert the power and increase
the flow in line _NNL3AA1 DDE2AA1 1_ over its admissible power limit. We will study how the RAO can help us solve the
resulting problems on the network thanks to remedial actions.

## Create CRAC

The [CRAC](/input-data/crac.md) is the data object that contains all the key information for the RAO, i.e. the
contingencies to simulate, the CNECs to optimise and the remedial actions to apply. The RAO's Java API allows users to
manually fill the CRAC with all the required and desired data.

The first step is to instantiate an empty CRAC using the CracFactory:

```java
Crac crac = CracFactory.findDefault().create();
```

Once created, the CRAC can be filled sequentially (some data must be provided before others for logical reasons) with
the information required to model our scenario.

### Create contingencies

Start by defining a [contingency](/input-data/crac/json.md#contingencies) called "contingency", on line
_NNL3AA1 DDE2AA1 1_, with the following code:

```java
crac.newContingency()
    .withId("contingency")
    .withContingencyElement("NNL3AA1  DDE2AA1  1", ContingencyElementType.LINE)
    .add();
```

### Add instants

Once the contingencies are added, we can now create the different [instants](/input-data/crac/json.md#instants-and-states)
of the optimisation process. An instant is added thanks to the `newInstant` method. Both an identifier and
an `InstantKind` (`PREVENTIVE`, `OUTAGE`, `AUTO` or `CURATIVE`) must be provided. The instants must also be declared in
**chronological order**.

For our example, we only need 3 instants:

1. one **preventive** instant that represents the base case
2. one **outage** instant that account for the state of the network right after the contingency (loss of line _NNL3AA1
   DDE2AA1 1_) occurred
3. one **curative** instant during which curative remedial actions can be applied to solve the problems resulting from
   the outage

```java
crac.newInstant("preventive", InstantKind.PREVENTIVE)
    .newInstant("outage", InstantKind.OUTAGE)
    .newInstant("curative", InstantKind.CURATIVE);
```

Now that contingencies and instants are all set, we can start adding CNECs and remedial actions to the CRAC.

### Create FlowCNECs

The next step is to create the [CNECs](/input-data/crac/json.md#cnecs). For our example and given the simple network
we are using, we will only consider [FlowCNECs](/input-data/crac/json.md#flow-cnecs) that correspond to lines in the
network that will have to be optimised flow-wise after contingencies (and in basecase). The FlowCNECs also have
thresholds that indicate the maximum admissible flow on the line for a given instant.

Let us make sure that the flow on line _NNL2AA1 BBE3AA1 1_ stays under 410 MW in basecase:

```java
crac.newFlowCnec()
    .withId("NNL2AA1  BBE3AA1  1 - preventive")
    .withInstant("preventive")
    .withOptimized()
    .withNetworkElement("NNL2AA1  BBE3AA1  1")
    .newThreshold()
       .withMin(-410d)
       .withMax(+410d)
       .withUnit(Unit.MEGAWATT)
       .withSide(TwoSides.ONE)
       .add()
    .add();
```

Similarly, we need to verify that the flow on the line does not excedd the 1000 MW TATL after the loss of line _NNL3AA1
DDE2AA1 1_:

```java
crac.newFlowCnec()
    .withId("NNL2AA1  BBE3AA1  1 - outage")
    .withInstant("outage")
    .withOptimized()
    .withContingency("contingency")
    .withNetworkElement("NNL2AA1  BBE3AA1  1")
    .newThreshold()
        .withMin(-1000d)
        .withMax(+1000d)
        .withUnit(Unit.MEGAWATT)
        .withSide(TwoSides.ONE)
        .add()
    .add();
```

Finally, let us assess that the flow goes back under the 410 MW PATL after the application of curative remedial actions:

```java
crac.newFlowCnec()
    .withId("NNL2AA1  BBE3AA1  1 - curative")
    .withInstant("curative")
    .withOptimized()
    .withContingency("contingency")
    .withNetworkElement("NNL2AA1  BBE3AA1  1")
    .newThreshold()
        .withMin(-410d)
        .withMax(+410d)
        .withUnit(Unit.MEGAWATT)
        .withSide(TwoSides.ONE)
        .add()
    .add();
```

### Add remedial actions

#### Add a preventive PST range action

Let us add preventive a [PST range action](/input-data/crac/json.md#range-actions). For simplicity's sake, it is
easier to rely on an `IidmPstHelper` which fetches the PST's information in the network to create the remedial action.

```java
IidmPstHelper iidmPstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

crac.newPstRangeAction()
   .withId("pst-range-action")
   .withNetworkElement("BBE2AA1  BBE3AA1  1")
   .withInitialTap(iidmPstHelper.getInitialTap())
   .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap())
   .newTapRange()
      .withMinTap(-16)
      .withMaxTap(16)
      .withRangeType(RangeType.ABSOLUTE)
      .add()
   .newOnInstantUsageRule()
      .withInstant("preventive")
      .withUsageMethod(UsageMethod.AVAILABLE)
      .add()
   .add();
```

#### Add a curative terminals connection action

We can finish by adding a terminals connection action to the CRAC, which consists in connecting lines _NNL2AA1 BBE3AA1 2_ and
_NNL2AA1 BBE3AA1 3_ (both parallel to _NNL2AA1 BBE3AA1 1_) to the rest of the network.

```java
crac.newNetworkAction()
      .withId("terminals-connection-action")
      .newTerminalsConnectionAction()
         .withNetworkElement("NNL2AA1  BBE3AA1  2")
         .withActionType(ActionType.CLOSE)
         .add()
      .newTerminalsConnectionAction()
         .withNetworkElement("NNL2AA1  BBE3AA1  3")
         .withActionType(ActionType.CLOSE)
         .add()
      .newOnContingencyStateUsageRule()
         .withInstant("curative")
         .withContingency("contingency")
         .withUsageMethod(UsageMethod.AVAILABLE)
         .add()
      .add();
```

## RAO Parameters

Next, define the parameters to run the RAO using the [RaoParameters](./parameters.md) object

```java
RaoParameters raoParameters = new RaoParameters();

// Enable DC mode for load-flow & sensitivity computations
LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
loadFlowParameters.setDc(true);
SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();
sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);

// Set "OpenLoadFlow" as load-flow provider
LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
loadFlowAndSensitivityParameters.setLoadFlowProvider("OpenLoadFlow");
loadFlowAndSensitivityParameters.setSensitivityWithLoadFlowParameters(sensitivityAnalysisParameters);
raoParameters.setLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);

// Ask the RAO to maximize minimum margin in MW, and to stop when network is secure (i.e. when margins are positive)
ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
objectiveFunctionParameters.setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
objectiveFunctionParameters.setUnit(Unit.MEGAWATT);
objectiveFunctionParameters.setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
objectiveFunctionParameters.setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
raoParameters.setObjectiveFunctionParameters(objectiveFunctionParameters);

// Enable "APPROXIMATED_INTEGERS" in PST optimization, for better accuracy
raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
```

## Run the RAO

 Run the RAO using the following code to produce a [`RaoResult`](/output-data/rao-result.md) object:

 ```java
 RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
 RaoResult raoResult = Rao.find().run(raoInputBuilder.build(), raoParameters);
```

All the important information regarding the optimisation process (activated remedial actions and CNEC flow at each
instant) can be found in this RAO Result.

## Step-by-step results

We will go through the results of the RAO, instant by instant, to analyse the different optimisation steps and study the
RAO's behaviour.

### Base case and preventive optimisation

As presented earlier, the whole electricity production (1000 MW) in the network is located at node _NNL1AA1_. The flow
is divided evenly among lines _NNL2AA1 BBE3AA1 1_ and _DDE2AA1 NNL3AA1 1_. The consumption (1000 MW as well) is entirely
locate at node _FFR1AA1_.

![Basecase network](/_static/img/tutorial/basecase.svg){.forced-white-background}

However, the PATL of line _NNL2AA1 BBE3AA1 1_ is set to 410 MW which is below the current 500 MW flow on the line. Thus,
remedial actions must be applied to solve this base case issue. In The CRAC, we only defined one preventive remedial
action which is the PST range action. By changing the PST's tap, we can change the line's impedance and thus modify the
flow.

```
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [start]
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Root leaf, cost: 90.00 (functional: 90.00, virtual: 0.00)
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #01: margin = -90.00 MW, element NNL2AA1  BBE3AA1  1 at state preventive, CNEC ID = "NNL2AA1  BBE3AA1  1 - preventive"
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #02: margin = 0.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - outage, CNEC ID = "NNL2AA1  BBE3AA1  1 - outage"
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Linear optimization on root leaf
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Loading library 'jniortools'
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Iteration 1: better solution found with a cost of -0.00 (functional: -0.00)
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Iteration 2: same results as previous iterations, optimal solution found
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Root leaf, 1 range action(s) activated, cost: -0.00 (functional: -0.00, virtual: 0.00)
[main] INFO  c.p.o.commons.logs.TechnicalLogs - range action(s): pst-range-action: -10
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = 0.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - outage, CNEC ID = "NNL2AA1  BBE3AA1  1 - outage"
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = 8.15 MW, element NNL2AA1  BBE3AA1  1 at state preventive, CNEC ID = "NNL2AA1  BBE3AA1  1 - preventive"
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "preventive": initial cost = 90.00 (functional: 90.00, virtual: 0.00), 1 range action(s) activated : pst-range-action: -10, cost after preventive optimization = -0.00 (functional: -0.00, virtual: 0.00)
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Preventive perimeter optimization [end]
```

When reading the preventive perimeter's logs above, we notice that the RAO set the new tap of the PST to -10 which
increases the flow margin on line _NNL2AA1 BBE3AA1 1_ up to 8.15 MW (i.e. reduces the flow to 402 MW). The preventive
perimeter is thus secured. The network with the preventive remedial action applied is displayed below.

![Network with PRAs applied](/_static/img/tutorial/preventive.svg){.forced-white-background}

### Loss of line

The contingency is then simulated: line _NNL3AA1 DDE2AA1 1_ is lost. The network's topology is modified and the new flow
is
now of 1000 MW (the whole production power) on line _NNL2AA1 BBE3AA1 1_.

![Network after outage](/_static/img/tutorial/outage.svg){.forced-white-background}

However, the line's TATL is exactly 1000 MW so the network is temporarily secure. Note that this result is coherent with
the most limiting element displayed at the end of the preventive perimeter logs:

```
Limiting element #01: margin = 0.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - outage, CNEC ID = "NNL2AA1  BBE3AA1  1 - outage"
```

Indeed, the flow on line _NNL2AA1 BBE3AA1 1_ is equal to the TATL which is equivalent to a zero margin. As the TATL can
only hold for a limited period of time, curative remedial actions must be applied to bring back the flow under the PATL.

### Curative optimisation

The RAO will now try applying the curative remedial action we defined in the CRAC, to bring the flow on line
_NNL2AA1 BBE3AA1 1_ back under the 410 MW PATL. This curative remedial action is a terminals connection action that closes lines
_NNL2AA1 BBE3AA1 2_ and _NNL2AA1 BBE3AA1 3_, which are both parallel to _NNL2AA1 BBE3AA1 1_, thus dividing the flow in
three. It is expected that the remedial action can solve the current problem.

```
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Post-contingency perimeters optimization [start]
[main] INFO  c.p.o.commons.logs.TechnicalLogs - Using base network '12Nodes' on variant 'ContingencyScenario3b0ea217-ed17-4122-9bf1-7d8ceebf4267'
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Optimizing scenario post-contingency contingency.
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Optimizing curative state contingency - curative.
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Root leaf, cost: 590.00 (functional: 590.00, virtual: 0.00)
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #01: margin = -590.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - curative, CNEC ID = "NNL2AA1  BBE3AA1  1 - curative"
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Linear optimization on root leaf
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - No range actions to optimize
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Root leaf, cost: 590.00 (functional: 590.00, virtual: 0.00)
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - No range actions activated
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #01: margin = -590.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - curative, CNEC ID = "NNL2AA1  BBE3AA1  1 - curative"
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Using base network '12NodesProdNL' on variant 'OpenRaoNetworkPool working variant e2cc75b1-3886-4172-85ae-5fac9232431d'
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Search depth 1 [start]
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Leaves to evaluate: 1
[ForkJoinPool-2-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Evaluated network action(s): terminals-connection-action, cost: -76.67 (functional: -76.67, virtual: 0.00)
[ForkJoinPool-2-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Optimized network action(s): terminals-connection-action, cost: -76.67 (functional: -76.67, virtual: 0.00)
[ForkJoinPool-2-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Stop criterion reached, other threads may skip optimization.
[ForkJoinPool-2-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Remaining leaves to evaluate: 0
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Search depth 1 [end]
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Search depth 1 best leaf: network action(s): terminals-connection-action, cost: -76.67 (functional: -76.67, virtual: 0.00)
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Search depth 1 best leaf: No range actions activated
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #01: margin = 76.67 MW, element NNL2AA1  BBE3AA1  1 at state contingency - curative, CNEC ID = "NNL2AA1  BBE3AA1  1 - curative"
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Search-tree RAO completed with status DEFAULT
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Best leaf: network action(s): terminals-connection-action, cost: -76.67 (functional: -76.67, virtual: 0.00)
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Best leaf: No range actions activated
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Limiting element #01: margin = 76.67 MW, element NNL2AA1  BBE3AA1  1 at state contingency - curative, CNEC ID = "NNL2AA1  BBE3AA1  1 - curative"
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.RaoBusinessLogs - Scenario "contingency": initial cost = 590.00 (functional: 590.00, virtual: 0.00), 1 network action(s) activated : terminals-connection-action, cost after curative optimization = -76.67 (functional: -76.67, virtual: 0.00)
[ForkJoinPool-1-worker-1] INFO  c.p.o.commons.logs.TechnicalLogs - Curative state contingency - curative has been optimized.
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - ----- Post-contingency perimeters optimization [end]
```

We can see in the logs that the remedial action was indeed applied, increasing the margin on line _NNL2AA1 BBE3AA1 1_ to
76.67 MW (i.e. decreasing the flow to 333 MW which is below the PATL). At the end of the curative perimeter, the network
is secure and the three parallel lines are all connected.

![Network with CRAs applied](/_static/img/tutorial/curative.svg){.forced-white-background}

### Final results

```
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Merging preventive and post-contingency RAO results:
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #01: margin = 0.00 MW, element NNL2AA1  BBE3AA1  1 at state contingency - outage, CNEC ID = "NNL2AA1  BBE3AA1  1 - outage"
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #02: margin = 8.15 MW, element NNL2AA1  BBE3AA1  1 at state preventive, CNEC ID = "NNL2AA1  BBE3AA1  1 - preventive"
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Limiting element #03: margin = 76.67 MW, element NNL2AA1  BBE3AA1  1 at state contingency - curative, CNEC ID = "NNL2AA1  BBE3AA1  1 - curative"
[main] INFO  c.p.o.commons.logs.RaoBusinessLogs - Cost before RAO = 590.00 (functional: 590.00, virtual: 0.00), cost after RAO = -0.00 (functional: -0.00, virtual: 0.00)
```

The final cost of the RAO is 0 which represents the worst margin on all CNECs (here it is the CNEC at the outage
instant). Because this cost is non-positive, it ensures that the network is indeed secure.

## Full example

This entire tutorial is condensed into the following Java code snippet so that you can simply copy and paste it.

```java
package org.example;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.craccreation.util.iidm.IidmPstHelper;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

public class Main {

    public static void main(String[] args) {
        // Import network from UCTE file
        String networkFilename = "12NodesProdNL.uct";
        Network network = Network.read(networkFilename, Main.class.getResourceAsStream("/%s".formatted(networkFilename)));

        // Initialise CRAC
        Crac crac = CracFactory.findDefault().create("crac");

        // Create instants
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);

        // Add contingency
        crac.newContingency()
            .withId("contingency")
            .withContingencyElement("DDE2AA1  NNL3AA1  1", ContingencyElementType.LINE)
            .add();

        // Add FlowCNECs
        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - preventive")
            .withInstant("preventive")
            .withOptimized()
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-410d)
            .withMax(+410d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - outage")
            .withInstant("outage")
            .withOptimized()
            .withContingency("contingency")
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-1000d)
            .withMax(+1000d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - curative")
            .withInstant("curative")
            .withOptimized()
            .withContingency("contingency")
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-410d)
            .withMax(+410d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        // Add PST range action (PRA + CRA)
        IidmPstHelper iidmPstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst-range-action")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap())
            .newTapRange()
            .withMinTap(-16)
            .withMaxTap(16)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        // Add auto terminals connection action
        crac.newNetworkAction()
            .withId("terminals-connection-action")
            .newTerminalsConnectionAction()
            .withNetworkElement("NNL2AA1  BBE3AA1  2")
            .withActionType(ActionType.CLOSE)
            .add()
            .newTerminalsConnectionAction()
            .withNetworkElement("NNL2AA1  BBE3AA1  3")
            .withActionType(ActionType.CLOSE)
            .add()
            .newOnContingencyStateUsageRule()
            .withInstant("curative")
            .withContingency("contingency")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        // RAO Parameters setting
        RaoParameters raoParameters = new RaoParameters();

        // Enable DC mode for load-flow & sensitivity computations
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(true);
        SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);

        // Set "OpenLoadFlow" as load-flow provider
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
        loadFlowAndSensitivityParameters.setLoadFlowProvider("OpenLoadFlow");
        loadFlowAndSensitivityParameters.setSensitivityWithLoadFlowParameters(sensitivityAnalysisParameters);
        raoParameters.setLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);

        // Ask the RAO to maximize minimum margin in MW, and to stop when network is secure (i.e. when margins are positive)
        ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
        objectiveFunctionParameters.setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        objectiveFunctionParameters.setUnit(Unit.MEGAWATT);
        objectiveFunctionParameters.setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        objectiveFunctionParameters.setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
        raoParameters.setObjectiveFunctionParameters(objectiveFunctionParameters);

        // Enable "APPROXIMATED_INTEGERS" in PST optimization, for better accuracy
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);

        // Run RAO

        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        RaoResult raoResult = Rao.find().run(raoInputBuilder.build(), raoParameters);
    }
}

```
