package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.ClientRepositoryImpl
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.report.ReportRepositoryImpl
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRepositoryImpl

val dataModule = module {
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single<ReportRepository> { ReportRepositoryImpl(get()) }
    single { SessionManager(get(), get()) }
}