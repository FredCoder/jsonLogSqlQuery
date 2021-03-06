package com.extensions.logmonitor.jsonLogModule.logFileAnalyzer.whereCond;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.extensions.logmonitor.jsonLogModule.queryExecute.Clearable;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @say little Boy, don't be sad.
 * @name Rezar
 * @time 2017年9月5日
 * @Desc this guy is to lazy , noting left.
 *
 */
@Slf4j
public class WhereCondition implements Clearable {
	private MultiCondition conditionSuper;
	private Map<String, List<OptExecute>> optExecuteQuickVisitCache = new HashMap<>();

	private Semaphore semaphore = new Semaphore(1);

	public WhereCondition setCondition(MultiCondition conditionSuper) {
		this.conditionSuper = conditionSuper;
		return this;
	}

	public WhereCondition quickVisitOptExecute() {
		try {
			this.semaphore.acquire();
			this.conditionSuper.visitQuickOptExecute(optExecuteQuickVisitCache);
		} catch (Exception e) {
			log.error("can not acquire semaphore:{} ", e);
		} finally {
			this.semaphore.release();
		}
		return this;
	}

	public boolean checkWhereIsSuccess(Map<OptExecute, Boolean> executeResult) {
		log.debug("executeWhereResult:{}", executeResult);
		return this.conditionSuper.visitQuickCondition(executeResult);
	}

	public Map<String, List<OptExecute>> getOptExecuteQuickVisitCache() {
		return Collections.unmodifiableMap(this.optExecuteQuickVisitCache);
	}

	@Override
	public void clearResource() {
	}

}
