package com.antest1.kcanotify.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.antest1.kcanotify.ui.component.KcaComposeActivity
import com.antest1.kcanotify.ui.theme.KcaTheme
import com.antest1.kcanotify.viewmodel.FleetInfoViewModel

class FleetInfoActivity : KcaComposeActivity() {
    private val viewModel: FleetInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
        setContent {
            KcaTheme {
                FleetInfoScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
    }
}
