package com.ericdmartell.maga.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.associations.SimpleMAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.factory.ActionFactory;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;

public class ObjectDelete {
	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private MAGACache cache;
	
	public ObjectDelete(DataSource dataSource, MAGACache cache, ActionFactory loadPathFactory) {
		this.dataSource = dataSource;
		this.loadPathFactory = loadPathFactory;
		this.cache = cache;
	}
	
	public void delete(MAGAObject obj) {
		//Delete assocs and object itself.
		List assocs = loadPathFactory.getNewAssociationLoad().loadWhereHasClass(SimpleMAGAAssociation.class);
		for (Object assoc : assocs) {
			loadPathFactory.getNewAssociationDelete().delete(obj, (SimpleMAGAAssociation) assoc);
		}
		JDBCUtil.executeUpdate("delete from " + obj.getClass().getSimpleName() + " where id = " + obj.id, dataSource);
		cache.dirtyObject(obj);
	}
	
	
}
