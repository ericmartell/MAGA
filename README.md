# Make AssociatedObjects GreatlyEasierToUse Again

Writing custom SQL to fetch from your database?  Sad!

Manually maintaining a cache, or not using a cache at all?  Very-unfunny!

What happened to the 30 thousand cache misses on a single endpoint?  Terrible.

Using an ORM that requires a ton of configuration and pigeonholes you into a certain cache/RDBMS?  Boring!


# What is MAGA?

The goal of MAGA was to create a simple, framework-independent ORM for RDB's that takes care of storing, retrieving, and caching objects efficiently and with little effort.

MAGA provides the following:

* **No More SQL**: A programatic way to persist, retreive and update objects and the relationship between objects with one line of code.
* **A consistently up-to-date schema**: An automatically maintained schema based on the Class definitions of your objects.  This includes join tables and associations between your objects as well.
* **Object histories**: Every creation, save, or delete results in a row in an ancillary table with the changed fields on the object, the stack that saved the object, and a timestamp.
* **Cache**: Every load, and every load from one object to another is cached for faster loads.  The cache synchronizes with the database for every operation, so you never have to worry about stale data.
* **Caches on Caches**: Define load templates of data you'd like to retrieve (usually an entire page of data), and you can get it back with a single cache hit.  Load templates are also automatically synchronized with your database.
* **Use the Tech You Like**: MAGA lets you choose your own cache (I have a Memcached and HashMap cache included in the test source), and your own RDBMS like MySQL or Postgres.


# Usage

MAGA deliniates data into two categories:  **Objects** and **associations**.  Objects store real data, and associations represent the graph between these objects.  Imagine a *House* and *Resident* class.  Both *House* and *Resident* are **Objects** whereas *House-->Resident* is an **association**.

**Creating Objects**

To create an object (backed in your database with the same name) simply extend the *SimpleORMObject* class.  Every object automatically has a "long" id field.  You can annotate your fields with @SimpleORMField to automatically create more fields that synchronize between your object and the database.  Options to add an index to the field within the database is also available.

```java
public class Obj1 extends SimpleORMObject {
	@SimpleORMField(isIndex=true)
	private String field1;
  
  public String getField1() {
    return field1;
  }
  
  public void setField1(String field1) {
    this.field1 = field1;
  }
}

```


**Creating Associations**

To create an association, extent *SimpleORMAssociation*.  Every association must provide the following:

* **class1**: One of the objects being joined.  In the case of the one-to-many join, **this must be the object without a join column**.
* **class2**: The other object being joined.  In the case of the one-to-many join, **this must be the object with a join column**.
* **type**: Must be either SimpleORMAssociation.MANY_TO_MANY or SimpleORMAssociation.ONE_TO_MANY.  MANY_TO_MANY use an intermediate join table.  ONE_TO_MANY use a column on the class2 object with the id of the object in class1.
* **class2Column**:  The name of the join column on the class2 object in the join is SimpleORMAssociation.ONE_TO_MANY.

```java
public class TestAssoc extends SimpleORMAssociation {

	@Override
	public Class class1() {
		return Obj1.class;
	}

	@Override
	public Class class2() {
		return Obj2.class;
	}

	@Override
	public int type() {
		return SimpleORMAssociation.MANY_TO_MANY;
	}

	@Override
	public String class2Column() {
		return "joinColumn";
	}

}
```
**Using MAGA in Application Code**
```java
SimpleORM orm = new SimpleORM(dataSource, cache);
```
Where *dataSource* is a *javax.sql.DataSource* and cache is a *com.ericdmartell.cache.Cache*, an abstract class implemented with whatever technology you'd like.  I've provided a Memcached and Java HashMap implementation.

orm has the following methods:
* **load(Class clazz, long id)**: Returns object of Class with id.
* **load(Class clazz, Collection<Long> ids)**: Returns list of objects of Class with provided ids.
* **loadAll(Class clazz)**: Returns list of all objects of Class.
* **loadAll(Class clazz)**: Returns list of all objects of Class.
* **save(SimpleORMObject toSave)**: Persists object state to database.
* **delete(SimpleORMObject toDelete)**: Removes object from database.
* **loadAssociatedObjects(SimpleORMObject baseObject, SimpleORMAssociation association)**: Taking an object and association, returns a list of objects that are associated with the provided object via the association.
* **addAssociation(SimpleORMObject baseObject, SimpleORMObject otherObject, SimpleORMAssociation association)**: Adds an association to the database between the two provided objects via the definition provided by the association object.
* **deleteAssociation(SimpleORMObject baseObject, SimpleORMObject otherObject, SimpleORMAssociation association)**: Removes the link between the provided objects by the association.
* **deleteAssociations(SimpleORMObject baseObject, SimpleORMAssociation association)**: Removes the link between the provided object and all other objects defined by the association.
* **schemaSync()**:  Updates the underlying database to match your MAGA class definitions.

