package com.ericdmartell.maga.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.factory.ActionFactory;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.HistoryUtil;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.ReflectionUtils;

public class AssociationDelete {

	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private MAGACache cache;

	public AssociationDelete(DataSource dataSource, MAGACache cache, ActionFactory loadPathFactory) {
		this.dataSource = dataSource;
		this.loadPathFactory = loadPathFactory;
		this.cache = cache;
	}

	public void delete(MAGAObject obj, MAGAAssociation association) {
		if (association.type() == MAGAAssociation.MANY_TO_MANY) {
			deleteAllManyToManyAssocs(obj, association);
		} else {
			if (obj.getClass() == association.class1()) {
				deleteOneToManyAssocsFromTheOneSide(obj, association);
			} else {
				deleteOneToManyAssocsFromTheManySide(obj, association);
			}
		}
	}

	private void deleteAllManyToManyAssocs(MAGAObject obj, MAGAAssociation association) {
		// We need to dirty all associations that include our object... all
		// these objects have an association pointing at our object.
		List<MAGAObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);

		// DB Part
		JDBCUtil.executeUpdate("delete from " + association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName() + " where " + obj.getClass().getSimpleName() + " = " + obj.id,
				dataSource);

		// Cache Part
		cache.dirtyAssoc(obj, association);
		for (MAGAObject toDirtyAssoc : objectsOnTheOtherSide) {
			cache.dirtyAssoc(toDirtyAssoc, association);
		}
	}

	private void deleteOneToManyAssocsFromTheOneSide(MAGAObject obj, MAGAAssociation association) {
		// We need to dirty all associations that include our object... all
		// these objects have an association pointing at our object.
		List<MAGAObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);
		
		//DB Part.  Take all linked objects and zero out their join column.
		for (MAGAObject objectOnOtherSide : objectsOnTheOtherSide) {
			JDBCUtil.executeUpdate("update " + association.class2().getSimpleName() + " set "
					+ association.class2Column() + " = 0 where id = " + objectOnOtherSide.id, dataSource);
		}
		
		//Cache Part
		cache.dirtyAssoc(obj, association);
		for (MAGAObject otherSideObject : objectsOnTheOtherSide) {
			//Since we changed a field on the object on the other side, we gotta dirty its entry.
			cache.dirtyObject(otherSideObject);
			cache.dirtyAssoc(otherSideObject, association);
		}

	}

	private void deleteOneToManyAssocsFromTheManySide(MAGAObject obj, MAGAAssociation association) {
		// We need to dirty all associations that include our object... since we're on the many side, it should
		// just be one object (or 0 if there was no association in the first place).
		List<MAGAObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);
		
		//DB Part and since we have a reference to an object whose column is being changed, we use reflection to change its field val.
		JDBCUtil.executeUpdate("update " + obj.getClass().getSimpleName() + " set "
				+ association.class2Column() + " = 0 where id = " + obj.id, dataSource);
		ReflectionUtils.setFieldValue(obj, association.class2Column(), 0);
		
		//Cache Part
		cache.dirtyObject(obj);
		cache.dirtyAssoc(obj, association);
		for (MAGAObject toDirtyAssoc : objectsOnTheOtherSide) {
			cache.dirtyAssoc(toDirtyAssoc, association);
		}
	}

	public void delete(MAGAObject obj, MAGAObject obj2, MAGAAssociation association) {
		if (association.type() == MAGAAssociation.MANY_TO_MANY) {
			deleteSpecificManyToMany(obj, obj2, association);
		} else {
			if (obj.getClass() == association.class1()) {
				deleteSpecificOneToManyFromOneSide(obj, obj2, association);
			} else {
				deleteSpecificOneToManyFromOneSide(obj2, obj, association);
			}
		}
	}
	
	private void deleteSpecificManyToMany(MAGAObject obj, MAGAObject obj2, MAGAAssociation association) {
		//DB Part
		JDBCUtil.executeUpdate("delete from " + association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName() + " where " + obj.getClass().getSimpleName() + " = " + obj.id
				+ " and " + obj2.getClass().getSimpleName() + "=" + obj2.id, dataSource);
		//Cache Part.
		cache.dirtyAssoc(obj, association);
		cache.dirtyAssoc(obj2, association);
	}
	
	private void deleteSpecificOneToManyFromOneSide(MAGAObject obj, MAGAObject obj2, MAGAAssociation association) {
		//We record history since we're change an actual object's field.
		MAGAObject oldObject = loadPathFactory.getNewObjectLoad().load(obj2.getClass(), obj2.id);
		
		//DB Part
		JDBCUtil.executeUpdate("update " + obj2.getClass().getSimpleName() + " set " + association.class2Column()
				+ " = 0 where id = " + obj2.id, dataSource);
		//Set the object reference with the join column to have the same value (0) as in the db.
		ReflectionUtils.setFieldValue(obj2, association.class2Column(), 0);
		
		//Record History
		HistoryUtil.recordHistory(oldObject, obj2, loadPathFactory, dataSource);
		
		//Cache Part
		cache.dirtyObject(obj2);
		cache.dirtyAssoc(obj, association);
		cache.dirtyAssoc(obj2, association);
	}
}
