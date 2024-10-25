/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;

import java.util.Properties;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.DOCUMENT_ID;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.DOMAIN_ID;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.MNEC_ACCEPTABLE_MARGIN_DIMINUTION;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.PROCESS_TYPE;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.RECEIVER_ID;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.RECEIVER_ROLE;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.RELATIVE_POSITIVE_MARGINS;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.REVISION_NUMBER;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.SENDER_ID;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.SENDER_ROLE;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.TIME_INTERVAL;
import static com.powsybl.openrao.data.cneexportercommons.CneConstants.WITH_LOOP_FLOWS;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CneHelper {
    private final Crac crac;
    private final RaoResult raoResult;
    private final Properties properties;

    public CneHelper(Crac crac, RaoResult raoResult, Properties properties) {
        this.crac = crac;
        this.raoResult = raoResult;
        this.properties = properties;
    }

    public Crac getCrac() {
        return crac;
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public boolean isRelativePositiveMargins() {
        return Boolean.parseBoolean(properties.getProperty(RELATIVE_POSITIVE_MARGINS, "false"));
    }

    public boolean isWithLoopFlows() {
        return Boolean.parseBoolean(properties.getProperty(WITH_LOOP_FLOWS, "false"));
    }

    public double getMnecAcceptableMarginDiminution() {
        return Double.parseDouble(properties.getProperty(MNEC_ACCEPTABLE_MARGIN_DIMINUTION, "0"));
    }

    public String getDocumentId() {
        return properties.getProperty(DOCUMENT_ID);
    }

    public int getRevisionNumber() {
        return Integer.parseInt(properties.getProperty(REVISION_NUMBER));
    }

    public String getDomainId() {
        return properties.getProperty(DOMAIN_ID);
    }

    public String getProcessType() {
        return properties.getProperty(PROCESS_TYPE);
    }

    public String getSenderId() {
        return properties.getProperty(SENDER_ID);
    }

    public String getSenderRole() {
        return properties.getProperty(SENDER_ROLE);
    }

    public String getReceiverId() {
        return properties.getProperty(RECEIVER_ID);
    }

    public String getReceiverRole() {
        return properties.getProperty(RECEIVER_ROLE);
    }

    public String getTimeInterval() {
        return properties.getProperty(TIME_INTERVAL);
    }
}
