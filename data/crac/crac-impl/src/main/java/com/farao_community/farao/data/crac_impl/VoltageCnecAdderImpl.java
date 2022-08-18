/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.threshold.VoltageThresholdAdder;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecAdderImpl extends AbstractCnecAdderImpl<VoltageCnecAdder> implements VoltageCnecAdder {

    private final Set<Threshold> thresholds = new HashSet<>();
    private static final String CNEC_TYPE = "VoltageCnec";

    VoltageCnecAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public VoltageCnecAdder withNetworkElement(String networkElementId, String networkElementName) {
        if (!this.networkElementsIdAndName.entrySet().isEmpty()) {
            throw new FaraoException("Cannot add multiple network elements for a voltage cnec.");
        }
        super.withNetworkElement(networkElementId, networkElementName);
        return this;
    }

    @Override
    public VoltageThresholdAdder newThreshold() {
        return new VoltageThresholdAdderImpl(this);
    }

    void addThreshold(ThresholdImpl threshold) {
        thresholds.add(threshold);
    }

    @Override
    protected String getTypeDescription() {
        return CNEC_TYPE;
    }

    @Override
    public VoltageCnec add() {
        checkCnec();

        if (optimized) {
            throw new FaraoException(format("Error while adding cnec %s : Farao does not allow the optimization of VoltageCnecs.", id));
        }

        checkAndInitThresholds();

        State state = getState();

        VoltageCnec cnec = new VoltageCnecImpl(id, name,
            owner.getNetworkElement(networkElementsIdAndName.keySet().iterator().next()),
            operator, state, optimized, monitored,
            thresholds, reliabilityMargin);

        owner.addVoltageCnec(cnec);
        return cnec;
    }

    private void checkAndInitThresholds() {
        /*
         This should be done here, and not in Threshold Adder, as some information of the VoltageCnec is required
         to perform those checks
         */

        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add an VoltageCnec without a threshold. Please use newThreshold");
        }

        if (this.thresholds.stream().anyMatch(th -> !th.getUnit().equals(Unit.KILOVOLT))) {
            throw new FaraoException("VoltageCnec threshold must be in KILOVOLT");
        }
    }
}
