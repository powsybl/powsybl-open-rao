/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionType;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TriggerConditionTest {

    class CnecImplTest implements Cnec {

        private final String id;

        CnecImplTest(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<NetworkElement> getNetworkElements() {
            return null;
        }

        @Override
        public State getState() {
            return null;
        }

        @Override
        public double getReliabilityMargin() {
            return 0;
        }

        @Override
        public PhysicalParameter getPhysicalParameter() {
            return null;
        }

        @Override
        public boolean isOptimized() {
            return false;
        }

        @Override
        public boolean isMonitored() {
            return false;
        }

        @Override
        public String getOperator() {
            return null;
        }

        @Override
        public String getBorder() {
            return null;
        }

        @Override
        public void setMonitored(boolean monitored) {

        }

        @Override
        public void setOptimized(boolean optimized) {

        }

        @Override
        public void addExtension(Class aClass, Extension extension) {

        }

        @Override
        public Extension getExtension(Class aClass) {
            return null;
        }

        @Override
        public Extension getExtensionByName(String s) {
            return null;
        }

        @Override
        public boolean removeExtension(Class aClass) {
            return false;
        }

        @Override
        public Collection getExtensions() {
            return null;
        }
    }

    class TriggerConditionImplTest implements TriggerCondition {
        private final Instant instant;
        private final Contingency contingency;
        private final Country country;
        private final Cnec<?> cnec;

        TriggerConditionImplTest(Instant instant, Contingency contingency, Country country, Cnec<?> cnec) {
            this.instant = instant;
            this.contingency = contingency;
            this.country = country;
            this.cnec = cnec;
        }

        @Override
        public Instant getInstant() {
            return instant;
        }

        @Override
        public Optional<Contingency> getContingency() {
            return Optional.ofNullable(contingency);
        }

        @Override
        public Optional<Cnec<?>> getCnec() {
            return Optional.ofNullable(cnec);
        }

        @Override
        public Optional<Country> getCountry() {
            return Optional.ofNullable(country);
        }

        @Override
        public UsageMethod getUsageMethod() {
            return null;
        }

        @Override
        public UsageMethod getUsageMethod(State state) {
            return null;
        }
    }

    private Instant instant = new InstantTest.InstantImplTest(0, InstantKind.CURATIVE);
    private Contingency contingency = new Contingency("contingency");
    private Cnec<?> cnec = new CnecImplTest("cnec");
    private Country country = Country.FR;

    @Test
    void testGetType() {
        assertEquals(TriggerConditionType.ON_INSTANT, new TriggerConditionImplTest(instant, null, null, null).getType());
        assertEquals(TriggerConditionType.ON_CONTINGENCY_STATE, new TriggerConditionImplTest(instant, contingency, null, null).getType());
        assertEquals(TriggerConditionType.ON_FLOW_CONSTRAINT_IN_COUNTRY, new TriggerConditionImplTest(instant, null, country, null).getType());
        assertEquals(TriggerConditionType.ON_FLOW_CONSTRAINT_IN_COUNTRY, new TriggerConditionImplTest(instant, contingency, country, null).getType());
        assertEquals(TriggerConditionType.ON_CONSTRAINT, new TriggerConditionImplTest(instant, null, null, cnec).getType());
        assertEquals(TriggerConditionType.ON_CONSTRAINT, new TriggerConditionImplTest(instant, contingency, null, cnec).getType());
        assertEquals(TriggerConditionType.ON_CONSTRAINT, new TriggerConditionImplTest(instant, null, country, cnec).getType());
        assertEquals(TriggerConditionType.ON_CONSTRAINT, new TriggerConditionImplTest(instant, contingency, country, cnec).getType());
    }
}
