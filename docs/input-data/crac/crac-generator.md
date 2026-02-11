# Automatic CRAC generator

## Introduction
This algorithm, desguised as a CRAC importer, allows the user to create a CRAC object seamlessly, using a network file.  
It supports all network formats supported by PowSyBl.  
Note, however, that the user is expected to provide some extensive CRAC creation parameters in order for the algorithm to work.  

## Usage
First, you must include it as a dependency in your maven module:
~~~xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>open-rao-crac-io-network</artifactId>
</dependency>
~~~
Then, the importer works like any other importer, only by replacing the CRAC input by a network file:
~~~java
CracCreationParameters parameters = new CracCreationParameters();
// The following extension is mandatory when using this importer, see details below
parameters.addExtension(NetworkCracCreationParameters.class, new NetworkCracCreationParameters());
CracCreationContext ccc = Crac.readWithContext(networkFileName, networkInputStream, networkObject, cracCreationParameters);
Crac crac = ccc.getCrac();
~~~
Note that the NetworkCracCreationParameters extension cannot currently be serialized in json format. You must create it 
programmatically.  
Read the following sections that explain how you can configure the importer.

## Instants
The NetworkCracCreationParameters object allows you to configure the [instants](json.md#instants-and-states) to create.  
By default, one "preventive", one "outage" and one "curative" instants are created.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
networkCracCreationParameters.setInstants(
    Map.of(
        InstantKind.PREVENTIVE, List.of("prev"),
        InstantKind.OUTAGE, List.of("out"),
        InstantKind.CURATIVE, List.of("cur1", "cur2", "cur3")
    )
);
~~~
:::
::::

## Contingencies
The NetworkCracCreationParameters' contingencies section allows you to set rules for [contingencies](json.md#contingencies) creation.  
By default, all branches of the network are declared as contingencies. Only N-1 is supported for the moment.  
You can set the following extra filters:
- countries: an optional set of countries to choose the branches in. If left empty, all branches in the network are considered. If set to an empty set, no country will be considered.  
- minAndMaxV: the nominal voltage range in which the branches will be considered as contingency elements. If min and or max is left empty, it will be ignored.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
networkCracCreationParameters.getContingencies().setCountries(Set.of(Country.FR, Country.BE)); // only branches in France & Belgium are considered as contingency elements
networkCracCreationParameters.getContingencies().setMinAndMaxV(150, null); // only branches above 150kV are considered ad contingency elements
~~~
:::
::::

## Critical elements
The NetworkCracCreationParameters' critical elements section allows you to set rules for flow [CNECs](json.md#cnecs) creation.  
You can set the following parameters:
- countries: an optional set of countries to choose the critical branches in. If left empty, all branches in the network are considered. If set to an empty set, no country will be considered.
- optimizedMinMaxV: the voltage level range in which branches are considered as [optimised CNECs](json.md#optimised-and-monitored-cnecs)
- monitoredMinMaxV: the voltage level range in which branches are considered as [monitored MNECs](json.md#optimised-and-monitored-cnecs). You can leave it empty if you don't want to create MNECs.
- optimizedMonitoredProvider: a function that allows you to indicate if a branch is optimized and/or monitored after a given Contingency or in basecase (contingency = null). The filter applies above the voltage filter. We advise you to use it to make the computations lighter (by default, all branches are optimized and monitored).
- thresholdDefinition: tells the generator where to find the CNEC [thresholds](json.md#flow-limits-on-a-flowcnec)
  - FROM_OPERATIONAL_LIMITS: operational limits (permanent & temporary) will be read in the network. It is expected you define applicableLimitDurationPerInstant or applicableLimitDurationPerInstantPerNominalV (see below). Limit multipliers (see below) apply. 
  - PERM_LIMIT_MULTIPLIER: only permanent limits will be read in the network. They will be multiplied as defined in limit multipliers (see below).
- limitMultiplierPerInstantPerNominalV: for every instant and every nominal voltage, you can define a limit multiplier to apply to the operational limit read in the network.
- limitMultiplierPerInstant: this will be the default value if you don't want to define the multipliers on a nominal voltage basis.
- applicableLimitDurationPerInstantPerNominalV: for every instant and every nominal voltage, this duration is used as a reference to select the operational limit that must be respected by the RAO at that instant.
- applicableLimitDurationPerInstant: this will be the default value if you don't want to define the durations on a nominal voltage basis.


::::{tabs}
:::{group-tab} JAVA creation API
~~~java
CriticalElements params = networkCracCreationParameters.getCriricalElements();
params.setCountries(Set.of(Country.FR, Country.BE)); // only branches in France & Belgium are considered as critical network elements
params.setOptimizedMinMaxV(150, null); // branches above 150kV are optimised
params.setMonitoredMinMaxV(null); // no monitored branches
params.setOptimizedMonitoredProvider((branch, contingency) -> return new OptimizedMonitored(areInSameCountry(branch, contingency), !areInSameCountry(branch, contingency))); // Optimized CNECs are only made up of elements and contingencies belonging to same country ; other branches are MNECs (just an example, given method areInSameCountry exists)
params.thresholdDefinition = ThresholdDefinition.FROM_OPERATIONAL_LIMITS; // my network does not contain extensive operation limit information
params.limitMultiplierPerInstant = Map.of("prev", 0.95, "out", 2.0, "cur1", 1.5, "cur2", 1.3, "cur3", 1.0); // these specify permanent limit multipliers for different instants, no matter the nominal voltage
~~~
:::
::::

## PST range actions
The NetworkCracCreationParameters' "PST range actions" section allows you to set rules for [PST range actions](json.md#pst-range-action) creation.  
You can set the following parameters:
- countries: only PSTs in these countries will be considered as remedial actions. Leave it empty for no filtering. Set to empty set to disable PST creation.
- availableTapRangesAtInstants: for every instant, the tap range available for the PSTs. If not set, only physical limits in the network will be set as absolute limits. If an instant is not listed, PSTs will not be available for that instant.
- pstRaPredicate: a predicate that allows you to filter out some PSTs that are not available at some instants (outage instants are filtered out in all cases).

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
PstRangeActions params = networkCracCreationParameters.getPstRangeActions();
params.setCountries(Set.of(Country.FR)); // only PSTs in France are considered as remedial actions
params.setAvailableRelativeRangesAtInstants(Map.of(
    "prev", new PstRangeActions.TapRange(-8, 8, RangeType.RELATIVE_TO_INITIAL_NETWORK), // can change up to 8 taps in preventive
    // not listing cur1 will make PSTs unavailable at that instant
    "cur2", new PstRangeActions.TapRange(-1, 1, RangeType.RELATIVE_TO_PREVIOUS_INSTANT), // 1 tap in curative2
    "cur3", new PstRangeActions.TapRange(-2, 2, RangeType.RELATIVE_TO_PREVIOUS_INSTANT) // 2 taps in curative3
));
params.setPstRaPredicate((twoWindingsTransformer, instant) -> instant.isPreventive() || notInFrance(twoWindingsTransformer)); // French PSTs are not available in curative
~~~
:::
::::

## Redispatching range actions
The NetworkCracCreationParameters' "Redispatching range actions" section allows you to set rules for the automatic generation of remedial actions representing redispatching. 
These will take the form of [injection range actions](json.md#injection-range-action) in the generated CRAC.
By default, all generators and loads in the network are considered. You can set the following parameters:
- countries: only generators and loads in these countries will be considered as redispatchable. Leave it empty for no filtering. Set to empty set to disable redispatching creation.
- rdRaPredicate: a predicate that allows you to filter out some generators/loads that are not available at some instants (outage instants are filtered out in all cases). By default, only generators are allowed. Be careful to set a custom filter if you have a large network, otherwise you may un into memory issues.
- raCostsProvider: a function that provides the activation & variation cost for every generation/load at every instant. Costs default to zero.
- raRangeProvider: a function that provides the available absolute (active power) range for every generation/load at every instant. The range is absolute. Mandatory if you allow redispatching on loads. For generators, uses limits from the network (minP and maxP) by default.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
RedispatchingRangeActions params = networkCracCreationParameters.getRedispatchingRangeActions();
params.setCountries(Set.of(Country.BE)); // only generators and loads in Belgium are considered in redispatching
params.setRdRaPredicate((injection, instant) -> instant.isPreventive() || injection.getType() == IdentifiableType.GENERATOR) // load shedding only available in curative
params.setRaCostsProvider((injection, instant) -> (injection.getType() == IdentifiableType.GENERATOR) ? new InjectionRangeActionCosts(0, 10, 10) : new InjectionRangeActionCosts(1000, 0, 0)); // generators have only a proportional cost of 10/MW, and load shedding has only a one-shot activation cost of 1000
params.setRaRangeProvider((injection, instant) -> (injection.getType() == IdentifiableType.GENERATOR) ? new MinAndMax(null, null) : new MinAndMAx(((Load)injection).getP0, 0)); // no limits for generators, load shedding limited by initial load
~~~
:::
::::

## Counter-trading range actions
The NetworkCracCreationParameters' "Counter-trading range actions" section allows you to set rules for the automatic generation of remedial actions representing counter-trading.
These will take the form of [injection range actions](json.md#injection-range-action) in the generated CRAC. Every country is represented by one injection range action, inside 
which all generators of that country are listed.  
Generators previously included in redispatching range actions are not considered in counter-trading.  
You can set the following parameters:  
- countries: mandatory field. List here all the countries for which you want to create counter-trading range actions. Set to empty set if you want to deactivate counter-trading.
- injectionPredicate: function that allows you to filter out generators that are not available for counter-trading at a given instant. If not set, no filter will be applied. Be careful to set it if you have a large network, otherwise you may un into memory issues.
- raCostsProvider: function that provides activation and variation cost of a counter-trading range action in a given country at a given instant.
- raRangeProvider: function that provides the MW range of the counter-trading action for a given country at a given instant. The range is relative to the values in the initial network. Note that if you do not provide it, the range will be zero (rendering the remedial action useless).
- glsks: optional field to set the GLSK of every country. It is currently not supported, propotional GLSK is supposed.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
CountertradingRangeActions params = networkCracCreationParameters.getCountertradingRangeActions();
params.setCountries(Set.of(Country.FR)); // counter-trading with France
params.setInjectionPredicate((injection, instant) -> injection.getType() == IdentifiableType.GENERATOR && ((Generator) injection).maxP > 100) // only generators with maxP > 100 MW participate in counter-trading
params.setRaCostsProvider((country, instant) -> new InjectionRangeActionCosts(1000, 100, 100));
params.setRaRangeProvider((country, instant) -> new MinAndMAx(-1500, 2000)); // counter-trading from -1500MW to +2000MW
~~~
:::
::::

## Balancing range action
The NetworkCracCreationParameters' "Balancing range action" section allows you to set rules for the automatic generation of one slack remedial action representing balancing.  
In most cases, you shouldn't need this range action. But it can become handy when you do not have enough flexibility in the redispatching & counter-trading 
range actions to ensure balance (P = D) at all times.  
This action will take the form of a [injection range action](json.md#injection-range-action) in the generated CRAC. Every instant has an associated injection range action, inside 
which all generators of the network are listed.  
Generators previously included in redispatching & counter-trading range actions are not considered in balancing.  
You can set the following parameters:  
- raRangeProvider: function that provides the MW range of the balancing action at a given instant. The range is relative to the values in the initial network. Note that if you do not provide it, the range will be zero, rendering the remedial action useless (it will not be created).
- injectionPredicate: function that allows you to filter out generators that are not available for balancing at a given instant. If not set, no filter will be applied. Be careful to set it if you have a large network, otherwise you may un into memory issues.
- raCostsProvider: function that provides activation and variation cost of a balancing range action at a given instant. As this is supposed to be a slack remedial action, we advise you to set it costs higher than redispatching & counter-trading.

::::{tabs}
:::{group-tab} JAVA creation API
~~~java
BalancingRangeAction params = networkCracCreationParameters.getBalancingRangeAction();
params.setInjectionPredicate((injection, instant) -> injection.getType() == IdentifiableType.GENERATOR && ((Generator) injection).maxP <= 100) // only generators with maxP <= 100 MW participate in balancing
params.setRaCostsProvider(instant -> new InjectionRangeActionCosts(10000, 1000, 1000));
~~~
:::
::::
