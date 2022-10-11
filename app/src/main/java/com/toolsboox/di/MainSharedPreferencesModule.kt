package com.toolsboox.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable

/**
 * Provide main shared preferences.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@Module
object MainSharedPreferencesModule {

    /**
     * Provides main shared preferences.
     */
    @Provides
    @Reusable
    internal fun provideSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MAIN", Context.MODE_PRIVATE)
    }
}
