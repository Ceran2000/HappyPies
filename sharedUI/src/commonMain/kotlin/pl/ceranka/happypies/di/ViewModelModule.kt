package pl.ceranka.happypies.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pl.ceranka.happypies.ui.screen.add_edit_report.AddReportViewModel
import pl.ceranka.happypies.ui.screen.add_edit_report.EditReportViewModel
import pl.ceranka.happypies.ui.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.ui.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.ui.screen.client_home.ClientHomeViewModel
import pl.ceranka.happypies.ui.screen.login.LoginViewModel
import pl.ceranka.happypies.ui.screen.report_details.ReportDetailsViewModel

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ClientListViewModel)
    viewModel { params -> ClientDetailsViewModel(get(), get(), get(), clientId = params.get()) }
    viewModel { params -> ClientHomeViewModel(get(), get(), get(), clientId = params.get()) }
    viewModel { (clientId: String, reportId: String) -> ReportDetailsViewModel(get(), get(), get(), get(), clientId, reportId) }
    viewModel { params -> AddReportViewModel(get(), get(), clientId = params.get()) }
    viewModel { params -> EditReportViewModel(get(), get(), reportId = params.get()) }
}