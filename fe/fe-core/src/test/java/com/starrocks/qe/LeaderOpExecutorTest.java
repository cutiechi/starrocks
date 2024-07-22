// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.starrocks.qe;

import com.starrocks.analysis.RedirectStatus;
import com.starrocks.common.Config;
import com.starrocks.common.ErrorCode;
import com.starrocks.common.ErrorReportException;
import com.starrocks.common.FeConstants;
import com.starrocks.common.util.UUIDUtil;
import com.starrocks.pseudocluster.PseudoCluster;
import com.starrocks.rpc.ThriftConnectionPool;
import com.starrocks.server.GlobalStateMgr;
import com.starrocks.server.WarehouseManager;
import com.starrocks.service.FrontendServiceImpl;
import com.starrocks.sql.ast.StatementBase;
import com.starrocks.sql.ast.UserIdentity;
import com.starrocks.thrift.FrontendService;
import com.starrocks.thrift.TMasterOpRequest;
import com.starrocks.thrift.TMasterOpResult;
import com.starrocks.thrift.TNetworkAddress;
import com.starrocks.utframe.MockGenericPool;
import com.starrocks.utframe.StarRocksAssert;
import com.starrocks.utframe.UtFrameUtils;
import com.starrocks.warehouse.DefaultWarehouse;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LeaderOpExecutorTest {
    private static ConnectContext connectContext;
    private static StarRocksAssert starRocksAssert;
    private static PseudoCluster cluster;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Config.bdbje_heartbeat_timeout_second = 60;
        Config.bdbje_replica_ack_timeout_second = 60;
        Config.bdbje_lock_timeout_second = 60;
        // set some parameters to speedup test
        Config.tablet_sched_checker_interval_seconds = 1;
        Config.tablet_sched_repair_delay_factor_second = 1;
        Config.enable_new_publish_mechanism = true;
        PseudoCluster.getOrCreateWithRandomPort(true, 1);
        GlobalStateMgr.getCurrentState().getTabletChecker().setInterval(1000);
        cluster = PseudoCluster.getInstance();

        FeConstants.runningUnitTest = true;
        Config.alter_scheduler_interval_millisecond = 100;
        Config.dynamic_partition_enable = true;
        Config.dynamic_partition_check_interval_seconds = 1;
        // create connect context
        connectContext = UtFrameUtils.createDefaultCtx();
        starRocksAssert = new StarRocksAssert(connectContext);

        starRocksAssert.withDatabase("d1").useDatabase("d1")
                .withTable(
                        "CREATE TABLE d1.t1(k1 int, k2 int, k3 int)" +
                                " distributed by hash(k1) buckets 3 properties('replication_num' = '1');")
                .withTable(
                        "CREATE TABLE d1.t2(k1 int, k2 int, k3 int)" +
                                " distributed by hash(k1) buckets 3 properties('replication_num' = '1');");
    }

    @Test
    public void testResourceGroupNameInAuditLog() throws Exception {

        String createGroup = "create resource group rg1\n" +
                "to\n" +
                "    (db='d1')\n" +
                "with (\n" +
                "    'cpu_core_limit' = '1',\n" +
                "    'mem_limit' = '50%',\n" +
                "    'concurrency_limit' = '20',\n" +
                "    'type' = 'normal'\n" +
                ");";
        cluster.runSql("d1", createGroup);

        String sql = "insert into t1 select * from t1";
        StatementBase stmtBase = UtFrameUtils.parseStmtWithNewParser(sql, connectContext);
        LeaderOpExecutor executor =
                new LeaderOpExecutor(stmtBase, stmtBase.getOrigStmt(), connectContext, RedirectStatus.FORWARD_NO_SYNC);

        mockFrontendService(new MockFrontendServiceClient());
        executor.execute();

        Assert.assertEquals("rg1", connectContext.getAuditEventBuilder().build().resourceGroup);
    }

    private static class MockFrontendServiceClient extends FrontendService.Client {
        private final FrontendService.Iface frontendService = new FrontendServiceImpl(null);

        public MockFrontendServiceClient() {
            super(null);
        }

        @Override
        public TMasterOpResult forward(TMasterOpRequest params) throws TException {
            return frontendService.forward(params);
        }
    }

    private static void mockFrontendService(MockFrontendServiceClient client) {
        ThriftConnectionPool.frontendPool = new MockGenericPool<FrontendService.Client>("leader-op-mocked-pool") {
            @Override
            public FrontendService.Client borrowObject(TNetworkAddress address, int timeoutMs) {
                return client;
            }
        };
    }

    @Test
    public void testForwardTooManyTimes() {
        ConnectContext connectContext = new ConnectContext();
        connectContext.setForwardTimes(LeaderOpExecutor.MAX_FORWARD_TIMES);

        try {
            new LeaderOpExecutor(new OriginStatement("show frontends"), connectContext, RedirectStatus.FORWARD_NO_SYNC)
                    .execute();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ErrorReportException);
            Assert.assertEquals(ErrorCode.ERR_FORWARD_TOO_MANY_TIMES, ((ErrorReportException) e).getErrorCode());
            return;
        }
        Assert.fail("should throw ERR_FORWARD_TOO_MANY_TIMES exception");
    }

    @Test
    public void testCreateTMasterOpRequest(@Mocked GlobalStateMgr globalStateMgr, @Mocked WarehouseManager warehouseManager) {
        new MockUp<GlobalStateMgr>() {
            @Mock
            public boolean isReady() {
                return true;
            }

        };
        new Expectations() {
            {
                globalStateMgr.getServingState();
                minTimes = 0;

                globalStateMgr.getWarehouseMgr();
                result = warehouseManager;
                minTimes = 1;

                warehouseManager.getWarehouse(10001L);
                result = new DefaultWarehouse(10001L, "wh1");
                minTimes = 1;

                warehouseManager.getWarehouse("wh1");
                result = new DefaultWarehouse(10001L, "wh1");
                minTimes = 1;
            }
        };
        ConnectContext connectContext = new ConnectContext();
        connectContext.setGlobalStateMgr(globalStateMgr);
        connectContext.setCurrentWarehouseId(10001L);
        connectContext.setCurrentUserIdentity(UserIdentity.ROOT);
        connectContext.setCurrentRoleIds(UserIdentity.ROOT);
        connectContext.setQueryId(UUIDUtil.genUUID());
        connectContext.setThreadLocalInfo();

        LeaderOpExecutor executor = new LeaderOpExecutor(new OriginStatement(""),
                connectContext, RedirectStatus.FORWARD_NO_SYNC);
        TMasterOpRequest request = executor.createTMasterOpRequest(connectContext, 1);
        Assert.assertEquals(10001L, request.getWarehouse_id());
    }
}
