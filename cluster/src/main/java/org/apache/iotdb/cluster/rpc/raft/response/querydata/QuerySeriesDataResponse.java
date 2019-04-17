/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.cluster.rpc.raft.response.querydata;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.iotdb.cluster.query.PathType;
import org.apache.iotdb.cluster.rpc.raft.response.BasicResponse;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.BatchData;

public class QuerySeriesDataResponse extends BasicResponse {

  private static final long serialVersionUID = -4783032133961145205L;

  private Map<PathType, List<TSDataType>> seriesDataTypes = new EnumMap<>(PathType.class);

  /**
   * Series type
   */
  private PathType pathType;

  private List<BatchData> seriesBatchData = new ArrayList<>();

  public QuerySeriesDataResponse(String groupId) {
    super(groupId, false, null, null);
  }

  public Map<PathType, List<TSDataType>> getSeriesDataTypes() {
    return seriesDataTypes;
  }

  public void setSeriesDataTypes(
      Map<PathType, List<TSDataType>> seriesDataTypes) {
    this.seriesDataTypes = seriesDataTypes;
  }

  public PathType getPathType() {
    return pathType;
  }

  public void setPathType(PathType pathType) {
    this.pathType = pathType;
  }

  public List<BatchData> getSeriesBatchData() {
    return seriesBatchData;
  }

  public void setSeriesBatchData(
      List<BatchData> seriesBatchData) {
    this.seriesBatchData = seriesBatchData;
  }
}
