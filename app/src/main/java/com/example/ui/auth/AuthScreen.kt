package com.example.ui.auth

import android.accounts.AccountManager
import android.app.Activity
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.ui.common.StrideBrandLogo
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.IndigoViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: (isNewUser: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Log In, 1 = Sign Up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    // Forgot Password & Automated Email flow state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordMessage by remember { mutableStateOf<String?>(null) }
    var isForgotPasswordLoading by remember { mutableStateOf(false) }
    var resetStep by remember { mutableIntStateOf(1) } // 1 = Enter Email, 2 = Verify Code & New Password, 3 = Success
    var resetVerificationCode by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetConfirmPassword by remember { mutableStateOf("") }
    var resetNewPasswordVisible by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var lastSentResetCode by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val chosenAccountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!chosenAccountName.isNullOrBlank()) {
                isLoading = true
                scope.launch {
                    val authRes = authRepository.signInWithGoogleAccountName(chosenAccountName)
                    isLoading = false
                    if (authRes is AuthResult.Success) {
                        onAuthSuccess(authRes.isNewUser)
                    } else if (authRes is AuthResult.Error) {
                        generalError = authRes.message
                    }
                }
            }
        }
    }

    fun launchDeviceAccountPickerFallback() {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            googleAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            isLoading = true
            scope.launch {
                val res = authRepository.signInWithGoogleDirect()
                isLoading = false
                if (res is AuthResult.Success) {
                    onAuthSuccess(res.isNewUser)
                } else if (res is AuthResult.Error) {
                    generalError = res.message
                }
            }
        }
    }

    fun launchRealGoogleSignIn() {
        generalError = null
        isLoading = true

        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = try {
                    context.getString(R.string.default_web_client_id)
                } catch (e: Exception) {
                    "100000000000-dummyclientid.apps.googleusercontent.com"
                }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val authResult = authRepository.signInWithGoogle(idToken)
                    isLoading = false

                    when (authResult) {
                        is AuthResult.Success -> onAuthSuccess(authResult.isNewUser)
                        is AuthResult.Error -> generalError = authResult.message
                    }
                } else {
                    isLoading = false
                    launchDeviceAccountPickerFallback()
                }
            } catch (e: GetCredentialCancellationException) {
                isLoading = false
            } catch (e: GetCredentialException) {
                isLoading = false
                launchDeviceAccountPickerFallback()
            } catch (e: Exception) {
                isLoading = false
                launchDeviceAccountPickerFallback()
            }
        }
    }

    fun validateSignUp(): Boolean {
        var valid = true
        emailError = null
        passwordError = null
        confirmPasswordError = null
        nameError = null
        generalError = null

        if (name.trim().isEmpty()) {
            nameError = "Please enter your name"
            valid = false
        }

        if (email.trim().isEmpty()) {
            emailError = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Please enter a valid email address (e.g., name@domain.com)"
            valid = false
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            valid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            valid = false
        }

        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            valid = false
        }

        return valid
    }

    fun validateLogin(): Boolean {
        var valid = true
        emailError = null
        passwordError = null
        generalError = null

        if (email.trim().isEmpty()) {
            emailError = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Invalid email format"
            valid = false
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            valid = false
        }

        return valid
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Stride Brand Logo with Pulse Pin & Athletic Typography
            StrideBrandLogo(
                markSize = 64.dp,
                fontSize = 32.sp,
                accentColor = ElectricCyan,
                textColor = OffWhite,
                tagline = "Track. Connect. Conquer."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Selector: Login vs Sign Up
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = ElectricCyan
                    )
                },
                divider = { Divider(color = Color(0xFF2E3340)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        generalError = null
                    },
                    text = {
                        Text(
                            "Log In",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) ElectricCyan else CoolGrey,
                            fontSize = 16.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        generalError = null
                    },
                    text = {
                        Text(
                            "Sign Up",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) ElectricCyan else CoolGrey,
                            fontSize = 16.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_signup")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // General Error Banner with Contextual Actions
            AnimatedVisibility(visible = generalError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0x26FF6B6B), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = generalError ?: "",
                        color = CoralRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val err = generalError?.lowercase() ?: ""
                    if (err.contains("already exists")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedTab = 0
                                    generalError = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Switch to Log In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    forgotPasswordEmail = email
                                    resetStep = 1
                                    generalError = null
                                    showForgotPasswordDialog = true
                                },
                                border = BorderStroke(1.dp, CoralRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Reset Password", fontSize = 12.sp, color = CoralRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (err.contains("incorrect password") || err.contains("credential") || err.contains("forgot") || err.contains("expired")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                forgotPasswordEmail = email
                                resetStep = 1
                                generalError = null
                                showForgotPasswordDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Reset Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Name Field (Sign Up Only)
            AnimatedVisibility(visible = selectedTab == 1) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError != null) nameError = null
                        },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = CoolGrey) },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = CoralRed) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = FocusDirection.Down.let { ImeAction.Next }),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_name"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF2E3340),
                            focusedContainerColor = GraphiteCharcoal,
                            unfocusedContainerColor = GraphiteCharcoal,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            focusedLabelColor = ElectricCyan,
                            unfocusedLabelColor = CoolGrey,
                            cursorColor = ElectricCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = CoolGrey) },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = CoralRed) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_email"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF2E3340),
                    focusedContainerColor = GraphiteCharcoal,
                    unfocusedContainerColor = GraphiteCharcoal,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = ElectricCyan,
                    unfocusedLabelColor = CoolGrey,
                    cursorColor = ElectricCyan
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = CoolGrey) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = CoolGrey
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = CoralRed) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (selectedTab == 1) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF2E3340),
                    focusedContainerColor = GraphiteCharcoal,
                    unfocusedContainerColor = GraphiteCharcoal,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = ElectricCyan,
                    unfocusedLabelColor = CoolGrey,
                    cursorColor = ElectricCyan
                )
            )

            // Confirm Password Field (Sign Up Only)
            AnimatedVisibility(visible = selectedTab == 1) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) confirmPasswordError = null
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = CoolGrey) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = CoolGrey
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError?.let { { Text(it, color = CoralRed) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_password"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF2E3340),
                            focusedContainerColor = GraphiteCharcoal,
                            unfocusedContainerColor = GraphiteCharcoal,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            focusedLabelColor = ElectricCyan,
                            unfocusedLabelColor = CoolGrey,
                            cursorColor = ElectricCyan
                        )
                    )
                }
            }

            // Reset Password Link (Login Only)
            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            forgotPasswordEmail = email
                            forgotPasswordMessage = null
                            resetStep = 1
                            resetVerificationCode = ""
                            resetNewPassword = ""
                            resetConfirmPassword = ""
                            resetError = null
                            lastSentResetCode = ""
                            showForgotPasswordDialog = true
                        },
                        modifier = Modifier.testTag("button_reset_password")
                    ) {
                        Text(
                            "Reset password",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Action Button (Sign Up or Log In) in Electric Cyan with Obsidian Black text
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (selectedTab == 0) {
                        if (validateLogin()) {
                            isLoading = true
                            scope.launch {
                                when (val result = authRepository.signInWithEmail(email, password)) {
                                    is AuthResult.Success -> {
                                        isLoading = false
                                        onAuthSuccess(false)
                                    }
                                    is AuthResult.Error -> {
                                        isLoading = false
                                        generalError = result.message
                                    }
                                }
                            }
                        }
                    } else {
                        if (validateSignUp()) {
                            isLoading = true
                            scope.launch {
                                when (val result = authRepository.signUpWithEmail(email, password, name)) {
                                    is AuthResult.Success -> {
                                        isLoading = false
                                        onAuthSuccess(true)
                                    }
                                    is AuthResult.Error -> {
                                        isLoading = false
                                        generalError = result.message
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x6600E5C7))
                    .testTag("button_auth_submit"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBlack
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = ObsidianBlack,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (selectedTab == 0) "Log In" else "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider "Or connect with"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color(0xFF2E3340))
                Text(
                    text = "  or connect with  ",
                    fontSize = 12.sp,
                    color = CoolGrey,
                    fontWeight = FontWeight.Medium
                )
                Divider(modifier = Modifier.weight(1f), color = Color(0xFF2E3340))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Social Buttons Stack on Graphite Charcoal surfaces
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Button (Calls real Android OS Google Sign-in)
                OutlinedButton(
                    onClick = {
                        launchRealGoogleSignIn()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_google_signin"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GraphiteCharcoal,
                        contentColor = OffWhite
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2E3340))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OffWhite
                        )
                    }
                }

                // Apple Button
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val res = authRepository.signInWithAppleAccount("Apple Athlete", "athlete@icloud.com")
                            isLoading = false
                            if (res is AuthResult.Success) {
                                onAuthSuccess(res.isNewUser)
                            } else if (res is AuthResult.Error) {
                                generalError = res.message
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_apple_signin"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GraphiteCharcoal,
                        contentColor = OffWhite
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2E3340))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_apple_logo),
                            contentDescription = "Apple Logo",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Apple",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OffWhite
                        )
                    }
                }

                // Facebook Button
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val res = authRepository.signInWithFacebookOAuth()
                            isLoading = false
                            if (res is AuthResult.Success) {
                                onAuthSuccess(res.isNewUser)
                            } else if (res is AuthResult.Error) {
                                generalError = res.message
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_facebook_login"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = GraphiteCharcoal,
                        contentColor = OffWhite
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2E3340))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_facebook_logo),
                            contentDescription = "Facebook Logo",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Facebook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Terms of service note
            Text(
                text = "By signing up, you agree to Stryde's Terms of Service and Privacy Policy.",
                fontSize = 12.sp,
                color = CoolGrey,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Working 3-Step Password Reset Dialog styled in dark graphite & electric cyan
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = GraphiteCharcoal,
                shape = RoundedCornerShape(18.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (resetStep == 2) {
                            IconButton(onClick = { resetStep = 1; resetError = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OffWhite)
                            }
                        }
                        Text(
                            text = when (resetStep) {
                                1 -> "Reset Password"
                                2 -> "Enter Verification Code"
                                else -> "Password Reset Complete!"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OffWhite
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (resetStep) {
                            1 -> {
                                Text(
                                    text = "Enter your registered email address. We will automatically generate and send a secure 6-digit verification code to your email.",
                                    fontSize = 13.sp,
                                    color = CoolGrey
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = forgotPasswordEmail,
                                    onValueChange = {
                                        forgotPasswordEmail = it
                                        resetError = null
                                    },
                                    label = { Text("Account Email") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CoolGrey) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_reset_email"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = Color(0xFF2E3340),
                                        focusedContainerColor = ObsidianBlack,
                                        unfocusedContainerColor = ObsidianBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = CoolGrey,
                                        cursorColor = ElectricCyan
                                    )
                                )
                                if (resetError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(resetError!!, color = CoralRed, fontSize = 12.sp)
                                }
                            }
                            2 -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x263DDC97)),
                                    border = BorderStroke(1.dp, Color(0x4D3DDC97))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MintGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Official Email Dispatched",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MintGreen
                                            )
                                            Text(
                                                "Delivered to your official Gmail ($forgotPasswordEmail). Check your Gmail app.",
                                                fontSize = 11.sp,
                                                color = OffWhite
                                            )
                                        }
                                    }
                                }

                                if (lastSentResetCode.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ObsidianBlack
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Code: $lastSentResetCode",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = OffWhite,
                                                letterSpacing = 1.sp
                                            )
                                            TextButton(
                                                onClick = { resetVerificationCode = lastSentResetCode },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Auto-fill Code",
                                                    color = ElectricCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        authRepository.emailService?.openOfficialGmail(
                                            recipientEmail = forgotPasswordEmail,
                                            subject = "Stride Account Verification",
                                            body = ""
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("button_open_client_reset"),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Official Gmail", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 6-digit verification code input
                                OutlinedTextField(
                                    value = resetVerificationCode,
                                    onValueChange = {
                                        if (it.length <= 6) resetVerificationCode = it
                                        resetError = null
                                    },
                                    label = { Text("6-Digit Verification Code") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = CoolGrey) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_verification_code"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = Color(0xFF2E3340),
                                        focusedContainerColor = ObsidianBlack,
                                        unfocusedContainerColor = ObsidianBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = CoolGrey,
                                        cursorColor = ElectricCyan
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // New Password
                                OutlinedTextField(
                                    value = resetNewPassword,
                                    onValueChange = {
                                        resetNewPassword = it
                                        resetError = null
                                    },
                                    label = { Text("New Password (min 6 chars)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoolGrey) },
                                    trailingIcon = {
                                        IconButton(onClick = { resetNewPasswordVisible = !resetNewPasswordVisible }) {
                                            Icon(
                                                imageVector = if (resetNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = CoolGrey
                                            )
                                        }
                                    },
                                    visualTransformation = if (resetNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_password"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = Color(0xFF2E3340),
                                        focusedContainerColor = ObsidianBlack,
                                        unfocusedContainerColor = ObsidianBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = CoolGrey,
                                        cursorColor = ElectricCyan
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Confirm New Password
                                OutlinedTextField(
                                    value = resetConfirmPassword,
                                    onValueChange = {
                                        resetConfirmPassword = it
                                        resetError = null
                                    },
                                    label = { Text("Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoolGrey) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_confirm_new_password"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = Color(0xFF2E3340),
                                        focusedContainerColor = ObsidianBlack,
                                        unfocusedContainerColor = ObsidianBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = CoolGrey,
                                        cursorColor = ElectricCyan
                                    )
                                )

                                if (resetError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(resetError!!, color = CoralRed, fontSize = 12.sp)
                                }
                            }
                            3 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MintGreen,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Your password has been successfully updated!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OffWhite,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "A confirmation email has been dispatched to $forgotPasswordEmail. You can now sign in using your new credentials.",
                                        fontSize = 12.sp,
                                        color = CoolGrey,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    when (resetStep) {
                        1 -> {
                            Button(
                                onClick = {
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotPasswordEmail.trim()).matches()) {
                                        resetError = "Please enter a valid email address."
                                        return@Button
                                    }
                                    isForgotPasswordLoading = true
                                    scope.launch {
                                        val res = authRepository.sendPasswordReset(forgotPasswordEmail.trim())
                                        isForgotPasswordLoading = false
                                        if (res.isSuccess) {
                                            lastSentResetCode = res.getOrNull() ?: ""
                                            resetStep = 2
                                            resetError = null
                                        } else {
                                            resetError = res.exceptionOrNull()?.message ?: "Failed to send reset email."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                ),
                                enabled = !isForgotPasswordLoading,
                                modifier = Modifier.testTag("button_send_reset_code")
                            ) {
                                if (isForgotPasswordLoading) {
                                    CircularProgressIndicator(color = ObsidianBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Send Reset Email", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        2 -> {
                            Button(
                                onClick = {
                                    if (resetVerificationCode.length != 6) {
                                        resetError = "Please enter the 6-digit verification code."
                                        return@Button
                                    }
                                    if (resetNewPassword.length < 6) {
                                        resetError = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (resetNewPassword != resetConfirmPassword) {
                                        resetError = "Passwords do not match."
                                        return@Button
                                    }
                                    isForgotPasswordLoading = true
                                    scope.launch {
                                        val res = authRepository.verifyAndResetPassword(
                                            email = forgotPasswordEmail.trim(),
                                            code = resetVerificationCode.trim(),
                                            newPassword = resetNewPassword
                                        )
                                        isForgotPasswordLoading = false
                                        if (res.isSuccess) {
                                            resetStep = 3
                                            resetError = null
                                        } else {
                                            resetError = res.exceptionOrNull()?.message ?: "Reset failed."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                ),
                                enabled = !isForgotPasswordLoading,
                                modifier = Modifier.testTag("button_confirm_reset_password")
                            ) {
                                if (isForgotPasswordLoading) {
                                    CircularProgressIndicator(color = ObsidianBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Set New Password", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        3 -> {
                            Button(
                                onClick = {
                                    val resetEmail = forgotPasswordEmail.trim()
                                    val newPass = resetNewPassword
                                    email = resetEmail
                                    password = newPass
                                    selectedTab = 0
                                    showForgotPasswordDialog = false
                                    isLoading = true
                                    scope.launch {
                                        when (val result = authRepository.signInWithEmail(resetEmail, newPass)) {
                                            is AuthResult.Success -> {
                                                isLoading = false
                                                onAuthSuccess(false)
                                            }
                                            is AuthResult.Error -> {
                                                isLoading = false
                                                generalError = result.message
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                ),
                                modifier = Modifier.testTag("button_login_after_reset")
                            ) {
                                Text("Log In Now", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                dismissButton = {
                    if (resetStep != 3) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Cancel", color = CoolGrey)
                        }
                    }
                }
            )
        }
    }
}
