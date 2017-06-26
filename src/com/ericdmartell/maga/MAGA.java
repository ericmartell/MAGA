package com.ericdmartell.maga;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.maga.actions.AssociationAdd;
import com.ericdmartell.maga.actions.AssociationDelete;
import com.ericdmartell.maga.actions.AssociationLoad;
import com.ericdmartell.maga.actions.ObjectDelete;
import com.ericdmartell.maga.actions.ObjectLoad;
import com.ericdmartell.maga.actions.ObjectUpdate;
import com.ericdmartell.maga.actions.SchemaSync;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.TrackedConnection;

import gnu.trove.set.hash.THashSet;

public class MAGA {

	public DataSource dataSource;
	public MAGACache cache;
	public MAGALoadTemplate template;

	public ThreadPoolExecutor executorPool = new ThreadPoolExecutor(50, 50, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(50));

	public MAGA(DataSource dataSource, Cache cache) {
		this.dataSource = dataSource;
		this.cache = MAGACache.getInstance(cache);
		this.template = null;
		executorPool.submit(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Set<TrackedConnection> reported = new THashSet<>();
					for (TrackedConnection connection : JDBCUtil.openConnections) {
						if (new Date().getTime() - connection.date.getTime() >= 5000) {
							System.out.println("Leaked Connection");
							connection.stack.printStackTrace();
							reported.add(connection);
						}
					}
					JDBCUtil.openConnections.removeAll(reported);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}

	public MAGA(DataSource dataSource, Cache cache, MAGALoadTemplate template) {
		this.dataSource = dataSource;
		this.cache = MAGACache.getInstance(cache);
		this.template = template;
		executorPool.submit(new Runnable() {
			@Override
			public void run() {
				while (true) {
					for (TrackedConnection connection : JDBCUtil.openConnections) {
						if (new Date().getTime() - connection.date.getTime() >= 5000) {
							System.out.println("Leaked Connection");
							connection.stack.printStackTrace();
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}

	public <T extends MAGAObject> T load(Class<T> clazz, String id) {
		return clazz.cast(new ObjectLoad(dataSource, cache, this, template).load(clazz, id));
	}

	public <T extends MAGAObject> List<T> load(Class<T> clazz, Collection<String> ids) {
		return (List<T>) new ObjectLoad(dataSource, cache, this, template).load(clazz, ids);
	}

	public <T extends MAGAObject> List<T> loadAll(Class<T> clazz) {
		return (List<T>) new ObjectLoad(dataSource, cache, this, template).loadAll(clazz);
	}

	public List<MAGAObject> loadTemplate(MAGALoadTemplate template) {
		return new ObjectLoad(dataSource, cache, this, template).loadTemplate(template);
	}

	public void save(MAGAObject toSave) {
		throwExceptionIfCantSave(toSave);
		new ObjectUpdate(dataSource, cache, this).update(toSave);
	}

	public void delete(MAGAObject toDelete) {
		throwExceptionIfCantSave(toDelete);
		throwExceptionIfObjectUnsaved(toDelete);
		new ObjectDelete(dataSource, cache, this).delete(toDelete);
		;
	}

	public List loadAssociatedObjects(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfObjectUnsaved(baseObject);
		return new AssociationLoad(dataSource, cache, this, template).load(baseObject, association);
	}

	public void addAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		new AssociationAdd(dataSource, cache, this).add(baseObject, otherObject, association);
		;
	}

	public void deleteAssociations(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfObjectUnsaved(baseObject);
		new AssociationDelete(dataSource, cache, this).delete(baseObject, association);
		;
	}

	public void deleteAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		new AssociationDelete(dataSource, cache, this).delete(baseObject, otherObject, association);
		;
	}

	public void schemaSync() {
		new SchemaSync(dataSource, cache).go();
	}

	private void throwExceptionIfCantSave(MAGAObject object) {
		if (object.templateAssociations != null) {
			throw new MAGAException("Templates and objects returned from templates are read-only");
		}
	}

	private void throwExceptionIfObjectUnsaved(MAGAObject object) {
		if (object.id == null) {
			throw new MAGAException(
					"Method unsupported for unsaved object [" + object.getClass().getName() + "] " + object.id);
		}
	}

	public List<MAGAAssociation> loadWhereHasClassWithJoinColumn(Class<? extends MAGAObject> class1) {
		return new AssociationLoad(dataSource, cache, this, template).loadWhereHasClassWithJoinColumn(class1);
	}

	public List loadWhereHasClass(Class clazz) {
		return new AssociationLoad(dataSource, cache, this, template).loadWhereHasClass(clazz);
	}

	public void dirtyObject(MAGAObject object) {
		cache.dirtyObject(object);
	}

	public void dirtyAssociation(MAGAObject object, MAGAAssociation association) {
		cache.dirtyAssoc(object, association);
	}

	public <T extends MAGAObject> List<T> loadWhere(Class<T> clazz, String where, Object... params) {
		return new AssociationLoad(dataSource, cache, this, template).loadWhere(clazz, where, params);

	}
}
