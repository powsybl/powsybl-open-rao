package com.farao_community.farao.data.crac_api;


/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface PstSetpointAdder {

    PstSetpointAdder withNetworkElement(String networkElementId, String networkElementName);

    PstSetpointAdder withNetworkElement(String networkElementId);

    PstSetpointAdder withSetpoint(double setPoint);

    PstSetpointAdder withRangeDefinition(RangeDefinition rangeDefinition);

    NetworkActionAdder add();
}
