# HVDC

## Definition

Hvdc Line are modelled using [HvdcLine of the iidm PowSyBl network model](inv:powsyblcore:*:*#hvdc-line) and an HVDC range action will always point to an HVDC Line.

An HVDC line can operate in **two modes**:
1. **Fixed Setpoint Mode**  
   In this mode, the power flow on the HVDC line is directly controlled via its **active power setpoint**.
2. **AC Emulation Mode**  
   The HVDC line operates in AC Emulation mode when the extension [**angle-droop active power control**](inv:powsyblecore:*:*#hvdc-angle-droop-active-power-control) is enabled. In this mode, the flow is determined by the **phase difference**:  
   $P = P_0 \cdot k (\phi_1 - \phi_2)$ and **ignores the active power setpoint**.

**Implication:**  

An HVDC Range Action move the active setpoint field of the HVDC Line. When a line is in AC Emulation mode, applying an **HVDC Range Action** has no effect because the line cannot be directly controlled via its active power setpoint. 
For the same reason, attempting to define a sensitivity for such a range action will therefore result in an **OLF error** see [open-load-flow doc](https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/sensitivity/getting_started.html)

## HVDC Range Action in each process

### Italy Nord Process (CSE Crac)

The HVDC line is disconnected and replaced by two injections, one on each side of the line, with opposite keys of 1 and -1).
By doing this the HVDC line is **always considered in fixed set point mode**. 

For more information on how the HVDC range action are handled see [Gridcapa IN Process](https://gridcapa.github.io/docs/process-documentation/in-cc/import-ec/process-description#pisa-hvdc-alignment)

For more information on how they are defined in Crac see [CSE Crac doc](../../../input-data/crac/cse.md#hvdc-range-actions).

=> All the problematics around ac emulation do not concern this process. As the HVDC range action in fixed setpoint mode works like a regular range action.

### SWE Process (CIM Crac) :

Usually, in this process we work with groups of 2 aligned HVDC range actions. 
These HVDC range actions are aligned, i.e. they share the same group ID. That means that they must have the same set-point.

For more information on how they are defined in Crac see [CIM Crac doc](../../../input-data/crac/cim.md#hvdc-range-actions).

For now this range action is only imported in auto instant.

## HVDC in Castor, the RAO algorithm

We now support the optimization of this range action in preventive and curative instant as well as auto !

### Managing HVDC Setpoints to Avoid Network Imbalance

> ⚠️ **Warning**
>
> When in the initial network, the HVDC line is in **AC Emulation mode**, the active power setpoint of the line will likely be random, as it does not impact the network balance.
>
> When AC Emulation is **deactivated**, the HVDC line follows its active power setpoint. If this setpoint is set to an extreme or unrealistic value, it can **unbalance the network**, potentially causing the **sensitivity calculations to fail to converge**.

To avoid network imbalance when deactivating AC Emulation, follow this procedure:
1. Run a **Load Flow** with the HVDC line in AC Emulation mode.
2. Record the **power flow** on the HVDC line from the network.
3. Deactivate AC Emulation.
4. Set the **active power setpoint** of the HVDC line to the previously recorded flow.

$\rightarrow$ This is only an issue if you need to run a sensi right after deactivation ac emulation without setting the setpoint of the HVDC line.


### Pre Treatment 

#### Creating acEmulationDeactivationAction
If an HVDC Range Action that uses an HVDC line in AC emulation mode is defined in the CRAC, a corresponding network action on this line called [**acEmulationDeactivationAction**](../../../input-data/crac/json.md#network-actions) with same usage rule is also created.
Only one **acEmulationDeactivationAction** is created per hvdcLine.

Introducing this network action enables the RAO to identify the optimal solution between two different situations:
1. Keeping the HVDC line in AC emulation mode, or
2. Switching to fixed setpoint mode and optimizing the setpoint through the MIP.

#### HVDC range action initially in ac emulation initial setpoint ?

We also run a loadflow at the very beginning of the rao to update the "initial setpoint" of an HVDC range action that starts in ac emulation mode. Now that
the initial setpoint of HVDC range action is read in the network during crac deserialisation, we have to do that to have an initial setpoint that makes a little more sense.

### HVDC Range Action optimization in a preventive and curative instant

This AcEmulationDeactivationAction network action is optimized like any other network action in preventive and curative instant.
1. At the root of each search tree (ie. preventive search tree, each curative contingency state search tree), a load flow. 
2. For a given leaf, if an **AcEmulationDeactivationAction** is applied, the active setpoint of the hvdc line is update in the network as explained in previous section.
3. The **HVDC Range Action** will be available in the MIP only if the HVDC line is *not* in AC emulation mode; otherwise, **the range action is filtered out at the before each MIP**. 

### HVDC Range Action in auto instant

Contrary to the other network action, the acEmulationDeactivationAction is not automatically applied !
Ac Emulation is deactivated only if we need to optimize/use the hvdc range action to secure automaton perimeter. 
In this case, we do as explained in [this section](#managing-hvdc-setpoints-to-avoid-network-imbalance), after updating the setpoint => a sensi is run and the 
HVDC range action is optimized like any other range action see [castor doc](../../castor.md).


## Warning

- The naming might be confusing, but an HVDC line can be in ac emulation mode even if the load flow is run in DC mode. These two notions are totally independent.
- The parameter `hvdcAcEmulation`: should in theory always be true see [powsybl-core doc](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/loadflow/configuration.html)
