package code.yousef.dari.shared.di

import code.yousef.dari.sama.interfaces.SecurityProvider
import code.yousef.dari.sama.interfaces.OpenBankingClient
import code.yousef.dari.sama.implementation.CommonSecurityProvider
import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.data.database.DatabaseDriverFactory
import code.yousef.dari.shared.data.network.ApiClient
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.AccountRepositoryImpl
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.data.repository.TransactionRepositoryImpl
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.GetTransactionsUseCase
import code.yousef.dari.shared.domain.usecase.SyncAccountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Networking Module
 * Provides HTTP client, API client, and networking configuration
 */
val networkingModule = module {
    
    // JSON configuration
    single {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    
    // HTTP Client
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message")
                    }
                }
                level = LogLevel.INFO
            }
            
            defaultRequest {
                // Common headers and configuration
                headers.append("User-Agent", "Dari-App/1.0.0")
                headers.append("Accept", "application/json")
            }
        }
    }
    
    // API Client wrapper
    singleOf(::ApiClient)
}

/**
 * Database Module
 * Provides database driver, database instance, and DAO access
 */
val databaseModule = module {
    
    // Platform-specific database driver
    single { DatabaseDriverFactory().createDriver() }
    
    // Database instance
    single { DariDatabase(get()) }
    
    // DAOs
    single { get<DariDatabase>().accountDao() }
    single { get<DariDatabase>().transactionDao() }
    single { get<DariDatabase>().budgetDao() }
    single { get<DariDatabase>().goalDao() }
    single { get<DariDatabase>().categoryDao() }
}

/**
 * Repository Module
 * Provides data layer implementations
 */
val repositoryModule = module {
    
    // Account Repository
    singleOf(::AccountRepositoryImpl) bind AccountRepository::class
    
    // Transaction Repository  
    singleOf(::TransactionRepositoryImpl) bind TransactionRepository::class
    
    // Additional repositories would be added here as they're implemented
    // singleOf(::BudgetRepositoryImpl) bind BudgetRepository::class
    // singleOf(::GoalRepositoryImpl) bind GoalRepository::class
}

/**
 * Use Case Module
 * Provides domain layer use cases
 */
val useCaseModule = module {
    
    // Account Use Cases
    factoryOf(::GetAccountsUseCase)
    factoryOf(::SyncAccountsUseCase)
    
    // Transaction Use Cases
    factoryOf(::GetTransactionsUseCase)
    
    // Additional use cases would be added here
    // factoryOf(::CreateBudgetUseCase)
    // factoryOf(::UpdateGoalUseCase)
}

/**
 * Security Module
 * Provides security-related dependencies including SAMA SDK
 */
val securityModule = module {
    
    // Security Provider (platform-specific implementation will be provided in platform modules)
    single<SecurityProvider> { CommonSecurityProvider() }
    
    // Mock OpenBankingClient for now (will be replaced with real implementation)
    single<OpenBankingClient> {
        object : OpenBankingClient {
            // Mock implementation
        }
    }
}

/**
 * Core Module
 * Combines all modules for easy initialization
 */
val coreModules = listOf(
    networkingModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    securityModule
)

/**
 * Development Module
 * Additional dependencies for development builds
 */
val developmentModule = module {
    // Development-specific configurations
    // Logger implementations, debug tools, etc.
}

/**
 * Production Module  
 * Additional dependencies for production builds
 */
val productionModule = module {
    // Production-specific configurations
    // Crash reporting, analytics, etc.
}