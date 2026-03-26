package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.*;

abstract class AbstractBufferedActionsNetworkVariant implements NetworkVariant {

    protected record AppliedRangeAction(RangeAction<?> rangeAction, double setpoint) {
    }

    protected record WorkingVariant(String fromVariant, String newVariantId,
                          List<AppliedRangeAction> appliedRangeActions,
                          List<NetworkAction> networkActions) {
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
        workingVariant = new WorkingVariant(fromVariant, newVariantId, new ArrayList<>(), new ArrayList<>());
    }

    protected void checkWorkingVariantIsSet() {
        if (workingVariant == null) {
            throw new OpenRaoException("Working variant not set");
        }
    }

    @Override
    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        workingVariant.appliedRangeActions.add(new AppliedRangeAction(rangeAction, setpoint));
    }

    @Override
    public void applyNetworkAction(NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        workingVariant.networkActions.add(networkAction);
    }
}
