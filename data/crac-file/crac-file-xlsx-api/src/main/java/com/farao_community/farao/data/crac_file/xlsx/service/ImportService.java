/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import com.farao_community.farao.data.crac_file.*;
import com.farao_community.farao.data.crac_file.xlsx.ExcelReader;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.xlsx.model.*;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Import Service
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Slf4j
public class ImportService {

    private static final String XLSX_CRAC = "Xlsx_CRAC_";

    final Supplier<Predicate<ContingencyElementXlsx>> contingencyElementSupplier = () -> contingencyElement -> contingencyElement.getActivation() == Activation.YES;

    public CracFile importContacts(InputStream inputStream, TimesSeries timesSeries, String fileName) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);

        Validation<FaraoException, List<MonitoredBranchXlsx>> monitoredBranchesValidation = Validation.valid(ExcelReader.of(MonitoredBranchXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("Branch_CBCO")
                .list());

        Validation<FaraoException, List<ContingencyElementXlsx>> contingencyElementsValidation = Validation.valid(ExcelReader.of(ContingencyElementXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("Branch_CO")
                .list());

        Validation<FaraoException, List<BranchTimeSeries>> branchTimeseriesValidation = Validation.valid(ExcelReader.of(BranchTimeSeries.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .timesSeries(timesSeries)
                .sheet("Branch_Timeseries")
                .list());

        Validation<FaraoException, List<RedispatchingRemedialActionXlsx>> redispatchingRemedialActionXlsx = Validation.valid(ExcelReader.of(RedispatchingRemedialActionXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("RA_RD_static")
                .list());

        Validation<FaraoException, List<RaTimeSeries>> radTimeSeries = Validation.valid(ExcelReader.of(RaTimeSeries.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("RA_RD_Timeseries")
                .list());

        Validation<FaraoException, List<RaPstTap>> radPstTap = Validation.valid(ExcelReader.of(RaPstTap.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("RA_PST_Tap")
                .list());

        Validation<FaraoException, List<RaTopologyXlsx>> raTopologyXlsx = Validation.valid(ExcelReader.of(RaTopologyXlsx.class)
                .from(new ByteArrayInputStream(bytes))
                .skipHeaderRow(true)
                .sheet("RA_Topology")
                .list());

        String id = XLSX_CRAC;

        // sort monitor branch for pre and post contingency
        Map<String, List<MonitoredBranch>> monitoredBranches = new MonitoredBranchValidation().monitoredBranchValidation(monitoredBranchesValidation, branchTimeseriesValidation, timesSeries);
        // Build Post Contingency list
        List<Contingency> postContingency = new PostContingencyValidation().postContingencyElementValidation(contingencyElementsValidation, monitoredBranches);
        // Build Pre Contingency List
        PreContingency preContingency = new PreContingencyValidation().preContingencyValidation(monitoredBranches.getOrDefault("", Collections.emptyList()));
        // building crac file
        LocalDate date = branchTimeseriesValidation.get().get(0).getDate();

        List<RemedialAction> allRemedialActions = new ArrayList<>();
        allRemedialActions.addAll(RdRemedialActionValidation.rdRemedialActionValidation(redispatchingRemedialActionXlsx, radTimeSeries, timesSeries));
        allRemedialActions.addAll(PstRemedialActionValidation.pstRemedialActionValidation(radPstTap));
        allRemedialActions.addAll(TopologicalRemedialActionValidation.topologicalRemedialActionValidation(raTopologyXlsx));

        return CracFile.builder()
                .id(CracTools.getId(id, timesSeries, date))
                .name(CracTools.getId(id, timesSeries, date))
                .contingencies(postContingency)
                .preContingency(preContingency)
                .description(CracTools.getDescription(fileName, timesSeries, date))
                .remedialActions(allRemedialActions)
                .build();
    }
}
