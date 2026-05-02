package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LazyNetworkVariantManager implements NetworkVariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LazyNetworkVariantManager.class);

    protected record WorkingVariant(String fromVariant, String newVariantId,
                                    AppliedRemedialActions appliedRemedialActions) {
    }

    private final Network network;
    private WorkingVariant workingVariant;
    private final Set<String> createdWorkingVariantIds = new HashSet<>();

    public LazyNetworkVariantManager(Network network) {
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

    protected void checkWorkingVariantIsSet() {
        if (workingVariant == null) {
            throw new OpenRaoException("Working variant not set");
        }
    }

    @Override
    public void applyRangeAction(State state, RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        LOGGER.info("Add range action '" + rangeAction.getId() + "' to variant '" + workingVariant.newVariantId + "'");
        workingVariant.appliedRemedialActions.addAppliedRangeAction(state, rangeAction, setpoint);
    }

    @Override
    public void applyNetworkAction(State state, NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        LOGGER.info("Add network action '" + networkAction.getId() + "' to variant '" + workingVariant.newVariantId + "'");
        workingVariant.appliedRemedialActions.addAppliedNetworkAction(state, networkAction);
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        Objects.requireNonNull(sensitivityComputer);
        ensureWorkingVariantIsCreated();
        sensitivityComputer.compute(network);
    }
}
