package com.farao_community.farao.data.crac_api;

import java.util.Optional;

public interface State {

    Instant getInstant();

    void setInstant(Instant instant);

    Optional<Contingency> getContingency();

    void setContingency(Optional<Contingency> contingency);
}
