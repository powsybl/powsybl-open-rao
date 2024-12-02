/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.AngleThresholdAdder;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class AngleCnecAdderImpl extends AbstractCnecAdderImpl<AngleCnecAdder> implements AngleCnecAdder {

    private final Set<Threshold> thresholds = new HashSet<>();
    private String exportingNetworkElementId;
    private String importingNetworkElementId;
    private static final String CNEC_TYPE = "AngleCnec";

    AngleCnecAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public AngleCnecAdder withNetworkElement(String networkElementId) {
        throw new OpenRaoException("For an angle cnec, use withExportingNetworkElement() and withImportingNetworkElement().");
    }

    @Override
    public AngleCnecAdder withNetworkElement(String networkElementId, String networkElementName) {
        throw new OpenRaoException("For an angle cnec, use withExportingNetworkElement() and withImportingNetworkElement().");
    }

    @Override
    public AngleCnecAdder withExportingNetworkElement(String exportingNetworkElementId) {
        this.withExportingNetworkElement(exportingNetworkElementId, exportingNetworkElementId);
        return this;
    }

    @Override
    public AngleCnecAdder withExportingNetworkElement(String exportingNetworkElementId, String exportingNetworkElementName) {
        this.exportingNetworkElementId = exportingNetworkElementId;
        super.withNetworkElement(exportingNetworkElementId, exportingNetworkElementName);
        return this;
    }

    @Override
    public AngleCnecAdder withImportingNetworkElement(String importingNetworkElementId) {
        this.withImportingNetworkElement(importingNetworkElementId, importingNetworkElementId);
        return this;
    }

    @Override
    public AngleCnecAdder withImportingNetworkElement(String importingNetworkElementId, String importingNetworkElementName) {
        this.importingNetworkElementId = importingNetworkElementId;
        super.withNetworkElement(importingNetworkElementId, importingNetworkElementName);
        return this;
    }

    @Override
    public AngleThresholdAdder newThreshold() {
        return new AngleThresholdAdderImpl(this);
    }

    void addThreshold(ThresholdImpl threshold) {
        thresholds.add(threshold);
    }

    @Override
    protected String getTypeDescription() {
        return CNEC_TYPE;
    }

    @Override
    public AngleCnec add() {
        checkCnec();

        if (optimized) {
            throw new OpenRaoException(format("Error while adding cnec %s : Open RAO does not allow the optimization of AngleCnecs.", id));
        }

        checkAndInitThresholds();

        State state = getState();

        AngleCnec cnec = new AngleCnecImpl(id, name,
            owner.getNetworkElement(exportingNetworkElementId),
            owner.getNetworkElement(importingNetworkElementId),
            operator, border, state, optimized, monitored,
            thresholds, reliabilityMargin);

        owner.addAngleCnec(cnec);
        return cnec;
    }

    private void checkAndInitThresholds() {
        /*
         This should be done here, and not in Threshold Adder, as some information of the AngleCnec is required
         to perform those checks
         */

        if (this.thresholds.isEmpty()) {
            throw new OpenRaoException("Cannot add an AngleCnec without a threshold. Please use newThreshold");
        }

        if (this.thresholds.stream().anyMatch(th -> !th.getUnit().equals(Unit.DEGREE))) {
            throw new OpenRaoException("AngleCnec threshold must be in DEGREE");
        }
    }

    @Override
    protected void checkCnec() {
        AdderUtils.assertAttributeNotNull(exportingNetworkElementId, CNEC_TYPE, "exporting network element", "withExportingNetworkElement()");
        AdderUtils.assertAttributeNotNull(importingNetworkElementId, CNEC_TYPE, "importing network element", "withImportingNetworkElement()");
        super.checkCnec();
    }
}
