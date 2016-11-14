package com.ericdmartell.maga.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;

public class ObjectDelete {
	private DataSource dataSource;
	private MAGACache cache;
	private MAGA maga;
	
	public ObjectDelete(DataSource dataSource, MAGACache cache, MAGA maga) {
		this.dataSource = dataSource;
		this.cache = cache;
		this.maga = maga;
	}
	
	public void delete(MAGAObject obj) {
		//Delete assocs and object itself.
		List assocs = maga.loadWhereHasClass(MAGAAssociation.class);
		for (Object assoc : assocs) {
			maga.deleteAssociations(obj, (MAGAAssociation) assoc);
		}
		JDBCUtil.executeUpdate("delete from " + obj.getClass().getSimpleName() + " where id = " + obj.id, dataSource);
		cache.dirtyObject(obj);
	}
	
	
}
