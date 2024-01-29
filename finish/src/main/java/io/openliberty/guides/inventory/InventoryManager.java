/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.guides.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.model.SystemData;
import io.openliberty.guides.observation.MetricObservationHandler;
import io.openliberty.guides.observation.OTelObservationHandler;
import io.openliberty.guides.observation.SimpleHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InventoryManager {

    private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
    private InventoryUtils invUtils = new InventoryUtils();
    private ObservationRegistry registry;
    
    @Inject
    MetricRegistry metricRegistry;
    
    private synchronized void init() {
        if (registry == null) {
            registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(new SimpleHandler());
            
            // MicroProfile Metrics
            registry.observationConfig().observationHandler(new MetricObservationHandler(metricRegistry));
            
            // OpenTelemetry Metrics
            OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
            MeterProvider meterProvider = openTelemetry.getMeterProvider();
            Meter meter = meterProvider.meterBuilder("otel").build();
            registry.observationConfig().observationHandler(new OTelObservationHandler(meter));
        }
    }
    
    
//    @Timed(name = "inventoryProcessingTime",
//            tags = { "method=get" },
//            absolute = true,
//            description = "Time needed to process the inventory")
    public Properties get(String hostname) {
        if (registry == null) {
            init();
        }
        return Observation.createNotStarted("inventoryProcessingTime", registry)
                .lowCardinalityKeyValue("method", "get")
                .observe(() -> {
                        return invUtils.getProperties(hostname);
        });
    }
    

//    @Timed(name = "inventoryAddingTime",
//            absolute = true,
//            description = "Time needed to add system properties to the inventory")
    public void add(String hostname, Properties systemProps) {
        Observation.createNotStarted("inventoryProcessingTime", registry)
        .lowCardinalityKeyValue("method", "add")
        .observe(() -> {
            Properties props = new Properties();
            props.setProperty("os.name", systemProps.getProperty("os.name"));
            props.setProperty("user.name", systemProps.getProperty("user.name"));

            SystemData host = new SystemData(hostname, props);
            if (!systems.contains(host)) {
                systems.add(host);
            }
        });
    }

//    @Timed(name = "inventoryProcessingTime",
//            tags = { "method=list" },
//            absolute = true,
//            description = "Time needed to process the inventory")
//    @Counted(name = "inventoryAccessCount", absolute = true, description = "Number of times the list of systems method is requested")
    public InventoryList list() {
        return Observation.createNotStarted("inventoryProcessingTime", registry)
                .lowCardinalityKeyValue("method", "list")
                .observe(() -> {
                    return new InventoryList(systems);
        });
    }

    @Gauge(unit = MetricUnits.NONE,
            // end::unitForGetTotal[]
            name = "inventorySizeGauge", absolute = true, description = "Number of systems in the inventory")
    public int getTotal() {
        return systems.size();
    }
}
