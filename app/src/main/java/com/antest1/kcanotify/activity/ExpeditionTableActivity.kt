package com.antest1.kcanotify.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.antest1.kcanotify.KcaUtils
import com.antest1.kcanotify.R
import com.antest1.kcanotify.ui.component.KcaComposeActivity
import com.antest1.kcanotify.ui.theme.KcaTheme
import com.antest1.kcanotify.viewmodel.ExpeditionTableViewModel

class ExpeditionTableActivity : KcaComposeActivity() {
    private val viewModel: ExpeditionTableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
        setContent {
            KcaTheme {
                ExpeditionTableScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
        KcaUtils.showDataLoadErrorToast(applicationContext, getString(R.string.download_check_error))
    }
}
