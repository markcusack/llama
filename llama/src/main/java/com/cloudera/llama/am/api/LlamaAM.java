/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional inforAMtion
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you AMy not use this file except in compliance
 * with the License.  You AMy obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.llama.am.api;

import com.cloudera.llama.am.impl.APIContractLlamaAM;
import com.cloudera.llama.am.impl.GangAntiDeadlockLlamaAM;
import com.cloudera.llama.am.impl.MultiQueueLlamaAM;
import com.cloudera.llama.am.impl.ObserverLlamaAM;
import com.cloudera.llama.util.LlamaException;
import com.cloudera.llama.util.ParamChecker;
import com.cloudera.llama.util.UUID;
import com.codahale.metrics.MetricRegistry;
import org.apache.hadoop.conf.Configuration;

import java.util.List;
import java.util.Map;

public abstract class LlamaAM {
  public static final String PREFIX_KEY = "llama.am.";

  public static final String METRIC_PREFIX = "llama.am.";

  public static final String RM_CONNECTOR_CLASS_KEY = PREFIX_KEY +
      "rm.connector.class";

  public static final String INITIAL_QUEUES_KEY = PREFIX_KEY +
      "initial.queues";

  public static final String GANG_ANTI_DEADLOCK_ENABLED_KEY = PREFIX_KEY +
      "gang.anti.deadlock.enabled";
  public static final boolean GANG_ANTI_DEADLOCK_ENABLED_DEFAULT = true;

  public static final String GANG_ANTI_DEADLOCK_NO_ALLOCATION_LIMIT_KEY =
      PREFIX_KEY + "gang.anti.deadlock.no.allocation.limit.ms";
  public static final long GANG_ANTI_DEADLOCK_NO_ALLOCATION_LIMIT_DEFAULT =
      30000;

  public static final String GANG_ANTI_DEADLOCK_BACKOFF_PERCENT_KEY =
      PREFIX_KEY + "gang.anti.deadlock.backoff.percent";
  public static final int GANG_ANTI_DEADLOCK_BACKOFF_PERCENT_DEFAULT =
      30;

  public static final String GANG_ANTI_DEADLOCK_BACKOFF_MIN_DELAY_KEY =
      PREFIX_KEY + "gang.anti.deadlock.backoff.min.delay.ms";
  public static final long GANG_ANTI_DEADLOCK_BACKOFF_MIN_DELAY_DEFAULT = 10000;

  public static final String GANG_ANTI_DEADLOCK_BACKOFF_MAX_DELAY_KEY =
      PREFIX_KEY + "gang.anti.deadlock.backoff.max.delay.ms";
  public static final long GANG_ANTI_DEADLOCK_BACKOFF_MAX_DELAY_DEFAULT = 30000;

  public static final String RESOURCES_CACHING_ENABLED_KEY =
      PREFIX_KEY + "resources.caching.enabled";
  public static final boolean RESOURCES_CACHING_ENABLED_DEFAULT = true;

  private static Configuration cloneConfiguration(Configuration conf) {
    Configuration clone = new Configuration(false);
    for (Map.Entry<String, String> entry : conf) {
      clone.set(entry.getKey(), entry.getValue());
    }
    return clone;
  }

  public static LlamaAM create(Configuration conf)
      throws LlamaException {
    conf = cloneConfiguration(conf);
    LlamaAM am = new MultiQueueLlamaAM(conf);
    if (conf.getBoolean(GANG_ANTI_DEADLOCK_ENABLED_KEY,
        GANG_ANTI_DEADLOCK_ENABLED_DEFAULT)) {
      am = new GangAntiDeadlockLlamaAM(conf, am);
    }
    am = new ObserverLlamaAM(am);
    return new APIContractLlamaAM(am);
  }

  private MetricRegistry metricRegistry;
  private Configuration conf;

  protected LlamaAM(Configuration conf) {
    this.conf = ParamChecker.notNull(conf, "conf");
  }

  public void setMetricRegistry(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  protected MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  public Configuration getConf() {
    return conf;
  }

  public abstract void start() throws LlamaException;

  public abstract void stop();

  public abstract boolean isRunning();

  public abstract List<String> getNodes() throws LlamaException;

  public abstract PlacedReservation reserve(UUID reservationId,
      Reservation reservation)
      throws LlamaException;

  public PlacedReservation reserve(Reservation reservation)
      throws LlamaException {
    UUID reservationId = UUID.randomUUID();
    return reserve(reservationId, reservation);
  }

  public abstract PlacedReservation getReservation(UUID reservationId)
      throws LlamaException;

  public static final UUID ADMIN_HANDLE = UUID.randomUUID();

  public abstract PlacedReservation releaseReservation(UUID handle,
      UUID reservationId)
      throws LlamaException;

  public abstract List<PlacedReservation> releaseReservationsForHandle(
      UUID handle)
      throws LlamaException;

  public abstract List<PlacedReservation> releaseReservationsForQueue(
      String queue) throws LlamaException;

  public abstract void addListener(LlamaAMListener listener);

  public abstract void removeListener(LlamaAMListener listener);

}
