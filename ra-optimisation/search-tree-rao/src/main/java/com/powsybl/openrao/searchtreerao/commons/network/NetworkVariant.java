package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;

public interface NetworkVariant {

    Network getNetwork();

    void setWorkingVariant(String fromVariant, String newVariantId);

    void removeWorkingVariants();

    void applyRangeAction(State state, RangeAction<?> rangeAction, double setpoint);

    void applyNetworkAction(State state, NetworkAction networkAction);

    void compute(SensitivityComputer sensitivityComputer);
}
