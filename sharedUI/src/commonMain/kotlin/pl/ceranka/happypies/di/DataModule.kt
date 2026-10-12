package pl.ceranka.happypies.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountRepositoryImpl
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.ClientRepositoryImpl
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.report.ReportRepositoryImpl

val dataModule = module {
    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single<ReportRepository> { ReportRepositoryImpl(get()) }
    // Lives as long as the app.
    single<AccountRepository> { AccountRepositoryImpl(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}