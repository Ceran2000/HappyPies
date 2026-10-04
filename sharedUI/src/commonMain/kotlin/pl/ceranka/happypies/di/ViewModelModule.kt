package pl.ceranka.happypies.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pl.ceranka.happypies.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.screen.login.LoginViewModel

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ClientListViewModel)
    viewModel { params -> ClientDetailsViewModel(get(), params.get()) }
}