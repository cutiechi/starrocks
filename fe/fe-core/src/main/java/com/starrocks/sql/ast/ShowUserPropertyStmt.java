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


package com.starrocks.sql.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.starrocks.authentication.AuthenticationMgr;
import com.starrocks.authentication.UserProperty;
import com.starrocks.catalog.Column;
import com.starrocks.catalog.ScalarType;
import com.starrocks.common.CaseSensibility;
import com.starrocks.common.PatternMatcher;
import com.starrocks.qe.ConnectContext;
import com.starrocks.qe.ShowResultSetMetaData;
import com.starrocks.server.GlobalStateMgr;
import com.starrocks.sql.parser.NodePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Show Property Stmt
//  syntax:
//      SHOW PROPERTY [FOR user] [LIKE key pattern]
public class ShowUserPropertyStmt extends ShowStmt {
    public static final ImmutableList<String> TITLE_NAMES = new ImmutableList.Builder<String>()
            .add("Key").add("Value").build();

    private String user;
    private String pattern;

    public ShowUserPropertyStmt(String user, String pattern) {
        this(user, pattern, NodePosition.ZERO);
    }

    public ShowUserPropertyStmt(String user, String pattern, NodePosition pos) {
        super(pos);
        this.user = user;
        this.pattern = pattern;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPatter() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<List<String>> getRows(ConnectContext connectContext) {
        List<List<String>> rows = new ArrayList<>();
        AuthenticationMgr authenticationManager = GlobalStateMgr.getCurrentState().getAuthenticationMgr();

        UserProperty userProperty = authenticationManager.getUserProperty(user);
        rows.add(Lists.newArrayList(UserProperty.PROP_MAX_USER_CONNECTIONS, String.valueOf(userProperty.getMaxConn())));
        rows.add(Lists.newArrayList(UserProperty.PROP_DATABASE, userProperty.getDatabase()));
        for (Map.Entry<String, String> entry : userProperty.getSessionVariables().entrySet()) {
            rows.add(Lists.newArrayList(String.format("%s.%s", "session", entry.getKey()), entry.getValue()));
        }

        if (pattern == null) {
            return rows;
        }

        List<List<String>> result = Lists.newArrayList();
        PatternMatcher matcher = PatternMatcher.createMysqlPattern(pattern,
                CaseSensibility.USER.getCaseSensibility());
        for (List<String> row : rows) {
            String key = row.get(0).split("\\" + SetUserPropertyVar.DOT_SEPARATOR)[0];
            if (matcher.match(key)) {
                result.add(row);
            }
        }

        return result;
    }

    @Override
    public ShowResultSetMetaData getMetaData() {
        ShowResultSetMetaData.Builder builder = ShowResultSetMetaData.builder();
        for (String col : TITLE_NAMES) {
            builder.addColumn(new Column(col, ScalarType.createVarchar(30)));
        }
        return builder.build();
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitShowUserPropertyStatement(this, context);
    }
}
