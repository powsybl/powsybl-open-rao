/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.cne_exporter_commons.CneUtil;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;

import java.util.*;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.B56_BUSINESS_TYPE;
import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.B57_BUSINESS_TYPE;

/**
 * Structures the chaining of RASeriesCreator and MonitoredSeriesCreator for SWE CNE format
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class SweConstraintSeriesCreator {
    private final CneHelper cneHelper;
    private final SweMonitoredSeriesCreator monitoredSeriesCreator;
    private final SweRemedialActionSeriesCreator remedialActionSeriesCreator;

    public SweConstraintSeriesCreator(CneHelper cneHelper, CimCracCreationContext cracCreationContext) {
        this.cneHelper = cneHelper;
        this.monitoredSeriesCreator = new SweMonitoredSeriesCreator(cneHelper, cracCreationContext);
        this.remedialActionSeriesCreator = new SweRemedialActionSeriesCreator(cneHelper, cracCreationContext);
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
        cneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).forEach(contingency ->
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
        constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeries(contingency));
        return constraintSeries;
    }

    private List<ConstraintSeries> generateB57() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        constraintSeries.add(generateBasecaseB57());
        cneHelper.getCrac().getContingencies().stream().sorted(Comparator.comparing(Contingency::getId)).forEach(contingency ->
            constraintSeries.add(generateContingencyB57(contingency))
        );
        return constraintSeries;
    }

    private ConstraintSeries generateBasecaseB57() {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B57_BUSINESS_TYPE);
        constraintSeries.getMonitoredSeries().addAll(monitoredSeriesCreator.generateMonitoredSeries(null));
        constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeriesReference(null));
        return constraintSeries;
    }

    private ConstraintSeries generateContingencyB57(Contingency contingency) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(CneUtil.generateUUID());
        constraintSeries.setBusinessType(B57_BUSINESS_TYPE);
        constraintSeries.getContingencySeries().add(generateContingencySeries(contingency));
        constraintSeries.getMonitoredSeries().addAll(monitoredSeriesCreator.generateMonitoredSeries(contingency));
        constraintSeries.getRemedialActionSeries().addAll(remedialActionSeriesCreator.generateRaSeriesReference(contingency));
        return constraintSeries;
    }

    private ContingencySeries generateContingencySeries(Contingency contingency) {
        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(contingency.getId());
        contingencySeries.setName(contingency.getName());
        return contingencySeries;
    }
}
