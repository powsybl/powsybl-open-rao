/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnConstraintSerializer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The remedial action is available only after a specific constraint, with a given method
 * and in a given state.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("on-constraint")
@JsonSerialize(using = OnConstraintSerializer.class)
public final class OnConstraint extends AbstractUsageRule {

    private Cnec cnec;

    public OnConstraint(UsageMethod usageMethod, State state, Cnec cnec) {
        super(usageMethod, state);
        this.cnec = cnec;
    }

    public Cnec getCnec() {
        return cnec;
    }

    public void setCnec(Cnec cnec) {
        this.cnec = cnec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OnConstraint rule = (OnConstraint) o;
        return super.equals(o) && rule.cnec.equals(cnec);
    }

    @Override
    public int hashCode() {
        return usageMethod.hashCode() * 23 + state.hashCode() * 53 + cnec.hashCode();
    }
}
