package it.reply.orchestrator.dto.ranker;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaaSMetric;

import java.io.Serializable;

/**
 * Hack to make jBPM async execution work (it requires serialization, thus all Serializable
 * objects!)
 * 
 * @author l.biava
 *
 */
public class PaaSMetricSerializable extends PaaSMetric implements Serializable {

  private static final long serialVersionUID = -8858480991803824156L;

}
