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

To create an object (backed in your database with the same name) simply extend the *SimpleORMObject* class.  Every object automatically has a "long" id field.  You can annotate your fields with @SimpleORMField to automatically create more fields that synchronize between your object and the database.

```java
public class Obj1 extends SimpleORMObject {
	@SimpleORMField
	private String field1;
  
  public String getField1() {
    return field1;
  }
  
  public void setField1(String field1) {
    this.field1 = field1;
  }
}

```
