package code.yousef.dari.shared.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            networkingModule,
            databaseModule,
            repositoryModule,
            useCaseModule,
            securityModule,
            viewModelModule,
            platformModule()
        )
    }

// Platform-specific module will be provided by each platform
expect fun platformModule(): Module