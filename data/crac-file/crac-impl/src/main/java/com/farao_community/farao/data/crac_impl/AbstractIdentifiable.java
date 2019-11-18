/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.farao_community.farao.data.crac_api.Identifiable;

/**
 *
 * @author Viktor Terrier <viktor.terrier at rte-france.com>
 */
abstract class AbstractIdentifiable<I extends Identifiable<I>> extends AbstractExtendable<I> implements Identifiable<I> {

    protected final String id;

    protected String name;

    AbstractIdentifiable(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name != null ? name : id;
    }

    @Override
    public String toString() {
        return id;
    }

}
