/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneCnecsCreator.createConstraintSeriesOfACnec;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneRemedialActionsCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class Cne {
    private CriticalNetworkElementMarketDocument marketDocument;
    private CneHelper cneHelper;

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

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        crac.getBranchCnecs().stream().sorted(Comparator.comparing(Identifiable::getId))
                .forEach(cnec -> createConstraintSeriesOfACnec(cnec, cneHelper, constraintSeriesList, isRelativePositiveMargins()));

        ConstraintSeries preventiveB56 = newConstraintSeries(generateRandomMRID(), B56_BUSINESS_TYPE);
        crac.getRangeActions().stream().sorted(Comparator.comparing(Identifiable::getId))
                .forEach(rangeAction -> createRangeRemedialActionSeries(rangeAction, cneHelper, constraintSeriesList, preventiveB56));
        crac.getNetworkActions().stream().sorted(Comparator.comparing(Identifiable::getId))
                .forEach(networkAction -> createNetworkRemedialActionSeries(networkAction, cneHelper, preventiveB56));

        // Add the remedial action series to B54 and B57
        addRemedialActionsToOtherConstraintSeries(preventiveB56.getRemedialActionSeries(), constraintSeriesList);
        constraintSeriesList.add(preventiveB56);

        /* Add all constraint series to the CNE */
        point.constraintSeries = constraintSeriesList;
    }

    /**
     * This method figures out if positive margins should be exported as relative margins in the objective function
     * This is necessary since we don't have access to RaoParameters in this exporter
     * If things change in the future, this should be changed
     */
    // TODO : change this if RaoParameters is added to RaoResult, and RaoResult accessible in exporter
    private boolean isRelativePositiveMargins() {
        return cneHelper.getCrac().getBranchCnecs().stream().anyMatch(branchCnec ->
            !Double.isNaN(branchCnec.getExtension(CnecResultExtension.class).getVariant(cneHelper.getPreOptimVariantId()).getAbsolutePtdfSum())
        );
    }
}
