package com.ericdmartell.simpleorm.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;
import com.ericdmartell.simpleorm.cache.SimpleORMCache;
import com.ericdmartell.simpleorm.factory.ActionFactory;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.ericdmartell.simpleorm.utils.HistoryUtil;
import com.ericdmartell.simpleorm.utils.JDBCUtil;
import com.ericdmartell.simpleorm.utils.ReflectionUtils;

public class AssociationDelete {

	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private SimpleORMCache cache;

	public AssociationDelete(DataSource dataSource, SimpleORMCache cache, ActionFactory loadPathFactory) {
		this.dataSource = dataSource;
		this.loadPathFactory = loadPathFactory;
		this.cache = cache;
	}

	public void delete(SimpleORMObject obj, SimpleORMAssociation association) {
		if (association.type() == SimpleORMAssociation.MANY_TO_MANY) {
			deleteAllManyToManyAssocs(obj, association);
		} else {
			if (obj.getClass() == association.class1()) {
				deleteOneToManyAssocsFromTheOneSide(obj, association);
			} else {
				deleteOneToManyAssocsFromTheManySide(obj, association);
			}
		}
	}

	private void deleteAllManyToManyAssocs(SimpleORMObject obj, SimpleORMAssociation association) {
		// We need to dirty all associations that include our object... all
		// these objects have an association pointing at our object.
		List<SimpleORMObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);

		// DB Part
		JDBCUtil.executeUpdate("delete from " + association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName() + " where " + obj.getClass().getSimpleName() + " = " + obj.id,
				dataSource);

		// Cache Part
		cache.dirtyAssoc(obj, association);
		for (SimpleORMObject toDirtyAssoc : objectsOnTheOtherSide) {
			cache.dirtyAssoc(toDirtyAssoc, association);
		}
	}

	private void deleteOneToManyAssocsFromTheOneSide(SimpleORMObject obj, SimpleORMAssociation association) {
		// We need to dirty all associations that include our object... all
		// these objects have an association pointing at our object.
		List<SimpleORMObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);
		
		//DB Part.  Take all linked objects and zero out their join column.
		for (SimpleORMObject objectOnOtherSide : objectsOnTheOtherSide) {
			JDBCUtil.executeUpdate("update " + association.class2().getSimpleName() + " set "
					+ association.class2Column() + " = 0 where id = " + objectOnOtherSide.id, dataSource);
		}
		
		//Cache Part
		cache.dirtyAssoc(obj, association);
		for (SimpleORMObject otherSideObject : objectsOnTheOtherSide) {
			//Since we changed a field on the object on the other side, we gotta dirty its entry.
			cache.dirtyObject(otherSideObject);
			cache.dirtyAssoc(otherSideObject, association);
		}

	}

	private void deleteOneToManyAssocsFromTheManySide(SimpleORMObject obj, SimpleORMAssociation association) {
		// We need to dirty all associations that include our object... since we're on the many side, it should
		// just be one object (or 0 if there was no association in the first place).
		List<SimpleORMObject> objectsOnTheOtherSide = loadPathFactory.getNewAssociationLoad().load(obj, association);
		
		//DB Part and since we have a reference to an object whose column is being changed, we use reflection to change its field val.
		JDBCUtil.executeUpdate("update " + obj.getClass().getSimpleName() + " set "
				+ association.class2Column() + " = 0 where id = " + obj.id, dataSource);
		ReflectionUtils.setFieldValue(obj, association.class2Column(), 0);
		
		//Cache Part
		cache.dirtyObject(obj);
		cache.dirtyAssoc(obj, association);
		for (SimpleORMObject toDirtyAssoc : objectsOnTheOtherSide) {
			cache.dirtyAssoc(toDirtyAssoc, association);
		}
	}

	public void delete(SimpleORMObject obj, SimpleORMObject obj2, SimpleORMAssociation association) {
		if (association.type() == SimpleORMAssociation.MANY_TO_MANY) {
			deleteSpecificManyToMany(obj, obj2, association);
		} else {
			if (obj.getClass() == association.class1()) {
				deleteSpecificOneToManyFromOneSide(obj, obj2, association);
			} else {
				deleteSpecificOneToManyFromOneSide(obj2, obj, association);
			}
		}
	}
	
	private void deleteSpecificManyToMany(SimpleORMObject obj, SimpleORMObject obj2, SimpleORMAssociation association) {
		//DB Part
		JDBCUtil.executeUpdate("delete from " + association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName() + " where " + obj.getClass().getSimpleName() + " = " + obj.id
				+ " and " + obj2.getClass().getSimpleName() + "=" + obj2.id, dataSource);
		//Cache Part.
		cache.dirtyAssoc(obj, association);
		cache.dirtyAssoc(obj2, association);
	}
	
	private void deleteSpecificOneToManyFromOneSide(SimpleORMObject obj, SimpleORMObject obj2, SimpleORMAssociation association) {
		//We record history since we're change an actual object's field.
		SimpleORMObject oldObject = loadPathFactory.getNewObjectLoad().load(obj2.getClass(), obj2.id);
		
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
