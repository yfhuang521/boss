package com.uu.modules.mifi.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.uu.common.persistence.BaseDao;
import com.uu.modules.mifi.condition.MifiUsageRecordLogCondition;
import com.uu.modules.mifi.entity.MifiUsageRecordLog;

@Repository
public class MifiUsageRecordLogDao extends BaseDao<MifiUsageRecordLog> {

	public static Logger logger = LoggerFactory.getLogger(MifiUsageRecordLogDao.class);

	/**
	 * 根据年获取使用记录年份列表
	 * 
	 * @athor shuxin
	 * @date 2016年9月9日下午4:35:55
	 * @return List<Map<String,String>>
	 */
	public List<Map<String, String>> findRecordLogForMonth(MifiUsageRecordLogCondition condition) {
		StringBuffer buffer = new StringBuffer("SELECT DATE_FORMAT(m.stamp_created,'%Y-%m') month FROM `mifi_usage_record_log` m  where m.is_check = 0");
		String beginDate = condition.getBeginDate();
		if(StringUtils.isNotBlank(beginDate)){
			buffer.append("	and DATE_FORMAT(m.stamp_created,'%Y-%m-%d') >= '").append(beginDate).append("'");
		}
		String endDate = condition.getEndDate();
		if(StringUtils.isNotBlank(endDate)){
			buffer.append("	and	DATE_FORMAT(m.stamp_created,'%Y-%m-%d') <= '").append(endDate).append("'");
		}
		buffer.append(" GROUP BY DATE_FORMAT(m.stamp_created,'%Y-%m') ORDER BY m.stamp_created");
		return findBySql(buffer.toString(), null, Map.class);
	}

	/**
	 * 通过月份查找每天的记录
	 * @athor shuxin
	 * @date 2016年9月9日下午4:46:17
	 * @param month
	 * @return
	 * List<Map<String,String>> 
	 */
	public List<Map<String, Object>> findRecordLogByMonth(String month) {
		String sql = "SELECT DATE_FORMAT(m.stamp_created,'%e') day, is_check FROM `mifi_usage_record_log` m WHERE DATE_FORMAT(m.stamp_created,'%Y-%m') = '"
				+ month + "' ORDER BY m.stamp_created";
		return findBySql(sql, null, Map.class);
	}
}
