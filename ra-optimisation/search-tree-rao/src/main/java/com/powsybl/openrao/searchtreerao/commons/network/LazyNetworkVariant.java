package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LazyNetworkVariant implements NetworkVariant {
    record WorkingVariant(String fromVariant, String newVariantId) {
    }

    private final Network network;
    private WorkingVariant workingVariant;
    private final Set<String> createdWorkingVariantIds = new HashSet<>();

    public LazyNetworkVariant(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    private void ensureWorkingVariantIsCreated() {
        if (workingVariant != null) {
            if (!network.getVariantManager().getVariantIds().contains(workingVariant.newVariantId)) {
                network.getVariantManager().cloneVariant(workingVariant.fromVariant, workingVariant.newVariantId, true);
            }
            createdWorkingVariantIds.add(workingVariant.newVariantId);
            network.getVariantManager().setWorkingVariant(workingVariant.newVariantId);
        }
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setWorkingVariant(String fromVariant, String newVariantId) {
        workingVariant = new WorkingVariant(fromVariant, newVariantId);
    }

    @Override
    public void removeWorkingVariants() {
        for (String variantId : createdWorkingVariantIds) {
            network.getVariantManager().removeVariant(variantId);
        }
    }

    @Override
    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        ensureWorkingVariantIsCreated();
        Objects.requireNonNull(rangeAction).apply(network, setpoint);
    }

    @Override
    public boolean applyNetworkAction(NetworkAction networkAction) {
        ensureWorkingVariantIsCreated();
        return Objects.requireNonNull(networkAction).apply(network);
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        ensureWorkingVariantIsCreated();
        Objects.requireNonNull(sensitivityComputer).compute(network);
    }
}
