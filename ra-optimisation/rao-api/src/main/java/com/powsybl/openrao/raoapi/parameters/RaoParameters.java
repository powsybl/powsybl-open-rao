/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOOP_FLOW_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MNEC_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.NOT_OPTIMIZED_CNECS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.OBJECTIVE_FUNCTION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RANGE_ACTIONS_OPTIMIZATION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RAO_PARAMETERS_VERSION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RELATIVE_MARGINS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.TOPOLOGICAL_ACTIONS_OPTIMIZATION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.VERSION;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@JsonSerialize(using = RaoParameters.Serializer.class)
@JsonDeserialize(using = RaoParameters.Deserializer.class)
public class RaoParameters extends AbstractExtendable<RaoParameters> {
    @JsonProperty(OBJECTIVE_FUNCTION)
    private ObjectiveFunctionParameters objectiveFunctionParameters;

    @JsonProperty(RANGE_ACTIONS_OPTIMIZATION)
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters;

    @JsonProperty(TOPOLOGICAL_ACTIONS_OPTIMIZATION)
    private TopoOptimizationParameters topoOptimizationParameters;

    @JsonProperty(NOT_OPTIMIZED_CNECS)
    private NotOptimizedCnecsParameters notOptimizedCnecsParameters;

    @JsonProperty(MNEC_PARAMETERS)
    @JsonDeserialize(contentAs = MnecParameters.class)
    private Optional<MnecParameters> mnecParameters;

    @JsonProperty(RELATIVE_MARGINS)
    @JsonDeserialize(contentAs = RelativeMarginsParameters.class)
    private Optional<RelativeMarginsParameters> relativeMarginsParameters;

    @JsonProperty(LOOP_FLOW_PARAMETERS)
    @JsonDeserialize(contentAs = LoopFlowParameters.class)
    private Optional<LoopFlowParameters> loopFlowParameters;

    private ReportNode reportNode;

    public RaoParameters(final ReportNode reportNode) {
        this.objectiveFunctionParameters = new ObjectiveFunctionParameters();
        this.rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
        this.topoOptimizationParameters = new TopoOptimizationParameters(reportNode);
        this.notOptimizedCnecsParameters = new NotOptimizedCnecsParameters();
        this.mnecParameters = Optional.empty();
        this.relativeMarginsParameters = Optional.empty();
        this.loopFlowParameters = Optional.empty();
        this.reportNode = reportNode;
    }

    // Getters and setters
    public void setObjectiveFunctionParameters(ObjectiveFunctionParameters objectiveFunctionParameters) {
        this.objectiveFunctionParameters = objectiveFunctionParameters;
    }

    public void setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    public void setTopoOptimizationParameters(TopoOptimizationParameters topoOptimizationParameters) {
        this.topoOptimizationParameters = topoOptimizationParameters;
    }

    public void setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters notOptimizedCnecsParameters) {
        this.notOptimizedCnecsParameters = notOptimizedCnecsParameters;
    }

    public void setMnecParameters(MnecParameters mnecParameters) {
        this.mnecParameters = Optional.of(mnecParameters);
    }

    public void setRelativeMarginsParameters(RelativeMarginsParameters relativeMarginsParameters) {
        this.relativeMarginsParameters = Optional.of(relativeMarginsParameters);
    }

    public void setLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
        this.loopFlowParameters = Optional.of(loopFlowParameters);
    }

    public ObjectiveFunctionParameters getObjectiveFunctionParameters() {
        return objectiveFunctionParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionsOptimizationParameters() {
        return rangeActionsOptimizationParameters;
    }

    public TopoOptimizationParameters getTopoOptimizationParameters() {
        return topoOptimizationParameters;
    }

    public NotOptimizedCnecsParameters getNotOptimizedCnecsParameters() {
        return notOptimizedCnecsParameters;
    }

    public Optional<MnecParameters> getMnecParameters() {
        return mnecParameters;
    }

    public Optional<RelativeMarginsParameters> getRelativeMarginsParameters() {
        return relativeMarginsParameters;
    }

    public Optional<LoopFlowParameters> getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public boolean hasExtension(Class<? extends Extension<RaoParameters>> extensionClass) {
        return getExtension(extensionClass) != null;
    }

    @JsonProperty(VERSION)
    private String version = RAO_PARAMETERS_VERSION;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (!RAO_PARAMETERS_VERSION.equals(version)) {
            throw new com.powsybl.openrao.commons.OpenRaoException(String.format("RaoParameters version '%s' cannot be deserialized. The only supported version currently is '%s'.", version, RAO_PARAMETERS_VERSION));
        }
        this.version = version;
    }

    public static class Deserializer extends com.fasterxml.jackson.databind.deser.std.StdDeserializer<RaoParameters> {

        private final transient ReportNode reportNode;

        public Deserializer() {
            this(ReportNode.NO_OP);
        }

        public Deserializer(final ReportNode reportNode) {
            super(RaoParameters.class);
            this.reportNode = reportNode;
        }

        @Override
        public RaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
            return deserialize(parser, deserializationContext, new RaoParameters(reportNode));
        }

        @Override
        public RaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, RaoParameters parameters) throws IOException {
            List<Extension<RaoParameters>> extensions = java.util.Collections.emptyList();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String currentName = parser.currentName();
                if (currentName == null) {
                    continue;
                }
                try {
                    switch (currentName) {
                        case VERSION -> parameters.setVersion(parser.nextTextValue());
                        case OBJECTIVE_FUNCTION -> {
                            parser.nextToken();
                            parameters.setObjectiveFunctionParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters.class));
                        }
                        case RANGE_ACTIONS_OPTIMIZATION -> {
                            parser.nextToken();
                            parameters.setRangeActionsOptimizationParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters.class));
                        }
                        case TOPOLOGICAL_ACTIONS_OPTIMIZATION -> {
                            parser.nextToken();
                            parameters.setTopoOptimizationParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.TopoOptimizationParameters.class));
                        }
                        case NOT_OPTIMIZED_CNECS -> {
                            parser.nextToken();
                            parameters.setNotOptimizedCnecsParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.NotOptimizedCnecsParameters.class));
                        }
                        case MNEC_PARAMETERS -> {
                            parser.nextToken();
                            parameters.setMnecParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.MnecParameters.class));
                        }
                        case RELATIVE_MARGINS -> {
                            parser.nextToken();
                            parameters.setRelativeMarginsParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters.class));
                        }
                        case LOOP_FLOW_PARAMETERS -> {
                            parser.nextToken();
                            parameters.setLoopFlowParameters(deserializationContext.readValue(parser, com.powsybl.openrao.raoapi.parameters.LoopFlowParameters.class));
                        }
                        case "extensions" -> {
                            parser.nextToken();
                            extensions = JsonUtil.updateExtensions(parser, deserializationContext, JsonRaoParameters.getExtensionSerializers(), parameters, reportNode);
                        }
                        default -> throw new com.powsybl.openrao.commons.OpenRaoException("Unexpected field in rao parameters: " + currentName);
                    }
                } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
                    if (e.getCause() instanceof com.powsybl.openrao.commons.OpenRaoException) {
                        throw (com.powsybl.openrao.commons.OpenRaoException) e.getCause();
                    }
                    throw e;
                }
            }
            extensions.forEach(extension -> parameters.addExtension((Class) extension.getClass(), extension));
            addOptionalExtensionsDefaultValuesIfExist(parameters, reportNode);
            return parameters;
        }
    }

    public static class Serializer extends com.fasterxml.jackson.databind.ser.std.StdSerializer<RaoParameters> {

        public Serializer() {
            super(RaoParameters.class);
        }

        @Override
        public void serialize(RaoParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(VERSION, parameters.getVersion());
            jsonGenerator.writeObjectField(OBJECTIVE_FUNCTION, parameters.getObjectiveFunctionParameters());
            jsonGenerator.writeObjectField(RANGE_ACTIONS_OPTIMIZATION, parameters.getRangeActionsOptimizationParameters());
            jsonGenerator.writeObjectField(TOPOLOGICAL_ACTIONS_OPTIMIZATION, parameters.getTopoOptimizationParameters());
            jsonGenerator.writeObjectField(NOT_OPTIMIZED_CNECS, parameters.getNotOptimizedCnecsParameters());
            parameters.getMnecParameters().ifPresent(p -> {
                try {
                    jsonGenerator.writeObjectField(MNEC_PARAMETERS, p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            parameters.getRelativeMarginsParameters().ifPresent(p -> {
                try {
                    jsonGenerator.writeObjectField(RELATIVE_MARGINS, p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            parameters.getLoopFlowParameters().ifPresent(p -> {
                try {
                    jsonGenerator.writeObjectField(LOOP_FLOW_PARAMETERS, p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonRaoParameters.getExtensionSerializers());
            jsonGenerator.writeEndObject();
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void handleUnknownProperty(String name, Object value) {
        throw new com.powsybl.openrao.commons.OpenRaoException(String.format("Unexpected field in rao parameters: %s", name));
    }

    // ConfigLoader

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<RaoParameters>> extends ExtensionConfigLoader<RaoParameters, E> {
    }

    private static final Supplier<ExtensionProviders<RaoParameters.ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(RaoParameters.ConfigLoader.class, "rao-parameters"));

    /**
     * @return RaoParameters from platform default config.
     */
    public static RaoParameters load(final ReportNode reportNode) {
        return load(PlatformConfig.defaultConfig(), reportNode);
    }

    /**
     * @param platformConfig PlatformConfig where the RaoParameters should be read from
     * @return RaoParameters from the provided platform config
     */
    public static RaoParameters load(final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(platformConfig);
        RaoParameters parameters = new RaoParameters(reportNode);
        load(parameters, platformConfig, reportNode);
        parameters.loadExtensions(platformConfig);
        addOptionalExtensionsDefaultValuesIfExist(parameters, reportNode);
        return parameters;
    }

    public static void load(final RaoParameters parameters, final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);
        parameters.setObjectiveFunctionParameters(ObjectiveFunctionParameters.load(platformConfig));
        parameters.setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters.load(platformConfig));
        parameters.setTopoOptimizationParameters(TopoOptimizationParameters.load(platformConfig, reportNode));
        parameters.setNotOptimizedCnecsParameters(NotOptimizedCnecsParameters.load(platformConfig));
        MnecParameters.load(platformConfig).ifPresent(parameters::setMnecParameters);
        RelativeMarginsParameters.load(platformConfig).ifPresent(parameters::setRelativeMarginsParameters);
        LoopFlowParameters.load(platformConfig).ifPresent(parameters::setLoopFlowParameters);
    }

    private void loadExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            Extension extension = provider.load(platformConfig, reportNode);
            if (extension != null) {
                addExtension(provider.getExtensionClass(), extension);
            }
        }
    }

    public static void addOptionalExtensionsDefaultValuesIfExist(final RaoParameters parameters, final ReportNode reportNode) {
        OpenRaoSearchTreeParameters extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (parameters.getMnecParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getMnecParameters().isEmpty()) {
                extension.setMnecParameters(new SearchTreeRaoMnecParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getMnecParameters().isPresent()) {
                parameters.setMnecParameters(new com.powsybl.openrao.raoapi.parameters.MnecParameters());
            }
        }
        if (parameters.getRelativeMarginsParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getRelativeMarginsParameters().isEmpty()) {
                extension.setRelativeMarginsParameters(new SearchTreeRaoRelativeMarginsParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getRelativeMarginsParameters().isPresent()) {
                parameters.setRelativeMarginsParameters(new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters());
            }
        }
        if (parameters.getLoopFlowParameters().isPresent()) {
            if (Objects.isNull(extension)) {
                parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(reportNode));
            }
            extension = parameters.getExtension(OpenRaoSearchTreeParameters.class);
            if (extension.getLoopFlowParameters().isEmpty()) {
                extension.setLoopFlowParameters(new SearchTreeRaoLoopFlowParameters());
            }
        } else {
            if (!Objects.isNull(extension) && extension.getLoopFlowParameters().isPresent()) {
                parameters.setLoopFlowParameters(new com.powsybl.openrao.raoapi.parameters.LoopFlowParameters());
            }
        }
    }
}
