package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
            for (State state : workingVariant.appliedRemedialActions().getStatesWithRa(network)) {
                workingVariant.appliedRemedialActions().applyOnNetwork(state, network);
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
