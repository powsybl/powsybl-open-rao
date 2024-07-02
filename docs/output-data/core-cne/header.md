The header contains meta-information about the process.  
Refer to the [JAVA API](/output-data/core-cne/java-api.md) section for more details.  

Example:

~~~xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<CriticalNetworkElement_MarketDocument xsi:schemaLocation="iec62325-451-n-cne_v2_4_FlowBased_v04.xsd" xmlns="urn:iec62325.351:tc57wg16:451-n:cnedocument:2:4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <mRID>DOCUMENT_ID</mRID>
    <revisionNumber>1</revisionNumber>
    <type>B06</type>
    <process.processType>A48</process.processType>
    <sender_MarketParticipant.mRID codingScheme="A01">SENDER_ID</sender_MarketParticipant.mRID>
    <sender_MarketParticipant.marketRole.type>A44</sender_MarketParticipant.marketRole.type>
    <receiver_MarketParticipant.mRID codingScheme="A01">RECEIVER_ID</receiver_MarketParticipant.mRID>
    <receiver_MarketParticipant.marketRole.type>A36</receiver_MarketParticipant.marketRole.type>
    <createdDateTime>2021-12-30T15:09:43Z</createdDateTime>
    <time_Period.timeInterval>
        <start>2021-10-30T22:00Z</start>
        <end>2021-10-31T23:00Z</end>
    </time_Period.timeInterval>
    <domain.mRID codingScheme="A01">DOMAIN_ID</domain.mRID>
    <TimeSeries>
        <mRID>CNE_RAO_CASTOR-TimeSeries-1</mRID>
        <businessType>B54</businessType>
        <curveType>A01</curveType>
        <Period>
            <timeInterval>
                <start>2019-01-07T23:00Z</start>
                <end>2019-01-08T00:00Z</end>
            </timeInterval>
            <resolution>PT60M</resolution>
            <Point>
                <position>1</position>
...
~~~
