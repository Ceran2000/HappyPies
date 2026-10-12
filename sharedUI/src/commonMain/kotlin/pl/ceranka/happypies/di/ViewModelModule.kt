package pl.ceranka.happypies.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pl.ceranka.happypies.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.screen.login.LoginViewModel
import pl.ceranka.happypies.screen.report_form.ReportFormViewModel

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ClientListViewModel)
    viewModel { params -> ClientDetailsViewModel(get(), get(), get(), clientId = params.get()) }
    viewModel { params -> ReportFormViewModel(get(), get(), clientId = params.get()) }
}