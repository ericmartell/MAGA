package com.ericdmartell.maga;

import java.math.BigDecimal;
import java.net.InetSocketAddress;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.spy.memcached.MemcachedClient;


public class MAGATest {
	private static DataSource dataSource;
	private static MemcachedClient client;
	@BeforeClass
	public static void setUp() throws Exception {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser("simpleorm");
		dataSource.setPassword("password");
		dataSource.setServerName("localhost");
		JDBCUtil.executeUpdate("drop schema simpleorm", dataSource);
		JDBCUtil.executeUpdate("create schema simpleorm", dataSource);
		dataSource = new MysqlDataSource();
		dataSource.setDatabaseName("simpleorm");
		dataSource.setUser("simpleorm");
		dataSource.setPassword("password");
		dataSource.setServerName("localhost");
		MAGATest.dataSource = dataSource;
		client = new MemcachedClient(new InetSocketAddress("localhost", 11211)); 
	}

	@Test
	public void schemaSync() {
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		JDBCUtil.executeQueryAndReturnLongs(dataSource, "select id from Obj1");
		JDBCUtil.executeQueryAndReturnLongs(dataSource, "select id from Obj2");
		JDBCUtil.executeQueryAndReturnLongs(dataSource, "select id from Obj3");
		JDBCUtil.executeQueryAndReturnLongs(dataSource, "select Obj1 from Obj1_to_Obj2");
	}
	
	@Test
	public void objectCreate() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		Assert.assertEquals(obj1, orm.load(Obj1.class, obj1.id));
	}
	
	@Test
	public void objectUpdate() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj1 toCompare = (Obj1) orm.load(Obj1.class, obj1.id);
		
		obj1.field1 = "I've changed this";
		orm.save(obj1);
		
		Assert.assertNotEquals(obj1, toCompare);
		Assert.assertEquals(obj1.id, toCompare.id);
		Assert.assertEquals(obj1, orm.load(Obj1.class, obj1.id));
	}

	@Test
	public void objectDelete() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		Assert.assertNotNull(orm.load(Obj1.class, obj1.id));
		orm.delete(obj1);
		Assert.assertNull(orm.load(Obj1.class, obj1.id));
	}
	
	@Test
	public void addManyToManyJoin() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
	}
	
	@Test
	public void deleteManyToManyJoin() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociation(obj1, obj2, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	public void deleteManyToManyJoins() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociations(obj1, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	@Test
	public void addOneToManyJoin() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
	}
	
	@Test
	public void deleteOneToManyJoinFromOneSide() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociation(obj1, obj2, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	@Test
	public void deleteOneToManyJoinsFromOneSide() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociations(obj1, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	@Test
	public void deleteOneToManyJoinFromManySide() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociation(obj2, obj1, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	@Test
	public void deleteOneToManyJoinsFromManySide() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		orm.deleteAssociations(obj2, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	
	@Test
	public void reflectionWorksWhenAddingAssocFromManySide() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).get(0), obj2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		Assert.assertEquals(obj2.joinColumn, obj1.id);
		
	}
	
	@Test
	public void assocWorksWhenModifyingJoinColumn() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		Obj2 obj2Other = new Obj2();
		obj2Other.field2 = "This is a test of field two (other)";
		orm.save(obj2Other);
		
		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj1, obj2Other, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).get(0).equals(obj2Other));
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
		obj2.joinColumn = obj1.id;
		orm.save(obj2);
		
		
		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).size(), 2);
		Assert.assertEquals(orm.loadAssociatedObjects(obj2, assoc).get(0), obj1);
		
		
	}
	@Test
	public void deleteManyToManyJoinsLeavingOtherAssocs() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (other)";
		orm.save(obj3);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);
		
		
		orm.deleteAssociations(obj2, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).size() == 1);
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	@Test
	public void deleteManyToManyJoinLeavingOtherAssocs() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (Other)";
		orm.save(obj3);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);
		
		
		orm.deleteAssociation(obj2, obj1, assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).size() == 1);
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	@Test
	public void deleteManyToManyJoinsLeavingNoAssocs() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);
		
		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);
		
		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (other)";
		orm.save(obj3);
		
		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);
		
		
		orm.deleteAssociations(obj1,  assoc);
		
		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
		
	}
	
	@Test
	public void testBigDecimal() {	
		MAGA orm = new MAGA(dataSource, new MemcachedCache(client));
		orm.schemaSync();
		Obj3 obj = new Obj3();
		obj.val = new BigDecimal(3.25);
		orm.save(obj);
		Assert.assertTrue(((Obj3) orm.load(Obj3.class, obj.id)).val.compareTo(new BigDecimal(3.25)) == 0) ;
		
	}
}