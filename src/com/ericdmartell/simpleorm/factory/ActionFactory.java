package com.ericdmartell.simpleorm.factory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.ericdmartell.simpleorm.actions.AssociationAdd;
import com.ericdmartell.simpleorm.actions.AssociationDelete;
import com.ericdmartell.simpleorm.actions.AssociationLoad;
import com.ericdmartell.simpleorm.actions.ObjectDelete;
import com.ericdmartell.simpleorm.actions.ObjectLoad;
import com.ericdmartell.simpleorm.actions.ObjectUpdate;
import com.ericdmartell.simpleorm.actions.SchemaSync;
import com.ericdmartell.simpleorm.cache.SimpleORMCache;
import com.ericdmartell.simpleorm.objects.SimpleORMLoadTemplate;

public class ActionFactory {
	private DataSource dataSource;
	private SimpleORMLoadTemplate template;
	private SimpleORMCache cache;
	
	
	public ThreadPoolExecutor executorPool = new ThreadPoolExecutor(10, 50, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(50));

	public ActionFactory(DataSource dataSource, SimpleORMCache cache, SimpleORMLoadTemplate template) {
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
