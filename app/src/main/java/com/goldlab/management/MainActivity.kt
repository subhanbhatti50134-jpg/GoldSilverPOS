
package com.goldlab.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldlab.management.data.LabDatabase
import com.goldlab.management.data.PurchaseRepository
import com.goldlab.management.ui.AuthGate
import com.goldlab.management.ui.LabApp
import com.goldlab.management.ui.LabViewModel
import com.goldlab.management.ui.LabViewModelFactory
import com.goldlab.management.ui.theme.LabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = LabDatabase.getInstance(applicationContext)

        val repository = PurchaseRepository(
            db.purchaseDao(),
            db.customerDao(),
            db.paymentDao(),
            db
        )

        setContent {
            val vm: LabViewModel = viewModel(
                factory = LabViewModelFactory(repository)
            )

            LabTheme {
                AuthGate {
                    LabApp(vm)
                }
            }
        }
    }
}

