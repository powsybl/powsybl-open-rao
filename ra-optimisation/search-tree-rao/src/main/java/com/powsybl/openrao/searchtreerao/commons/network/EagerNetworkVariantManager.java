package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EagerNetworkVariantManager implements NetworkVariantManager {
    private final Network network;
    private final Set<String> createdWorkingVariantIds = new HashSet<>();

    public EagerNetworkVariantManager(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setWorkingVariant(String fromVariant, String newVariantId) {
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
    public void applyRangeAction(State state, RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction).apply(network, setpoint);
    }

    @Override
    public void applyNetworkAction(State state, NetworkAction networkAction) {
        boolean applicationSuccess = Objects.requireNonNull(networkAction).apply(network);
        if (!applicationSuccess) {
            throw new OpenRaoException(String.format("%s could not be applied on the network", networkAction.getId()));
        }
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        Objects.requireNonNull(sensitivityComputer).compute(network);
    }
}
