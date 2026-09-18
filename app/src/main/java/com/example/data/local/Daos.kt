package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AdvanceRequest
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Department
import com.example.data.model.Employee
import com.example.data.model.Penalty
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE status != 'deleted' ORDER BY id ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE status != 'deleted' ORDER BY id ASC")
    suspend fun getAllEmployeesDirect(): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    fun getEmployeeByIdFlow(id: Long): Flow<Employee?>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM employees WHERE username = :username AND status != 'deleted' LIMIT 1")
    suspend fun getEmployeeByUsername(username: String): Employee?

    @Query("SELECT * FROM employees WHERE (username = :u OR no = :n OR mobile = :m) AND status != 'deleted' LIMIT 1")
    suspend fun getEmployeeByFields(u: String, n: String, m: String): Employee?

    suspend fun getEmployeeByIdentifier(identifier: String): Employee? =
        getEmployeeByFields(identifier, identifier, identifier)

    @Query("SELECT * FROM employees WHERE supId = :supId AND status != 'deleted'")
    fun getSubordinates(supId: Long): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE role = 'gm' AND status = 'active'")
    suspend fun getGmEmployees(): List<Employee>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getEmployeeCount(): Int
}

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY id ASC")
    fun getAllDepartments(): Flow<List<Department>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(departments: List<Department>)
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances ORDER BY id DESC")
    fun getAllAdvances(): Flow<List<AdvanceRequest>>

    @Query("SELECT * FROM advances WHERE empId = :empId ORDER BY id DESC")
    fun getAdvancesByEmpId(empId: Long): Flow<List<AdvanceRequest>>

    @Query("SELECT * FROM advances WHERE id = :id LIMIT 1")
    suspend fun getAdvanceById(id: Long): AdvanceRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceRequest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(advances: List<AdvanceRequest>)

    @Update
    suspend fun updateAdvance(advance: AdvanceRequest)
}

@Dao
interface PenaltyDao {
    @Query("SELECT * FROM penalties ORDER BY id DESC")
    fun getAllPenalties(): Flow<List<Penalty>>

    @Query("SELECT * FROM penalties WHERE empId = :empId ORDER BY id DESC")
    fun getPenaltiesByEmpId(empId: Long): Flow<List<Penalty>>

    @Query("SELECT * FROM penalties WHERE id = :id LIMIT 1")
    suspend fun getPenaltyById(id: Long): Penalty?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenalty(penalty: Penalty): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(penalties: List<Penalty>)

    @Update
    suspend fun updatePenalty(penalty: Penalty)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE empId = :empId ORDER BY date ASC, id ASC")
    fun getTransactionsByEmpId(empId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY id DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Long)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLog>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}
