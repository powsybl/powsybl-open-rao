# Virtual hubs

In OpenRAO, virtual hubs are used to compute loop-flows. Virtual hubs are one-node areas which should be considered as market areas when calculating loop-flows.

They are defined in a specific configuration file. For example :

~~~xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Configuration>
    <MarketAreas>
        <MarketArea Code="MA1" Eic="MA1_EIC" MCParticipant="false"/>
        <MarketArea Code="MA2" Eic="MA2_EIC" MCParticipant="true"/>
        <MarketArea Code="MA3" Eic="MA3_EIC" MCParticipant="true"/>
    </MarketAreas>
    <VirtualHubs>
        <VirtualHub Eic="EIC1" Code="Virtual_Hub_MA1_1" RelatedMA="MA1" MCParticipant="true" NodeName="XNODE_1"/>
        <VirtualHub Eic="EIC2" Code="Virtual_Hub_MA1_2" RelatedMA="MA1" MCParticipant="false" NodeName="XNODE_2"/>
        <VirtualHub Eic="EIC3" Code="Virtual_Hub_MA2_MA1" RelatedMA="MA2" MCParticipant="true" NodeName="XNODE_3"/>
        <VirtualHub Eic="EIC4" Code="Virtual_Hub_MA2" RelatedMA="MA2" MCParticipant="false" NodeName="XNODE_4"/>
    </VirtualHubs>
</Configuration>

~~~

A virtual hub therefore has : 
- an EIC code (e.g. EIC1) 
- and an associated UCTE node (e.g. XNODE_1).

The EIC code can also be found in the [RefProg file](reference-program.md). The virtual hub is indeed referenced in the RefProg file with a given associated net position.
The EIC code can however not be found in the GLSK file, the GLSK of the virtual hub is implicitly a factor of 100% on the unique UCTE node of the hub.


