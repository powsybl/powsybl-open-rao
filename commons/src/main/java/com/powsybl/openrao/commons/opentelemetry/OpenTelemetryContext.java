/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.opentelemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 *
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OpenTelemetryContext {

  private final Optional<Span> span;
  private final Optional<Context> context;

  public OpenTelemetryContext(Tracer tracer, String spanName) {
    var hasTracer = tracer != null;
    if (hasTracer) {
      var newSpan = tracer.spanBuilder(spanName).startSpan();
      this.context = Optional.of(newSpan.storeInContext(Context.current()));
      this.span = Optional.of(newSpan);
    } else {
      this.span = Optional.empty();
      this.context = Optional.empty();
    }
  }

  @Nullable
  public Scope makeCurrent() {
    return context.map(Context::makeCurrent).orElse(null);
  }

  public void addEvent(String name) {
    span.ifPresent(s -> s.addEvent(name));
  }

  public void setStatus(StatusCode statusCode) {
    span.ifPresent(s -> s.setStatus(statusCode));
  }

  public void setStatus(StatusCode statusCode, String description) {
    span.ifPresent(s -> s.setStatus(statusCode, description));
  }

  public void end() {
    span.ifPresent(s -> s.end());
  }

  public void recordException(Exception e) {
    span.ifPresent(s -> s.recordException(e));
  }

}