package com.example.smartlock.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartlock.viewmodel.DoorViewModel

class DoorViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if(modelClass.isAssignableFrom(DoorViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return DoorViewModel(context) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
}