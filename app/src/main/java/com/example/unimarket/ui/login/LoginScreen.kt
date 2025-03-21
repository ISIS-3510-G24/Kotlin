package com.example.unimarket.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loginSuccessState = viewModel.loginSuccess.value
    var errorMessageState = viewModel.errorMessage.value

    // If login is successful, navigate to the home screen
    LaunchedEffect(loginSuccessState) {
        if (loginSuccessState == true) {
            onLoginSuccess()
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title and subtitle
        Image(
            painter = painterResource(id = R.drawable.onboarding1),
            contentDescription = "UniMarket Logo",
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        // "Forgot password" button
        TextButton(onClick = { viewModel.resetPassword(email) }) {
            Text("Forgot password?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = { viewModel.loginUser(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        // Error message
        if (errorMessageState != null) {
            Text(
                text = errorMessageState,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Text to navigate to the register screen
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Not a member?")
            TextButton(onClick = onNavigateToRegister) {
                Text("Register now")
            }
        }

        //Spacer(modifier = Modifier.height(16.dp))

//        // Options to continue with Google or Facebook
//        Text("Or continue with", style = MaterialTheme.typography.bodyMedium)
//        Spacer(modifier = Modifier.height(8.dp))
//        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            Button(onClick = { /* TODO: Google Sign-In */ }) {
//                Text("Google")
//            }
//            Button(onClick = { /* TODO: Facebook Login */ }) {
//                Text("Facebook")
//            }
//        }
    }
}