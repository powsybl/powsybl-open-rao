package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EagerNetworkVariant implements NetworkVariant {
    private final Network network;
    private final Set<String> createdWorkingVariantIds = new HashSet<>();

    public EagerNetworkVariant(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void createWorkingVariant(String fromVariant, String newVariantId) {
        if (!network.getVariantManager().getVariantIds().contains(newVariantId)) {
            network.getVariantManager().cloneVariant(fromVariant, newVariantId, true);
        }
        createdWorkingVariantIds.add(newVariantId);
        network.getVariantManager().setWorkingVariant(newVariantId);
    }

    @Override
    public void removeWorkingVariants() {
        for (String variantId : createdWorkingVariantIds) {
            network.getVariantManager().removeVariant(variantId);
        }
    }

    @Override
    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction).apply(network, setpoint);
    }

    @Override
    public boolean applyNetworkAction(NetworkAction networkAction) {
        return Objects.requireNonNull(networkAction).apply(network);
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        Objects.requireNonNull(sensitivityComputer).compute(network);
    }
}
