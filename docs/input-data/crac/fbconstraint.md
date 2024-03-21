# FlowBasedConstraint CRAC format

## Header overview

```xml
<FlowBasedConstraintDocument DtdRelease="4" DtdVersion="0" xmlns="flowbased" xmlns:xsi="..." xsi:noNamespaceSchemaLocation="...">
  <DocumentIdentification v="DOCUMENT_ID"/>
  <DocumentVersion v="1"/>
  <DocumentType v="B06"/>
  <ProcessType v="A01"/>
  <SenderIdentification codingScheme="A01" v="SENDER_EIC"/>
  <SenderRole v="A36"/>
  <ReceiverIdentification codingScheme="A01" v="RECEIVER_EIC"/>
  <ReceiverRole v="A44"/>
  <CreationDateTime v="2019-08-15T14:07:28Z"/>
  <ConstraintTimeInterval v="2019-01-07T23:00Z/2019-01-08T23:00Z"/>
  <Domain codingScheme="A01" v="REGION_EIC"/>
  ...
</FlowBasedConstraintDocument>
```
A flow-based constraint document has a time interval for its validity and a lot of its sub-objects have their own time 
interval of validity as well. Therefore, **this document has to be imported for a specific datetime** – hourly-precise – 
to be able to select only the available elements for this datetime.  

## Critical branches

```xml
<criticalBranch id="de2_nl3_N">
  <timeInterval v="2019-01-07T23:00Z/2019-01-08T23:00Z"/>
  <branch eic="1234567890123450" from="DDE2AA11" name="DDE2AA11 NNL3AA11 1" order="1" to="NNL3AA11"/>
  <imaxFactor>1</imaxFactor>
  <imaxType>SEASONAL</imaxType>
  <permanentImaxFactor>1</permanentImaxFactor>
  <temporaryImaxFactor>1.1</temporaryImaxFactor>
  <frmMw>100</frmMw>
  <minRAMfactor>75</minRAMfactor>
  <CNEC>true</CNEC>
  <MNEC>false</MNEC>
  <direction>DIRECT</direction>
  <tsoOrigin>DE</tsoOrigin>
</criticalBranch>
```
A critical branch represents a CNEC. As it is an identifiable it has a unique ID, "de2_nl3_N" in the example above.  

As it has been indicated previously, a critical branch has a time interval of validity, so it will be imported only if 
the import datetime is contained within the time interval of validity.  

```xml
<criticalBranch id="fr4_de1_CO1">
  <timeInterval v="2019-01-07T23:00Z/2019-01-08T23:00Z"/>
  ...
  <outage id="CO1_fr2_fr3_1" location="FR-FR" name="co1_fr2_fr3_1">
      <branch eic="1234567890123450" from="FFR2AA1 " to="FFR3AA1 " order="1"/>
  </outage>
</criticalBranch>
```

As it is defined in the CRAC model a CNEC is associated to a state. If the &lt;outage&gt; tag is not present as it is in 
the first example the CNEC is associated to the "preventive state" otherwise **two different CNECs will be created** one 
on state ("Outage", contingency_id) and another one on state ("Curative", contingency_id). These two CNECs will be on the 
same network element but will have different thresholds as it is explained in the **thresholds section**. As two CNECs are 
created from a unique CNEC with its ID, a suffix is added to the IDs of the new CNECs " - Outage" and " - Curative".  

### Branch definition

In the definitions of critical branches or outages appear the &lt;branch&gt; tag, it can be quite singular. This type of 
CRAC has to be associated with a network in UCTE format therefore a branch is designated by two nodes – from and to – that 
are UCTE nodes and an order code which is defined in the UCTE format literature. The name represents only a more 
human-readable name but there is no guaranty on its unicity.  

A branch can also be defined this way :

```xml
<branch eic="RANDOM_EIC" from="FROM__21" to="TO____21"  elementName="NAME" name="[FR-DE] NAME OF CRITICAL BRANCH [DIR]"/>
```

Instead of using order code as third identifier, the element name can be used as it is defined in the UCTE format 
literature as well. This implies that network elements from the IIDM network have to be identifiable with two different 
type of IDs "fromNode toNode orderCode" and "fromNode toNode elementName". This is handled by the aliases, a network 
element has its ID "fromNode toNode orderCode" when it's imported from UCTE format, but aliases can be created to 
identify it with other names.  

Another problem is that from/to nodes can be inverted in the CRAC compared to what is present in the network. Such 
branches are correctly identified when the file is imported, but appears to be inverted in the resulting CRAC - meaning 
that their flow sign might be different between the 'CORE-definition' of the branch and the 'FARAO-definition' of the 
branch. The inversion of the branch is tracked in the [CracCreationContext](creation-context#fbconstraint) to handle 
properly the sign of the flow when the results of the RAO are exported.

### Thresholds

Thresholds can be defined in several ways in this format. There are two types of thresholds – permanent and temporary. 
In our model  permanent limits make sense on preventive and curative states whereas temporary limits make sense only 
the outage states.  

Permanent thresholds:  
- &lt;imaxA&gt; and &lt;imaxFactor&gt; : the first one is an absolute threshold in Amps the second one is a percentage 
  of Imax as a limit – Imax being defined for each line in the UCTE network file.
- &lt;permanentImaxA&gt; and &lt;permanentImaxFactor&gt; : same definition of absolute and relative limits as defined previously.  
  
Both tags can be present, they will be added, the most limiting will be the effective one during the optimisation.  

Temporary thresholds:  
- &lt;temporaryImaxA&gt; and &lt;temporaryImaxFactor&gt; : same definitions  
  
To define thresholds we also have to take &lt;direction&gt; tag into account. As mentioned in the model a network 
element has a direction – from/to node – then a threshold can be effective only for positive values of the flow for this 
direction for example, this is the definition of DIRECT. If the value is OPPOSITE the threshold will be effective only 
for negative values of the flow on this line. And eventually the value BOTH will make it effective for both negative 
and positive values.  

### Additional values

- An FRM value in specified for each CNEC, its value is in MW. Then the effective threshold value for the optimiser will 
  be the one of the threshold diminished by the FRM value. Several conversions might take place whether we want the 
  threshold in A or in MW.
- A critical branch can be either a CNEC (optimised) or a MNEC (monitored) or actually both of these. For those that are 
  neither a CNEC nor a MNEC they will be ignored.
- TSO origin for the critical branch is also specified. As tie-lines are usually defined in two parts in the CRAC there 
  will always be only one TSO implied per CNEC.

## Remedial actions

```xml
<complexVariant id="01_REMEDIAL_ACTION_FR" name="OPEN_LINE_A">
    <timeInterval v="2021-01-15T23:00Z/2021-01-16T23:00Z"/>
    <tsoOrigin>FR</tsoOrigin>
    <actionsSet >
        <preventive>true</preventive>
        <curative>false</curative>
        <enforced>false</enforced>
        ...
    </actionsSet>
</complexVariant>
``` 

Remedial actions can be of different types, but they will always have :
- An ID
- A human-readable name
- Its TSO origin

Then it has the &lt;actionSet&gt; tag :
- &lt;preventive&gt; tag : if true a usage rule will be added to the remedial action to make it available on preventive state
- &lt;curative&gt; tag : if true some usage rules will be added to the remedial action to make it available on specified 
  curative states. To specify these states &lt;afterCOId&gt; tag will be used, they defined the outages after which this 
  remedial action will be available.
- &lt;enforced&gt; tag : it is not used for now  
  
Eventually different &lt;action&gt; tags can be used to define the concrete actions on the network for the remedial action.

### Network actions
```xml
<action type="STATUS">
    <branch from="FROM__11" to="TO____11"  elementName="NAME1"/>
    <value>OPEN</value>
</action>
```
Network actions are defined with the type "STATUS". The information they need is:
- The network element that is modified
- The type of action (OPEN/CLOSE)

### Range actions
```xml
<action type="PSTTAP">
    <branch from="FROM__22" to="TO____22"  elementName="NAME2"/>
    <range>
        <min>-10</min>
        <max>10</max>
    </range>
    <PSTGroupId>PST_G1</PSTGroupId>
</action>
```
Only PST range actions can be defined in FlowBasedConstraint documents. They are fully defined using:
- Their network element
- Their allowed tap range
- Eventually, if they belong to a group of aligned PSTs, the ID of the group 


### Special rules
In order to ensure the imported CRAC is usable in the RAO, FARAO implements these special rules for FlowBasedConstraint documents:
- If multiple PST remedial actions are defined for the same network element and the same state, only one is imported (priority is given to PSTs that have a group ID defined)
- FARAO adds LoopFlow constraints for all critical branches with names ending with "[XX]" (where "XX" is a country code), even if the critical branch is internal to a country

