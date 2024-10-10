Remedial actions have their own Constraint_Series to report the ones selected by the RAO **or by the 
[monitoring module](/castor/monitoring/monitoring.md)** while using **ANGLE** as Physical parameter.     
The remedial actions' Constraint_Series all have a **B56 businessType**.

Example:

~~~xml
<Constraint_Series>
  <mRID>542c55e-ac23-49a9-b26f-097081883e7f</mRID>
  <businessType>B56</businessType>
  <Contingency_Series>
    <mRID>Co-1</mRID>
    <name>contingency one</name>
  </Contingency_Series>
  <RemedialAction_Series>
    <mRID>RA-19</mRID>
    <name>topological remedial action 19</name>
    <applicationMode_MarketObjectStatus.status>A20</applicationMode_MarketObjectStatus.status>
  </RemedialAction_Series>
  <RemedialAction_Series>
    <mRID>RA-192@600@</mRID>
    <name>HVDC remedial action@600@</name>
    <applicationMode_MarketObjectStatus.status>A20</applicationMode_MarketObjectStatus.status>
  </RemedialAction_Series>
  <RemedialAction_Series>
    <mRID>RA-45</mRID>
    <name>PST remedial action 45</name>
    <applicationMode_MarketObjectStatus.status>A20</applicationMode_MarketObjectStatus.status>
    <RegisteredResource>
      <mRID codingScheme="A01">pst_in_network@-5@</mRID>
      <name>PST remedial action 45</name>
      <pSRType.psrType>A06</pSRType.psrType>
      <marketObjectStatus.status>A26</marketObjectStatus.status>
      <resourceCapacity.defaultCapacity>-5</resourceCapacity.defaultCapacity>
      <resourceCapacity.unitSymbol>C62</resourceCapacity.unitSymbol>
      <marketObjectStatus.status>A26</marketObjectStatus.status>
    </RegisteredResource>
  </RemedialAction_Series>
...
~~~

#### mRID

Random ID, unique in the CNE document.

#### businessType

Always **B56**, to signify that this is a remedial-action Constraint_Series.

#### Contingency_Series (optional)

If this tag exists, then this Constraint_Series reports selected **automatic** or **curative** remedial actions by 
the RAO or by the angle monitoring module, after the contingency defined in this tag:
- **mRID**: unique ID of the contingency as it is defined in the native CRAC
- **name**: name of the contingency as it is defined in the native CRAC

#### RemedialAction_Series

##### mRID

Unique ID of the selected remedial action, as identified in the original CRAC.
> 💡  **NOTE**  
> If it is an HVDC range action, this field is followed by @setpoint@, where "setpoint" is the optimal set-point
> selected for the HVDC, in megawatts (see example above)

##### name

Name of the selected remedial action, as identified in the original CRAC.
> 💡  **NOTE**  
> If it is an HVDC range action, this field is followed by @setpoint@, where "setpoint" is the optimal set-point
> selected for the HVDC, in megawatts (see example above)

##### applicationMode_MarketObjectStatus.status

This tag can have one of these three values:
- **A18**: the remedial action was selected in preventive
- **A20**: the remedial action was selected as automatic
- **A19**: the remedial action was selected in curative

##### RegisteredResource (optional)

This tag is only exported for **PST remedial actions**, to hold their tap values.

##### mRID

Unique ID of the remedial action's PST network element in the network, followed by "@tap@", where tap is its optimal 
tap selected by the RAO or by the angle monitoring module.

##### name

Name of the selected remedial action, as identified in the original CRAC.

##### pSRType.psrType

Only one possible value for now:
- **A06**: remedial action is a PST

##### resourceCapacity.defaultCapacity

Optimal tap position for the PST (at instant identified by **applicationMode_MarketObjectStatus.status**, eventually after 
contingency identified by **Contingency_Series**).

##### resourceCapacity.unitSymbol

Unit of the tap given in **resourceCapacity.defaultCapacity**. Only one supported value for now:
- **C62**: dimensionless

##### marketObjectStatus.status

Only one supported value for now:
- **A26**: the tap is given in absolute value (not relatively to initial network, nor to previous instant, ...)
