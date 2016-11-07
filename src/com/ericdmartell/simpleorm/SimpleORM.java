package com.ericdmartell.simpleorm;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.sql.DataSource;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;
import com.ericdmartell.simpleorm.cache.SimpleORMCache;
import com.ericdmartell.simpleorm.factory.ActionFactory;
import com.ericdmartell.simpleorm.objects.SimpleORMLoadTemplate;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.ericdmartell.simpleorm.utils.SimpleORMException;
import com.ericmartell.cache.Cache;

public class SimpleORM {
	private ActionFactory loadPathFactory;
	
	public SimpleORM(DataSource dataSource, Cache cache) {
		loadPathFactory = new ActionFactory(dataSource, SimpleORMCache.getInstance(cache), null);
	}
	
	public SimpleORM(DataSource dataSource, Cache cache, ForkJoinPool pool, SimpleORMLoadTemplate template) {
		loadPathFactory = new ActionFactory(dataSource, SimpleORMCache.getInstance(cache), template);
	}
	
	public SimpleORMObject load(Class clazz, long id) {		
		return loadPathFactory.getNewObjectLoad().load(clazz, id);
	}

	public List<SimpleORMObject> load(Class clazz, Collection<Long> ids) {
		return loadPathFactory.getNewObjectLoad().load(clazz, ids);
	}
	public List<SimpleORMObject> loadAll(Class clazz) {
		return loadPathFactory.getNewObjectLoad().loadAll(clazz);
	}
	public List<SimpleORMObject> loadTemplate(SimpleORMLoadTemplate template, Object... args) {
		return loadPathFactory.getNewObjectLoad().loadTemplate(template, args);
	}

	public void save(SimpleORMObject toSave) {
		throwExceptionIfCantSave(toSave);
		loadPathFactory.getNewObjectUpdate().update(toSave);
	}

	public void delete(SimpleORMObject toDelete) {
		throwExceptionIfCantSave(toDelete);
		throwExceptionIfObjectUnsaved(toDelete);
		loadPathFactory.getNewObjectDelete().delete(toDelete);
	}

	public List<SimpleORMObject> loadAssociatedObjects(SimpleORMObject baseObject, SimpleORMAssociation association) {
		throwExceptionIfObjectUnsaved(baseObject);
		return loadPathFactory.getNewAssociationLoad().load(baseObject, association);
	}

	public void addAssociation(SimpleORMObject baseObject, SimpleORMObject otherObject,
			SimpleORMAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationAdd().add(baseObject, otherObject, association);
	}

	public void deleteAssociations(SimpleORMObject baseObject, SimpleORMAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationDelete().delete(baseObject, association);
	}

	public void deleteAssociation(SimpleORMObject baseObject, SimpleORMObject otherObject,
			SimpleORMAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationDelete().delete(baseObject, otherObject, association);
	}
	
	public void schemaSync() {
		loadPathFactory.getNewSchemaSync().go();
	}
	
	private void throwExceptionIfCantSave(SimpleORMObject object) {
		if (object.templateAssociations != null) {
			throw new SimpleORMException("Templates and objects returned from templates are read-only");
		}
	}
	private void throwExceptionIfObjectUnsaved(SimpleORMObject object) {
		if (object.id == 0) {
			throw new SimpleORMException("Method unsupported for unsaved object [" + object.getClass().getName() + "] " + object.id);
		}
	}

}
