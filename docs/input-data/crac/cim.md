# CIM CRAC format

## Header overview

```xml
<CRAC_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-n:CRACdocument:2:3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:iec62325.351:tc57wg16:451-n:CRACdocument:2:3 iec62325-451-n-crac_v2_3.xsd">
  <mRID>CIM_CRAC_DOCUMENT</mRID>
  <revisionNumber>1</revisionNumber>
  <type>B15</type>
  <process.processType>A48</process.processType>
  <sender_MarketParticipant.mRID codingScheme="A01">FAKE</sender_MarketParticipant.mRID>
  <sender_MarketParticipant.marketRole.type>A36</sender_MarketParticipant.marketRole.type>
  <receiver_MarketParticipant.mRID codingScheme="A01">FAKE</receiver_MarketParticipant.mRID>
  <receiver_MarketParticipant.marketRole.type>A04</receiver_MarketParticipant.marketRole.type>
  <createdDateTime>2021-03-31T15:02:00Z</createdDateTime>
  <status>
    <value>A42</value>
  </status>
  <time_Period.timeInterval>
    <start>2021-04-01T22:00Z</start>
    <end>2021-04-02T22:00Z</end>
  </time_Period.timeInterval>
  <domain.mRID codingScheme="A01">10YCB-FR-ES-PT-S</domain.mRID>
  <TimeSeries>
    <mRID>TimeSeries_1</mRID>
    ...
  </TimeSeries>
  <TimeSeries>
    <mRID>TimeSeries_2</mRID>
    ...
  </TimeSeries>

  ...
</CRAC_MarketDocument>
```
A crac market document has a time interval for its validity. Therefore, **this document has to be imported for a specific datetime** – hourly-precise.  
Moreover, only timeseries whose mRIDs are configured in the CimCracCreationParameters [timeseries-mrids](creation-parameters.md#timeseries-mrids) 
parameter are imported. This allows the import of border-specific contraints and remedial actions.
Each timeseries is configured using a curve: either **SEQUENTIAL_FIXED_SIZE_BLOCKS_CURVE_TYPE** (value "A01"), 
or **VARIABLE_SIZED_BLOCK_CURVE**  (value "A03").
- In the case of SEQUENTIAL_FIXED_SIZE_BLOCKS_CURVE_TYPE, all period's points represent one resolution timespan (**resolution** is a parameter i.e. 60 minutes), their **position** parameter gives their relative start and end in their periods timespan (starting from "1")  depending on resolution.
- In the case of VARIABLE_SIZED_BLOCK_CURVE, period's points can represent multiple resolution timespans, their **position** parameter gives their relative start, and their end is either equal to the next defined point's beginning, or the end of the period if no other point is defined after.

### Period examples

#### SEQUENTIAL_FIXED_SIZE_BLOCKS_CURVE_TYPE

All period points are present from position 1 to position 24, and represent an hour (because resolution is "PT60M" = 60 minutes):
- point position 1 is applicable for timestamps between 2021-04-01T22:00Z (included) and 2021-04-01T23:00Z (excluded)
- point position 2 is applicable for timestamps between 2021-04-01T23:00Z (included) and 2021-04-01T24:00Z (excluded)
- point position 3 is applicable for timestamps between 2021-04-01T24:00Z (included) and 2021-04-02T01:00Z (excluded), etc
```xml
  <TimeSeries>
    <mRID>TimeSeries2</mRID>
    <businessType>B54</businessType>
    <curveType>A01</curveType>
    <in_Domain.mRID codingScheme="A01">FAKE</in_Domain.mRID>
    <out_Domain.mRID codingScheme="A01">FAKE</out_Domain.mRID>
    <Period>
      <timeInterval>
        <start>2021-04-01T22:00Z</start>
        <end>2021-04-02T22:00Z</end>
      </timeInterval>
      <resolution>PT60M</resolution>
      <Point>
        <position>1</position>
        <Series>
          ...
        </Series>
      </Point>
      <Point>
        <position>2</position>
        <Series>
          ...
        </Series>
      </Point>
      (...)
      <Point>
        <position>24</position>
        <Series>
          ...
        </Series>
      </Point>
    </Period>
  </TimeSeries>
```

#### VARIABLE_SIZED_BLOCK_CURVE
- point position 5 is applicable for timestamps between 2021-04-02T02:00Z (included) and 2021-04-02T11:00Z (excluded)
- point position 14 is applicable for timestamps between 2021-04-02T11:00Z (included) and 2021-04-02T15:00Z (excluded)
- point position 18 is applicable for timestamps between 2021-04-02T15:00Z (included) and 2021-04-02T22:00Z (excluded)
```xml
  <TimeSeries>
    <mRID>TimeSeries2</mRID>
    <businessType>B54</businessType>
    <curveType>A03</curveType>
    <in_Domain.mRID codingScheme="A01">FAKE</in_Domain.mRID>
    <out_Domain.mRID codingScheme="A01">FAKE</out_Domain.mRID>
    <Period>
      <timeInterval>
        <start>2021-04-01T22:00Z</start>
        <end>2021-04-02T22:00Z</end>
      </timeInterval>
      <resolution>PT60M</resolution>
      <Point>
        <position>5</position>
        <Series>
          ...
        </Series>
      </Point>
      <Point>
        <position>14</position>
        <Series>
          ...
        </Series>
      </Point>
      <Point>
        <position>18</position>
        <Series>
          ...
        </Series>
      </Point>
    </Period>
  </TimeSeries>
```

## Contingencies

```xml
 <Series>
    <mRID>ContingenciesSeries1</mRID>
    <businessType>B55</businessType>
    <name>ContingenciesSeries</name>
    <optimization_MarketObjectStatus.status>A52</optimization_MarketObjectStatus.status>
    <Contingency_Series>
      <mRID>Co-1</mRID>
      <name>Co-1-name</name>
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>Co1NetworkElementName</name>
        <in_Domain.mRID codingScheme="A01">1------0</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">1------0</out_Domain.mRID>
      </RegisteredResource>
    </Contingency_Series>

  ...
</Series>
```
Contingencies are listed in Series of business type B55, B56 or B57. They are defined in Contingency_Series elements. The associated network elements are defined in the registered resources.

## CNECs

### FlowCnecs

```xml
<Series>
    <mRID>CNECs_after_all_contingencies</mRID>
    <businessType>B57</businessType>
    <name>CNECs_after_all_contingencies</name>
    <optimization_MarketObjectStatus.status>A52</optimization_MarketObjectStatus.status>
    <Contingency_Series> ⚪OPTIONAL
    ...
    </Contingency_Series>
    <Monitored_Series>
     <mRID>CNEC-2</mRID>
     <name>CNEC-2-name</name>
      <RegisteredResource>
       <mRID codingScheme="A02">FAKE_ID</mRID>
         <name>CNEC-2</name>
        <in_Domain.mRID codingScheme="A01">1---W</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">1--W</out_Domain.mRID>
        <Measurements>
          <measurementType>A02</measurementType>
           <unitSymbol>P1</unitSymbol>
           <positiveFlowIn>A02</positiveFlowIn>
          <analogValues.value>2.0</analogValues.value>
         </Measurements>
         <Measurements>
          <measurementType>A07</measurementType>
          <unitSymbol>AMP</unitSymbol>
        <positiveFlowIn>A01</positiveFlowIn>
        <analogValues.value>2.0</analogValues.value>
      </Measurements>
     </RegisteredResource>
    </Monitored_Series>

  ...
</Series>
```
CNECs are defined in Monitored_Series elements, in B56 or B57 Series. A CNEC has one registered resource that defines its network element via its mrID. The optimization_MarketObjectStatus indicates if the CNEC is monitored or optimized.   

As it is defined in the CRAC model, a CNEC is associated to a state. If the Series containing the Monitored_Series has one or many Contingency_Series that have previously been [correctly defined](#contingencies), CNECs shall be created after these referenced contingencies. When no Contingency_Series are present in this Series, CNECs shall be created after all the contingencies present in the CRAC. The Measurements tags define the instants on which CNECs are defined via the measurementType tag, and the CNEC's threshold values.  

Finally, a CNEC can be named in the following way : _[network element name] - [side (placeholder if the branch is a TieLine)] - [direction in which the CNEC is monitored (placeholder)] - [monitored (placeholder for MNECs)] - [contingency (placeholder for when a contingency is defined.)] - [instant]_.

### AngleCnecs

```xml
<Series>
  <mRID>Angle1</mRID>
  <businessType>B56</businessType>
  <name>Angle1-name</name>
  <optimization_MarketObjectStatus.status>A52</optimization_MarketObjectStatus.status>
  <AdditionalConstraint_Series>
    <mRID>AngleCnec1</mRID>
    <businessType>B87</businessType>
    <name>AngleCnec1-name</name>
    <measurement_Unit.name>DD</measurement_Unit.name>
    <quantity.quantity>30</quantity.quantity>
    <RegisteredResource>
      <mRID codingScheme="A02">networkElementId</mRID>
      <name>RegisteredResource1</name>
      <marketObjectStatus.status>A47</marketObjectStatus.status>
    </RegisteredResource>
    <RegisteredResource>
      <mRID codingScheme="A02">networkElementId</mRID>
      <name>RegisteredResource2</name>
      <marketObjectStatus.status>A46</marketObjectStatus.status>
    </RegisteredResource>
  </AdditionalConstraint_Series>
  <Contingency_Series>
    ...
  </Contingency_Series>
  <RemedialAction_Series>
    ...
  </RemedialAction_Series>
</Series>
```
AngleCnecs are easily distinguishable thanks to the AdditionalConstraint_Series tag.  
They define an AngleCnec in curative with an importing element, an exporting element (cf. the two registered resources) 
and with a threshold with a max bound defined by quantity. In order to be secure, they must respect:  
exporting element angle - importing element angle <= max bound    
In the CIM CRAC, AngleCnecs are actually defined with their corresponding remedial actions in B56 Series (ie Remedial Action series). The Contingency_Series (unique) refers to the contingency after which the AngleCnec is monitored.  

### VoltageCnecs

[VoltageCnecs are defined in the CimCracCreationParameters](creation-parameters.md#voltage-cnecs-creation-parameters). 
Nevertheless, they are imported via the CimCracCreator because that's where the information on which contingencies are imported lies. 
Only voltage CNECs with contingencies that were previously correctly defined shall be imported.

## Remedial Actions

Remedial actions are listed in Series of business type B56. 

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_SERIES</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <Contingency_Series> ⚪OPTIONAL
    ...
    </Contingency_Series>
    <Monitored_Series> ⚪OPTIONAL
    ...
    </Monitored_Series>
    <RemedialAction_Series>
      <mRID>RTE-PRA_1</mRID>
      <name>RTE preventive remedial action number 1</name>
      <businessType>B59</businessType>
      <applicationMode_MarketObjectStatus.status>A18</applicationMode_MarketObjectStatus.status>
      <availability_MarketObjectStatus.status>A39</availability_MarketObjectStatus.status>
      ...
      <Shared_Domain> ⚪OPTIONAL
        <mRID codingScheme="A01">10YFR-XXX------C</mRID>
    </Shared_Domain>
    </RemedialAction_Series>
    ...
</Series>
```
BusinessType in B56 series should always be B59. In RemedialAction_Series, the applicationMode_MarketObjectStatus defines the Remedial Action's instant :
- Preventive : A18
- Curative : A19
- Preventive and curative : A27
- Auto : A20.  

By default, the operator is read from the RemedialAction_Series' mRID, as the string that precedes the first "-" character. In the example above, the operator would be "RTE".  

RemedialAction_Series may also contain Contingency_Series, Monitored_Series and Shared_Domain tags. Remedial actions' [usage rules](json.md#remedial-actions-and-usages-rules) will be defined depending on these tags: 
- RemedialAction_Series that don't have any Monitored_Series children tags nor any Shared_Domain tags define **FreeToUse** remedial actions.
- When Monitored_Series tags exist, they define CNECs for which the remedial action series is available. These CNECs could have been defined previously in B57 series, or they are only defined in this B56 series following the [same logic described previously](#cnecs). When the RemedialAction_Series also contains a Contingency_Series, the only CNECs from the Monitored_Series tags that will be considered are those that list CNECs defined with a contingency from the Contingency_Series. For each remaining CNEC, the remedial action is defined with a **OnFlowConstraint** on the remedial action's instant.
- When the RemedialAction_Series has no Monitored_Series, and a Shared_Domain tag, the Shared_Domain tag must be taken into account. It represents a country. Then, the remedial action is defined with a **OnFlowConstraintInCountry** on the remedial action's instant after the given contingencies.  

Some remedial actions may have to be aligned in order to keep the same set-point value. 
When it's the case, this information is retrieved [from the CracCreationParameters file](creation-parameters.md#range-action-groups-cim) 
and the remedial actions are defined with a common groupId.

### PST Range Actions

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_AFTER_ALL_CONTINGENCIES</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      <mRID>PRA_1</mRID>
      <name>PRA_1</name>
      <businessType>B59</businessType>
      <applicationMode_MarketObjectStatus.status>A18</applicationMode_MarketObjectStatus.status>
      <availability_MarketObjectStatus.status>A39</availability_MarketObjectStatus.status>
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
        <pSRType.psrType>A06</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">1------0</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">1------0</out_Domain.mRID>
        <marketObjectStatus.status>A26</marketObjectStatus.status>
        <resourceCapacity.maximumCapacity>33.0</resourceCapacity.maximumCapacity>
        <resourceCapacity.minimumCapacity>1.0</resourceCapacity.minimumCapacity>
        <resourceCapacity.unitSymbol>C62</resourceCapacity.unitSymbol>
      </RegisteredResource>
    </RemedialAction_Series>
          ...
</Series>
```

PST Range Actions have a unique RegisteredResource with A06 psrType, that stands for phase shift transformer. They can be defined with a range, rangeType being specified by the marketObjectStatus and min and max range values being defined by resourceCapacity min and max capacities. 

### Network Actions

Network actions have one or more registered resources, that represent elementary actions. Each elementary action's type is defined by its psrType :  
- **phase tap changer tap position actions** have a A06 psrType, that stands for phase shift transformer. They differ from range actions as they have a specific set-point instead of an allowed range. That's why they have a default capacity tag instead of a minimumCapacity and/or a maximumCapacity tag.

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_1</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      ...
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
        <pSRType.psrType>A06</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">1------0</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">1------0</out_Domain.mRID>
        <marketObjectStatus.status>A25</marketObjectStatus.status>
        <resourceCapacity.defaultCapacity>11</resourceCapacity.defaultCapacity>
        <resourceCapacity.unitSymbol>C62</resourceCapacity.unitSymbol>
        </RegisteredResource>
      ...
      </RemedialAction_Series>
        ...
</Series>
```

- **generator actions** have a A04 psrType, that stand for generation. In a similar way to pst set-points, generator actions do not have a minimumCapacity nor a maximumCapacity, but they have a defaultCapacity when the marketObjectStatus is A26 (stands for ABSOLUTE) with unitSymbol MAW. The marketObjectStatus may also be A23 for STOP. In that case, no defaultCapacity may be present.

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_1</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      ...
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
       <pSRType.psrType>A04</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">10YPT-XXX------W</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">10YPT-XXX------W</out_Domain.mRID>
        <marketObjectStatus.status>A26</marketObjectStatus.status>
        <resourceCapacity.defaultCapacity>100</resourceCapacity.defaultCapacity>
        <resourceCapacity.unitSymbol>MAW</resourceCapacity.unitSymbol>
        </RegisteredResource>
      ...
      </RemedialAction_Series>
        ...
</Series>
```

- **load actions** have a A05 psrType, that stand for load. They are similar to generator actions.

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_1</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      ...
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
       <pSRType.psrType>A05</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">10YPT-XXX------W</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">10YPT-XXX------W</out_Domain.mRID>
        <marketObjectStatus.status>A26</marketObjectStatus.status>
        <resourceCapacity.defaultCapacity>100</resourceCapacity.defaultCapacity>
        <resourceCapacity.unitSymbol>MAW</resourceCapacity.unitSymbol>
        </RegisteredResource>
      ...
      </RemedialAction_Series>
        ...
</Series>
```

- **switch actions** have A07 psrType, that stand for circuit breaker. In a similar way to other elementary actions, switch actions do not have a minimumCapacity nor a maximumCapacity. Since they aren't defined by a set-point, they don't have a defaultCapacity either. They are entirely defined by the marketObjectStatus : A21 for OPEN, and A22 for CLOSE.

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_1</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      ...
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
        <pSRType.psrType>A07</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">10YES-XXX------0</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">10YES-XXX------0</out_Domain.mRID>
        <marketObjectStatus.status>A22</marketObjectStatus.status>
        </RegisteredResource>
      ...
      </RemedialAction_Series>
        ...
</Series>
```

- **terminals connection actions** have a A01, A02 or a B24 psrType, that respectively stand for tie-line, line and transformer. They are similar to switch actions.

```xml
<Series>
    <mRID>RA-Series-1</mRID>
    <businessType>B56</businessType>
    <name>RA_1</name>
    <optimization_MarketObjectStatus.status>Z01</optimization_MarketObjectStatus.status>
    <RemedialAction_Series>
      ...
      <RegisteredResource>
        <mRID codingScheme="A02">networkElementId</mRID>
        <name>RA1-name</name>
        <pSRType.psrType>A01</pSRType.psrType>
        <in_Domain.mRID codingScheme="A01">10YES-XXX------0</in_Domain.mRID>
        <out_Domain.mRID codingScheme="A01">10YES-XXX------0</out_Domain.mRID>
        <marketObjectStatus.status>A21</marketObjectStatus.status>
        </RegisteredResource>
      ...
      </RemedialAction_Series>
        ...
</Series>
```

A network action is imported if all of its elementary actions are imported.

### HVDC Range Actions

In the CIM CRAC format as it is actually used, the HVDC remedial action is defined in two separate remedial actions, each representing one direction:
- the first RemedialAction_Series is a range action in the A -> B direction, with 0 to XXX MW available.
- the second RemedialAction_Series is a range action in the B -> A direction, with 0 to XXX MW available.  

Each of these RemedialAction_Series can contain 4 RegisteredResources, allowing to use 2 HVDC lines as a remedial action group (i.e. setting both lines to same set-point):
- the first RegisteredResource sets the HVDC line #1 to "active power set-point" mode.
- the second RegisteredResource defines the allowed set-point range on the HVDC line #1.
- the third RegisteredResource sets the HVDC line #2 to "active power set-point" mode.
- the fourth RegisteredResource defines the allowed set-point range on the HVDC line #2.

In the end, two HVDC range actions with an absolute range of -XXX MW to XXX MW each are defined, on both HVDC lines. These HVDC range actions are aligned, i.e. they share the same group ID. That means that they must have the same set-point. 

## Extra rules

In order to ensure the imported CRAC is usable in the RAO, OpenRAO implements the following special rules:
- Hybrid (range-actions + network-actions) remedial actions are prohibited.
- If AUTO CNECs exist without any automaton that can eventually secure them, these CNECs are duplicated in the 
  outage instant in order to be secured by the preventive RAO.
- HVDC set-point remedial actions that require the deactivation of [angle-droop active power control](inv:powsyblcore:*:*#hvdc-angle-droop-active-power-control-extension)
  (AC-emulation) are only supported at an auto instant.
