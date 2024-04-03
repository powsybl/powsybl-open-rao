# CSE GLSK

## Header

~~~xml
<GSKDocument DtdVersion="5" DtdRelease="0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="gsk-document.xsd">
    <DocumentIdentification v="NAME"/>
    <DocumentVersion v="1"/>
    <DocumentType v="B22"/>
    <ProcessType v="A01"/>
    <SenderIdentification v="SENDER_EIC" codingScheme="A01"/>
    <SenderRole v="A36"/>
    <ReceiverIdentification v="RECEIVER_EIC" codingScheme="A01"/>
    <ReceiverRole v="A04"/>
    <CreationDateTime v="2017-10-30T09:27:21Z"/>
    <GSKTimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
    <Domain v="REGION_EIC" codingScheme="A01"/>
    ...
</GSKDocument>
~~~

Contents of the header:
- **DocumentIdentification**: ID for the document
- **DocumentVersion**: version of the document
- **DocumentType**: standard code defining the document type*. In the example above, "B22" means "Generation and load shift keys document".
- **ProcessType**: standard code defining the process type*. In the example above, "A01" means "day ahead".
- **SenderIdentification**: ID of the sender of the GLSK document*. Usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/).
- **SenderRole**: standard code defining the role of the sender*. In the example above, "A36" means "capacity coordinator".
- **ReceiverIdentification**: ID of the receiver of the GLSK document*. Usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/).
- **ReceiverRole**: standard code defining the role of the receiver*. In the example above, "A04" means "system operator".
- **CreationDateTime**: document creation time.
- **GSKTimeInterval**: time interval of applicability of the GSK document.
- **Domain**: ID of the geographical applicability of the document. Usually an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/).

_*Refer to the [ENTSO-E website](https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/) for the
complete reference documents._
_You can find a complete list of codes [in the XSDs](https://www.entsoe.eu/Documents/EDI/Library/CIM_xsd_package.zip).
Note that codes that begin with "Z" are region-specific._

## GLSK Definition

Proper GLSK are defined within the tag **TimeSeries**. Generally, every TSO defines its own **TimeSeries**.

~~~xml
<...Block>
    <TimeSeriesIdentification v="1"/>
    <BusinessType v="Z02"/>
    <Area v="REGION_EIC" codingScheme="A01"/>
    <Name v="REGION_NAME"/>
    <TimeInterval v="2022-11-26T18:00Z/2022-11-26T19:00Z"/>
    <...Block></...Block>
</TimeSeries>
~~~

- **TimeSeriesIdentification**: unique ID of the **TimeSeries** in the document.
- **BusinessType**: code defining the type of network element to be shifted:
  - **Z02**: generators
  - **Z05**: loads
- **Area**: geographical applicability of the **TimeSeries**. Type is given by the content of **codingScheme**:
    - A01: the coding scheme is the [EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/), maintained by ENTSO-E.
    - A02: the coding scheme used for Common Grid Model Exchange Standard (CGMES).
    - A10: the coding scheme for the preceding attribute is the Global Location Number (GLN 13) or Global Service Relation Number (GSRN 18), maintained by GS1.
    - ... other region-specific codes*.
- **Name**: human-readable name of the region.
- **TimeInterval**: time interval of applicability of the GLSK document.
- **...Block**: one or multiple GLSK blocks containing actual values, can be of different types, explained in the following paragraphs.

_*Refer to the [ENTSO-E website](https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/) for the
complete reference documents._
_You can find a complete list of codes [in the XSDs](https://www.entsoe.eu/Documents/EDI/Library/CIM_xsd_package.zip)._

### Manual GLSK

The **ManualGSKBlock** tag can be used to define manually all the UCTE nodes associated with their weight within the GLSK.

~~~xml
<ManualGSKBlock>
  <Factor v="1"/>
  <Node>
    <Name v="Node1"/>
    <Factor v="0.19"/>
  </Node>
  <Node>
    <Name v="Node2"/>
    <Factor v="1.99"/>
  </Node>
  ...
</ManualGSKBlock>
~~~

- **Factor**: a multiplier for all following factor values.
- **Node**: list of custom GLSK definitions
  - **Name**: UCTE ID of the node on which the generation/load should be shifted
  - **Factor**: the shift key for the given node

### Proportional Block

The **PropGSKBlock** and **PropLSKBlock** tags can be used to define [proportional GLSK](/input-data/glsk/glsk.md#proportional-to-target-power-glsk).  
**PropGSKBlock** affects generators, and **PropLSKBlock** affects loads.

~~~xml
<PropGSKBlock>
  <Factor v="0.5"/>
</PropGSKBlock>
<PropLSKBlock>
  <Factor v="0.5"/>
  <Node>
    <Name v="Node1"/>
  </Node>
  <Node>
    <Name v="Node2"/>
  </Node>
  <Node>
    <Name v="Node3"/>
  </Node>
</PropLSKBlock>
~~~

- **Factor**: a multiplier for all following factor values.
- **Node** (optional): list of custom GLSK definitions
  - **Name**: UCTE ID of the node on which the generation/load should be shifted

  _Note: if no **Node** is defined in the block, all generators/loads in the network (for the given region) are affected_

### Reserve Block

The **ReserveGSKBlock** tag can be used to define [remaining capacity GLSK](/input-data/glsk/glsk.md#proportional-to-remaining-capacity).  

~~~xml
<ReserveGSKBlock>
  <Factor v="1"/>
  <Node>
    <Name v="Node1"/>
    <Pmin v="0"/>
    <Pmax v="-29"/>
  </Node>
  <Node>
    <Name v="Node2"/>
    <Pmin v="0"/>
    <Pmax v="-29"/>
  </Node>
</ReserveGSKBlock>
~~~

- **Factor**: a multiplier for all following factor values.
- **Node**: list of custom GLSK definitions
  - **Name**: UCTE ID of the node on which the generation/load should be shifted
  - **Pmin**: minimum active power target
  - **Pmax**: maximum active power target (can be < Pmin, if shifting needs to decrease generation / increase load, 
    like in example above)

### Merit-order Block

The **MeritOrderGSKBlock** tag can be used to define [merit-order GLSK](/input-data/glsk/glsk.md#merit-order-glsk).  

~~~xml
<MeritOrderGSKBlock>
  <Factor v="1"/>
  <Up>
    <Factor v="1"/>
    <Node>
      <Name v="NodeUp1"/>
      <Pmax v="-360"/>
    </Node>
    <Node>
      <Name v="NodeUp2"/>
      <Pmax v="-384"/>
    </Node>
    <Node>
      <Name v="NodeUp3"/>
      <Pmax v="-411"/>
    </Node>
  </Up>
  <Down>
    <Factor v="1"/>
    <Node>
      <Name v="NodeDown1"/>
      <Pmin v="-122"/>
    </Node>
    <Node>
      <Name v="NodeDown2"/>
      <Pmin v="-260"/>
    </Node>
  </Down>
</MeritOrderGSKBlock>
~~~

- **Factor**: a multiplier for all following factor values.
- **Up**: list of nodes used to increase generation / decrease load
  - **Factor**: a multiplier for all **Up** factor values.
  - **Node**: list of custom GLSK definitions, **sorted by ascending merit order**
    - **Name**: UCTE ID of the node on which the generation/load should be shifted
    - **Pmax**: maximum active power target on the node
- **Down**: list of nodes used to decrease generation / increase load
  - **Factor**: a multiplier for all **Down** factor values.
  - **Node**: list of custom GLSK definitions, **sorted by ascending merit order**
    - **Name**: UCTE ID of the node on which the generation/load should be shifted
    - **Pmax**: maximum active power target on the node
