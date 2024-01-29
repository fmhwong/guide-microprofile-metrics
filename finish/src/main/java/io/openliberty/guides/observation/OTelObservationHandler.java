package io.openliberty.guides.observation;

import java.util.HashMap;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;

public class OTelObservationHandler implements MeterObservationHandler<Observation.Context> {

    private Meter meter;
    private HashMap<String, Long> timerMap = new HashMap<String, Long>();
    
    public OTelObservationHandler(Meter meter) {
        this.meter = meter;
    }
    
    @Override
    public void onStart(Context context) {
        String timerName = context.getName() + ".active";
        DoubleHistogram doubleHistrogram = meter.histogramBuilder(timerName).build();
        context.put(timerName, doubleHistrogram);
        timerMap.put(timerName + context.getLowCardinalityKeyValues().toString(), System.nanoTime());
    }

    @Override
    public void onStop(Context context) {
        String timerName = context.getName() + ".active";
        long duration_nano = System.nanoTime() - timerMap.get(timerName + context.getLowCardinalityKeyValues().toString());
        double duration_sec = (double) duration_nano / 1000000000.0;
        DoubleHistogram doubleHistrogram = context.getRequired(timerName);
        AttributesBuilder attributesBuilder = Attributes.builder();
        KeyValues keyValues = context.getLowCardinalityKeyValues();
        for (KeyValue kv : keyValues) {
            attributesBuilder.put(kv.getKey(), kv.getValue());
        }
        doubleHistrogram.record(duration_sec, attributesBuilder.build());
    }

}
