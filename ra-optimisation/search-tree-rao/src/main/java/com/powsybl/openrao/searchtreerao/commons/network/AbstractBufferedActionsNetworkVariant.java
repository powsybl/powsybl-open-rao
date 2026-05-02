package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Objects;

abstract class AbstractBufferedActionsNetworkVariant implements NetworkVariant {

    protected record WorkingVariant(String fromVariant, String newVariantId,
                                    AppliedRemedialActions appliedRemedialActions) {
    }

    protected final Network network;
    protected WorkingVariant workingVariant;

    protected AbstractBufferedActionsNetworkVariant(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    protected void checkWorkingVariantIsNotSet() {
        if (workingVariant != null) {
            throw new OpenRaoException("Working variant already set");
        }
    }

    @Override
    public void setWorkingVariant(String fromVariant, String newVariantId) {
        checkWorkingVariantIsNotSet();
        workingVariant = new WorkingVariant(fromVariant, newVariantId, new AppliedRemedialActions());
    }

    protected void checkWorkingVariantIsSet() {
        if (workingVariant == null) {
            throw new OpenRaoException("Working variant not set");
        }
    }

    @Override
    public void applyRangeAction(State state, RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        workingVariant.appliedRemedialActions.addAppliedRangeAction(state, rangeAction, setpoint);
    }

    @Override
    public void applyNetworkAction(State state, NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        System.out.println("ADD " + networkAction.getId() + " to " + workingVariant.newVariantId);
        workingVariant.appliedRemedialActions.addAppliedNetworkAction(state, networkAction);
    }
}
