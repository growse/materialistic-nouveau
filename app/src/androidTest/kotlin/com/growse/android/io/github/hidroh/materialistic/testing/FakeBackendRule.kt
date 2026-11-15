package com.growse.android.io.github.hidroh.materialistic.testing

import androidx.test.core.app.ApplicationProvider
import com.growse.android.io.github.hidroh.materialistic.data.MaterialisticDatabase
import org.junit.rules.ExternalResource

/**
 * Gives each test an empty [FakeBackend], and drops items cached in the database by earlier tests
 * so every fetch reaches the fake.
 */
class FakeBackendRule : ExternalResource() {
  override fun before() = reset()

  override fun after() = reset()

  private fun reset() {
    FakeBackend.reset()
    MaterialisticDatabase.getInstance(ApplicationProvider.getApplicationContext()).clearAllTables()
  }
}
