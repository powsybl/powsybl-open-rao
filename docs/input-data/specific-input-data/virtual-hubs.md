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

The UCTE node is optional. If not provided, the `<VirtualHub>` element will be ignored.


## Internal HVDCs

~~~xml
<Configuration>
    <MarketAreas>
        <MarketArea Code="MA1" Eic="MA1_EIC" MCParticipant="false"/>
    </MarketAreas>
    <InternalHVDCs>
        <HVDC Eic="HVDC-EIC" Code="HVDC-CODE" RelatedMA="MA1" MTHVDC="false" Bipolar="true">
            <pole id="1">
                <converterList>
                    <converter node="NODE__1A" station="St1"/>
                    <converter node="NODE__1B" station="St2"/>
                </converterList>
                <lineList>
                    <line id="Line_1" from="NODE__1A" to="NODE__1B"/>
                </lineList>
            </pole>
            <pole id="2">
                <converterList>
                    <converter node="NODE__2A" station="St1"/>
                    <converter node="NODE__2B" station="St2"/>
                </converterList>
                <lineList>
                    <line id="Line_2" from="NODE__2A" to="NODE__2B"/>
                </lineList>
            </pole>
        </HVDC>
    </InternalHVDCs>
    ...
</Configuration>
~~~

For German HVDC remedial actions, specific data must be defined in the `<internalHVDCs>` section of the VirtualHubs file
along with some data in the [FlowBasedConstraint CRAC file](../crac/fbconstraint.md#hvdc-range-actions).

Each **HVDC line** is represented by an `<HVDC>` element with required attributes:
- an `Eic` code (e.g. HVDC-EIC);
- a `Code` (e.g. HVDC-CODE);
- a `RelatedMA` (e.g. MA1) which represents the physical market area where the internal HVDC is connected; it must match the `Code` attribute of one of the `<MarketArea>` elements defined above;
- a `MTHVDC` boolean attribute which is **true** for multiterminal HVDC or **false** for Point-to-Point HVDC;
- a `Bipolar` boolean attribute which is **true** for bipolar HVDC or **false** for monopolar HVDC.

`<HVDC>` elements also contain one or two `<pole>` elements, depending on whether the HVDC is monopolar or bipolar.

Each **pole** has an `id` attribute (e.g. 1 and 2) and contains a list of converters and a list of lines.

`<converterList>` must contain at least two `<converter>`, which represent the ends of the line and are defined with:
- a `node` attribute (e.g. NODE__1A) which must match a node code of the UCTE network
- and a `station` attributes (e.g. St1) .

> ℹ️  **NOTE**  
> Converters that share the same `station` attribute will be aligned.
> It means that nodes of that station will share the same setpoint value at each time.

`<lineList>` must contain at least one `<line>`, which is defined with:
- an `id` (e.g. Line_1)
- a `from` and a `to` attribute (e.g. NODE__1A and NODE__1B) representing the ends of the line and must match the `node` attribute of one of the `<converter>` defined above.

