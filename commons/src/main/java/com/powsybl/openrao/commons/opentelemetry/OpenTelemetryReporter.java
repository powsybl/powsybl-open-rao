/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.opentelemetry;

import com.powsybl.openrao.commons.OpenRaoException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 *
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public final class OpenTelemetryReporter {

  protected static final String OPEN_RAO = "open-rao";

  protected static final String VERSION = "1.0.0";

  /**
   * The open telemetry tracer
   */
  private static Tracer TRACER;
  private static boolean TRACE_ALL_LOGS = false;

  /**
   * Logger for the spans creation
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryReporter.class);

  /**
   * Private constructor
   */
  private OpenTelemetryReporter() {
  }

  /**
   * Initializes the OpenTelemetry SDK with a simple logging exporter.
   * In a production environment, you would use an exporter like OTLP
   * to send data to a backend (e.g., Jaeger, Prometheus, etc.).
   *
   * @param endPoint the Open Telemetry trace collector (ex: '<a href="http://localhost:4317">...</a>')
   * @param service the service name for traces
   * @param version the version of the service for traces
   */
  public static void initOpenTelemetry(String endPoint, String service, String version) {

    OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(endPoint).build();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(otlpGrpcSpanExporter).build())
        .setResource(Resource.getDefault().merge(
            Resource.builder()
                .put("service.name", service != null ? service : OPEN_RAO)
                .put("service.version", version != null ? version : VERSION).build()
        )).build();
    OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
    TRACER = tracerProvider.get(OPEN_RAO);
  }

  /**
   * Set the open telemetry tracer used to trace open-rao
   *
   * @param tracerProvider
   */
  public static void setOpenTelemetryTracer(SdkTracerProvider tracerProvider, boolean traceAllLogs) {
    TRACE_ALL_LOGS = traceAllLogs;
    var hasProvider = tracerProvider != null;
    TRACER = hasProvider ? tracerProvider.get(OPEN_RAO) : GlobalOpenTelemetry.getTracer(OPEN_RAO);
  }

  @SuppressWarnings("unused")
  private static SpanExporter getStdIoSpanExporter() {
    return new SpanExporter() {
      @Override
      public CompletableResultCode export(Collection<SpanData> collection) {
        for (SpanData spanData : collection) {
          LOGGER.info("[{}] >> {}\t{}\t{}\t{} ms", spanData.getParentSpanId(), spanData.getSpanId(), spanData.getName(), spanData.getStatus(), Math.ceil((double) (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000000));
        }
        return null;
      }

      @Override
      public CompletableResultCode flush() {
        return null;
      }

      @Override
      public CompletableResultCode shutdown() {
        return null;
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
  }

  /**
   * A generic utility method to wrap a method call with a span.
   * This method handles span creation, context management, and error handling.
   *
   * @param spanName The name of the span to create.
   * @param runnable The operation to execute, wrapped in a Callable.
   * @param <T>      The return type of the Callable.
   * @return The result of the Callable.
   * @throws Exception if the Callable throws an exception.
   */
  public static <T> T withSpan(String spanName, ThrowingFunction<OpenTelemetryContext, T> runnable) {
    return doWithSpan(spanName, runnable);
  }

  /**
   * An overloaded utility method for methods that return void.
   *
   * @param spanName The name of the span to create.
   * @param runnable The operation to execute, wrapped in a Runnable.
   */
  public static void withSpan(String spanName, ThrowingConsumer<OpenTelemetryContext> runnable) {
    doWithSpan(spanName, ctx -> {
      runnable.accept(ctx);
      return null;
    });
  }

  protected static <T> T doWithSpan(String spanName, ThrowingFunction<OpenTelemetryContext, T> callable) {
    var ctx = new OpenTelemetryContext(TRACER, spanName);
    try {
      ctx.addEvent("Executing operation: " + spanName);
      T result = callable.apply(ctx);
      ctx.setStatus(StatusCode.OK);
      return result;
    } catch (Exception e) {
      ctx.setStatus(StatusCode.ERROR, "Operation failed: " + e);
      ctx.recordException(e);
      throw new OpenRaoException(e.getMessage());
    } finally {
      ctx.end();
    }
  }

  /**
   *
   * Runs the given task in an existing Open Telemetry Context, i.e. withSpan() has been called before
   * This is necessary when running tasks in different Threads (e.g. using ForkJoinTask)
   *
   * @param cx
   * @param task
   */
  public static <T> T inContext(OpenTelemetryContext cx, Callable<T> task)
      throws Exception {
    try (var scope = cx.makeCurrent()) {
      return task.call();
    }
  }

  public static void inContext(OpenTelemetryContext cx, Runnable task) {
    try (var scope = cx.makeCurrent()) {
      task.run();
    }
  }

  public static void trace(Logger logger, String format, Object... arguments) {
    doLog(logger.isTraceEnabled(), logger::trace, format, arguments);
  }

  public static void info(Logger logger, String format, Object... arguments) {
    doLog(logger.isInfoEnabled(), logger::info, format, arguments);
  }

  public static void warn(Logger logger, String format, Object... arguments) {
    doLog(logger.isWarnEnabled(), logger::warn, format, arguments);
  }

  public static void error(Logger logger, String format, Object... arguments) {
    doLog(logger.isErrorEnabled(), logger::error, format, arguments);
  }

  public static void debug(Logger logger, String format, Object... arguments) {
    doLog(logger.isDebugEnabled(), logger::debug, format, arguments);
  }

  private static void doLog(boolean loggerEnabled, Consumer<String> logWriter, String format, Object... arguments) {
    if (!loggerEnabled) {
      return;
    }
    var msg = format(format, arguments);
    if (TRACE_ALL_LOGS) {
      OpenTelemetryReporter.withSpan(msg, cx -> {
        logWriter.accept(msg);
      });
    } else {
      logWriter.accept(msg);
    }
  }

  private static String format(String pattern, Object[] args) {
    // Use the SLF4J MessageFormatter's arrayFormat method
    return MessageFormatter.arrayFormat(pattern, args).getMessage();
  }

}