package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.*;

public class LazyNetworkVariant extends AbstractBufferedActionsNetworkVariant {
    private final Set<String> createdWorkingVariantIds = new HashSet<>();

    public LazyNetworkVariant(Network network) {
        super(network);
    }

    private void ensureWorkingVariantIsCreated() {
        if (workingVariant != null) {
            if (!network.getVariantManager().getVariantIds().contains(workingVariant.newVariantId())) {
                network.getVariantManager().cloneVariant(workingVariant.fromVariant(), workingVariant.newVariantId(), true);
            }
            createdWorkingVariantIds.add(workingVariant.newVariantId());
            network.getVariantManager().setWorkingVariant(workingVariant.newVariantId());
            // apply buffered actions
            for (AppliedRangeAction appliedRangeAction : workingVariant.appliedRangeActions()) {
                appliedRangeAction.rangeAction().apply(network, appliedRangeAction.setpoint());
            }
            workingVariant.appliedRangeActions().clear();
            for (NetworkAction networkAction : workingVariant.networkActions()) {
                boolean applicationSuccess =networkAction.apply(network);
                if (!applicationSuccess) {
                    throw new OpenRaoException(String.format("%s could not be applied on the network", networkAction.getId()));
                }
            }
            workingVariant = null;
        }
    }

    @Override
    public void removeWorkingVariants() {
        for (String variantId : createdWorkingVariantIds) {
            network.getVariantManager().removeVariant(variantId);
        }
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        Objects.requireNonNull(sensitivityComputer);
        ensureWorkingVariantIsCreated();
        sensitivityComputer.compute(network);
    }
}
