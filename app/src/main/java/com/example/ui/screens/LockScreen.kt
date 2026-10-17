package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PinEntryPad
import com.example.ui.viewmodel.NotelyViewModel
import com.example.util.BiometricHelper

@Composable
fun LockScreen(
    viewModel: NotelyViewModel,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    val activity = remember(context) { context.findFragmentActivity() }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasPromptedBiometric by remember { mutableStateOf(false) }

    fun triggerBiometrics() {
        if (activity != null && isBiometricAvailable) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Unlock Notely",
                subtitle = "Confirm your fingerprint or biometric credential",
                onSuccess = {
                    viewModel.unlockWithBiometrics()
                    onUnlocked()
                },
                onError = { err ->
                    // Fall back to PIN
                }
            )
        }
    }

    LaunchedEffect(isBiometricEnabled, isBiometricAvailable) {
        if (isBiometricEnabled && isBiometricAvailable && !hasPromptedBiometric) {
            hasPromptedBiometric = true
            triggerBiometrics()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PinEntryPad(
                    title = "Notely is Locked",
                    subtitle = "Enter 4-digit PIN or use Biometrics",
                    pin = enteredPin,
                    onPinChange = { pin ->
                        enteredPin = pin
                        errorMessage = null
                        if (pin.length == 4) {
                            val success = viewModel.unlockApp(pin)
                            if (success) {
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect PIN. Try again."
                                enteredPin = ""
                            }
                        }
                    },
                    errorMessage = errorMessage
                )

                if (isBiometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { triggerBiometrics() },
                        modifier = Modifier.testTag("biometric_unlock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Use Biometrics")
                    }
                }
            }
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
