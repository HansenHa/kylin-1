/*
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kylin.query.util;

import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.util.LocalFileMetadataTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class QueryUtilTest extends LocalFileMetadataTestCase {

    static final String catalog = "CATALOG";

    @Before
    public void setUp() throws Exception {
        this.createTestMetadata();
    }

    @After
    public void after() throws Exception {
        this.cleanupTestMetadata();
    }

    @Test
    public void testMassageSql() {
        {
            String sql = "select ( date '2001-09-28' + interval floor(1.2) day) from test_kylin_fact";
            String s = QueryUtil.massageSql(sql, "default", 0, 0, "DEFAULT");
            Assert.assertEquals("select ( date '2001-09-28' + interval '1' day) from test_kylin_fact", s);
        }
        {
            String sql = "select ( date '2001-09-28' + interval floor(2) month) from test_kylin_fact group by ( date '2001-09-28' + interval floor(2) month)";
            String s = QueryUtil.massageSql(sql, "default", 0, 0, "DEFAULT");
            Assert.assertEquals(
                    "select ( date '2001-09-28' + interval '2' month) from test_kylin_fact group by ( date '2001-09-28' + interval '2' month)",
                    s);
        }
        {
            String sql = "select count(*) test_limit from test_kylin_fact where price > 10.0";
            String s = QueryUtil.massageSql(sql, "default", 50000, 0, "DEFAULT");
            Assert.assertEquals(
                    "select count(*) test_limit from test_kylin_fact where price > 10.0\n" +
                            "LIMIT 50000",
                    s);
        }
        {
            String sql = "select count(*) test_offset from test_kylin_fact where price > 10.0";
            String s = QueryUtil.massageSql(sql, "default", 0, 50, "DEFAULT");
            Assert.assertEquals(
                    "select count(*) test_offset from test_kylin_fact where price > 10.0\n" +
                            "OFFSET 50",
                    s);
        }
        {
            String sql = "select count(*) test_limit_and_offset from test_kylin_fact where price > 10.0";
            String s = QueryUtil.massageSql(sql, "default", 50000, 50, "DEFAULT");
            Assert.assertEquals(
                    "select count(*) test_limit_and_offset from test_kylin_fact where price > 10.0\n" +
                            "LIMIT 50000\nOFFSET 50",
                    s);
        }

        {
            String newLine = System.getProperty("line.separator");
            String sql = "select count(*)     test_limit from " + newLine + "test_kylin_fact where price > 10.0";
            newLine = newLine.replace("\r", " ").replace("\n", newLine);
            String s = QueryUtil.massageSql(sql, "default", 50000, 0, "DEFAULT");
            Assert.assertEquals(
                    "select count(*)     test_limit from " + newLine + "test_kylin_fact where price > 10.0\nLIMIT 50000",
                    s);
        }
        {
            String newLine = System.getProperty("line.separator");
            String sql = "select count(*)     test_offset from " + newLine + "test_kylin_fact where price > 10.0";
            newLine = newLine.replace("\r", " ").replace("\n", newLine);
            String s = QueryUtil.massageSql(sql, "default", 50000, 0, "DEFAULT");
            Assert.assertEquals(
                    "select count(*)     test_offset from " + newLine + "test_kylin_fact where price > 10.0\nLIMIT 50000",
                    s);
        }
        {
            String newLine = System.getProperty("line.separator");
            String sql = "select count(*)     test_limit_and_offset from " + newLine + "test_kylin_fact where price > 10.0";
            newLine = newLine.replace("\r", " ").replace("\n", newLine);
            String s = QueryUtil.massageSql(sql, "default", 50000, 0, "DEFAULT");
            Assert.assertEquals(
                    "select count(*)     test_limit_and_offset from " + newLine + "test_kylin_fact where price > 10.0\nLIMIT 50000",
                    s);
        }
    }

    @Test
    public void testIsSelect() {
        {
            String sql = "select ( date '2001-09-28' + interval floor(1.2) day) from test_kylin_fact";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(true, selectStatement);
        }
        {
            String sql = " Select ( date '2001-09-28' + interval floor(1.2) day) from test_kylin_fact";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(true, selectStatement);
        }
        {
            String sql = " \n" + "Select ( date '2001-09-28' + interval floor(1.2) day) from test_kylin_fact";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(true, selectStatement);
        }
        {
            String sql = "--comment\n"
                    + " /* comment */Select ( date '2001-09-28' + interval floor(1.2) day) from test_kylin_fact";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(true, selectStatement);
        }
        {
            String sql = " UPDATE Customers\n" + "SET ContactName = 'Alfred Schmidt', City= 'Frankfurt'\n"
                    + "WHERE CustomerID = 1;";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(false, selectStatement);
        }
        {
            String sql = " explain plan for select count(*) from test_kylin_fact\n";
            boolean selectStatement = QueryUtil.isSelectStatement(sql);
            Assert.assertEquals(true, selectStatement);
        }
    }

    @Test
    public void testKeywordDefaultDirtyHack() {
        {
            KylinConfig.getInstanceFromEnv().setProperty("kylin.query.escape-default-keyword", "true");
            String sql = "select * from DEFAULT.TEST_KYLIN_FACT";
            String s = QueryUtil.massageSql(sql, "default", 0, 0, "DEFAULT");
            Assert.assertEquals("select * from \"DEFAULT\".TEST_KYLIN_FACT", s);
        }
    }

    @Test
    public void testForceLimit() {
        KylinConfig.getInstanceFromEnv().setProperty("kylin.query.force-limit", "10");
        String sql1 = "select   * \nfrom DEFAULT.TEST_KYLIN_FACT";
        String result = QueryUtil.massageSql(sql1, "default", 0, 0, "DEFAULT");
        Assert.assertEquals("select   * \nfrom DEFAULT.TEST_KYLIN_FACT\nLIMIT 10", result);

        String sql2 = "select   2 * 8 from DEFAULT.TEST_KYLIN_FACT";
        result = QueryUtil.massageSql(sql2, "default", 0, 0, "DEFAULT");
        Assert.assertEquals("select   2 * 8 from DEFAULT.TEST_KYLIN_FACT", result);
    }

    @Test
    public void testRemoveCommentInSql() {

        String originSql = "select count(*) from test_kylin_fact where price > 10.0";

        {
            String sqlWithComment = "-- comment \n" + originSql;

            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "-- comment \n -- comment\n" + originSql;
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {

            String sqlWithComment = "-- \n -- comment \n" + originSql;
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = originSql + "-- \n -- comment \n";
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "-- \n -- comment \n" + originSql + "-- \n -- comment \n";
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment */ " + originSql + "-- \n -- comment \n";
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment1/comment2 */ " + originSql;
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment1 * comment2 */ " + originSql;
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment1 * comment2 */ /* comment3 / comment4 */ -- comment 5\n" + originSql;
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment1 * \ncomment2 */ -- comment 5\n" + originSql + "/* comment3 / comment4 */";
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }

        {
            String sqlWithComment = "/* comment1 * \ncomment2 */ -- comment 3\n" + originSql + "-- comment 5";
            Assert.assertEquals(originSql, QueryUtil.removeCommentInSql(sqlWithComment));
        }
    }

    @Test
    public void testUnknownErrorResponseMessage() {
        String msg = QueryUtil.makeErrorMsgUserFriendly(new NullPointerException());
        Assert.assertEquals("Unknown error.", msg);
    }

    @Test
    public void testRemoveCatalog() {

        String[] beforeRemoveSql = new String[] {
                "select name, count(*) as cnt from schema1.user where bb.dd >2 group by name",
                "select name, count(*) as cnt from .default2.user where dd >2 group by name",
                "select name, count(*) as cnt from %s.default2.user where dd >2 group by name",
                "select name, count(*) as cnt from %s.user.a.cu where dd >2 group by name",
                "select name, count(*) as cnt from %s.default2.user where dd >2 group by name",
                "select name, count() as cnt from %s.test.kylin_sales inner join " + "%s.test.kylin_account "
                        + "ON kylin_sales.BUYER_ID=kylin_account.ACCOUNT_ID group by name",
                "select schema1.table1.col1 from %s.schema1.table1" };
        String[] afterRemoveSql = new String[] {
                "select name, count(*) as cnt from schema1.user where bb.dd >2 group by name",
                "select name, count(*) as cnt from .default2.user where dd >2 group by name",
                "select name, count(*) as cnt from default2.user where dd >2 group by name",
                "select name, count(*) as cnt from user.a.cu where dd >2 group by name",
                "select name, count(*) as cnt from default2.user where dd >2 group by name",
                "select name, count() as cnt from test.kylin_sales inner join " + "test.kylin_account "
                        + "ON kylin_sales.BUYER_ID=kylin_account.ACCOUNT_ID group by name",
                "select schema1.table1.col1 from schema1.table1" };
        Assert.assertEquals(afterRemoveSql.length, beforeRemoveSql.length);
        for (int i = 0; i < beforeRemoveSql.length; i++) {
            String before = beforeRemoveSql[i];
            before = before.replace("%s", catalog);
            String after = afterRemoveSql[i];
            Assert.assertEquals(after, QueryUtil.removeCatalog(before, catalog));
        }
    }
}
