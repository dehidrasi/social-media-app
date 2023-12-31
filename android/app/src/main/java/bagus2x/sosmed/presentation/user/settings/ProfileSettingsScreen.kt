package bagus2x.sosmed.presentation.user.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import bagus2x.sosmed.presentation.common.components.Button

@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    viewModel: ProfileSettingsViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileSettingsScreen(
        stateProvider = { state },
        signOut = viewModel::signOut
    )
}

@Composable
fun ProfileSettingsScreen(
    stateProvider: () -> ProfileSettingsState,
    signOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxWidth()
    ) {
        Button(onClick = signOut) {
            Text(text = "Sign Out")
        }
        val context = LocalContext.current
        Button(onClick = {
            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }) {
            Text(text = "Open dev options")
        }
    }
}
