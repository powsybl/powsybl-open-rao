package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.PstSetpointAdder;
import com.farao_community.farao.data.crac_api.RangeDefinition;

import java.util.Objects;

public class PstSetpointImplAdder implements PstSetpointAdder {

    private NetworkActionImplAdder ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private Double setpoint;
    private RangeDefinition rangeDefinition;

    PstSetpointImplAdder(NetworkActionImplAdder ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public PstSetpointAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public PstSetpointAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public PstSetpointAdder withSetpoint(double setPoint) {
        this.setpoint = setPoint;
        return this;
    }

    @Override
    public PstSetpointAdder withRangeDefinition(RangeDefinition rangeDefinition) {
        this.rangeDefinition = rangeDefinition;
        return this;
    }

    @Override
    public NetworkActionAdder add() {

        if (Objects.isNull(networkElementId)) {
            throw new FaraoException("A network element should be defined when adding a PstSetPoint");
        }

        if (Objects.isNull(setpoint)) {
            throw new FaraoException("A setpoint should be defined when adding a PstSetPoint");
        }

        if (Objects.isNull(rangeDefinition)) {
            throw new FaraoException("A range definition should be defined when adding a PstSetPoint");
        }

        NetworkElement networkElement;
        if (Objects.isNull(networkElementName)) {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId);
        } else {
            networkElement = this.ownerAdder.getOwner().addNetworkElement(networkElementId, networkElementName);
        }

        PstSetpointImpl pstSetpoint = new PstSetpointImpl(networkElement, setpoint, rangeDefinition);
        ownerAdder.addElementaryAction(pstSetpoint);
        return ownerAdder;
    }
}
