# Reference program

The reference program contains the values of the power exchanges between the different market areas.
In OpenRAO, this is used to compute loop-flows.

The main tag for the document is **PublicationDocument**.

## Header

~~~xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<PublicationDocument DtdRelease="1" DtdVersion="0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="publication-document-v2r0.xsd">
    <DocumentIdentification v="DOCUMENT_ID"/>
    <DocumentVersion v="5"/>
    <DocumentType v="A45"/>
    <SenderIdentification codingScheme="A01" v="SENDER_EIC"/>
    <SenderRole v="A44"/>
    <ReceiverIdentification codingScheme="A01" v="RECEIVER_EIC"/>
    <ReceiverRole v="A36"/>
    <CreationDateTime v="2020-02-26T11:12:12.304+01:00"/>
    <PublicationTimeInterval v="2019-01-07T23:00Z/2019-01-08T23:00Z"/>
    ...
</PublicationDocument>
~~~

Contents of the header:
- **DocumentIdentification**: ID for the document
- **DocumentVersion**: version of the document
- **DocumentType**: standard code defining the document type*. In the example above, "A45" means "Measurement Value Document".
- **SenderIdentification**: ID of the sender of the Reference Program document*. Type is given by the content of **codingScheme**:
  - A01: the coding scheme is the [EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/), maintained by ENTSO-E.
  - A02: the coding scheme used for Common Grid Model Exchange Standard (CGMES).
  - A10: the coding scheme for the preceding attribute is the Global Location Number (GLN 13) or Global Service Relation Number (GSRN 18), maintained by GS1.
  - ... other region-specific codes*.
- **SenderRole**: standard code defining the role of the sender*. In the example above, "A44" means "Regional Security Coordinator (RSC)".
- **ReceiverIdentification**: ID of the receiver of the reference program document*. Type is given by the content of **codingScheme**.
- **ReceiverRole**: standard code defining the role of the receiver*. In the example above, "A36" means "Capacity Coordinator".
- **CreationDateTime**: document creation time.
- **PublicationTimeInterval**: time interval of applicability of the Reference Program document.

_*Refer to the [ENTSO-E website](https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/) for the
complete reference documents._
_You can find a complete list of codes [in the XSDs](https://www.entsoe.eu/Documents/EDI/Library/CIM_xsd_package.zip)._

## Net position definition

~~~xml
    <PublicationTimeSeries>
        <TimeSeriesIdentification v="DE-FR"/>
        <BusinessType v="A66"/>
        <InArea codingScheme="A01" v="10YFR-RTE------C"/>
        <OutArea codingScheme="A01" v="10YCB-GERMANY--8"/>
        <MeasureUnitQuantity v="MAW"/>
        <Period>
            <TimeInterval v="2019-01-07T23:00Z/2019-01-08T23:00Z"/>
            <Resolution v="PT60M"/>
            <Interval>
                <Pos v="1"/>
                <Qty v="-1600"/>
            </Interval>
            <Interval>
              <Pos v="2"/>
              <Qty v="400"/>
            </Interval>
            ...
        </Period>
    </PublicationTimeSeries>
~~~

- **TimeSeriesIdentification**: ID for the time series
- **BusinessType**: standard code defining the business type. In the example above, "A66" means "Energy Flow".
- **InArea**: ID of the area importing the flow (exporting if the value is negative). Type is given by the content of **codingScheme**.
- **OutArea**: ID of the area exporting the flow (importing if the value is negative). Type is given by the content of **codingScheme**.
- **MeasureUnitQuantity**: Unit of the value. In the example above, "MAW".
- **TimeInterval**: time interval covered by the period.
- **Resolution**: the resolution of the data. In the example above, "PT60M" means we have one value every 60 minutes.
- **Interval**: contains the **Pos** (position, in the example above, "1" means the first interval, ie from the start  
  of **TimeInterval** to **TimeInterval** + **Resolution**) and **Qty** (the value of the exchange).
