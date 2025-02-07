package com.hufudb.openhufu.core.sql.rel;

import com.google.common.collect.ImmutableList;
import com.hufudb.openhufu.core.sql.rule.FQRules;
import com.hufudb.openhufu.data.schema.Schema;
import com.hufudb.openhufu.expression.ExpressionFactory;
import com.hufudb.openhufu.plan.LeafPlan;
import java.util.List;
import org.apache.calcite.adapter.enumerable.EnumerableLimitRule;
import org.apache.calcite.adapter.enumerable.EnumerableProjectToCalcRule;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.rules.JoinCommuteRule;
import org.apache.calcite.rel.rules.JoinPushThroughJoinRule;
import org.apache.calcite.rel.type.RelDataType;

public class FQTableScan extends TableScan implements FQRel {
  final FQTable FQTable;
  final RelDataType projectRowType;

  public FQTableScan(RelOptCluster cluster, RelTraitSet traitSet, RelOptTable table,
      FQTable FQTable, RelDataType projectRowType) {
    super(cluster, traitSet, ImmutableList.of(), table);
    this.FQTable = FQTable;
    this.projectRowType = projectRowType;
  }

  public Schema getSchema() {
    return FQTable.getSchema();
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    return super.computeSelfCost(planner, mq).multiplyBy(.05);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return this;
  }

  @Override
  public RelDataType deriveRowType() {
    return projectRowType != null ? projectRowType : super.deriveRowType();
  }

  @Override
  public void register(RelOptPlanner planner) {
    planner.addRule(FQRules.TO_ENUMERABLE);
    for (RelOptRule rule : FQRules.RULES) {
      planner.addRule(rule);
    }
    planner.removeRule(JoinCommuteRule.Config.DEFAULT.toRule());
    planner.removeRule(JoinPushThroughJoinRule.LEFT);
    planner.removeRule(JoinPushThroughJoinRule.RIGHT);
    planner.removeRule(CoreRules.AGGREGATE_REDUCE_FUNCTIONS);
    planner.removeRule(EnumerableProjectToCalcRule.Config.DEFAULT.toRule());
    planner.removeRule(EnumerableLimitRule.Config.DEFAULT.toRule());
  }

  @Override
  public void implement(Implementor implementor) {
    LeafPlan plan = new LeafPlan();
    implementor.setSchemaManager(FQTable.getRootSchema());
    plan.setTableName(FQTable.getTableName());
    plan.setSelectExps(ExpressionFactory.createInputRef(FQTable.getSchema()));
    implementor.setCurrentPlan(plan);
  }
}
