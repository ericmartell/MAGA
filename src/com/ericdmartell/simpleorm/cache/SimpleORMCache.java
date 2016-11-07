package com.ericdmartell.simpleorm.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ericdmartell.simpleorm.associations.SimpleORMAssociation;
import com.ericdmartell.simpleorm.objects.SimpleORMLoadTemplate;
import com.ericdmartell.simpleorm.objects.SimpleORMObject;
import com.ericmartell.cache.Cache;

import gnu.trove.map.hash.THashMap;

public class SimpleORMCache implements Cache {
	
	private SimpleORMCache() {
		
	}
	private Cache delegate;
	
	// key is just Classname:ID
	public List<String> getKeys(Class<SimpleORMObject> clazz, Collection<Long> ids) {
		List<String> ret = new ArrayList<>();
		for (long id : ids) {
			ret.add(getKey(clazz, id));
		}
		return ret;
	}

	public final String getKey(SimpleORMObject simpleORMObject) {
		return getKey((Class<SimpleORMObject>) simpleORMObject.getClass(), simpleORMObject.id);
	}

	public final String getKey(Class<SimpleORMObject> clazz, long id) {
		return clazz.getName() + ":" + id;
	}

	public final void setObjects(List<SimpleORMObject> simpleORMObjects, 
			SimpleORMLoadTemplate dependentTemplate) {
		for (SimpleORMObject toSet : simpleORMObjects) {
			setObject(toSet, dependentTemplate);
		}
	}

	public final void setObject(SimpleORMObject simpleORMObject, 
			SimpleORMLoadTemplate dependentTemplate) {
		set(getKey(simpleORMObject), simpleORMObject);
		if (dependentTemplate != null) {
			addTemplateDependency(simpleORMObject, dependentTemplate);
		}
	}

	public final void addTemplateDependency(SimpleORMObject simpleORMObject, SimpleORMLoadTemplate dependentTemplate) {
		String dependencyKey = getKey(simpleORMObject) + ":template_dependencies";
		List<String> existingTemplateKeys = (List<String>) get(dependencyKey);
		if (existingTemplateKeys == null) {
			existingTemplateKeys = new ArrayList<>();
		} else {
			existingTemplateKeys = new ArrayList<>(existingTemplateKeys);
		}
		if (!existingTemplateKeys.contains(dependentTemplate.getKey())) {
			existingTemplateKeys.add(dependentTemplate.getKey());
			set(dependencyKey, existingTemplateKeys);
		}
	}

	public final List<SimpleORMObject> getObjects(Class<SimpleORMObject> clazz, Collection<Long> ids) {

		Map<String, Object> ret = getBulk(getKeys(clazz, ids));

		return new ArrayList(ret.values());
	}

	public final List<Long> getAssociatedIds(SimpleORMObject obj, SimpleORMAssociation association) {
		return (List<Long>) get(getAssocKey(obj, association));
	}

	public final String getAssocKey(SimpleORMObject obj, SimpleORMAssociation association) {
		return association.class1().getSimpleName() + ":" + association.class2().getSimpleName() + ":"
				+ obj.getClass().getSimpleName() + ":" + obj.id;
	}

	public final void setAssociatedIds(SimpleORMObject obj, SimpleORMAssociation association, List<Long> associations,
			 SimpleORMLoadTemplate dependentTemplate) {
		set(getAssocKey(obj, association), associations);
		if (dependentTemplate != null) {
			addTemplateDependencyOnAssoc(obj, association, dependentTemplate);
		}
	}

	public final void addTemplateDependencyOnAssoc(SimpleORMObject simpleORMObject, SimpleORMAssociation association,
			SimpleORMLoadTemplate dependentTemplate) {
		String dependencyKey = getAssocKey(simpleORMObject, association) + ":template_dependencies";
		List<String> existingTemplateKeys = (List<String>) get(dependencyKey);
		if (existingTemplateKeys == null) {
			existingTemplateKeys = new ArrayList<>();
		} else {
			existingTemplateKeys = new ArrayList<>(existingTemplateKeys);
		}
		if (!existingTemplateKeys.contains(dependentTemplate.getKey())) {
			existingTemplateKeys.add(dependentTemplate.getKey());
			set(dependencyKey, existingTemplateKeys);
		}
	}

	public final void dirtyAssoc(SimpleORMObject obj, SimpleORMAssociation association) {
		String key = getAssocKey(obj, association);
		dirty(key);
		String dependencyKey = key + ":template_dependencies";
		List<String> existingTemplateKeys = (List<String>) get(dependencyKey);
		
		if (existingTemplateKeys != null) {
			for (String existingTemplateKey : existingTemplateKeys) {
				dirty(existingTemplateKey);
			}
		}
		dirty(dependencyKey);
	}

	public final void dirtyObject(SimpleORMObject obj) {
		String key = getKey(obj);
		dirty(key);

		String dependencyKey = key + ":template_dependencies";
		List<String> existingTemplateKeys = (List<String>) get(dependencyKey);
		if (existingTemplateKeys != null) {
			for (String existingTemplateKey : existingTemplateKeys) {
				dirty(existingTemplateKey);
			}
		}

	}

	public final void cacheAssociatedObjectsForTemplate(SimpleORMObject baseObject, SimpleORMAssociation association,
			List<SimpleORMObject> associatedObjects) {
		if (baseObject.templateAssociations == null) {
			baseObject.templateAssociations = new THashMap<>();
		}
		baseObject.templateAssociations.put(association, associatedObjects);

	}

	public final List<SimpleORMObject> getAssociatedObjectsForTemplate(SimpleORMObject baseObject,
			SimpleORMAssociation association) {
		if (baseObject.templateAssociations == null) {
			return null;
		}
		return baseObject.templateAssociations.get(association);

	}
	
	@Override
	public void set(String key, Object val) {
		delegate.set(key, val);
	}
	
	@Override
	public Map<String, Object> getBulk(List<String> keys) {
		return delegate.getBulk(keys);
	}
	
	@Override
	public Object get(String key) {
		return delegate.get(key);
	}
	
	@Override
	public void flush() {
		delegate.flush();
		
	}
	
	@Override
	public void dirty(String key) {
		delegate.dirty(key);
		
	}
	
	public static SimpleORMCache getInstance(Cache cache) {
		SimpleORMCache ret = new SimpleORMCache();
		ret.delegate = cache;
		return ret;
	}
	
}
