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
package org.apache.iotdb.cluster.query.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.iotdb.cluster.config.ClusterConfig;
import org.apache.iotdb.cluster.config.ClusterDescriptor;
import org.apache.iotdb.cluster.entity.Server;
import org.apache.iotdb.cluster.query.manager.querynode.ClusterLocalQueryManager;
import org.apache.iotdb.cluster.query.manager.querynode.ClusterLocalSingleQueryManager;
import org.apache.iotdb.cluster.query.reader.querynode.ClusterBatchReaderWithoutTimeGenerator;
import org.apache.iotdb.cluster.query.reader.querynode.IClusterBatchReader;
import org.apache.iotdb.cluster.utils.EnvironmentUtils;
import org.apache.iotdb.cluster.utils.QPExecutorUtils;
import org.apache.iotdb.jdbc.Config;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClusterLocalManagerTest {

  private Server server;
  private static final ClusterConfig CLUSTER_CONFIG = ClusterDescriptor.getInstance().getConfig();
  private static ClusterLocalQueryManager manager = ClusterLocalQueryManager.getInstance();

  private static final String URL = "127.0.0.1:6667/";

  private String[] createSQLs = {
      "set storage group to root.vehicle",
      "CREATE TIMESERIES root.vehicle.d0.s0 WITH DATATYPE=INT32, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s1 WITH DATATYPE=TEXT, ENCODING=PLAIN",
      "CREATE TIMESERIES root.vehicle.d0.s3 WITH DATATYPE=TEXT, ENCODING=PLAIN"
  };
  private String[] insertSQLs = {
      "insert into root.vehicle.d0(timestamp,s0) values(10,100)",
      "insert into root.vehicle.d0(timestamp,s0,s1) values(12,101,'102')",
      "insert into root.vehicle.d0(timestamp,s3) values(19,'103')",
      "insert into root.vehicle.d0(timestamp,s0,s1) values(22,1031,'3102')",
      "insert into root.vehicle.d0(timestamp,s1) values(192,'1033')"
  };
  private String queryStatementsWithoutFilter = "select * from root.vehicle";
  private String queryStatementsWithFilter = "select * from root.vehicle where d0.s0 > 10 and d0.s0 < 101 or d0.s0 = 3";

  @Before
  public void setUp() throws Exception {
    EnvironmentUtils.cleanEnv();
    EnvironmentUtils.closeStatMonitor();
    EnvironmentUtils.closeMemControl();
    QPExecutorUtils.setLocalNodeAddr("0.0.0.0", 0);
    CLUSTER_CONFIG.createAllPath();
    server = Server.getInstance();
    server.start();
    EnvironmentUtils.envSetUp();
    Class.forName(Config.JDBC_DRIVER_NAME);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
    EnvironmentUtils.cleanEnv();
  }

  @Test
  public void testClusterLocalQueryManagerWithoutFilter() throws Exception {
    try (Connection connection = DriverManager
        .getConnection(Config.IOTDB_URL_PREFIX + URL, "root", "root")) {
      insertData(connection);
      Statement statement = connection.createStatement();

      // first query
      boolean hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      ConcurrentHashMap<String, Long> map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(1, map.size());
      for (String taskId : map.keySet()) {
        assertNotNull(manager.getSingleQuery(taskId));
      }

      // second query
      hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(2, map.size());
      for (String taskId : map.keySet()) {
        assertNotNull(manager.getSingleQuery(taskId));
      }

      // third query
      hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(3, map.size());
      for (String taskId : map.keySet()) {
        assertNotNull(manager.getSingleQuery(taskId));
      }
      statement.close();
    }
  }

  @Test
  public void testClusterLocalQueryManagerWithFilter() throws Exception {
    try (Connection connection = DriverManager
        .getConnection(Config.IOTDB_URL_PREFIX + URL, "root", "root")) {
      insertData(connection);
      Statement statement = connection.createStatement();

      // first query
      boolean hasResultSet = statement.execute(queryStatementsWithFilter);
      assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      assertEquals(10, resultSet.getLong(1));
      assertEquals(100, resultSet.getInt(2));
      assertNull(resultSet.getString(3));
      assertNull(resultSet.getString(4));
      ConcurrentHashMap<String, Long> map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(1, map.size());
      for (String taskId : map.keySet()) {
        assertNotNull(manager.getSingleQuery(taskId));
      }
      assertFalse(resultSet.next());

//      // second query
//      hasResultSet = statement.execute(queryStatementsWithoutFilter);
//      assertTrue(hasResultSet);
//      resultSet = statement.getResultSet();
//      assertTrue(resultSet.next());
//      map = ClusterLocalQueryManager.getTaskIdMapJobId();
//      assertEquals(2, map.size());
//      for (String taskId : map.keySet()) {
//        assertNotNull(manager.getSingleQuery(taskId));
//      }
//
//      // third query
//      hasResultSet = statement.execute(queryStatementsWithoutFilter);
//      assertTrue(hasResultSet);
//      resultSet = statement.getResultSet();
//      assertTrue(resultSet.next());
//      map = ClusterLocalQueryManager.getTaskIdMapJobId();
//      assertEquals(3, map.size());
//      for (String taskId : map.keySet()) {
//        assertNotNull(manager.getSingleQuery(taskId));
//      }
      statement.close();
    }
  }

  @Test
  public void testClusterLocalSingleQueryWithoutFilterManager() throws Exception {
    try (Connection connection = DriverManager
        .getConnection(Config.IOTDB_URL_PREFIX + URL, "root", "root")) {
      insertData(connection);
      Statement statement = connection.createStatement();

      // first query
      boolean hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      ConcurrentHashMap<String, Long> map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(1, map.size());
      for (String taskId : map.keySet()) {
        ClusterLocalSingleQueryManager singleQueryManager = manager.getSingleQuery(taskId);
        assertNotNull(singleQueryManager);
        assertEquals((long) map.get(taskId), singleQueryManager.getJobId());
        assertEquals(1, singleQueryManager.getQueryRound());
        assertNull(singleQueryManager.getFilterReader());
        Map<String, IClusterBatchReader> selectSeriesReaders = singleQueryManager
            .getSelectSeriesReaders();
        assertEquals(3, selectSeriesReaders.size());
        Map<String, TSDataType> typeMap = singleQueryManager.getDataTypeMap();
        for (Entry<String, IClusterBatchReader> entry : selectSeriesReaders.entrySet()) {
          String path = entry.getKey();
          TSDataType dataType = typeMap.get(path);
          IClusterBatchReader clusterBatchReader = entry.getValue();
          assertNotNull(((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getReader());
          assertEquals(dataType,
              ((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getDataType());
        }
      }

      // second query
      hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(2, map.size());
      for (String taskId : map.keySet()) {
        ClusterLocalSingleQueryManager singleQueryManager = manager.getSingleQuery(taskId);
        assertNotNull(singleQueryManager);
        assertEquals((long) map.get(taskId), singleQueryManager.getJobId());
        assertEquals(1, singleQueryManager.getQueryRound());
        assertNull(singleQueryManager.getFilterReader());
        Map<String, IClusterBatchReader> selectSeriesReaders = singleQueryManager
            .getSelectSeriesReaders();
        assertEquals(3, selectSeriesReaders.size());
        Map<String, TSDataType> typeMap = singleQueryManager.getDataTypeMap();
        for (Entry<String, IClusterBatchReader> entry : selectSeriesReaders.entrySet()) {
          String path = entry.getKey();
          TSDataType dataType = typeMap.get(path);
          IClusterBatchReader clusterBatchReader = entry.getValue();
          assertNotNull(((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getReader());
          assertEquals(dataType,
              ((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getDataType());
        }
      }

      // third query
      hasResultSet = statement.execute(queryStatementsWithoutFilter);
      assertTrue(hasResultSet);
      resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      map = ClusterLocalQueryManager.getTaskIdMapJobId();
      assertEquals(3, map.size());
      for (String taskId : map.keySet()) {
        ClusterLocalSingleQueryManager singleQueryManager = manager.getSingleQuery(taskId);
        assertNotNull(singleQueryManager);
        assertEquals((long) map.get(taskId), singleQueryManager.getJobId());
        assertEquals(1, singleQueryManager.getQueryRound());
        assertNull(singleQueryManager.getFilterReader());
        Map<String, IClusterBatchReader> selectSeriesReaders = singleQueryManager
            .getSelectSeriesReaders();
        assertEquals(3, selectSeriesReaders.size());
        Map<String, TSDataType> typeMap = singleQueryManager.getDataTypeMap();
        for (Entry<String, IClusterBatchReader> entry : selectSeriesReaders.entrySet()) {
          String path = entry.getKey();
          TSDataType dataType = typeMap.get(path);
          IClusterBatchReader clusterBatchReader = entry.getValue();
          assertNotNull(((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getReader());
          assertEquals(dataType,
              ((ClusterBatchReaderWithoutTimeGenerator) clusterBatchReader).getDataType());
        }
      }
      statement.close();
    }
  }
//
//  @Test
//  public void testClusterLocalSingleQueryWithFilterManager() throws Exception {
//    try (Connection connection = DriverManager
//        .getConnection(Config.IOTDB_URL_PREFIX + URL, "root", "root")) {
//      insertData(connection);
//      Statement statement = connection.createStatement();
//      boolean hasResultSet = statement.execute(queryStatementsWithFilter);
//      assertTrue(hasResultSet);
//      ResultSet resultSet = statement.getResultSet();
//      assertTrue(resultSet.next());
//      ConcurrentHashMap<Long, String> map = ClusterRpcQueryManager.getJobIdMapTaskId();
//      assertEquals(1, map.size());
//      for (String taskId : map.values()) {
//        ClusterRpcSingleQueryManager singleManager = manager.getSingleQuery(taskId);
//        assertNotNull(singleManager);
//        assertEquals(0, singleManager.getQueryRounds());
//        assertEquals(taskId, singleManager.getTaskId());
//
//        // select path plans
//        assertFalse(singleManager.getFilterPathPlans().isEmpty());
//        Map<String, QueryPlan> selectPathPlans = singleManager.getSelectPathPlans();
//        assertEquals(1, selectPathPlans.size());
//        for (QueryPlan queryPlan : selectPathPlans.values()) {
//          List<Path> paths = queryPlan.getPaths();
//          List<Path> correctPaths = new ArrayList<>();
//          correctPaths.add(new Path("root.vehicle.d0.s0"));
//          correctPaths.add(new Path("root.vehicle.d0.s1"));
//          correctPaths.add(new Path("root.vehicle.d0.s3"));
//          assertEquals(correctPaths, paths);
//          assertNotNull(queryPlan.getExpression());
//        }
//
//        // select series by group id
//        assertTrue(singleManager.getSelectSeriesByGroupId().isEmpty());
//
//        // select series reader
//        assertTrue(singleManager
//            .getSelectSeriesReaders().isEmpty());
//
//        // filter path plans
//        assertFalse(singleManager.getFilterPathPlans().isEmpty());
//        Map<String, QueryPlan> filterSeriesPath = singleManager.getFilterPathPlans();
//        assertEquals(1, filterSeriesPath.size());
//        for (QueryPlan queryPlan : filterSeriesPath.values()) {
//          List<Path> paths = queryPlan.getPaths();
//          List<Path> correctPaths = new ArrayList<>();
//          correctPaths.add(new Path("root.vehicle.d0.s0"));
//          assertEquals(correctPaths, paths);
//          assertNotNull(queryPlan.getExpression());
//        }
//
//        // filter series by group id
//        assertTrue(singleManager.getFilterSeriesByGroupId().isEmpty());
//
//        // filter series reader
//        assertTrue(singleManager.getFilterSeriesReaders().isEmpty());
//
//      }
//      statement.close();
//    }
//  }

  private void insertData(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      for (String createSql : createSQLs) {
        statement.execute(createSql);
      }
      for (String insertSql : insertSQLs) {
        statement.execute(insertSql);
      }
    }
  }

}