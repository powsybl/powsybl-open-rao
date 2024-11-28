/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.openrao.data.cneexportercommons.CneUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.swecneexporter.xsd.*;

import java.util.*;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;

/**
 * Structures the chaining of RASeriesCreator and MonitoredSeriesCreator for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class SweConstraintSeriesCreator {
    private final SweCneHelper sweCneHelper;
    private final SweMonitoredSeriesCreator monitoredSeriesCreator;
    private final SweRemedialActionSeriesCreator remedialActionSeriesCreator;
    private final SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator;

    public SweConstraintSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.monitoredSeriesCreator = new SweMonitoredSeriesCreator(sweCneHelper, cracCreationContext);
        this.remedialActionSeriesCreator = new SweRemedialActionSeriesCreator(sweCneHelper, cracCreationContext);
        this.additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
    }

    public List<ConstraintSeries> generate() {
        List<ConstraintSeries> allConstraintSeries = new ArrayList<>();
        allConstraintSeries.addAll(generateB56());
        allConstraintSeries.addAll(generateB57());
        return allConstraintSeries;
    }

    private List<ConstraintSeries> generateB56() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        constraintSeries.add(generateBasecaseB56());
        sweCneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).forEach(contingency ->
            constraintSeries.add(generateContingencyB56(contingency))
        );
        return constraintSeries;
    }

    private ConstraintSeries generateBasecaseB56() {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B56_BUSINESS_TYPE);
        constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeries(null));
        return constraintSeries;
    }

    private ConstraintSeries generateContingencyB56(Contingency contingency) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B56_BUSINESS_TYPE);
        constraintSeries.getContingencySeries().add(generateContingencySeries(contingency));
        if (sweCneHelper.isContingencyDivergent(contingency)) {
            addDivergenceReasonCode(constraintSeries);
        } else {
            constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeries(contingency));
        }
        return constraintSeries;
    }

    private static void addDivergenceReasonCode(ConstraintSeries constraintSeries) {
        Reason reason = new Reason();
        reason.setCode(DIVERGENCE_CODE);
        reason.setText(DIVERGENCE_TEXT);
        constraintSeries.getReason().add(reason);
    }

    private List<ConstraintSeries> generateB57() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        constraintSeries.add(generateBasecaseB57());
        sweCneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).forEach(contingency ->
            constraintSeries.add(generateContingencyB57(contingency))
        );
        return constraintSeries;
    }

    private ConstraintSeries generateBasecaseB57() {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B57_BUSINESS_TYPE);
        constraintSeries.getAdditionalConstraintSeries().addAll(additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(null));
        constraintSeries.getMonitoredSeries().addAll(monitoredSeriesCreator.generateMonitoredSeries(null));
        constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeriesReference(null));
        return constraintSeries;
    }

    private ConstraintSeries generateContingencyB57(Contingency contingency) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B57_BUSINESS_TYPE);
        constraintSeries.getAdditionalConstraintSeries().addAll(additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency));
        constraintSeries.getContingencySeries().add(generateContingencySeries(contingency));
        if (sweCneHelper.isContingencyDivergent(contingency)) {
            constraintSeries.getMonitoredSeries().addAll(monitoredSeriesCreator.generateMonitoredSeries(contingency));
            addDivergenceReasonCode(constraintSeries);
        } else {
            constraintSeries.getMonitoredSeries().addAll(monitoredSeriesCreator.generateMonitoredSeries(contingency));
            constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeriesReference(contingency));
        }
        return constraintSeries;
    }

    private ContingencySeries generateContingencySeries(Contingency contingency) {
        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(contingency.getId());
        contingencySeries.setName(contingency.getName().orElse(contingency.getId()));
        return contingencySeries;
    }
}
