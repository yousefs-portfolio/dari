package code.yousef.dari.shared.di

import code.yousef.dari.shared.data.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    
    // Database Driver Factory - Android specific
    single { 
        DatabaseDriverFactory(androidContext()) 
    }
    
    // Platform-specific implementations can be added here
    // For example: file system access, platform-specific services, etc.
}