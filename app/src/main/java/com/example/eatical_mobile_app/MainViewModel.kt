package com.example.eatical_mobile_app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _state: MutableLiveData<MainStates> = MutableLiveData(MainStates.LOADING)
    val state: LiveData<MainStates> = _state

    init {
        viewModelScope.launch {
            delay(1000)
            _state.value = MainStates.GETTING_LOCATION_PERMISSION
        }
    }

    fun setState(newState: MainStates){
        _state.value = newState
    }
}