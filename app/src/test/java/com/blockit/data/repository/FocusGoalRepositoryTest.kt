package com.blockit.data.repository

import com.blockit.data.local.db.dao.FocusGoalDao
import com.blockit.data.local.db.entity.FocusGoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FocusGoalRepositoryTest {

    private val storage = mutableListOf<FocusGoalEntity>()
    private val stateFlow = MutableStateFlow<List<FocusGoalEntity>>(emptyList())

    private val fakeDao = object : FocusGoalDao {
        override fun observeGoalsForDate(targetDate: String): Flow<List<FocusGoalEntity>> {
            return stateFlow
        }

        override suspend fun getGoalsForDate(targetDate: String): List<FocusGoalEntity> {
            return storage.filter { it.targetDate == targetDate }
        }

        override fun observeGoalsForSession(sessionId: Long): Flow<List<FocusGoalEntity>> {
            return stateFlow
        }

        override suspend fun getGoalById(id: Long): FocusGoalEntity? {
            return storage.find { it.id == id }
        }

        override suspend fun insertGoal(goal: FocusGoalEntity): Long {
            val newId = (storage.size + 1).toLong()
            val inserted = goal.copy(id = newId)
            storage.add(inserted)
            stateFlow.value = storage.toList()
            return newId
        }

        override suspend fun insertGoals(goals: List<FocusGoalEntity>) {
            goals.forEach { insertGoal(it) }
        }

        override suspend fun updateGoal(goal: FocusGoalEntity) {
            val index = storage.indexOfFirst { it.id == goal.id }
            if (index >= 0) {
                storage[index] = goal
                stateFlow.value = storage.toList()
            }
        }

        override suspend fun setGoalCompletion(id: Long, isCompleted: Boolean, completedAtMs: Long?) {
            val index = storage.indexOfFirst { it.id == id }
            if (index >= 0) {
                storage[index] = storage[index].copy(isCompleted = isCompleted, completedAtMs = completedAtMs)
                stateFlow.value = storage.toList()
            }
        }

        override suspend fun linkGoalToSession(id: Long, sessionId: Long) {
            val index = storage.indexOfFirst { it.id == id }
            if (index >= 0) {
                storage[index] = storage[index].copy(sessionId = sessionId)
                stateFlow.value = storage.toList()
            }
        }

        override suspend fun deleteGoal(id: Long) {
            storage.removeAll { it.id == id }
            stateFlow.value = storage.toList()
        }

        override suspend fun deleteGoalsForDate(targetDate: String) {
            storage.removeAll { it.targetDate == targetDate }
            stateFlow.value = storage.toList()
        }
    }

    private lateinit var repository: FocusGoalRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        storage.clear()
        stateFlow.value = emptyList()
        repository = FocusGoalRepository(fakeDao, testDispatcher)
    }

    @Test
    fun addGoal_trimsTitleAndSetsDateAndOrderIndex() = runTest(testDispatcher) {
        val today = FocusGoalRepository.getTodayIsoDate()
        val id = repository.addGoal("  Finish Chapter 3  ", targetDate = today)

        assertEquals(1L, id)
        assertEquals(1, storage.size)
        val added = storage.first()
        assertEquals("Finish Chapter 3", added.title)
        assertEquals(today, added.targetDate)
        assertEquals(0, added.orderIndex)
        assertFalse(added.isCompleted)
    }

    @Test
    fun addGoal_rejectsBlankTitle() = runTest(testDispatcher) {
        val id = repository.addGoal("   ")
        assertEquals(-1L, id)
        assertTrue(storage.isEmpty())
    }

    @Test
    fun toggleGoalCompletion_updatesCompletedAndTimestamp() = runTest(testDispatcher) {
        val id = repository.addGoal("Draft architecture spec")
        repository.toggleGoalCompletion(id, true)

        val updated = storage.first()
        assertTrue(updated.isCompleted)
        assertNotNull(updated.completedAtMs)

        repository.toggleGoalCompletion(id, false)
        val uncompleted = storage.first()
        assertFalse(uncompleted.isCompleted)
        assertNull(uncompleted.completedAtMs)
    }

    @Test
    fun linkGoalToSession_updatesSessionId() = runTest(testDispatcher) {
        val id = repository.addGoal("Code review")
        repository.linkGoalToSession(id, 42L)

        val linked = storage.first()
        assertEquals(42L, linked.sessionId)
    }

    @Test
    fun deleteGoal_removesFromStorage() = runTest(testDispatcher) {
        val id1 = repository.addGoal("Goal 1")
        val id2 = repository.addGoal("Goal 2")

        repository.deleteGoal(id1)
        assertEquals(1, storage.size)
        assertEquals(id2, storage.first().id)
    }

    @Test
    fun relativeDateLabel_resolvesTodayAndTomorrow() {
        val today = FocusGoalRepository.getTodayIsoDate()
        val tomorrow = FocusGoalRepository.getTomorrowIsoDate()

        assertEquals("Today", FocusGoalRepository.getRelativeDateLabel(today))
        assertEquals("Tomorrow", FocusGoalRepository.getRelativeDateLabel(tomorrow))
    }

    @Test
    fun getDayOptions_returnsSequenceIncludingTodayAndTomorrow() {
        val options = FocusGoalRepository.getDayOptions(daysBack = 1, daysForward = 3)
        assertEquals(5, options.size)
        assertTrue("Contains Today option", options.any { it.isToday && it.label == "Today" })
        assertTrue("Contains Tomorrow option", options.any { it.label == "Tomorrow" })
    }
}
