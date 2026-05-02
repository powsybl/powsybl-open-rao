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

import java.util.*;

public class VirtualNetworkVariantManager implements NetworkVariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualNetworkVariantManager.class);

    protected record VirtualVariant(String variantId, AppliedRemedialActions appliedRemedialActions) {
    }

    private final Network network;
    private final Map<String, VirtualVariant> variantsById = new HashMap<>();
    private VirtualVariant workingVariant;

    public VirtualNetworkVariantManager(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setWorkingVariant(String fromVariant, String newVariantId) {
        VirtualVariant variant = variantsById.get(newVariantId);
        if (variant != null) {
            workingVariant = variant;
        } else {
            if (network.getVariantManager().getVariantIds().contains(fromVariant)) {
                LOGGER.info("Create virtual variant '" + newVariantId + "' from variant '" + fromVariant + "'");
                workingVariant = new VirtualVariant(newVariantId, new AppliedRemedialActions());
                variantsById.put(newVariantId, workingVariant);
            } else {
                throw new OpenRaoException("Cannot set working variant from " + fromVariant + " to " + newVariantId + ": variant not found");
            }
        }
    }

    @Override
    public void removeWorkingVariants() {
        LOGGER.info("Remove all virtual variants");
        variantsById.clear();
        workingVariant = null;
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
        LOGGER.info("Add range action '" + rangeAction.getId() + "' to virtual variant '" + workingVariant.variantId + "'");
        workingVariant.appliedRemedialActions.addAppliedRangeAction(state, rangeAction, setpoint);
    }

    @Override
    public void applyNetworkAction(State state, NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        LOGGER.info("Add network action '" + networkAction.getId() + "' to virtual variant '" + workingVariant.variantId + "'");
        workingVariant.appliedRemedialActions.addAppliedNetworkAction(state, networkAction);
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        sensitivityComputer.compute(network, workingVariant.appliedRemedialActions());
    }
}
