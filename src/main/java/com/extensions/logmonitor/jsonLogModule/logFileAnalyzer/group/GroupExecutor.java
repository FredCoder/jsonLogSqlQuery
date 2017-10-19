package com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.dataCache.selectDataCache.QueryResultDataItem;
import com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.select.QueryExecute;
import com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.whereCond.OptExecute;
import com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.whereCond.WhereCondition;
import com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.whereCond.optExecutes.EqOpt;
import com.extensions.logmonitor.jsonLogModule.queryExecute.Clearable;
import com.extensions.logmonitor.util.GenericsUtils;
import com.extensions.logmonitor.util.TupleUtil;
import com.extensions.logmonitor.util.TwoTuple;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2017年9月8日
 * @Desc this guy is to lazy , noting left.
 *
 */
@Slf4j
public class GroupExecutor implements Clearable {

	// 系统配置
	private List<String> groupByPaths = new ArrayList<>();
	private WhereCondition groupWhereCondition;
	// 针对列上的分组函数
	private Map<String, List<QueryExecute<? extends Object>>> havingGroupFunQuery = new HashMap<>();
	// 针对列上的分组函数2
	private Map<OptExecute, QueryExecute<? extends Object>> havingOptExecuteToQueryMapper = new HashMap<>();
	/**
	 * 内存分组存储
	 */
	private ThreadLocal<Map<Long, Boolean>> groupByValues = new ThreadLocal<Map<Long, Boolean>>() {
		public Map<Long, Boolean> initialValue() {
			return new HashMap<>();
		}
	};
	private boolean needHaving;

	/**
	 * @param queryReusltDataItem
	 */
	public void doHaving(QueryResultDataItem queryReusltDataItem) {
		if (!this.needHaving) {
			return;
		}
		Map<String, List<OptExecute>> optExecuteQuickVisitCache = this.groupWhereCondition
				.getOptExecuteQuickVisitCache();
		for (String queryPath : optExecuteQuickVisitCache.keySet()) {
			for (OptExecute optExecute : optExecuteQuickVisitCache.get(queryPath)) {
				Object value = this.havingOptExecuteToQueryMapper.get(optExecute).end(queryReusltDataItem.getGroupId());
				boolean optSuccess = checkOptExecuteForGroupCondition(optExecute, value);
				log.debug("optExecuteType:{} optExecute :{} and value:{} and optSuccess:{}",
						optExecute.getClass().getSimpleName(), optExecute, value, optSuccess);
				this.groupWhereCondition.addWhereExecuteResult(optExecute, optSuccess);
			}
		}
		boolean checkWhereIsSuccess = this.groupWhereCondition.checkWhereIsSuccess();
		this.groupByValues.get().put(queryReusltDataItem.getGroupId(), checkWhereIsSuccess);
	}

	/**
	 * @param optExecute
	 * @param value
	 * @return
	 */
	private boolean checkOptExecuteForGroupCondition(OptExecute optExecute, Object value) {
		boolean optSuccess = optExecute.OptSuccess(value);
		if (!(optExecute instanceof EqOpt)) {
			return !optSuccess;
		}
		return optSuccess;
	}

	public TwoTuple<Boolean, Long> putQueryResultDataItem(QueryResultDataItem queryResultDataItem) {
		Map<Long, Boolean> map = this.groupByValues.get();
		Map<String, Object> queryResult = queryResultDataItem.getQueryResult();
		GroupByKey gb = new GroupByKey();
		for (String groupByItem : this.groupByPaths) {
			Object object = queryResult.get(groupByItem);
			gb.addGroupByFieldValue(object);
		}
		boolean isHasExists = (map.get(gb.getHashValue()) != null);
		// System.out.println("group for queryResultDataItem:" +
		// queryResultDataItem + " isHasExists:" + isHasExists
		// + " with group id:" + gb.getHashValue());
		if (!isHasExists) {
			map.put(gb.getHashValue(), false);
		}
		return TupleUtil.tuple(isHasExists, gb.getHashValue());
	}

	/**
	 * 添加分组项
	 * 
	 * @param groupByItem
	 * @return
	 */
	public GroupExecutor addOrderByItem(String groupByItem) {
		groupByPaths.add(groupByItem);
		return this;
	}

	public GroupExecutor addGroupByCondition(WhereCondition whereCondition) {
		this.groupWhereCondition = whereCondition;
		return this;
	}

	public WhereCondition getGroupByCondition() {
		return this.groupWhereCondition;
	}

	public List<String> getGroupByItem() {
		return this.groupByPaths;
	}

	public void addGroupFunQuery(OptExecute optExecute, QueryExecute<? extends Object> queryExecute) {
		this.havingOptExecuteToQueryMapper.put(optExecute, queryExecute);
		GenericsUtils.addListIfNotExists(this.havingGroupFunQuery, queryExecute.getQueryPathWithFieldName(),
				queryExecute);
	}

	@Override
	public void clearResource() {
		this.groupByValues.remove();
	}

	/**
	 * @param superPath
	 * @return
	 */
	public List<QueryExecute<? extends Object>> getFunExecuteWithSuperPath(String superPath) {
		return havingGroupFunQuery.get(superPath);
	}

	/**
	 * @return
	 */
	public Map<Long, Boolean> getHavingFilter() {
		return this.groupWhereCondition == null ? null : this.groupByValues.get();
	}

	/**
	 * @param b
	 */
	public void setNeedHaving(boolean needHaving) {
		this.needHaving = true;
	}
	
	public boolean needHaving(){
		return this.needHaving;
	}

}
