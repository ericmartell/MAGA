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

public class AssociationAdd {

	private DataSource dataSource;
	private ActionFactory loadPathFactory;
	private SimpleORMCache cache;

	public AssociationAdd(DataSource dataSource, SimpleORMCache cache, ActionFactory loadPathFactory) {
		this.dataSource = dataSource;
		this.loadPathFactory = loadPathFactory;
		this.cache = cache;
	}

	public void add(SimpleORMObject obj, SimpleORMObject obj2, SimpleORMAssociation association) {
		if (association.type() == SimpleORMAssociation.MANY_TO_MANY) {
			manyToMany(obj, obj2, association);
		} else {
			if (obj.getClass() == association.class1()) {
				oneToMany(obj, obj2, association);
			} else {
				oneToMany(obj2, obj, association);
			}
		}
	}

	private void manyToMany(SimpleORMObject obj, SimpleORMObject obj2, SimpleORMAssociation association) {
		
		
		// DB Part
		JDBCUtil.executeUpdate("insert into " + association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName() + "(" + obj.getClass().getSimpleName() + ","
				+ obj2.getClass().getSimpleName() + ") values(" + obj.id + "," + obj2.id + ")", dataSource);

		// Cache Part
		cache.dirtyAssoc(obj, association);
		cache.dirtyAssoc(obj2, association);
	}

	private void oneToMany(SimpleORMObject objOfClass1, SimpleORMObject objOfClass2, SimpleORMAssociation association) {
		
		// We need this because if we're adding an assoc for a one-many, we might be switching the assoc of the old one.
		SimpleORMObject oldOneOfTheOneToMany = null;
		List<SimpleORMObject> listOfOldOneOfTheOneToMany = loadPathFactory.getNewAssociationLoad().load(objOfClass2, association);
		if (!listOfOldOneOfTheOneToMany.isEmpty()) {
			oldOneOfTheOneToMany = listOfOldOneOfTheOneToMany.get(0);
		}
		
		
		// For historical changes.
		SimpleORMObject oldObject = loadPathFactory.getNewObjectLoad().load(objOfClass2.getClass(), objOfClass2.id);

		// DB/Live object field swap Part.
		JDBCUtil.executeUpdate("update " + objOfClass2.getClass().getSimpleName() + " set " + association.class2Column()
				+ " = " + objOfClass1.id + " where id = " + objOfClass2.id, dataSource);
		ReflectionUtils.setFieldValue(objOfClass2, association.class2Column(), objOfClass1.id);

		// Cache Part.
		cache.dirtyObject(objOfClass2);
		cache.dirtyAssoc(objOfClass2, association);
		cache.dirtyAssoc(objOfClass1, association);
		if (oldOneOfTheOneToMany != null) {
			cache.dirtyAssoc(oldOneOfTheOneToMany, association);
		}

		// Since we changed an actual object, we record the change.
		HistoryUtil.recordHistory(oldObject, objOfClass2, loadPathFactory, dataSource);
	}
}
