package com.ericdmartell.simpleorm.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;
import com.ericdmartell.simpleorm.cache.SimpleORMCache;
import com.ericdmartell.simpleorm.factory.ActionFactory;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.ericdmartell.simpleorm.utils.JDBCUtil;

public class ObjectDelete {
	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private SimpleORMCache cache;
	
	public ObjectDelete(DataSource dataSource, SimpleORMCache cache, ActionFactory loadPathFactory) {
		this.dataSource = dataSource;
		this.loadPathFactory = loadPathFactory;
		this.cache = cache;
	}
	
	public void delete(SimpleORMObject obj) {
		//Delete assocs and object itself.
		List assocs = loadPathFactory.getNewAssociationLoad().loadWhereHasClass(SimpleORMAssociation.class);
		for (Object assoc : assocs) {
			loadPathFactory.getNewAssociationDelete().delete(obj, (SimpleORMAssociation) assoc);
		}
		JDBCUtil.executeUpdate("delete from " + obj.getClass().getSimpleName() + " where id = " + obj.id, dataSource);
		cache.dirtyObject(obj);
	}
	
	
}
