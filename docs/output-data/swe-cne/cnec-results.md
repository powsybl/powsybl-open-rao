CNEC results after RAO are reported in **Constraint_Series** tags, with **businessType** value **B57**.  

Example:

~~~xml
<Constraint_Series>
  <mRID>0cb4110-02cf-4516-943e-7f717bc1c2b8</mRID>
  <businessType>B57</businessType>
  <Contingency_Series>
    <mRID>Co-175</mRID>
    <name>contingency 175</name>
  </Contingency_Series>
  <AdditionalConstraint_Series>
    <mRID>REN-AC-2</mRID>
    <businessType>B87</businessType>
    <name>Angle-ALTO-LINDOSO-CARTELLE-EXPORT</name>
    <quantity.quantity>-39.8</quantity.quantity>
  </AdditionalConstraint_Series>
  <Monitored_Series>
    <mRID>MS-1</mRID>
    <name>Branch Paris-Berlin monitored in curative</name>
    <RegisteredResource>
      <mRID codingScheme="A02">_68c74a71-224a-245f-0b52-eac5045761eb</mRID>
      <name>Branch Paris-Berlin</name>
      <in_AggregateNode.mRID codingScheme="A02">_7fb8ba77-76a2-7343-b1f9-10d8fb9bdae1</in_AggregateNode.mRID>
      <out_AggregateNode.mRID codingScheme="A02">_d5c2a18c-ef2d-10ae-4419-c832c53860b1</out_AggregateNode.mRID>
      <Measurements>
        <measurementType>A01</measurementType>
        <unitSymbol>AMP</unitSymbol>
        <positiveFlowIn>A02</positiveFlowIn>
        <analogValues.value>270</analogValues.value>
      </Measurements>
      <Measurements>
        <measurementType>A13</measurementType>
        <unitSymbol>AMP</unitSymbol>
        <positiveFlowIn>A02</positiveFlowIn>
        <analogValues.value>2606</analogValues.value>
      </Measurements>
    </RegisteredResource>
  </Monitored_Series>
  <Monitored_Series>
    <mRID>MS-2</mRID>
    <name>Branch Paris-Brussels monitored in curative</name>
...
~~~

#### mRID

Random ID, unique in the CNE document.

#### businessType

One possible value to signify it's a CNEC result: **B57**.

#### Contingency_Series (optional)

Exists if the CNEC is monitored after a contingency (if the CNEC is preventive, this tag does not exist): 
- **mRID**: unique ID of the contingency as it is defined in the native CRAC
- **name**: name of the contingency as it is defined in the native CRAC
> ⚠️  **NOTE**  
> Multiple Contingency_Series can exist, meaning that the following results are valid for all the listed contingencies. 
> Avoiding redundant information allows a more compact file.

#### Reason (optional)

When sensitivity computation fails in a given perimeter (in basecase or after a specific contingency identified by 
**Contingency_Series**), this tag is present with the following information:
- **code**: "B40"
- **text**: "Load flow divergence"  

Then no more results are exported for the failed perimeter.  
 
#### AdditionalConstraint_Series (optional)

If angle CNECs are monitored in the actual perimeter (in basecase or after a specific contingency identified by
**Contingency_Series**), this tag contains angle values from the angle monitoring module.

##### mRID

Unique ID of the angle CNEC, as defined in the [orignal CRAC file](/input-data/crac/cim.md#angle-cnecs).  

##### businessType

One possible value: **B87** (angle monitoring).

##### name

Name of the angle CNEC, as defined in the [orignal CRAC file](/input-data/crac/cim.md#angle-cnecs).  

##### quantity.quantity

Phase shift angle value of the CNEC, expressed in degrees.

#### Monitored_Series

##### mRID

Unique ID of the CNEC as defined in the native CRAC file.

##### name

Name of the CNEC as defined in the native CRAC file.

#### RegisteredResource

##### mRID

Unique ID of the CNEC's network element, as defined in the native CRAC file and in the network.

##### name

The name of the CNEC as defined in the native CRAC file.

##### in_AggregateNode.mRID

Unique ID, in the network, of the voltage level on the branch's left side.

##### out_AggregateNode.mRID

Unique ID, in the network, of the voltage level on the branch's right side.

##### PTDF_Domain (optional)

Only in the hypothetical case of a [relative margins objective function](/parameters/parameters.md#objective-function-type).  
- **mRID**: [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/) of the area
- **pTDF_Quantity.quantity**: value of the PTDF associated to the bidding zone for the critical network element  
> ⚠️  **NOTE**  
> This tag is currently not supported in FARAO, since the SWE CC process (only process to use the SWE CNE format) 
> does not optimise relative margins

##### Measurements
- measurementType: type of measurement, possible values are:
  - **A01**: flow
  - **A02**: PATL in the preventive state (input data)
  - **A07**: TATL right after outage (input data)
  - **A12**: TATL at auto instant (input data)
  - **A13**: PATL in curative (input data)
- unitSymbol: unit of measurement. Only supported value is **AMP** (ampere)
- positiveFlowIn: sign of the value, possible values are:
  - **A01**: measurement is positive (its actual value is analogValues.value)
  - **A02**: measurement is negative (its actual value is -analogValues.value)
- analogValues.value: absolute value of the measurement

Depending on whether the Constraint_Series is preventive, some or all of these measurements are expected:

| Measurement type | Measurement       | Unit(s) | Exported in preventive B57 | Exported in non-preventive B57 |
|------------------|-------------------|---------|----------------------------|--------------------------------|
| **A01**          | Flow              | AMP     | ✔️                         | ✔️                             |
| **A02**          | PATL (preventive) | AMP     | ✔️                         |                                |
| **A07**          | TATL (outage)     | AMP     | ️                          | at least one                   |
| **A12**          | TATL (auto)       | AMP     | ️                          | of these three                 |
| **A13**          | PATL (curative)   | AMP     | ️                          | measurements️                  |


#### RemedialAction_Series (optional)

This tag is used to report remedial actions that were selected by the RAO for the CNEC's state.

##### mRID

Unique ID of the selected remedial action, as identified in the original CRAC.

##### name

Name of the selected remedial action, as identified in the original CRAC.

##### applicationMode_MarketObjectStatus.status

Three possible values:
- **A18**: the remedial action was selected in preventive
- **A20**: the remedial action was selected as an automaton
- **A19**: the remedial action was selected in curative
