CNEC results after RAO are reported in **Constraint_Series** tags, with **businessType** value **B88**, **B57** or **B54**.  

Example: 
~~~xml
<Constraint_Series>
    <mRID>ME_one_co_one_dir</mRID>
    <businessType>B88</businessType>
    <Party_MarketParticipant>
        <mRID codingScheme="A01">10XFR-RTE------Q</mRID>
    </Party_MarketParticipant>
    <optimization_MarketObjectStatus.status>A52</optimization_MarketObjectStatus.status>
    <Contingency_Series>
        <mRID>Co_one</mRID>
        <name>Co_one</name>
    </Contingency_Series>
    <Monitored_Series>
        <mRID>ME_one_co_one_dir</mRID>
        <name>ME_one|Co_one</name>
        <RegisteredResource>
            <mRID codingScheme="A02">ME_one_co_one_dir</mRID>
            <name>ME_one</name>
            <Measurements>
                <measurementType>A01</measurementType>
                <unitSymbol>MAW</unitSymbol>
                <positiveFlowIn>A02</positiveFlowIn>
                <analogValues.value>1118</analogValues.value>
            </Measurements>
            <Measurements>
                <measurementType>A02</measurementType>
                <unitSymbol>AMP</unitSymbol>
                <positiveFlowIn>A01</positiveFlowIn>
...
~~~

#### mRID
Unique ID of the CNEC as defined in the native CRAC file.

#### businessType
Three possible values for CNEC results:  
- **B88**: these are the CNEC initial results (i.e. before remedial action optimisation)
- **B57**: these are the CNEC intermediate results, after preventive remedial actions optimisation and before curative
  remedial actions optimisation
- **B54**: these are the CNEC final results, after curative remedial actions optimisation
  > ⚠️  **NOTE**
  > In order to align OpenRAO with other capacity calculation tools, **B54** series are not exported for CNECs if no 
  > curative remedial actions were applied (even though it makes sense to export them, because PATL results are not 
  > exported in **B57** series, as explained further in this article)

#### Party_MarketParticipant

mRID: [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/) of the CNEC's operator (in mRID tag).

#### optimization_MarketObjectStatus.status

Two possible values:
- **A52**: the CNEC is [optimised](/input-data/crac/json.md#cnecs)
- **A49**: the CNEC is [monitored](/input-data/crac/json.md#cnecs)  
  > ⚠️  **NOTE**
  > In order to align OpenRAO with other capacity calculation tools, CNECs that are both optimised and monitored are 
  > currently only exported with an **A52** type

#### Contingency_Series (optional)

Exists if the CNEC is monitored after a contingency (if the CNEC is preventive, this tag does not exist): 
- **mRID**: unique ID of the contingency as it is defined in the native CRAC
- **name**: name of the contingency as it is defined in the native CRAC

#### Monitored_Series

##### mRID

Unique ID of the CNEC as defined in the native CRAC file (same as mRID of Constraint_Series).

##### name

Formatted "native name|contingency name", where:
- "native name" is the name of the CNEC as defined in the native CRAC file;
- "contingency name" is the name of the contingency referenced in Contingency_Series.

#### RegisteredResource

##### mRID

Unique ID of the CNEC as defined in the native CRAC file (same as mRID of Constraint_Series).

##### name

The name of the CNEC as defined in the native CRAC file.

##### Measurements

- measurementType: type of measurement, possible values are:
  - **A01**: flow
  - **A02**: PATL (input data)
  - **A07**: TATL (input data)
  - **A03**: flow reliability margin (input data)
  - **Z11**: absolute zonal PTDF sum
  - **Z12**: flow margin in regard to the PATL 
  - **Z13**: objective function value for this CNEC in regard to the PATL  
    This value is equal to Z12 unless RAO is run with [relative positive margins](/parameters.md#type), 
    in which case it will be equal to Z12 / Z11 when Z12 is positive.
  - **Z14**: flow margin in regard to the TATL 
  - **Z15**: objective function value for this CNEC in regard to the TATL  
    This value is equal to Z14 unless RAO is run with [relative positive margins](/parameters.md#type), 
    in which case it will be equal to Z14 / Z11 when Z14 is positive.
  - **Z16**: loop-flow
  - **Z17**: loop-flow threshold (input data)
- unitSymbol: unit of measurement, possible values are:
  - **MAW**: megawatt
  - **AMP**: ampere
  - **C62**: dimensionless
- positiveFlowIn: sign of the value, possible values are:
  - **A01**: measurement is positive (its actual value is analogValues.value)
  - **A02**: measurement is negative (its actual value is -analogValues.value)
- analogValues.value: absolute value of the measurement

Depending on the business type of the Constraint_Series, some or all of these measurements are expected:  

| Measurement type | Measurement         | Unit(s)  | Exported in B88 (initial) | Exported in B57 (after PRA) | Exported in B54 (after CRA) |
|------------------|---------------------|----------|---------------------------|-----------------------------|-----------------------------|
| **A01**          | Flow                | MAW      | ✔️                         | ✔️                           | ✔️                           |
| **A02**          | PATL                | MAW, AMP | ✔️                         |                             | ✔️                           |
| **A07**          | TATL                | MAW, AMP | ✔️                         | ✔️                           |                             |
| **A03**          | FRM                 | MAW      | ✔️                         | ✔️                           | ✔️                           |
| **Z11**          | PTDF sum            | C62      | ✔️                         | ✔️                           | ✔️                           |
| **Z12**          | PATL margin         | MAW      | ✔️                         |                             | ✔️                           |
| **Z13**          | PATL objective      | MAW      | ✔️                         |                             | ✔️                           |
| **Z14**          | TATL margin         | MAW      | ✔️                         | ✔️                           |                             |
| **Z15**          | TATL objective      | MAW      | ✔️                         | ✔️                           |                             |
| **Z16**          | Loop-flow           | MAW      | ✔️                         | ✔️                           | ✔️                           |
| **Z17**          | Loop-flow threshold | MAW      | ✔️                         | ✔️                           | ✔️                           |

#### RemedialAction_Series (optional)

This tag is used to report remedial actions that were selected by the RAO for the CNEC's state:
- If a PRA is selected for the CNEC's state, it will be reported inside the B54 & B57 Constraint_Series of this CNEC 
- If a CRA is selected for the CNEC's state, it will be reported inside the B57 Constraint_Series of this CNEC

##### mRID

Unique ID of the selected remedial action, as identified in the original CRAC.

##### name

Name of the selected remedial action, as identified in the original CRAC.

##### applicationMode_MarketObjectStatus.status

Two possible values:
- **A18**: the remedial action was selected in preventive
- **A19**: the remedial action was selected in curative
