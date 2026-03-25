package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

import java.util.*;

public class LazyNetworkVariant implements NetworkVariant {
    record AppliedRangeAction(RangeAction<?> rangeAction, double setpoint) {
    }

    record WorkingVariant(String fromVariant, String newVariantId,
                          List<AppliedRangeAction> appliedRangeActions,
                          List<NetworkAction> networkActions) {
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
            // apply buffered actions
            for (AppliedRangeAction appliedRangeAction : workingVariant.appliedRangeActions) {
                appliedRangeAction.rangeAction.apply(network, appliedRangeAction.setpoint);
            }
            workingVariant.appliedRangeActions.clear();
            for (NetworkAction networkAction : workingVariant.networkActions) {
                boolean applicationSuccess =networkAction.apply(network);
                if (!applicationSuccess) {
                    throw new OpenRaoException(String.format("%s could not be applied on the network", networkAction.getId()));
                }
            }
            workingVariant = null;
        }
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setWorkingVariant(String fromVariant, String newVariantId) {
        workingVariant = new WorkingVariant(fromVariant, newVariantId, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public void removeWorkingVariants() {
        for (String variantId : createdWorkingVariantIds) {
            network.getVariantManager().removeVariant(variantId);
        }
    }

    private void checkWorkingVariantIsSet() {
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

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        Objects.requireNonNull(sensitivityComputer);
        ensureWorkingVariantIsCreated();
        sensitivityComputer.compute(network);
    }
}
