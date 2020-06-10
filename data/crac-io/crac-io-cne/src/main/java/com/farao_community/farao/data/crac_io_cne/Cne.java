/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneCnecsCreator.createMonitoredRegisteredResource;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class Cne {
    private CriticalNetworkElementMarketDocument marketDocument;
    private CneHelper cneHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(Cne.class);

    public Cne(Crac crac, Network network) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper(crac, network);
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public void generate() {

        DateTime networkDate = cneHelper.getCrac().getNetworkDate();
        fillHeader(networkDate);
        addTimeSeriesToCne(networkDate);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);
        cneHelper.initializeAttributes();

        // fill CNE
        createAllConstraintSeries(point);
    }

    /*****************
     HEADER
     *****************/
    // fills the header of the CNE
    private void fillHeader(DateTime networkDate) {
        marketDocument.setMRID(generateRandomMRID());
        marketDocument.setRevisionNumber("1");
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(CNE_PROCESS_TYPE);
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_SENDER_MRID));
        marketDocument.setSenderMarketParticipantMarketRoleType(CNE_SENDER_MARKET_ROLE_TYPE);
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_RECEIVER_MRID));
        marketDocument.setReceiverMarketParticipantMarketRoleType(CNE_RECEIVER_MARKET_ROLE_TYPE);
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(createEsmpDateTimeInterval(networkDate));
    }

    /*****************
     TIME_SERIES
     *****************/
    // creates and adds the TimeSeries to the CNE
    private void addTimeSeriesToCne(DateTime networkDate) {
        try {
            SeriesPeriod period = newPeriod(networkDate, SIXTY_MINUTES_DURATION, newPoint(1));
            marketDocument.timeSeries = Collections.singletonList(newTimeSeries(B54_BUSINESS_TYPE_TS, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     CONSTRAINT_SERIES
     *****************/
    // Creates and fills all ConstraintSeries
    private void createAllConstraintSeries(Point point) {

        Crac crac = cneHelper.getCrac();
        Network network = cneHelper.getNetwork();

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        crac.getCnecs().forEach(cnec -> {
            /* Country of cnecs */
            Line cnecLine = network.getLine(cnec.getNetworkElement().getId());
            Set<Country> countries = new HashSet<>();

            if (cnecLine != null) {
                cnecLine.getTerminal1().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
                cnecLine.getTerminal2().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
            }

            /* Create Constraint series */
            ConstraintSeries constraintSeriesB54 = newConstraintSeries(cnec.getId(), B54_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB57 = newConstraintSeries(cnec.getId(), B57_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB88 = newConstraintSeries(cnec.getId(), B88_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);

            /* Add contingency if exists */
            Optional<Contingency> optionalContingency = cnec.getState().getContingency();
            if (optionalContingency.isPresent()) {
                ContingencySeries contingencySeries = newContingencySeries(optionalContingency.get().getId(), optionalContingency.get().getName());
                constraintSeriesB54.contingencySeries.add(contingencySeries);
                constraintSeriesB57.contingencySeries.add(contingencySeries);
                constraintSeriesB88.contingencySeries.add(contingencySeries);
            }

            /* Add critical network element */
            List<Analog> measurementsB54 = new ArrayList<>();
            List<Analog> measurementsB57 = new ArrayList<>();
            List<Analog> measurementsB88 = new ArrayList<>();

            CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
            if (cnecResultExtension != null) {
                String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());
                CneCnecsCreator.createB54B57Measurements(cnec, measurementType, cneHelper.getPostOptimVariantId(), measurementsB54, measurementsB57);
                CneCnecsCreator.createB88Measurements(cnec, measurementType, cneHelper.getPreOptimVariantId(), measurementsB88);

                MonitoredRegisteredResource monitoredRegisteredResourceB54 = createMonitoredRegisteredResource(cnec, network, measurementsB54);
                constraintSeriesB54.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB54));

                MonitoredRegisteredResource monitoredRegisteredResourceB57 = createMonitoredRegisteredResource(cnec, network, measurementsB57);
                constraintSeriesB57.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB57));

                MonitoredRegisteredResource monitoredRegisteredResourceB88 = createMonitoredRegisteredResource(cnec, network, measurementsB88);
                constraintSeriesB88.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB88));

            } else {
                LOGGER.warn(String.format("Results of CNEC %s are not exported.", cnec.getName()));
            }

            /* Add constraint series to the list */
            constraintSeriesList.add(constraintSeriesB54);
            constraintSeriesList.add(constraintSeriesB57);
            constraintSeriesList.add(constraintSeriesB88);
        });

        crac.getRangeActions().forEach(rangeAction -> {
            RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
            if (rangeActionResultExtension != null) {
                RangeActionResult preOptimRangeActionResult = rangeActionResultExtension.getVariant(cneHelper.getPreOptimVariantId());
                RangeActionResult postOptimRangeActionResult = rangeActionResultExtension.getVariant(cneHelper.getPostOptimVariantId());

                if (preOptimRangeActionResult != null && postOptimRangeActionResult != null
                    && isActivated(crac.getPreventiveState().getId(), preOptimRangeActionResult, postOptimRangeActionResult)
                    && !rangeAction.getNetworkElements().isEmpty()) {

                    ConstraintSeries preOptimConstraintSeriesB56 = newConstraintSeries(cutString(rangeAction.getId() + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);
                    ConstraintSeries postOptimConstraintSeriesB56 = newConstraintSeries(cutString(rangeAction.getId() + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);

                    RemedialActionSeries preOptimRemedialActionSeries = newRemedialActionSeries(rangeAction.getId(), rangeAction.getName());
                    RemedialActionSeries postOptimRemedialActionSeries = newRemedialActionSeries(rangeAction.getId(), rangeAction.getName(), PREVENTIVE_MARKET_OBJECT_STATUS);
                    try {
                        Country country = Country.valueOf(rangeAction.getOperator());
                        preOptimRemedialActionSeries.partyMarketParticipant.add(newPartyMarketParticipant(country));
                        postOptimRemedialActionSeries.partyMarketParticipant.add(newPartyMarketParticipant(country));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn(String.format("Operator %s is not a country id.", rangeAction.getOperator()));
                    }

                    rangeAction.getNetworkElements().forEach(networkElement -> {
                        if (preOptimRangeActionResult instanceof PstRangeResult) {
                            int tap = ((PstRangeResult) preOptimRangeActionResult).getTap(crac.getPreventiveState().getId());
                            RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
                            preOptimRemedialActionSeries.registeredResource.add(registeredResource);
                        }
                    });
                    preOptimConstraintSeriesB56.remedialActionSeries.add(preOptimRemedialActionSeries);

                    rangeAction.getNetworkElements().forEach(networkElement -> {
                        int tap = ((PstRangeResult) postOptimRangeActionResult).getTap(crac.getPreventiveState().getId());
                        RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
                        postOptimRemedialActionSeries.registeredResource.add(registeredResource);
                        postOptimRemedialActionSeries.setMRID(postOptimRemedialActionSeries.getMRID() + "@" + tap + "@");
                    });
                    postOptimConstraintSeriesB56.remedialActionSeries.add(postOptimRemedialActionSeries);
                    RemedialActionSeries shortPostOptimRemedialActionSeries = createShortRemedialActionSeries(postOptimRemedialActionSeries);
                    constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).forEach(constraintSeries -> constraintSeries.remedialActionSeries.add(shortPostOptimRemedialActionSeries));

                    constraintSeriesList.add(preOptimConstraintSeriesB56);
                    constraintSeriesList.add(postOptimConstraintSeriesB56);
                }
            }
        });

        crac.getNetworkActions().forEach(networkAction -> {
            NetworkActionResultExtension networkActionResultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
            if (networkActionResultExtension != null) {
                NetworkActionResult preOptimNetworkActionResult = networkActionResultExtension.getVariant(cneHelper.getPreOptimVariantId());
                NetworkActionResult postOptimNetworkActionResult = networkActionResultExtension.getVariant(cneHelper.getPostOptimVariantId());

                if (preOptimNetworkActionResult != null && postOptimNetworkActionResult != null
                    && isActivated(crac.getPreventiveState().getId(), preOptimNetworkActionResult, postOptimNetworkActionResult)
                    && !networkAction.getNetworkElements().isEmpty()) {

                    ConstraintSeries preOptimConstraintSeriesB56 = newConstraintSeries(cutString(networkAction.getId() + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);
                    ConstraintSeries postOptimConstraintSeriesB56 = newConstraintSeries(cutString(networkAction.getId() + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);

                    RemedialActionSeries preOptimRemedialActionSeries = newRemedialActionSeries(networkAction.getId(), networkAction.getName());
                    RemedialActionSeries postOptimRemedialActionSeries = newRemedialActionSeries(networkAction.getId(), networkAction.getName(), PREVENTIVE_MARKET_OBJECT_STATUS);
                    try {
                        Country country = Country.valueOf(networkAction.getOperator());
                        preOptimRemedialActionSeries.partyMarketParticipant.add(newPartyMarketParticipant(country));
                        postOptimRemedialActionSeries.partyMarketParticipant.add(newPartyMarketParticipant(country));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn(String.format("Operator %s is not a country id.", networkAction.getOperator()));
                    }

                    preOptimConstraintSeriesB56.remedialActionSeries.add(preOptimRemedialActionSeries);
                    postOptimConstraintSeriesB56.remedialActionSeries.add(postOptimRemedialActionSeries);
                    constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).forEach(constraintSeries -> constraintSeries.remedialActionSeries.add(postOptimRemedialActionSeries));

                    constraintSeriesList.add(preOptimConstraintSeriesB56);
                    constraintSeriesList.add(postOptimConstraintSeriesB56);
                }
            }
        });

        /* Add all constraint series to the CNE */
        point.constraintSeries = constraintSeriesList;
    }

    private RemedialActionSeries createShortRemedialActionSeries(RemedialActionSeries remedialActionSeries) {
        return newRemedialActionSeries(remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getBusinessType());
    }
}
