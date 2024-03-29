The header contains meta-information about the process.  
Refer to the [JAVA API](#java-api) section for more details.  

Example:

~~~xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<CriticalNetworkElement_MarketDocument xsi:schemaLocation="iec62325-451-n-cne_v2_3.xsd" xmlns="urn:iec62325.351:tc57wg16:451-n:cnedocument:2:3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <mRID>documentId</mRID>
    <revisionNumber>3</revisionNumber>
    <type>B06</type>
    <process.processType>A48</process.processType>
    <sender_MarketParticipant.mRID codingScheme="A01">senderId</sender_MarketParticipant.mRID>
    <sender_MarketParticipant.marketRole.type>A44</sender_MarketParticipant.marketRole.type>
    <receiver_MarketParticipant.mRID codingScheme="A01">receiverId</receiver_MarketParticipant.mRID>
    <receiver_MarketParticipant.marketRole.type>A36</receiver_MarketParticipant.marketRole.type>
    <createdDateTime>2022-09-28T14:47:09Z</createdDateTime>
    <time_Period.timeInterval>
        <start>2021-04-02T00:30Z</start>
        <end>2021-04-02T01:30Z</end>
    </time_Period.timeInterval>
    <TimeSeries>
        <mRID>CNE_RAO_CASTOR-TimeSeries-1</mRID>
        <businessType>B54</businessType>
        <curveType>A01</curveType>
        <Period>
            <timeInterval>
                <start>2021-04-02T03:00Z</start>
                <end>2021-04-02T04:00Z</end>
            </timeInterval>
            <resolution>PT60M</resolution>
            <Point>
                <position>1</position>
...
~~~
