# NC CRAC format

## Presentation

For the NC process, the CRAC data is split over multiple XML files called **NC profiles**, each one with its own
specific purpose, and which were inspired by the CGM format. This format
was [introduced by ENTSO-E](https://www.entsoe.eu/data/cim/cim-for-grid-models-exchange/). The objects in the different
NC profiles reference one another using **mRID** links (UUID format) which makes it possible to separate the
information among several distinct files.

## Header overview

The OpenRAO importer only supports NC version `2.3` (see
[ENTSO-E website](https://www.entsoe.eu/Documents/CIM_documents/Grid_Model_CIM/MetadataAndHeaderDataExchangeSpecification_v2.3.0.pdf)).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:eumd="http://entsoe.eu/ns/Metadata-European#"
        xmlns:eu="http://iec.ch/TC57/CIM100-European#"
        xmlns:nc="http://entsoe.eu/ns/nc#"
        xmlns:prov="http://www.w3.org/ns/prov#"
        xmlns:md="http://iec.ch/TC57/61970-552/ModelDescription/1#"
        xmlns:skos="http://www.w3.org/2004/02/skos/core#"
        xmlns:dcat="http://www.w3.org/ns/dcat#"
        xmlns:cim="http://iec.ch/TC57/CIM100#"
        xmlns:dcterms="http://purl.org/dc/terms/#">
    <md:FullModel rdf:about="urn:uuid:e6b94ef6-e043-4d29-a258-1718d6d2f507">
        <dcat:keyword>...</dcat:keyword>
        <dcat:startDate>2023-01-01T00:00:00Z</dcat:startDate>
        <dcat:endDate>2100-01-01T00:00:00Z</dcat:endDate>
        ...
    </md:FullModel>
    ...
</rdf:RDF>
```

Each NC profile is identified by a `dcat:keyword` that states which category of features it bears. To be valid for
OpenRAO, a profile must have exactly one keyword defined in its header. Besides, OpenRAO currently handles 5 different NC
profiles, the keyword and purpose of which are gathered in the following table:

| Keyword | Full Name                | Purpose                                |
|---------|--------------------------|----------------------------------------|
| AE      | Assessed Element         | Definition of CNECs.                   |
| CO      | Contingency              | Definition of contingencies.           |
| ER      | Equipment Reliability    | Definition of AngleCNECs' thresholds.  |
| RA      | Remedial Action          | Definition of remedial actions.        |
| SSI     | Steady State Instruction | Overriding data for specific instants. |

Besides, each NC profile has a period of validity delimited by the `dcat:startDate` and `dcat:endDate` fields (both
required) in the header. If the time at which the import occurs is outside of this time interval, the profile is
ignored.

> Several other fields can be added to the header but these will be ignored by OpenRAO.

## Profiles overview

The CRAC data is spread over different profiles that reference one another. The relation between the objects and the
fields read by OpenRAO are displayed in the following chart.

> Fields preceded by a "~" are optional.

![NC profiles usage overview](/_static/img/CSA-profiles.png)

## Instants

The NC CRAC is systematically created with 4 instants: preventive, outage and auto.

The curative instants must be declared alongside an application window (number of seconds after the outage) in the [NC CRAC creation parameters](creation-parameters.md#nc-specific-parameters).

## Contingencies

The [contingencies](json.md#contingencies) are described in the **CO** profile. They can be represented by three types of
objects: `OrdinaryContingency`, `ExceptionalContingency` and  `OutOfRangeContingency`. The contingency must be
associated to the impacted network elements through `ContingencyEquipment` objects.

::::{tabs}
:::{group-tab} OrdinaryContingency
```xml
<!-- CO Profile -->
<rdf:RDF>
    ...
    <nc:OrdinaryContingency rdf:ID="_ordinary-contingency">
        <cim:IdentifiedObject.mRID>ordinary-contingency</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Ordinary contingency</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of ordinary contingency.</cim:IdentifiedObject.description>
        <nc:Contingency.normalMustStudy>true</nc:Contingency.normalMustStudy>
        <nc:Contingency.EquipmentOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
    </nc:OrdinaryContingency>
    <cim:ContingencyEquipment rdf:ID="_contingency-equipment">
        <cim:IdentifiedObject.mRID>contingency-equipment</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Contingency equipment</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of contingency equipment.</cim:IdentifiedObject.description>
        <cim:ContingencyElement.Contingency rdf:resource="#_ordinary-contingency"/>
        <cim:ContingencyEquipment.contingentStatus
                rdf:resource="http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService"/>
        <cim:ContingencyEquipment.Equipment rdf:resource="#_equipment"/>
    </cim:ContingencyEquipment>
    ...
</rdf:RDF>
```
:::
:::{group-tab} ExceptionalContingency
```xml
<!-- CO Profile -->
<rdf:RDF>
    ...
    <nc:ExceptionalContingency rdf:ID="_exceptional-contingency">
        <cim:IdentifiedObject.mRID>exceptional-contingency</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Exceptional contingency</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of exceptional contingency.</cim:IdentifiedObject.description>
        <nc:Contingency.normalMustStudy>true</nc:Contingency.normalMustStudy>
        <nc:Contingency.EquipmentOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
    </nc:ExceptionalContingency>
    <cim:ContingencyEquipment rdf:ID="_contingency-equipment">
        <cim:IdentifiedObject.mRID>contingency-equipment</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Contingency equipment</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of contingency equipment.</cim:IdentifiedObject.description>
        <cim:ContingencyElement.Contingency rdf:resource="#_exceptional-contingency"/>
        <cim:ContingencyEquipment.contingentStatus
                rdf:resource="http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService"/>
        <cim:ContingencyEquipment.Equipment rdf:resource="#_equipment"/>
    </cim:ContingencyEquipment>
    ...
</rdf:RDF>
```
:::
:::{group-tab} OutOfRangeContingency
```xml
<!-- CO Profile -->
<rdf:RDF>
    ...
    <nc:OutOfRangeContingency rdf:ID="_out-of-range-contingency">
        <cim:IdentifiedObject.mRID>out-of-range-contingency</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Out-of-range contingency</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of out-of-range contingency.</cim:IdentifiedObject.description>
        <nc:Contingency.normalMustStudy>true</nc:Contingency.normalMustStudy>
        <nc:Contingency.EquipmentOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
    </nc:OutOfRangeContingency>
    <cim:ContingencyEquipment rdf:ID="_contingency-equipment">
        <cim:IdentifiedObject.mRID>contingency-equipment</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Contingency equipment</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of contingency equipment.</cim:IdentifiedObject.description>
        <cim:ContingencyElement.Contingency rdf:resource="#_out-of-range-contingency"/>
        <cim:ContingencyEquipment.contingentStatus
                rdf:resource="http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService"/>
        <cim:ContingencyEquipment.Equipment rdf:resource="#_equipment"/>
    </cim:ContingencyEquipment>
    ...
</rdf:RDF>
```
:::
::::

A contingency is imported only if the `normalMustStudy` field is set to `true` and if it is referenced by at least one
valid `ContingencyEquipment`, i.e. having `Equipment` pointing to an existing network element and a `contingentStatus`
being `outOfService`. A contingency with no associated `ContingencyEquipment` will be ignored.

> - The contingency can still be imported if `normalMustStudy` is set to `false` if the contingency is also defined in
    the SSI profile with its field `mustStudy` set to `true`.
> - The network elements must be defined in the CGMES.

From the `OrdinaryContingency` / `ExceptionalContingency` / `OutOfRangeContingency` object, the `mRID` is used as the
contingency's identifier. Besides, the `EquipmentOperator` is converted to a friendly name and concatenated with the
`name` to create the contingency's name (if the TSO is missing, only the name is used; if the name is missing, the
`mRID` will be used instead). Finally, the `ContingencyEquipment`'s `Equipment` is used as the contingency's network
element.

## CNECs

The [CNECs](json.md#cnecs) are described in the **AE** profile with an `AssessedElement` object which bears the identifier,
name, instant(s) and operator information.

```xml
<!-- AE Profile -->
<rdf:RDF>
    ...
    <nc:AssessedElement rdf:ID="_assessed-element">
        <cim:IdentifiedObject.mRID>assessed-element</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Assessed element</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of assessed element.</cim:IdentifiedObject.description>
        <nc:AssessedElement.inBaseCase>true</nc:AssessedElement.inBaseCase>
        <nc:AssessedElement.normalEnabled>true</nc:AssessedElement.normalEnabled>
        <nc:AssessedElement.isCombinableWithRemedialAction>false</nc:AssessedElement.isCombinableWithRemedialAction>
        <nc:AssessedElement.isCombinableWithContingency>false</nc:AssessedElement.isCombinableWithContingency>
        <nc:AssessedElement.AssessedSystemOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
    </nc:AssessedElement>
    ...
</rdf:RDF>
```

The CNEC is imported only if the `normalEnabled` fields is set to `true` or missing. If the `inBaseCase` field is set
to `true` a **preventive** CNEC is created from this assessed element (but this does not mean that a curative CNEC
cannot be created as well). The `AssessedSystemOperator` and the `name` are concatenated together with the CNEC's
instant (with the pattern *TSO_name - instant*) to create the CNEC's name.

> The CNEC can still be imported if `normalEnabled` is set to `false` if the AssessedElement is also defined in the SSI
> profile with its field `enabled` set to `true`.

Finally, in order to specify the type, value(s) of the threshold(s) and associated network elements of the CNEC, two
options are possible:

- using an `OperationalLimit` that points to an eponymous object in either the ER or EQ profile depending on the type of
  CNEC (see below)
- (FlowCNECs only) using a `ConductingEquipment` that points to a line to define FlowCNECs for the PATL and all the TATL
  of the line at once

> If none or both fields are present the AssessedElement will be ignored.

A CNEC can also be made curative by linking it to a contingency through an `AssessedElementWithContingency`. In this
case, the contingency's name is added to the CNEC's name.

```xml
<!-- AE Profile -->
<rdf:RDF>
    ...
    <nc:AssessedElementWithContingency rdf:ID="_assessed-element-with-contingency">
        <nc:AssessedElementWithContingency.mRID>assessed-element-with-contingency
        </nc:AssessedElementWithContingency.mRID>
        <nc:AssessedElementWithContingency.Contingency rdf:resource="#_contingency"/>
        <nc:AssessedElementWithContingency.AssessedElement rdf:resource="#_assessed-element"/>
        <nc:AssessedElementWithContingency.combinationConstraintKind
                rdf:resource="http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included"/>
        <nc:AssessedElementWithContingency.normalEnabled>true</nc:AssessedElementWithContingency.normalEnabled>
    </nc:AssessedElementWithContingency>
    ...
</rdf:RDF>
```

Finally, the created CNEC can be optimized (resp. monitored) if the `AssessedElement` has a `SecuredForRegion` (resp. `ScannedForRegion`) attribute pointing to the [EI Code of the CCR](creation-parameters.md#capacity-calculation-region-eic-code) declared in the NC parameters extension (default is `10Y1001C--00095L`, i.e. SWE CCR).

The distinction between the types of CNEC (FlowCNEC, AngleCNEC or VoltageCNEC) comes from the type of `OperationalLimit`
of the Assessed Element (or the use of a `ConductingEquipment` for FlowCNECs).

### FlowCNEC

::::{tabs}
:::{group-tab} OperationalLimit
The CNEC is a [FlowCNEC](json.md#flow-cnecs) if its associated `OperationalLimit` is a `CurrentLimit` which can be found in
the **EQ** profile (CGMES file).

```xml
<!-- AE Profile -->
<rdf:RDF>
    ...
    <nc:AssessedElement rdf:ID="_assessed-element">
        <cim:IdentifiedObject.mRID>assessed-element</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Assessed element</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of assessed element.</cim:IdentifiedObject.description>
        <nc:AssessedElement.inBaseCase>true</nc:AssessedElement.inBaseCase>
        <nc:AssessedElement.normalEnabled>true</nc:AssessedElement.normalEnabled>
        <nc:AssessedElement.isCombinableWithRemedialAction>false</nc:AssessedElement.isCombinableWithRemedialAction>
        <nc:AssessedElement.isCombinableWithContingency>false</nc:AssessedElement.isCombinableWithContingency>
        <nc:AssessedElement.AssessedSystemOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
        <nc:AssessedElement.OperationalLimit rdf:resource="#_current-limit"/>
    </nc:AssessedElement>
    ...
</rdf:RDF>
```

```xml
<!-- EQ (CGMES) Profile -->
<rdf:RDF>
    ...
    <cim:OperationalLimitSet rdf:ID="_operational-limit-set">
        <cim:IdentifiedObject.mRID>operational-limit-set</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit set</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit set</cim:IdentifiedObject.description>
        <cim:OperationalLimitSet.Terminal rdf:resource="#_terminal"/>
    </cim:OperationalLimitSet>
    <cim:OperationalLimitType rdf:ID="_operational-limit-type">
        <cim:IdentifiedObject.mRID>operational-limit-type</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit type</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit type</cim:IdentifiedObject.description>
        <cim:OperationalLimitType.direction
                rdf:resource="http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.absoluteValue"/>
        <eu:OperationalLimitType.kind rdf:resource="http://iec.ch/TC57/CIM100-European#LimitKind.tatl"/>
        <cim:OperationalLimitType.acceptableDuration>30</cim:OperationalLimitType.acceptableDuration>
    </cim:OperationalLimitType>
    <nc:CurrentLimit rdf:ID="_current-limit">
        <cim:IdentifiedObject.mRID>current-limit</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Current limit</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of current limit</cim:IdentifiedObject.description>
        <cim:OperationalLimit.OperationalLimitType rdf:resource="#_operational-limit-type"/>
        <cim:OperationalLimit.OperationalLimitSet rdf:resource="#_operational-limit-set"/>
        <nc:CurrentLimit.value>100.0</nc:CurrentLimit.value>
    </nc:CurrentLimit>
    ...
</rdf:RDF>
```

The CNEC's threshold value (in AMPERES) is determined by the `value` field of the `CurrentLimit` and must be positive.
Whether this is the maximum or minimum threshold of the CNEC depends on the `OperationalLimitType`'s `direction`:

- if the `direction` is `high`, the maximum value of the threshold is `+ normalValue`
- if the `direction` is `absoluteValue`, the maximum value of the threshold is + `normalValue` and the minimum value of
  the threshold is `- normalValue`

The CNEC's threshold side depends on the nature of the `OperationalLimitSet`'s `Terminal` which must reference an
existing line in the network and which also defines the CNEC's network element:

- if the line is a `CGMES.Terminal1` or a `CGMES.Terminal_Boundary_1` in PowSyBl, the threshold of the CNEC is on the
  side **one**
- if the line is a `CGMES.Terminal2` or a `CGMES.Terminal_Boundary_2` in PowSyBl, the threshold of the CNEC is on the
  side **two**

Depending on the `OperationalLimitType`'s `kind` (PATL or TATL) and its `acceptableDuration` (if TATL), the FlowCNEC's instant can be deduced (see [this section](#tatl-to-flowcnec-instant-association) for more information).

If the `AssessedElement` is `inBaseCase` and the limit is a PATL, a preventive FlowCNEC is added as well.

:::
:::{group-tab} ConductingEquipment

FlowCNECs can also be defined with a `ConductingEquipement` that points to a line in the CGMES.

```xml
<!-- AE Profile -->
<rdf:RDF>
    ...
    <nc:AssessedElement rdf:ID="_assessed-element">
        <cim:IdentifiedObject.mRID>assessed-element</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Assessed element</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of assessed element.</cim:IdentifiedObject.description>
        <nc:AssessedElement.inBaseCase>true</nc:AssessedElement.inBaseCase>
        <nc:AssessedElement.normalEnabled>true</nc:AssessedElement.normalEnabled>
        <nc:AssessedElement.isCombinableWithRemedialAction>false</nc:AssessedElement.isCombinableWithRemedialAction>
        <nc:AssessedElement.isCombinableWithContingency>false</nc:AssessedElement.isCombinableWithContingency>
        <nc:AssessedElement.AssessedSystemOperator rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
        <nc:AssessedElement.ConductingEquipement rdf:resource="#_conducting-equipment"/>
    </nc:AssessedElement>
    ...
</rdf:RDF>
```

In that case, several FlowCNECs can be defined at once depending on the number of TATLs defined for the line (given that
the `AssessedElement` is linked to a contingency).

The whole set of limits of the line is use to determine which limit is associated to which instant (see [this section](#tatl-to-flowcnec-instant-association) for more information).

For each contingency, a curative FlowCNEC is also created using the PATL. Finally, if the `AssessedElement`
is `inBaseCase` a preventive FlowCNEC is added using the PATL as well. In all case, the limit's threshold is used for
both the maximum (positive) and minimum (negative) FlowCNEC's thresholds.
:::
::::

#### TATL to FlowCNEC instant association

Because of the three curative instants used in the NC process, the definition of instants for FlowCNECs is based on an algorithm that requires to know the acceptable duration of all the TATLs of a line.

This association is more complex, as the set of TATLs used is not fixed from one line to the next, but the RAO must be able to map each of these limits at all 5 post-contingency instants (outage, auto and curative 1, 2 and 3).

Below are some examples of cases using different sets of TATLs:

::::{tabs}
:::{group-tab} General Case
![Association TATL-instant](/_static/img/tatl-instant.png){.forced-white-background}
:::
:::{group-tab} No TATL 300
![Association TATL-instant](/_static/img/tatl-instant-no-tatl-300.png){.forced-white-background}
:::
:::{group-tab} No TATL 600
![Association TATL-instant](/_static/img/tatl-instant-no-tatl-600.png){.forced-white-background}
:::
::::

The mapping between the different limits and the instants can be done with an algorithm described below. For this to work, the [NC extension](creation-parameters.md#csa-specific-parameters) of the CracCreationParameters must have been set in the first place to associate each curative instant to its [application time](creation-parameters.md#cra-application-window) and to indicate which TSOs [use the PATL as the final state's limit](creation-parameters.md#use-patl-in-final-state) and which do not.

By default, the first curative instant is associated to 300 seconds, the second curative to 600 seconds and the third curative to 1200 seconds.

> **Vocabulary**
>
> In the following, we define the highest (resp. lowest) limit as the limit with the highest (resp. lowest) threshold.
> We also define the longest (resp. shortest) limit as the limit with the longest (resp. shortest) duration. We assume the PATL to have an infinite duration.

For a given AssessedElement, no matter if it is defined using a `ConductingEquipment` or just an `OperationalLimit`, all of the line's limits must be fetched. Then, in order to know to which instant a given limit (PATL/TATL) belongs, the following algorithm must be run:

- _Preventive_: PATL
- _Outage_:
  - if TATLs with duration in [0, 300[ exist: lowest of these limits
  - otherwise, highest limit with a duration >= 0
- _Auto_: highest limit with a duration >= 300
- _First curative (aka "curative 300")_: highest limit with a duration >= 600
- _Second curative (aka "curative 600")_: highest limit with a duration >= 1200
- _Third curative (aka "curative 1200")_:
  - If the TSO uses the PATL for the final state: PATL or longest TATL if the TSO does not use the PATL for the final state
  - If the TSO does **not** use the PATL for the final state: longest TATL if the TSO does not use the PATL for the final state

> **Remarks**
> 1. For each instant, if no TATL meets the condition "highest limit with a duration >= X", the PATL is used instead (or the longest TATL if the TSO does not use the PATL for the final state)
> 2. If the TSO does not use the PATL for the final state but the line has no TATL, the PATL is used by default for all the instants

Then given a limit (PATL or TATL), the FlowCNEC's instant is retrieved as followed:
- if the limit is associated to one or several instants by the previous algorithm, one FlowCNEC is created per such instant
- if not, the instants associated to the closest limit with a longer duration (and which was mapped to instants by the algorithm) are used instead

> **Remark about interconnections**
> 
> Note that to avoid potential interconnection lines that may have different limits on both sides, one FlowCNEC is created for each side

### AngleCNEC

The CNEC is an [AngleCNEC](json.md#angle-cnecs) if its associated `OperationalLimit` is a `VoltageAngleLimit` which can be
found in the **ER** profile.

```xml
<!-- ER Profile -->
<rdf:RDF>
    ...
    <cim:OperationalLimitSet rdf:ID="_operational-limit-set">
        <cim:IdentifiedObject.mRID>operational-limit-set</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit set</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit set</cim:IdentifiedObject.description>
        <cim:OperationalLimitSet.Terminal rdf:resource="#_terminal-2"/>
    </cim:OperationalLimitSet>
    <cim:OperationalLimitType rdf:ID="_operational-limit-type">
        <cim:IdentifiedObject.mRID>operational-limit-type</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit type</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit type</cim:IdentifiedObject.description>
        <cim:OperationalLimitType.direction
                rdf:resource="http://iec.ch/TC57/CIM100#OperationalLimitDirectionKind.absoluteValue"/>
    </cim:OperationalLimitType>
    <nc:VoltageAngleLimit rdf:ID="_voltage-angle-limit">
        <cim:IdentifiedObject.mRID>voltage-angle-limit</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Voltage angle limit</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of voltage angle limit</cim:IdentifiedObject.description>
        <cim:OperationalLimit.OperationalLimitType rdf:resource="#_operational-limit-type"/>
        <cim:OperationalLimit.OperationalLimitSet rdf:resource="#_operational-limit-set"/>
        <nc:VoltageAngleLimit.normalValue>100.0</nc:VoltageAngleLimit.normalValue>
        <nc:VoltageAngleLimit.isFlowToRefTerminal>true</nc:VoltageAngleLimit.isFlowToRefTerminal>
        <nc:VoltageAngleLimit.AngleReferenceTerminal rdf:resource="#_terminal-1"/>
    </nc:VoltageAngleLimit>
    ...
</rdf:RDF>
```

The CNEC's threshold value (in DEGREES) is determined by the `normalValue` field of the `VoltageAngleLimit` and must be
positive. Whether this is the maximum or minimum threshold of the CNEC depends on the `OperationalLimitType`'
s `direction`:

- if the `direction` is `high`, the maximum value of the threshold is `+ normalValue`
- if the `direction` is `low`, the minimum value of the threshold is `- normalValue`
- if the `direction` is `absoluteValue`, the maximum value of the threshold is + `normalValue` and the minimum value of
  the threshold is `- normalValue`

An AngleCNEC also has two terminals, one being the importing element and the other being the exporting element, which
imposes the flow direction. Two terminals are referenced by the AngleCNEC in the NC profiles. The first one (called
*terminal_1*) is referenced by the `VoltageAngleLimit`'s `AngleReferenceTerminal` field. The second one (called
*terminal_2*) is referenced by the `OperationalLimitSet`'s `Terminal` field. The flow direction is determined depending
on the `VoltageAngleLimit`'s `isFlowToRefTerminal` field value:

- if it is missing of `false`, the importing element is *terminal_1* and the exporting element is *terminal_2*
- if it is present of `true`, the exporting element is *terminal_1* and the importing element is *terminal_2*

> ⚠️ Note that if the `OperationalLimitType`'s `direction` is **not** `absoluteValue`, the `isFlowToRefTerminal` must be
> present otherwise the AngleCNEC will be ignored.

### VoltageCNEC

The CNEC is a [VoltageCNEC](json.md#voltage-cnecs) if its associated `OperationalLimit` is a `VoltageLimit` which can be
found in the **EQ** profile (CGMES file).

```xml
<!-- EQ (CGMES) Profile -->
<rdf:RDF>
    ...
    <cim:OperationalLimitSet rdf:ID="_operational-limit-set">
        <cim:IdentifiedObject.mRID>operational-limit-set</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit set</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit set</cim:IdentifiedObject.description>
        <cim:OperationalLimitSet.Equipment rdf:resource="#_equipment"/>
    </cim:OperationalLimitSet>
    <cim:OperationalLimitType rdf:ID="_operational-limit-type">
        <cim:IdentifiedObject.mRID>operational-limit-type</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Operational limit type</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of operational limit type</cim:IdentifiedObject.description>
        <cim:OperationalLimitType.isInfiniteDuration>true</cim:OperationalLimitType.isInfiniteDuration>
        <entsoe:OperationalLimitType.limitType rdf:resource="http://entsoe.eu/CIM/SchemaExtension/3/1#LimitTypeKind.highVoltage" />
    </cim:OperationalLimitType>
    <nc:VoltageLimit rdf:ID="voltage-limit">
        <cim:IdentifiedObject.mRID>voltage-limit</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Voltage limit</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of voltage limit</cim:IdentifiedObject.description>
        <cim:OperationalLimit.OperationalLimitType rdf:resource="#_operational-limit-type"/>
        <cim:OperationalLimit.OperationalLimitSet rdf:resource="#_operational-limit-set"/>
        <nc:VoltageLimit.value>100.0</nc:VoltageLimit.value>
    </nc:VoltageLimit>
    ...
</rdf:RDF>
```

The VoltageCNEC's `isInfiniteDuration` field is optional and its default value is `true`. If it is set to `false` the voltageCNEC will
be ignored.

The CNEC's threshold value (in KILOVOLTS) is determined by the `value` field of the `VoltageLimit` and must be positive.
Whether this is the maximum or minimum threshold of the CNEC depends on the `OperationalLimitType`'s `limitType`:

- if the `limitType` is `highVoltage`, the maximum value of the threshold is `+ normalValue`
- if the `limitType` is `lowVoltage`, the minimum value of the threshold is `- normalValue`

The VoltageCNEC's network element is determined by the `OperationalLimitSet`'s `Equipment`. The latter must reference an
existing BusBarSection in the network.

## Remedial Actions

The [remedial actions](json.md#remedial-actions-and-usages-rules) are described in the **RA** profile. The most general way to describe a
remedial action is with a `GridStateAlterationRemedialAction` object that bears the identifier, name, operator, speed
and instant of the remedial action.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:GridStateAlterationRemedialAction rdf:ID="_remedial-action">
        <cim:IdentifiedObject.mRID>remedial-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>RA</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of RA</cim:IdentifiedObject.description>
        <nc:RemedialAction.normalAvailable>true</nc:RemedialAction.normalAvailable>
        <nc:RemedialAction.isManual>true</nc:RemedialAction.isManual>
        <nc:RemedialAction.kind rdf:resource="http://entsoe.eu/ns/nc#RemedialActionKind.preventive"/>
        <nc:RemedialAction.timeToImplement>PT50S</nc:RemedialAction.timeToImplement>
        <nc:RemedialAction.RemedialActionSystemOperator
                rdf:resource="http://data.europa.eu/energy/EIC/10XFR-RTE------Q"/>
    </nc:GridStateAlterationRemedialAction>
    ...
</rdf:RDF>
```

The remedial action is imported only if the `normalAvailable` field is set to `true`.

> The remedial action can still be imported if `normalAvailable` is set to `false` if the remedial action is also
> defined in the SSI profile with its field `avilable` set to `true`.

As for the [contingencies](#contingencies), the `mRID` is used as the remedial action's identifier and
the `RemedialActionSystemOperator` and `name` are concatenated together to create the remedial action's name. The
instant of the remedial action is determined by the `kind` which can be either `preventive` or `curative`.

> If the remedial action has its `kind` field set to `curative`, the remedial action will be imported for all curative instants at once.

If the remedial action is of kind `curative` and its `isManual` attribute is present and set to `false`, the importer will interpret this remedial action as an automaton. Thus, its [usage rules](#usage-rules) will all be defined for the auto instant.

> Preventive automatons are not supported by OpenRAO so preventive remedial actions with `isManual` set to false will be ignored.

Finally, the `timeToImplement` is converted to a number of seconds and used as the remedial action's speed.

> This `timeToImplement` may also be used to convert a curative remedial action to an auto remedial action (see [this section](#using-gridstatealterationremedialaction-and-timetoimplement)).

In the following, we describe the different types of remedial actions that can be imported in OpenRAO from the NC
profiles. The general pattern is to link a `GridStateAlteration` object which references the parent remedial
action (`GridStateAlterationRemedialAction`) and a `StaticPropertyRange` which contains the physical and numerical
definition of the remedial action. The field of the `StaticPropertyRange` are:

- `normalValue` which contains the numerical data of the remedial action (such as the set-point, the PST tap, ...)
- `PropertyReference` which indicated the network element's property that will be affected by the remedial action
- `valueKind` which indicates whether the `normalValue` is defined independently of the previous state (`absolute`) of
  the network element or relatively (`incremental`)
- `direction` which bears the logic of whether the `normalValue` is an extreme value, an increase / decrease or just a
  standalone value

### PST Range Action

A [PST range action](json.md#pst-range-action) is described by a `TapPositionAction` object which references its parent
remedial action (`GridStateAlterationRemedialAction`) and the PST affected by the action.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:TapPositionAction rdf:ID="_tap-position-action">
        <cim:IdentifiedObject.mRID>tap-position-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Tap position action</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of tap position action</cim:IdentifiedObject.description>
        <nc:GridStateAlteration.normalEnabled>true</nc:GridStateAlteration.normalEnabled>
        <nc:GridStateAlteration.GridStateAlterationRemedialAction rdf:resource="#_remedial-action"/>
        <nc:GridStateAlteration.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/TapChanger.step"/>
        <nc:TapPositionAction.TapChanger rdf:resource="#_tap-changer"/>
    </nc:TapPositionAction>
    ...
</rdf:RDF>
```

The PST range action is considered only if the `normalEnabled` field is set to `true`. Besides, the `TapChanger` must
reference an existing PST in the network and the `PropertyReference` must necessarily be `TapChanger.step` since it is
the PST's tap position which shifts.

> The PST range action can still be imported if `normalEnabled` is set to `false` if the TapPositionAction is also
> defined in the SSI profile with its field `enabled` set to `true`.

To be valid, the `TapPositionAction` must itself be referenced by at most two `StaticPropertyRange` objects which
provide the numerical values for the minimum and/or maximum reachable taps. If no `StaticPropertyRange` is present, the
range of the remedial action will be set from the PST range read in the network.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:StaticPropertyRange rdf:ID="_static-property-range-for-tap-position-action-max">
        <cim:IdentifiedObject.mRID>static-property-range-for-tap-position-action-max</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Upper bound for tap position action</cim:IdentifiedObject.name>
        <nc:RangeConstraint.GridStateAlteration rdf:resource="#_tap-position-action"/>
        <nc:RangeConstraint.normalValue>7.0</nc:RangeConstraint.normalValue>
        <nc:RangeConstraint.direction rdf:resource="http://entsoe.eu/ns/nc#RelativeDirectionKind.up"/>
        <nc:RangeConstraint.valueKind rdf:resource="http://entsoe.eu/ns/nc#ValueOffsetKind.absolute"/>
        <nc:StaticPropertyRange.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/TapChanger.step"/>
    </nc:StaticPropertyRange>
    <nc:StaticPropertyRange rdf:ID="_static-property-range-for-tap-position-action-min">
        <cim:IdentifiedObject.mRID>static-property-range-for-tap-position-action-min</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Lower bound for tap position action</cim:IdentifiedObject.name>
        <nc:RangeConstraint.GridStateAlteration rdf:resource="#_tap-position-action"/>
        <nc:RangeConstraint.normalValue>-7.0</nc:RangeConstraint.normalValue>
        <nc:RangeConstraint.direction rdf:resource="http://entsoe.eu/ns/nc#RelativeDirectionKind.down"/>
        <nc:RangeConstraint.valueKind rdf:resource="http://entsoe.eu/ns/nc#ValueOffsetKind.absolute"/>
        <nc:StaticPropertyRange.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/TapChanger.step"/>
    </nc:StaticPropertyRange>
    ...
</rdf:RDF>
```

For the `StaticPropertyRange`, the `PropertyReference` must also be `TapChanger.step`. The value of the tap is
determined by the `normalValue`: if the `direction` is `up` this is the maximum reachable tap and if it is `down` it is
the minimum. Note that the `valueKind` must be `absolute` to indicate that the limit does not depend on the previous
PST's state. Up to two `StaticPropertyRange` objects can be linked to the same PST range action to set the minimum
and/or maximum tap.

> The `normalValue` can be overridden in the SSI profile if a `RangeConstraint` with the same mRID as
> the `StaticPropertyRange` is defined. In that case, the field `value` of the `RangeConstraint` will be considered
> instead.

### Network Actions

#### Switch Action

A [switch action](json.md#network-actions) is described by a `TopologyAction` object which references its parent
remedial action (`GridStateAlterationRemedialAction`) and the switch affected by the action.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:TopologyAction rdf:ID="_topology-action">
        <cim:IdentifiedObject.mRID>topology-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Topology action</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of topology action</cim:IdentifiedObject.description>
        <nc:GridStateAlteration.normalEnabled>true</nc:GridStateAlteration.normalEnabled>
        <nc:GridStateAlteration.GridStateAlterationRemedialAction rdf:resource="#_remedial-action"/>
        <nc:GridStateAlteration.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/Switch.open"/>
        <nc:TopologyAction.Switch rdf:resource="#_switch"/>
    </nc:TopologyAction>
    ...
</rdf:RDF>
```

The topological action is considered only if the `normalEnabled` field is set to `true`. Besides, the `Switch` must
reference an existing switch in the network and the `PropertyReference` must necessarily be `Switch.open` since a
topological action is about opening or closing such a switch.

> The topological action can still be imported if `normalEnabled` is set to `false` if the TopologyAction is also
> defined in the SSI profile with its field `enabled` set to `true`.

To be valid, the `TopologyAction` must itself be referenced by one `StaticPropertyRange` object which indicates whether
to open or to close the switch.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:StaticPropertyRange rdf:ID="_static-property-range-for-topology-action">
        <cim:IdentifiedObject.mRID>static-property-range-for-topology-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Example of StaticPropertyRange to open a switch</cim:IdentifiedObject.name>
        <nc:RangeConstraint.GridStateAlteration rdf:resource="#_tap-position-action"/>
        <nc:RangeConstraint.normalValue>1</nc:RangeConstraint.normalValue>
        <nc:RangeConstraint.direction rdf:resource="http://entsoe.eu/ns/nc#RelativeDirectionKind.none"/>
        <nc:RangeConstraint.valueKind rdf:resource="http://entsoe.eu/ns/nc#ValueOffsetKind.absolute"/>
        <nc:StaticPropertyRange.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/Switch.open"/>
    </nc:StaticPropertyRange>
    ...
</rdf:RDF>
```

For the `StaticPropertyRange`, the `PropertyReference` must also be `Switch.open`. Note that the `valueKind` must
be `absolute` and the `direction` must be `none` to indicate that the limit does not depend on the previous switch's
state. Finally, the `normalValue` field sets the behaviour of the switch:

- if it is 0 the switch will be closed
- if it is 1 the switch will be opened

> The `normalValue` can be overridden in the SSI profile if a `RangeConstraint` with the same mRID as
> the `StaticPropertyRange` is defined. In that case, the field `value` of the `RangeConstraint` will be considered
> instead.

#### Generator Action and Load Action

A [generator action](json.md#network-actions) or a [load action](json.md#network-actions) is described by a
`RotatingMachineAction` object which references its parent remedial action (`GridStateAlterationRemedialAction`) and
the network element affected by the action (a generator or a load).

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:RotatingMachineAction rdf:ID="_rotating-machine-action">
        <cim:IdentifiedObject.mRID>rotating-machine-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Rotating machine action</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of rotating machine action</cim:IdentifiedObject.description>
        <nc:GridStateAlteration.normalEnabled>true</nc:GridStateAlteration.normalEnabled>
        <nc:GridStateAlteration.GridStateAlterationRemedialAction rdf:resource="#_remedial-action"/>
        <nc:GridStateAlteration.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/RotatingMachine.p"/>
        <nc:RotatingMachineAction.RotatingMachine rdf:resource="#_rotating-machine"/>
    </nc:RotatingMachineAction>
    ...
</rdf:RDF>
```

The rotating machine action is considered only if the `normalEnabled` field is set to `true`. Besides,
the `RotatingMachine` must reference an existing generator or load in the network and the `PropertyReference` must
necessarily be `RotatingMachine.p` since the remedial action acts on the generator or load's power.

> The rotating machine action can still be imported if `normalEnabled` is set to `false` if the RotatingMachineAction is
> also defined in the SSI profile with its field `enabled` set to `true`.

To be valid, the `RotatingMachineAction` must itself be referenced by a `StaticPropertyRange` which provides the targeted
active power value.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:StaticPropertyRange rdf:ID="_static-property-range-for-rotating-machine-action">
        <cim:IdentifiedObject.mRID>static-property-range-for-rotating-machine-action</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Set-point in MW</cim:IdentifiedObject.name>
        <nc:RangeConstraint.GridStateAlteration rdf:resource="#_rotating-machine-action"/>
        <nc:RangeConstraint.normalValue>100.0</nc:RangeConstraint.normalValue>
        <nc:RangeConstraint.direction rdf:resource="http://entsoe.eu/ns/nc#RelativeDirectionKind.none"/>
        <nc:RangeConstraint.valueKind rdf:resource="http://entsoe.eu/ns/nc#ValueOffsetKind.absolute"/>
        <nc:StaticPropertyRange.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/RotatingMachine.p"/>
    </nc:StaticPropertyRange>
    ...
</rdf:RDF>
```

For the `StaticPropertyRange`, the `PropertyReference` must also be `RotatingMachine.p`. The value of the active power
(in MW) is determined by the `normalValue` given that the `valueKind` is `absolute` and that the `direction` is none to
indicate that the active power is an imposed value without any degree of freedom for the RAO.

> The `normalValue` can be overridden in the SSI profile if a `RangeConstraint` with the same mRID as
> the `StaticPropertyRange` is defined. In that case, the field `value` of the `RangeConstraint` will be considered
> instead.

#### Shunt Compensator Position Action

A [shunt compensator position action](json.md#network-actions) is described by a `ShuntCompensatorModification` object
which references its parent remedial action (`GridStateAlterationRemedialAction`) and the network element affected by
the action (a shunt compensator).

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:ShuntCompensatorModification rdf:ID="_shunt-compensator-modification">
        <cim:IdentifiedObject.mRID>shunt-compensator-modification</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Shunt compensator modification</cim:IdentifiedObject.name>
        <cim:IdentifiedObject.description>Example of shunt compensator modification</cim:IdentifiedObject.description>
        <nc:GridStateAlteration.normalEnabled>true</nc:GridStateAlteration.normalEnabled>
        <nc:GridStateAlteration.GridStateAlterationRemedialAction rdf:resource="#_remedial-action"/>
        <nc:GridStateAlteration.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections"/>
        <nc:ShuntCompensatorModification.ShuntCompensator rdf:resource="#_shunt-compensator"/>
    </nc:ShuntCompensatorModification>
    ...
</rdf:RDF>
```

The shunt compensator modification is considered only if the `normalEnabled` field is set to `true`. Besides,
the `ShuntCompensator` must reference a shunt compensator in the network and the `PropertyReference` must necessarily
be `ShuntCompensator.sections` since the remedial action acts on the number of sections of the shunt compensator.

> The shunt compensator modification can still be imported if `normalEnabled` is set to `false` if the
> ShuntCompensatorModification is also defined in the SSI profile with its field `enabled` set to `true`.

To be valid, the `ShuntCompensatorModification` must itself be referenced by a `StaticPropertyRange` which provides the
value of the targeted section count.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:StaticPropertyRange rdf:ID="_static-property-range-for-shunt-compensator-modification">
        <cim:IdentifiedObject.mRID>static-property-range-for-shunt-compensator-modification</cim:IdentifiedObject.mRID>
        <cim:IdentifiedObject.name>Set-point in SECTION_COUNT</cim:IdentifiedObject.name>
        <nc:RangeConstraint.GridStateAlteration rdf:resource="#_shunt-compensator-modification"/>
        <nc:RangeConstraint.normalValue>5.0</nc:RangeConstraint.normalValue>
        <nc:RangeConstraint.direction rdf:resource="http://entsoe.eu/ns/nc#RelativeDirectionKind.none"/>
        <nc:RangeConstraint.valueKind rdf:resource="http://entsoe.eu/ns/nc#ValueOffsetKind.absolute"/>
        <nc:StaticPropertyRange.PropertyReference
                rdf:resource="http://energy.referencedata.eu/PropertyReference/ShuntCompensator.sections"/>
    </nc:StaticPropertyRange>
    ...
</rdf:RDF>
```

For the `StaticPropertyRange`, the `PropertyReference` must also be `ShuntCompensator.sections`. The value of the
section count is determined by the `normalValue` given that the `valueKind` is `absolute` and that
the `direction`is none to indicate that the number of section is an imposed value without any degree of freedom for the
RAO. Note that `normalValue` must be integer-*castable* (i.e. a float number with null decimal part) to model a number
of sections.

> The `normalValue` can be overridden in the SSI profile if a `RangeConstraint` with the same mRID as
> the `StaticPropertyRange` is defined. In that case, the field `value` of the `RangeConstraint` will be considered
> instead.

:::
::::

### Usage Rules

#### OnInstant

By default, if no additional information is given, the remedial action is imported with an **onInstant usage rule**
and an **AVAILABLE usage method**.

#### OnContingencyState

If the remedial action is linked to a contingency, its usage method is no longer onInstant and is now
**onContingencyState**. This link is created with a `ContingencyWithRemedialAction` object that bounds together the
remedial action and the contingency. The usage method will be `AVAILABLE` is the remedial action is a CRA and `FORCED`
if it is an automaton.

```xml
<!-- RA Profile -->
<rdf:RDF>
    ...
    <nc:ContingencyWithRemedialAction rdf:ID="_contingency-with-remedial-action">
        <nc:ContingencyWithRemedialAction.mRID>contingency-with-remedial-action</nc:ContingencyWithRemedialAction.mRID>
        <nc:ContingencyWithRemedialAction.combinationConstraintKind
                rdf:resource="http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included"/>
        <nc:ContingencyWithRemedialAction.RemedialAction rdf:resource="#_remedial-action"/>
        <nc:ContingencyWithRemedialAction.Contingency rdf:resource="#_contingency"/>
        <nc:ContingencyWithRemedialAction.normalEnabled>true</nc:ContingencyWithRemedialAction.normalEnabled>
    </nc:ContingencyWithRemedialAction>
    ...
</rdf:RDF>
```

> **⛔ Illegal situations**
> - A preventive remedial action cannot be linked to a contingency.
> - If several links between the same remedial action and the same contingency exist, they will all be ignored to avoid any ambiguity.

#### OnConstraint

If the remedial action is linked to an assessed element (a CNEC), its usage method is no longer onInstant and is now
**onConstraint**. This link is created with a `AssessedElementWithRemedialAction` object that bounds together the
assessed element and the contingency. The usage method will be `AVAILABLE` is the remedial action is a CRA and `FORCED`
if it is an automaton.

```xml
<!-- AE Profile -->
<rdf:RDF>
    ...
    <nc:AssessedElementWithRemedialAction rdf:ID="_assessed-element-with-remedial-action">
        <nc:AssessedElementWithRemedialAction.mRID>assessed-element-with-remedial-action
        </nc:AssessedElementWithRemedialAction.mRID>
        <nc:AssessedElementWithRemedialAction.combinationConstraintKind
                rdf:resource="http://entsoe.eu/ns/nc#ElementCombinationConstraintKind.included"/>
        <nc:AssessedElementWithRemedialAction.AssessedElement rdf:resource="#_assessed-element"/>
        <nc:AssessedElementWithRemedialAction.RemedialAction rdf:resource="#_remedial-action"/>
        <nc:AssessedElementWithRemedialAction.normalEnabled>true</nc:AssessedElementWithRemedialAction.normalEnabled>
    </nc:AssessedElementWithRemedialAction>
    ...
</rdf:RDF>
```

> - If several FlowCNECs were created from the `AssessedElement`, an onConstraint usage rule is created for each as long as the instant of the FlowCNEC occurs after the instant of the remedial action (except for ARAs for which only auto CNECs are supported)
> - If the `AssessedElement` is also linked to a `Contingency`, only the FlowCNECs monitored after said contingency will be considered for the onConstraint usage rules (see previous bullet point)