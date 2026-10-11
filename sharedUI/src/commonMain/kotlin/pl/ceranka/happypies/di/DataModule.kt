package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.ClientRepositoryImpl
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.report.ReportRepositoryImpl
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRepositoryImpl

/** Repositories shared by both platforms; each platform provides only the FirestoreGateway. */
val dataModule = module {
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single<ReportRepository> { ReportRepositoryImpl(get()) }
}
