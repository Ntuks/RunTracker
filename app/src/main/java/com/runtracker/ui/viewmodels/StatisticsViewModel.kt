package com.runtracker.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.runtracker.repsitories.MainRepository

class StatisticsViewModel  @ViewModelInject constructor(
    val mainRepository: MainRepository
): ViewModel() {
}