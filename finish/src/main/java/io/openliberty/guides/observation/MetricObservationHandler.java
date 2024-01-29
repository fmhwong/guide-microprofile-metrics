package io.openliberty.guides.observation;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;


public class MetricObservationHandler implements MeterObservationHandler<Observation.Context> {

    private final MetricRegistry metricRegistry;

    public MetricObservationHandler(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onStart(Observation.Context context) {
        String timerName = "mp." + context.getName() + ".active";

        KeyValues keyValues = context.getLowCardinalityKeyValues();
        int size = 0;
        for (KeyValue kv : keyValues) {
            size++;
        }
        Tag[] tags = new Tag[size];
        int counter = 0;
        for (KeyValue kv : keyValues) {
            tags[counter] = new Tag(kv.getKey(), kv.getValue());
            counter++;
        }
        Timer timer = metricRegistry.timer(timerName, tags);
        context.put(timerName, timer);
    }

    @Override
    public void onStop(Observation.Context context) {
        String timerName = "mp." + context.getName() + ".active";
        Timer timer = context.getRequired(timerName);
        timer.time().stop();
    }

    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
    }

}
