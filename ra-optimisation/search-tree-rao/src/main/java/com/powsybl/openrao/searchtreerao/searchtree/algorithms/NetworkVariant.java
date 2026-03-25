package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

public interface NetworkVariant {

    Network getNetwork();

    void createWorkingVariant(String fromVariant, String newVariantId);

    void removeWorkingVariants();

    void applyRangeAction(RangeAction<?> rangeAction, double setpoint);

    boolean applyNetworkAction(NetworkAction networkAction);

    void compute(SensitivityComputer sensitivityComputer);
}
