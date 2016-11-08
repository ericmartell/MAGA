package com.ericdmartell.maga;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.sql.DataSource;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.maga.associations.SimpleMAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.factory.ActionFactory;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.MAGAException;

public class MAGA {
	private ActionFactory loadPathFactory;
	
	public MAGA(DataSource dataSource, Cache cache) {
		loadPathFactory = new ActionFactory(dataSource, MAGACache.getInstance(cache), null);
	}
	
	public MAGA(DataSource dataSource, Cache cache, MAGALoadTemplate template) {
		loadPathFactory = new ActionFactory(dataSource, MAGACache.getInstance(cache), template);
	}
	
	public MAGAObject load(Class clazz, long id) {		
		return loadPathFactory.getNewObjectLoad().load(clazz, id);
	}

	public List<MAGAObject> load(Class clazz, Collection<Long> ids) {
		return loadPathFactory.getNewObjectLoad().load(clazz, ids);
	}
	public List<MAGAObject> loadAll(Class clazz) {
		return loadPathFactory.getNewObjectLoad().loadAll(clazz);
	}
	public List<MAGAObject> loadTemplate(MAGALoadTemplate template, Object... args) {
		return loadPathFactory.getNewObjectLoad().loadTemplate(template, args);
	}

	public void save(MAGAObject toSave) {
		throwExceptionIfCantSave(toSave);
		loadPathFactory.getNewObjectUpdate().update(toSave);
	}

	public void delete(MAGAObject toDelete) {
		throwExceptionIfCantSave(toDelete);
		throwExceptionIfObjectUnsaved(toDelete);
		loadPathFactory.getNewObjectDelete().delete(toDelete);
	}

	public List<MAGAObject> loadAssociatedObjects(MAGAObject baseObject, SimpleMAGAAssociation association) {
		throwExceptionIfObjectUnsaved(baseObject);
		return loadPathFactory.getNewAssociationLoad().load(baseObject, association);
	}

	public void addAssociation(MAGAObject baseObject, MAGAObject otherObject,
			SimpleMAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationAdd().add(baseObject, otherObject, association);
	}

	public void deleteAssociations(MAGAObject baseObject, SimpleMAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationDelete().delete(baseObject, association);
	}

	public void deleteAssociation(MAGAObject baseObject, MAGAObject otherObject,
			SimpleMAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationDelete().delete(baseObject, otherObject, association);
	}
	
	public void schemaSync() {
		loadPathFactory.getNewSchemaSync().go();
	}
	
	private void throwExceptionIfCantSave(MAGAObject object) {
		if (object.templateAssociations != null) {
			throw new MAGAException("Templates and objects returned from templates are read-only");
		}
	}
	private void throwExceptionIfObjectUnsaved(MAGAObject object) {
		if (object.id == 0) {
			throw new MAGAException("Method unsupported for unsaved object [" + object.getClass().getName() + "] " + object.id);
		}
	}

}
