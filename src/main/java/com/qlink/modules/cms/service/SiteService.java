/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package main.java.com.qlink.modules.cms.service;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uu.common.persistence.Page;
import com.uu.common.service.BaseService;
import com.uu.modules.cms.dao.SiteDao;
import com.uu.modules.cms.entity.Site;
import com.uu.modules.cms.utils.CmsUtils;

/**
 * 站点Service
 * @author admin
 * @version 2013-01-15
 */
@Service
public class SiteService extends BaseService {

	@Autowired
	private SiteDao siteDao;
	
	public Site get(String id) {
		return siteDao.get(id);
	}
	
	public Page<Site> find(Page<Site> page, Site site) {
		DetachedCriteria dc = siteDao.createDetachedCriteria();
		if (StringUtils.isNotEmpty(site.getName())){
			dc.add(Restrictions.like("name", "%"+site.getName()+"%"));
		}
		dc.add(Restrictions.eq(Site.FIELD_DEL_FLAG, site.getDelFlag()));
		//dc.addOrder(Order.asc("id"));
		return siteDao.find(page, dc);
	}

	//@Transactional(readOnly = false)
	public void save(Site site) {
		if (site.getCopyright()!=null){
			site.setCopyright(StringEscapeUtils.unescapeHtml4(site.getCopyright()));
		}
		siteDao.save(site);
		CmsUtils.removeCache("site_"+site.getId());
		CmsUtils.removeCache("siteList");
	}
	
	//@Transactional(readOnly = false)
	public void delete(String id, Boolean isRe) {
		siteDao.updateDelFlag(id, isRe!=null&&isRe?Site.DEL_FLAG_NORMAL:Site.DEL_FLAG_DELETE);
		CmsUtils.removeCache("site_"+id);
		CmsUtils.removeCache("siteList");
	}
	
}
