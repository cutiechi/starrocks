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
import com.starrocks.catalog.Column;
import com.starrocks.catalog.ScalarType;
import com.starrocks.qe.ShowResultSetMetaData;
import com.starrocks.sql.parser.NodePosition;

public class ShowDictionaryStmt extends ShowStmt {
    public static final ImmutableList<String> TITLE_NAMES = new ImmutableList.Builder<String>()
            .add("DictionaryId").add("DictionaryName").add("CatalogName").add("DbName").add("dictionaryObject")
            .add("dictionaryKeys").add("dictionaryValues").add("status").add("lastSuccessRefreshTime")
            .add("lastSuccessFinishedTime").add("nextSchedulableTime").add("ErrorMessage")
            .add("approximated dictionaryMemoryUsage (Bytes)").build();

    private final String dictionaryName;
    public ShowDictionaryStmt(String dictionaryName, NodePosition pos) {
        super(pos);
        this.dictionaryName = dictionaryName;
    }

    public String getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public ShowResultSetMetaData getMetaData() {
        ShowResultSetMetaData.Builder builder = ShowResultSetMetaData.builder();
        for (String title : TITLE_NAMES) {
            builder.addColumn(new Column(title, ScalarType.createVarchar(30)));
        }
        return builder.build();
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitShowDictionaryStatement(this, context);
    }
}