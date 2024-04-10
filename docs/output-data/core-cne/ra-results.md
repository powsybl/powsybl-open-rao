In addition to being reported inside CNECs' Constraint_Series, remedial actions have their own Constraint_Series 
to report the ones selected by the RAO.  
The remedial actions' Constraint series all have a **B56 businessType**.

Example:

~~~xml
<Constraint_Series>
    <mRID>RAseries_27295</mRID>
    <businessType>B56</businessType>
    <Contingency_Series>
        <mRID>Co_one</mRID>
        <name>Co_one</name>
    </Contingency_Series>
    <RemedialAction_Series>
        <mRID>pst_one</mRID>
        <name>pst_one</name>
        <applicationMode_MarketObjectStatus.status>A19</applicationMode_MarketObjectStatus.status>
        <Party_MarketParticipant>
            <mRID codingScheme="A01">10X1001A1001A094</mRID>
        </Party_MarketParticipant>
        <RegisteredResource>
            <mRID codingScheme="A01">pst_one</mRID>
            <name>FPRAGN22 FDEPHT21 1</name>
            <pSRType.psrType>A06</pSRType.psrType>
            <marketObjectStatus.status>A26</marketObjectStatus.status>
            <resourceCapacity.defaultCapacity>-8</resourceCapacity.defaultCapacity>
            <resourceCapacity.unitSymbol>C62</resourceCapacity.unitSymbol>
        </RegisteredResource>
    </RemedialAction_Series>
    <RemedialAction_Series>
        <mRID>network_three</mRID>
        <name>network_three</name>
        <applicationMode_MarketObjectStatus.status>A18</applicationMode_MarketObjectStatus.status>
        <Party_MarketParticipant>
            <mRID codingScheme="A01">10XFR-RTE------Q</mRID>
        </Party_MarketParticipant>
    </RemedialAction_Series>
...
~~~

#### mRID

Random ID, unique in the CNE document.

#### businessType

Always **B56**, to signify that this is a remedial-action Constraint_Series.

#### Contingency_Series (optional)

If this tag exists, then this Constraint_Series reports selected **curative** remedial actions by the RAO, after the 
contingency defined in this tag:
- **mRID**: unique ID of the contingency as it is defined in the native CRAC
- **name**: name of the contingency as it is defined in the native CRAC

#### RemedialAction_Series

##### mRID

Unique ID of the selected remedial action, as identified in the original CRAC.

##### name

Name of the selected remedial action, as identified in the original CRAC.

##### applicationMode_MarketObjectStatus.status (optional)

If this tag exists, it can have one of two values:
- **A18**: the remedial action was selected in preventive
- **A19**: the remedial action was selected in curative

If it doesn't exist, it means that the following describes the remedial action **before** optimisation (only useful 
to know the initial set-points of range actions).

##### Party_MarketParticipant

[ENTSO-E EICode](https://www.entsoe.eu/data/energy-identification-codes-eic/) of the remedial action's operator (in mRID tag).

##### RegisteredResource (optional)

This tag is only exported for **range actions**, to hold their set-point values.

##### mRID

Unique ID of the selected remedial action, as identified in the original CRAC (same as mRID of RemedialAction_Series).

##### name

Identifier of range action's UCTE network element.

##### pSRType.psrType

Only one possible value for now:
- **A06**: remedial action is a PST

##### resourceCapacity.defaultCapacity

Value of the range action's set-point (tap position for a PST range action).  
- If **applicationMode_MarketObjectStatus.status** tag does not exist, this is the remedial action's initial set-point 
  in the network.
- If it does and is equal to **A18**, this is the optimal preventive set-point.
- If it does and is equal to **A19**, this is the optimal curative set-point, for contingency defined in 
  **Contingency_Series** above.

##### resourceCapacity.unitSymbol

Unit of the set-point given in **resourceCapacity.defaultCapacity**. Only one supported value for now:
- **C62**: dimensionless
