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
* **Use the Tech You Like**: MAGA lets you choose your own cache (We have a Memcached and HashMap cache included in the test source), and your own RDBMS like MySQL or Postgres.  "Something Something Something **dependency injection** something something something."  https://www.youtube.com/watch?v=4APcgsRdW6w
* **Unchecked Exceptions**: Because it's 2016.

# Usage

MAGA deliniates data into two categories:  **Objects** and **associations**.  Objects store real data, and associations represent the graph between these objects.  Imagine a *House* and *Resident* class.  Both *House* and *Resident* are **Objects** whereas *House-->Resident* is an **association**.

**Creating Objects**

To create an object (backed in your database with the same name) simply extend the *MAGAObject* class.  Every object automatically has a "long" id field.  You can annotate your fields with @MAGAORMField to automatically create more fields that synchronize between your object and the database.  Options to add an index to the field within the database is also available.

```java
public class Obj1 extends MAGAObject {
	@MAGAField(isIndex=true)
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

To create an association, extent *MAGAAssociation*.  Every association must provide the following:

* **class1**: One of the objects being joined.  In the case of the one-to-many join, **this must be the object without a join column**.
* **class2**: The other object being joined.  In the case of the one-to-many join, **this must be the object with a join column**.
* **type**: Must be either MAGAAssociation.MANY_TO_MANY or MAGAAssociation.ONE_TO_MANY.  MANY_TO_MANY use an intermediate join table.  ONE_TO_MANY use a column on the class2 object with the id of the object in class1.
* **class2Column**:  The name of the join column on the class2 object in the join is MAGAAssociation.ONE_TO_MANY.

```java
public class TestAssoc extends MAGAAssociation {

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
		return MAGAAssociation.MANY_TO_MANY;
	}

	@Override
	public String class2Column() {
		return "joinColumn";
	}

}
```
**Using MAGA in Application Code**

```java
MAGA orm = new MAGA(dataSource, cache);
```
Where *dataSource* is a *javax.sql.DataSource* and cache is a *com.ericdmartell.cache.Cache*, an abstract class implemented with whatever technology you'd like.  We've provided a Memcached and Java HashMap implementation.

MAGA has the following methods:
* **load(Class clazz, long id)**: Returns object of Class with id.
* **load(Class clazz, Collection<Long> ids)**: Returns list of objects of Class with provided ids.
* **loadAll(Class clazz)**: Returns list of all objects of Class.
* **loadAll(Class clazz)**: Returns list of all objects of Class.
* **save(MAGAObject toSave)**: Persists object state to database.
* **delete(MAGAObject toDelete)**: Removes object from database.
* **loadAssociatedObjects(MAGAObject baseObject, MAGAAssociation association)**: Taking an object and association, returns a list of objects that are associated with the provided object via the association.
* **addAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association)**: Adds an association to the database between the two provided objects via the definition provided by the association object.
* **deleteAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association)**: Removes the link between the provided objects by the association.
* **deleteAssociations(MAGAObject baseObject, MAGAAssociation association)**: Removes the link between the provided object and all other objects defined by the association.
* **schemaSync()**:  Updates the underlying database to match your MAGA class definitions.
* **loadTemplate(MAGALoadTemplate template, Object... args)**: Uses a Load Template to return objects.

# Using MAGALoadTemplates
When every object and association exists in your cache, you'll never be going to your database, but you'll be making a lot of trips to your cache.  Considering the network overhead of accessing remote caches like Memcached, deserialization between the cache service and the JVM, etc, you might have a desire to further optimize your loads.

When there is a common load in place that returns "a graph of objects and associations" (for lack of a better description), you can save this as a single entry in the cache, in addition to all the individual entries for each object and association.  We call this a **Load Template**.

To create a load template, extent MAGALoadTemplate and implement the getKey() and run() methods.  Note that run can accept any number of parameters.  GetKey should be unique to each graph of returned data.  IE, if have a site that displays restaurant menus, your key would likely end up being "Menu" + (restaurant id), and would return all categories, menu items, etc.

After the first run, the entire graph of returned data will be cached as a single entry in the cache.  This means if you load out the template again, all object loads and association loads that existed within the original load of the template will require no farther trips to the cache or database.

When any dependent object or association is changed, the template will reload and return valid data.

# Object History

For every object MAGA manages, we create a table called (Object_name)_history, which shows every write to the object and what changed.  This is done off-thread, so you don't need to worry about the perf hit.


# Minutiae

MAGA is a lazily populated cache.  When you load Objects, we check the cache, then the database (which then populates the cache).  When you load associations, we check the cache for a list of ids of the remote class defined by the association.  If we miss, we load the ids out of database, and save them in cache.  We then load all objects for the ids using the object load path.

In some cases, updating fields within an object will change associations.  For instance, if there is a one-to-many join with an object having a field otherObjectId, changing the id and saving will implicitly change this object's associations.  This is taken care of internally... there is no need for manual updating of the object's associations.  Likewise, updating an object's associations will automatically change all of its fields within objects in scope via reflection.  The goal here is to make this totally invisible to the programmer.

This cache is going to be more effective in a read-intensive than a write-intensive environment.  Writes typically result in multiple dirties, but at peak, the cache can have over a 90% hit ratio as long as your data doesn't churn like crazy.  If you're looking to build a social network, this might not be for you.  If you're looking for a simple ORM for ecommerce, you're probably a lot closer to the ideal use case.  Templates are especially ideal for displaying high read/low write data.

# Who is MAGA?

MAGA's authors are Eric Martell and Alex Wyler, who co-founded @EatStreet.  Alex wrote the first version of MAGA which is live on EatStreet.com currently, and Eric worked on porting it to open source and adding templates.
