/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.cnec.Cnec;

/**
 * Enum representing the instants at which {@link Cnec} can be monitored and
 * {@link RemedialAction} applied.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class Instant implements Comparable<Instant> {
    // TODO : add an interface

    public enum Kind {
        PREVENTIVE(0),
        OUTAGE(1),
        AUTO(2),
        CURATIVE(3);

        private final int order;

        private Kind(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private final int order;
    private final String id;
    private final Kind kind;

    public Instant(int order, String id, Kind kind) {
        this.order = order;
        this.id = id;
        this.kind = kind;
        // TODO : lock preventive and outage instants
    }

    private static Instant preventive;
    private static Instant outage;

    public static Instant preventive() {
        if (preventive == null) {
            preventive = new Instant(0, "preventive", Kind.PREVENTIVE);
        }
        return preventive;
    }

    public static Instant outage() {
        if (outage == null) {
            outage = new Instant(1, "outage", Kind.OUTAGE);
        }
        return outage;
    }

    public String getId() {
        return id;
    }

    public Kind getKind() {
        return kind;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return id;
    }

    public boolean comesBefore(Instant otherInstant) {
        return this.order < otherInstant.order;
    }

    // TODO : UT
    public boolean comesAfter(Instant otherInstant) {
        return this.order > otherInstant.order;
    }

    public boolean isPreventive() {
        return kind.equals(Kind.PREVENTIVE);
    }

    public boolean isOutage() {
        return kind.equals(Kind.OUTAGE);
    }

    public boolean isAuto() {
        return kind.equals(Kind.AUTO);
    }

    public boolean isCurative() {
        return kind.equals(Kind.CURATIVE);
    }

    @Override
    public int compareTo(Instant otherInstant) {
        return Integer.compare(this.order, otherInstant.order);
    }
}
