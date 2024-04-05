# UCTE GLSK

GLSK in UCTE format are defined within XML files. This format is used for the CORE region.  
The main tag of the document is **GSKDocument**.

## Header

~~~xml
<GSKDocument xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" DtdVersion="1" DtdRelease="0" xsi:noNamespaceSchemaLocation="gsk-document.xsd">
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

Proper GLSK are defined within the tag **GSKSeries**

~~~xml
<GSKSeries>
    <TimeSeriesIdentification v="1"/>
    <BusinessType v="Z02" share="50"/>
    <Area v="10YFR-RTE------C" codingScheme="A01"/>
    ...
</GSKSeries>
~~~

- **Area**: defines the geographical zone handled by the GLSK. codingScheme value at A01 declares that is
  an [ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/), it will be the case most of the time
  but other types of code could be used to describe the area.
- **BusinessType**: can have two values here: 
  - **Z02** means it is a GSK 
  - **Z05** means it is an LSK
  - The **share** value represents the proportion of this series for the whole area. For example if the RTE area is
    defined in two different series – one with Z02 share=0.3 and one with Z05 share=0.7 – and that we want to apply a
    shift of 1000MW in RTE zone, we will apply a shift of 300MW on the generators and a shift of 700MW on the loads.

So according to this format to embrace all the elements of an area we have to **combine potentially 2 series so that GSK
and LSK can be gathered**.

There are several ways to define the set of generators and loads available in the GLSK. As we will se all the blocks of
GLSK definition have a common tag **GSK_Name** that gives a human-readable name to the GLSK.

### Automatic GLSK

GLSK in UCTE format can be defined with a list of UCTE nodes to consider in a **AutoGSK_Block**. 
With this tag there is no need to define a factor associated to the node, it will be done as a 
[proportional to target power GLSK](/input-data/glsk/glsk.md#proportional-to-target-power-glsk).

~~~xml
<AutoGSK_Block>
   <GSK_Name v="FR"/>
   <TimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
   <AutoNodes>
      <NodeName v="FFR1AA1 "/>
   </AutoNodes>
   <AutoNodes>
      <NodeName v="FFR2AA1 "/>
   </AutoNodes>
</AutoGSK_Block>
~~~

- **GSK_Name**: human-readable name for the block.
- **TimeInterval**: time interval of applicability of the GLSK block.
- **ManualNodes**: list of custom GLSK definitions
    - **NodeName**: UCTE ID of the node for which the generation/load should be shifted

### Country GLSK

The **CountryGSK_Block** tag can be used to define a GLSK without even defining an explicit list of nodes. 
It will be a [proportional to target power GLSK](/input-data/glsk/glsk.md#proportional-to-target-power-glsk) defined on all the nodes of the 
area. The matching is done through the network, meaning all the nodes in the network that belong to the specified area 
are added to the GLSK.

> ⚠️  **NOTE**  
> As it is currently implemented, this only works for areas that are countries. If the area is not a country the
> matching would not be done properly.

~~~xml
<CountryGSK_Block>
   <GSK_Name v="FR"/>
   <TimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
</CountryGSK_Block>
~~~

- **GSK_Name**: human-readable name for the block.
- **TimeInterval**: time interval of applicability of the GLSK block.

### Manual GLSK
The **ManualGSK_Block** tag can be used to define manually all the UCTE nodes associated with their weight within the GLSK.

~~~xml
<ManualGSK_Block>
    <GSK_Name v="FR"/>
    <TimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
    <ManualNodes>
        <NodeName v="FFR2AA1 "/>
        <Factor v="0.3"/>
    </ManualNodes>
    <ManualNodes>
        <NodeName v="FFR1AA1 "/>
        <Factor v="0.7"/>
    </ManualNodes>
</ManualGSK_Block>
~~~

- **GSK_Name**: human-readable name for the block.
- **TimeInterval**: time interval of applicability of the GLSK block.
- **ManualNodes**: list of custom GLSK definitions
    - **NodeName**: UCTE ID of the node on which the generation/load should be shifted
    - **Factor**: the shift key for the given node