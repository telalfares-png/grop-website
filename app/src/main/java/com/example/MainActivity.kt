package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.EmployeeRepository
import com.example.ui.MainApp
import com.example.ui.MainViewModel
import com.example.ui.theme.EmployeeManagerTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels {
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(application) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      EmployeeManagerTheme {
        MainApp(viewModel = viewModel)
      }
    }
  }
}

