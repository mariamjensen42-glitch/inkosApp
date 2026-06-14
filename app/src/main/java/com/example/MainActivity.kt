package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelExtended
import com.example.ui.navigation.InkOSNavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val extendedViewModel: MainViewModelExtended by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support full-bleed edge-to-edge layouts as per Material 3 guidelines
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                InkOSNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
