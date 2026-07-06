package com.example.whabotpro.data.db

import androidx.room.*
import com.example.whabotpro.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessInfoDao {
    @Query("SELECT * FROM business_info WHERE id = '1'")
    fun observe(): Flow<BusinessInfo?>

    @Query("SELECT * FROM business_info WHERE id = '1'")
    fun get(): BusinessInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(info: BusinessInfo)
}

@Dao
interface KbItemDao {
    @Query("SELECT * FROM kb_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<KbItem>>

    @Query("SELECT * FROM kb_items WHERE section = :section ORDER BY updatedAt DESC")
    fun observeBySection(section: String): Flow<List<KbItem>>

    @Query("SELECT * FROM kb_items WHERE section = :section ORDER BY updatedAt DESC")
    fun getBySection(section: String): List<KbItem>

    @Query("SELECT * FROM kb_items ORDER BY updatedAt DESC")
    fun getAll(): List<KbItem>

    @Query("SELECT * FROM kb_items WHERE id = :id")
    fun getById(id: String): KbItem?

    @Query("SELECT * FROM kb_items WHERE section = :section AND title = :title LIMIT 1")
    fun findByTitleAndSection(section: String, title: String): KbItem?

    @Query("SELECT * FROM kb_items WHERE section = :section AND title LIKE '%' || :query || '%'")
    fun searchInSection(section: String, query: String): List<KbItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: KbItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(items: List<KbItem>)

    @Query("DELETE FROM kb_items WHERE id = :id")
    fun delete(id: String)

    @Query("SELECT COUNT(*) FROM kb_items WHERE section = :section")
    fun countBySection(section: String): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE section = :section ORDER BY name")
    fun getBySection(section: String): List<Category>

    @Query("SELECT * FROM categories WHERE section = :section AND name = :name LIMIT 1")
    fun findByNameAndSection(section: String, name: String): Category?

    @Query("SELECT * FROM categories ORDER BY name")
    fun getAll(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(cat: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(cats: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id")
    fun delete(id: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAll(): List<Order>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getById(id: String): Order?

    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    fun findByNumber(orderNumber: String): Order?

    @Query("SELECT * FROM orders WHERE customerPhone = :phone ORDER BY createdAt DESC")
    fun findByPhone(phone: String): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(order: Order)

    @Query("DELETE FROM orders WHERE id = :id")
    fun delete(id: String)
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox ORDER BY timestamp DESC LIMIT 200")
    fun observeAll(): Flow<List<InboxMessage>>

    @Query("SELECT * FROM inbox ORDER BY timestamp DESC LIMIT 200")
    fun getAll(): List<InboxMessage>

    @Insert
    fun insert(msg: InboxMessage)

    @Query("DELETE FROM inbox")
    fun clear()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun getAll(): List<Contact>

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    fun findByPhone(phone: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    fun delete(id: String)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY title")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE active = 1 ORDER BY title")
    fun getActive(): List<Rule>

    @Query("SELECT * FROM rules WHERE title = :title LIMIT 1")
    fun findByTitle(title: String): Rule?

    @Query("SELECT * FROM rules ORDER BY title")
    fun getAll(): List<Rule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(rule: Rule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(rules: List<Rule>)

    @Query("DELETE FROM rules WHERE id = :id")
    fun delete(id: String)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 500")
    fun observeAll(): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 500")
    fun getAll(): List<LogEntry>

    @Insert
    fun insert(entry: LogEntry)

    @Query("DELETE FROM logs")
    fun clear()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = '1'")
    fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = '1'")
    fun get(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(settings: AppSettings)
}
