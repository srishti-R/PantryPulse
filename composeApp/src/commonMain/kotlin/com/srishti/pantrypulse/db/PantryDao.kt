import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.srishti.pantrypulse.db.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items")
    fun getAllItemsFlow(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem)

    @Update
    suspend fun updateItem(item: PantryItem)

    @Delete
    suspend fun deleteItem(item: PantryItem)
}