/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AreaIDString;
import com.farao_community.farao.data.swe_cne_exporter.xsd.ESMPDateTimeInterval;
import com.farao_community.farao.data.swe_cne_exporter.xsd.PartyIDString;
import com.farao_community.farao.data.swe_cne_exporter.xsd.ResourceIDString;
import org.threeten.extra.Interval;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.cutString;

/**
 * Auxiliary methods
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class SweCneUtil {
    private SweCneUtil() {
    }

    public static boolean isAngleMonitoringUnsecure(Crac crac, RaoResult raoResult) {
        // TODO : replace this with raoResult.isSecure(ANGLE) when ready
        return crac.getAngleCnecs().stream().anyMatch(angleCnec -> raoResult.getMargin(Instant.CURATIVE, angleCnec, Unit.DEGREE) < 0);
    }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeInterval(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        OffsetDateTime utcDateTime = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);
        timeInterval.setStart(dateFormat.format(utcDateTime));
        timeInterval.setEnd(dateFormat.format(utcDateTime.plusHours(1)));
        return timeInterval;

    }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeIntervalForWholeDay(String intervalString) {
        Interval interval = Interval.parse(intervalString);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.from(ZoneOffset.UTC));
        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();
        timeInterval.setStart(dateFormat.format(interval.getStart()));
        timeInterval.setEnd(dateFormat.format(interval.getEnd()));
        return timeInterval;
    }

    // Creation of ID with code scheme
    public static ResourceIDString createResourceIDString(String codingScheme, String value) {
        ResourceIDString resourceMRID = new ResourceIDString();
        resourceMRID.setCodingScheme(codingScheme);
        resourceMRID.setValue(cutString(value, 60));
        return resourceMRID;
    }

    // Creation of ID with code scheme
    public static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(cutString(value, 16));
        return marketParticipantMRID;
    }

    // Creation of area ID with code scheme
    public static AreaIDString createAreaIDString(String codingScheme, String value) {
        AreaIDString areaIDString = new AreaIDString();
        areaIDString.setCodingScheme(codingScheme);
        areaIDString.setValue(cutString(value, 16));
        return areaIDString;
    }
}
