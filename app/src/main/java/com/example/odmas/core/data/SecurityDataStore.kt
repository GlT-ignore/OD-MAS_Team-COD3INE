package com.example.odmas.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single source-of-truth DataStore definition.
 * There must be exactly one DataStore per file name across the process.
 */
val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "security_state"
)


