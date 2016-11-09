package com.ericdmartell.maga;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.maga.associations.MAGAAssociation;
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
	
	public <T extends MAGAObject> T load(Class<T> clazz, long id) {		
		return clazz.cast(loadPathFactory.getNewObjectLoad().load(clazz, id));
	}

	public <T extends MAGAObject> List<T> load(Class<T> clazz, Collection<Long> ids) {
		return (List<T>) loadPathFactory.getNewObjectLoad().load(clazz, ids);
	}
	public <T extends MAGAObject> List<T> loadAll(Class<T> clazz) {
		return (List<T>) loadPathFactory.getNewObjectLoad().loadAll(clazz);
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

	public List<MAGAObject> loadAssociatedObjects(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfObjectUnsaved(baseObject);
		return loadPathFactory.getNewAssociationLoad().load(baseObject, association);
	}

	public void addAssociation(MAGAObject baseObject, MAGAObject otherObject,
			MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationAdd().add(baseObject, otherObject, association);
	}

	public void deleteAssociations(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfObjectUnsaved(baseObject);
		loadPathFactory.getNewAssociationDelete().delete(baseObject, association);
	}

	public void deleteAssociation(MAGAObject baseObject, MAGAObject otherObject,
			MAGAAssociation association) {
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
