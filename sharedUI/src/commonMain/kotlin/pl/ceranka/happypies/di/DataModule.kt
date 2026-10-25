package pl.ceranka.happypies.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountRepositoryImpl
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.ClientRepositoryImpl
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.report.ReportRepositoryImpl
import pl.ceranka.happypies.data.trainer.TrainerRepository
import pl.ceranka.happypies.data.trainer.TrainerRepositoryImpl

/** A coroutine scope that lives as long as the app, for work that must outlive any screen. */
val ApplicationScope = named("ApplicationScope")

val dataModule = module {
    single<CoroutineScope>(ApplicationScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single<TrainerRepository> { TrainerRepositoryImpl(get()) }
    single<ReportRepository> { ReportRepositoryImpl(get()) }
    single<AccountRepository> { AccountRepositoryImpl(get(), get(), get(ApplicationScope)) }
}
