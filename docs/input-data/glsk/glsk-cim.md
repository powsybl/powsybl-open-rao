# CIM GLSK

GLSK in CIM format are defined within XML files. This format is used for the SWE region.  
The main tag of the document is **GLSK_MarketDocument**.

## Header

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<GLSK_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-n:glskdocument:2:1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:iec62325.351:tc57wg16:451-n:glskdocument:2:1 iec62325-451-n-glsk_v2_1.xsd">
  <mRID>DOCUMENT_ID</mRID>
  <revisionNumber>1</revisionNumber>
  <type>B22</type>
  <process.processType>A48</process.processType>
  <sender_MarketParticipant.mRID codingScheme="A01">SENDER_EIC</sender_MarketParticipant.mRID>
  <sender_MarketParticipant.marketRole.type>A36</sender_MarketParticipant.marketRole.type>
  <receiver_MarketParticipant.mRID codingScheme="A01">RECEIVER_EIC</receiver_MarketParticipant.mRID>
  <receiver_MarketParticipant.marketRole.type>A04</receiver_MarketParticipant.marketRole.type>
  <createdDateTime>2002-06-15T15:49:00Z</createdDateTime>
  <status>
    <value>A42</value>
  </status>
  <time_Period.timeInterval>
    <start>2002-06-16T22:00Z</start>
    <end>2002-06-17T22:00Z</end>
  </time_Period.timeInterval>
  <domain.mRID codingScheme="A01">REGION_EIC</domain.mRID>
    ...
</GLSK_MarketDocument>
~~~

Contents of the header:
- **mRID**: ID for the document
- **revisionNumber**: version of the document
- **type**: standard code defining the document type*. In the example above, "B22" means "Generation and load shift keys document".
- **process.processType**: standard code defining the process type*. In the example above, "A48" means "Day-ahead capacity determination".
- **sender_MarketParticipant.mRID**: ID of the sender of the GLSK document*. Type is given by the content of **codingScheme**:
  - A01: the coding scheme is the [EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/), maintained by ENTSO-E.
  - A02: the coding scheme used for Common Grid Model Exchange Standard (CGMES).
  - A10: the coding scheme for the preceding attribute is the Global Location Number (GLN 13) or Global Service Relation Number (GSRN 18), maintained by GS1.
  - ... other region-specific codes*.
- **sender_MarketParticipant.marketRole.type**: standard code defining the role of the sender*. In the example above, "A36" means "capacity coordinator".
- **receiver_MarketParticipant.mRID**: ID of the receiver of the GLSK document*. Type is given by the content of **codingScheme**.
- **receiver_MarketParticipant.marketRole.type**: standard code defining the role of the receiver*. In the example above, "A04" means "system operator".
- **createdDateTime**: document creation time.
- **status**: standard code defining the status of the document type*. In the example above, "A42" means "The network data provided in the document or series concerns the whole area described by the document or series".
- **time_Period.timeInterval**: time interval of applicability of the GLSK document.
- **domain.mRID**: ID of the geographical applicability of the document. Type is given by the content of **codingScheme**.

_*Refer to the [ENTSO-E website](https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/) for the
complete reference documents._
_You can find a complete list of codes [in the XSDs](https://www.entsoe.eu/Documents/EDI/Library/CIM_xsd_package.zip)._

## GLSK Definition

Proper GLSK are defined within the tags **TimeSeries**. Usually every TSO defines its GLSK in one separate 
**TimeSeries** tag.  

~~~xml
<TimeSeries>
  <mRID>TSO-TimeSeries-1</mRID>
  <subject_Domain.mRID codingScheme="A01">AREA_EIC</subject_Domain.mRID>
  <curveType>A03</curveType>
  <Period>
    <timeInterval>
      <start>2018-03-27T22:00Z</start>
      <end>2018-03-28T22:00Z</end>
    </timeInterval>
    <resolution>PT60M</resolution>
    <Point>
      <position>1</position>
      <SKBlock_TimeSeries>
        <businessType>...</businessType>
        ...
      </SKBlock_TimeSeries>
    </Point>
  </Period>
</TimeSeries>
~~~

Contents of this tag:
- **mRID**: unique ID of this **TimeSeries**
- **subject_Domain.mRID**: ID of the area of applicability of this GLSK definition. Type is given by the content of **codingScheme**.
- **curveType**: five possible values:
  - **A01**: the curve is made of successive Intervals of time (Blocks) of constant duration (size), where the size of the Blocks is equal to the Resolution of the Period.
  - **A02**: the curve is made of successive instants of time** (Points).
  - **A03**: the curve is made of successive Intervals of time (Blocks) of variable duration (size), where the end date and end time of each Block are equal to the start date and start time of the next Interval. For the last Block the end date and end time of the last Interval would be equal to EndDateTime of TimeInterval.
  - **A04**: the curve is made of successive Intervals of time of variable duration (size), where the end date and end time of each interval are equal to the start date and start time of the next Interval.
  - **A05**: the curve is a restriction of the curve type A04, i.e. overlapping breakpoints. The restriction is that a single Period is allowed.
- **Period**: contains the GLSK values for different points in time
  - **timeInterval**: applicability timeframe of the current **Period** tag
  - **resolution**: time-step resolution of following **Point** tags (ISO 8601). In the example above, "PT60M" means 60 minutes.
  - **Point**: contains the GLSK values for a specific point inside the **timeInterval**
    - **position**: position of the point in the **timeInterval**, considering the **resolution**, and the convention defined in **curveType**
    - **SKBlock_TimeSeries**: actual GLSK definition for the given point in time. Their type is defined by **businessType**, which 
    can have different values. We will detail the following values:
      - **B42**: Base case proportional shift key. In this case only one **SKBlock_TimeSeries** should be defined.
      - **B43**: Proportional to participation factors shift key. In this case only one **SKBlock_TimeSeries** should be defined.
      - **B44**: Proportional to the remaining capacity shift key. In this case only one **SKBlock_TimeSeries** should 
      be defined. _(Note: this type is actually not supported by PowSyBl.)_
      - **B45**: Merit order shift key. In this case multiple **SKBlock_TimeSeries** can be defined.  

### Base case proportional shift key

When **businessType** is B42, the GSK or LSK are proportional to the base case generation or load.  
This type of GLSK is [described here](/input-data/glsk/glsk.md#proportional-to-target-power-glsk).

~~~xml
<SKBlock_TimeSeries>
  <businessType>B42</businessType>
  <mktPSRType.psrType>A04</mktPSRType.psrType>
  <measurement_Unit.name>C62</measurement_Unit.name>
</SKBlock_TimeSeries>
~~~

- **businessType**: B42 means "The GSK or LSK are proportional to the base case generation or load"
- **mktPSRType.psrType**: standard code defining the network elements on which the shifting should operate:
  - **A04**: generators
  - **A05**: loads
- **measurement_Unit.name**: unit of the GLSK. Only "C62" (dimensionless) is possible in this case.

### Proportional to participation factors shift key

When **businessType** is B43, the GSK or LSK are proportional to the participation factors.  
This type of GLSK is [described here](/input-data/glsk/glsk.md#equally-balanced-glsk).

~~~xml
<SKBlock_TimeSeries>
  <businessType>B43</businessType>
  <mktPSRType.psrType>A04</mktPSRType.psrType>
  <measurement_Unit.name>C62</measurement_Unit.name>
</SKBlock_TimeSeries>
~~~
- **businessType**: B43 means "The GSK or LSK are proportional to the participation factors"
- **mktPSRType.psrType**: standard code defining the network elements on which the shifting should operate:
  - **A04**: generators
  - **A05**: loads
- **measurement_Unit.name**: unit of the GLSK. Only "C62" (dimensionless) is possible in this case.

### Proportional to the remaining capacity shift key
When **businessType** is B44, the GSK is proportional to the remaining available capacity.  
This type of GLSK is [described here](/input-data/glsk/glsk.md#proportional-to-remaining-capacity).

~~~xml
<SKBlock_TimeSeries>
  <businessType>B44</businessType>
  <mktPSRType.psrType>A04</mktPSRType.psrType>
  <measurement_Unit.name>C62</measurement_Unit.name>
</SKBlock_TimeSeries>
~~~

- **businessType**: B44 means "The GSK is proportional to the remaining available capacity"
- **mktPSRType.psrType**: standard code defining the network elements on which the shifting should operate:
  - **A04**: generators
  - **A05**: loads
- **measurement_Unit.name**: unit of the GLSK. Only "C62" (dimensionless) is possible in this case.

### Merit order shift key

When **businessType** is B45, the GSK is proportional to a merit order list.  
This type of GLSK is [described here](/input-data/glsk/glsk.md#merit-order-glsk).

~~~xml
<SKBlock_TimeSeries>
  <businessType>B45</businessType>
  <mktPSRType.psrType>A04</mktPSRType.psrType>
  <flowDirection.direction>A01</flowDirection.direction>
  <measurement_Unit.name>MAW</measurement_Unit.name>
  <attributeInstanceComponent.position>3</attributeInstanceComponent.position>
  <RegisteredResource>
    <mRID codingScheme="A02">RESOURCE_ID</mRID>
    <name>RESOURCE_NAME</name>
    <resourceCapacity.maximumCapacity>500</resourceCapacity.maximumCapacity>
    <resourceCapacity.minimumCapacity>0</resourceCapacity.minimumCapacity>
  </RegisteredResource>
</SKBlock_TimeSeries>
~~~

- **businessType**: B45 means "The GSK is proportional to a merit order list"
- **mktPSRType.psrType**: standard code defining the network element on which the shifting should operate:
  - **A04**: generators
  - **A05**: loads
- **flowDirection.direction**: direction in which the generation change on this element can operate:
  - **A01**: "UP". Up signifies that the available power can be used by the Purchasing area to increase energy.
  - **A02**: "DOWN". Down signifies that the available power can be used by the Purchasing area to decrease energy.
- **measurement_Unit.name**: unit of the GLSK. Only "MAW" (megawatt) is possible in this case.
- **attributeInstanceComponent.position**: the relative position of the current timestamp in the applicability timeframe   
  of the current **Period** tag (first timestamp has position 1)
- **RegisteredResource**: describes the network element that should be shifted
  - **mRID**: unique ID of network element in the network. "codingScheme="A02"" means it is a CGMES ID.
  - **name**: human-readable name
  - **resourceCapacity.maximumCapacity**: maximum generation/load capacity to respect on the resource, in **measurement_Unit.name**
  - **resourceCapacity.minimumCapacity**: minimum generation/load capacity to respect on the resource, in **measurement_Unit.name**
