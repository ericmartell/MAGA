package com.ericdmartell.maga.factory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.ericdmartell.maga.actions.AssociationAdd;
import com.ericdmartell.maga.actions.AssociationDelete;
import com.ericdmartell.maga.actions.AssociationLoad;
import com.ericdmartell.maga.actions.ObjectDelete;
import com.ericdmartell.maga.actions.ObjectLoad;
import com.ericdmartell.maga.actions.ObjectUpdate;
import com.ericdmartell.maga.actions.SchemaSync;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;

public class ActionFactory {
	private DataSource dataSource;
	private MAGALoadTemplate template;
	private MAGACache cache;
	
	
	public ThreadPoolExecutor executorPool = new ThreadPoolExecutor(10, 50, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(50));

	public ActionFactory(DataSource dataSource, MAGACache cache, MAGALoadTemplate template) {
		this.dataSource = dataSource;
		this.cache = cache;
		this.template = template;
	}

	public AssociationAdd getNewAssociationAdd() {
		return new AssociationAdd(dataSource, cache, this);
	}

	public AssociationDelete getNewAssociationDelete() {
		return new AssociationDelete(dataSource, cache, this);
	}

	public AssociationLoad getNewAssociationLoad() {
		return new AssociationLoad(dataSource, cache, this, template);
	}

	public ObjectDelete getNewObjectDelete() {
		return new ObjectDelete(dataSource, cache, this);
	}

	public ObjectLoad getNewObjectLoad() {
		return new ObjectLoad(dataSource, cache, this, template);
	}

	public ObjectUpdate getNewObjectUpdate() {
		return new ObjectUpdate(dataSource, cache, this);
	}

	public SchemaSync getNewSchemaSync() {
		return new SchemaSync(dataSource, cache);
	}

}
