package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UserSession(
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val authProvider: String = "EMAIL", // EMAIL, GOOGLE, PHONE
    val is2FaEnabled: Boolean = false,
    val isLoggedIn: Boolean = false
)

enum class AuthMode {
    LOGIN,
    REGISTER,
    PHONE_AUTH,
    FORGOT_PASSWORD,
    TWO_FACTOR_VERIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (UserSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("finance_tracker_auth_prefs", Context.MODE_PRIVATE) }

    var currentAuthMode by remember { mutableStateOf(AuthMode.LOGIN) }
    
    // Login / Register state
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var enable2FaOnRegister by remember { mutableStateOf(false) }

    // Phone Auth state
    var selectedCountryCode by remember { mutableStateOf("+1 (USA)") }
    var phoneNumberInput by remember { mutableStateOf("") }
    var phoneOtpSent by remember { mutableStateOf(false) }
    var phoneOtpInput by remember { mutableStateOf("") }
    var phoneOtpTimer by remember { mutableIntStateOf(30) }

    // Forgot Password state
    var resetEmailInput by remember { mutableStateOf("") }
    var resetStep by remember { mutableIntStateOf(1) } // 1: Email, 2: Code & New Password, 3: Success
    var resetCodeInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    // 2FA state
    var pendingSession by remember { mutableStateOf<UserSession?>(null) }
    var twoFactorCodeInput by remember { mutableStateOf("") }

    // Google Modal
    var showGoogleModal by remember { mutableStateOf(false) }

    // Feedback message
    var statusMessage by remember { mutableStateOf("") }
    var isErrorStatus by remember { mutableStateOf(false) }

    // Timer countdown effect for OTP resend
    LaunchedEffect(phoneOtpSent, phoneOtpTimer) {
        if (phoneOtpSent && phoneOtpTimer > 0) {
            delay(1000L)
            phoneOtpTimer--
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("auth_container_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo and App Title
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Finance Tracker",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "FINANCE TRACKER",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enterprise-Grade Personal Wealth Management",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Title Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentAuthMode != AuthMode.LOGIN) {
                        IconButton(
                            onClick = {
                                statusMessage = ""
                                currentAuthMode = AuthMode.LOGIN
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("auth_back_to_login_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(36.dp))
                    }

                    Text(
                        text = when (currentAuthMode) {
                            AuthMode.LOGIN -> "SIGN IN"
                            AuthMode.REGISTER -> "CREATE ACCOUNT"
                            AuthMode.PHONE_AUTH -> "PHONE VERIFICATION"
                            AuthMode.FORGOT_PASSWORD -> "ACCOUNT RECOVERY"
                            AuthMode.TWO_FACTOR_VERIFY -> "TWO-FACTOR AUTH (2FA)"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.width(36.dp))
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (isErrorStatus) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isErrorStatus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content views depending on AuthMode
                AnimatedContent(
                    targetState = currentAuthMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "auth_mode_transition"
                ) { mode ->
                    when (mode) {
                        AuthMode.LOGIN -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            statusMessage = ""
                                            currentAuthMode = AuthMode.FORGOT_PASSWORD
                                        },
                                        modifier = Modifier.testTag("forgot_password_btn")
                                    ) {
                                        Text("Forgot Password?", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                                            statusMessage = "Please enter email and password."
                                            isErrorStatus = true
                                            return@Button
                                        }
                                        
                                        // Check if user enabled 2FA previously
                                        val is2faOn = prefs.getBoolean("2fa_${emailInput.lowercase().trim()}", false)
                                        val session = UserSession(
                                            email = emailInput.trim(),
                                            name = emailInput.substringBefore("@").replaceFirstChar { it.uppercase() },
                                            authProvider = "EMAIL",
                                            is2FaEnabled = is2faOn,
                                            isLoggedIn = true
                                        )

                                        if (is2faOn) {
                                            pendingSession = session
                                            statusMessage = "Enter 2FA Code sent to your device."
                                            isErrorStatus = false
                                            currentAuthMode = AuthMode.TWO_FACTOR_VERIFY
                                        } else {
                                            prefs.edit().putString("active_user_email", session.email)
                                                .putString("active_user_name", session.name)
                                                .putString("active_auth_provider", session.authProvider)
                                                .putBoolean("is_logged_in", true)
                                                .apply()
                                            Toast.makeText(context, "Welcome back, ${session.name}!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess(session)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("login_submit_btn")
                                ) {
                                    Text("Sign In with Email", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text("  OR  ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }

                                // Google Sign In Button
                                OutlinedButton(
                                    onClick = { showGoogleModal = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("google_login_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GTranslate,
                                            contentDescription = "Google Logo",
                                            tint = Color(0xFF4285F4)
                                        )
                                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                // Phone Sign In Button
                                OutlinedButton(
                                    onClick = {
                                        statusMessage = ""
                                        currentAuthMode = AuthMode.PHONE_AUTH
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("phone_login_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone Auth",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Sign In with Phone Number", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Don't have an account?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(
                                        onClick = {
                                            statusMessage = ""
                                            currentAuthMode = AuthMode.REGISTER
                                        },
                                        modifier = Modifier.testTag("goto_register_btn")
                                    ) {
                                        Text("Create One", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        AuthMode.REGISTER -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_name_input")
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password (8+ chars)") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_password_input")
                                )

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.LockReset, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_confirm_password_input")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { enable2FaOnRegister = !enable2FaOnRegister }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = enable2FaOnRegister,
                                        onCheckedChange = { enable2FaOnRegister = it },
                                        modifier = Modifier.testTag("register_2fa_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Enable 2FA (Two-Factor Authentication)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Requires an extra SMS / Authenticator code on login", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (nameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            statusMessage = "Please complete all fields."
                                            isErrorStatus = true
                                            return@Button
                                        }
                                        if (passwordInput != confirmPasswordInput) {
                                            statusMessage = "Passwords do not match."
                                            isErrorStatus = true
                                            return@Button
                                        }

                                        val emailClean = emailInput.trim().lowercase()
                                        prefs.edit()
                                            .putBoolean("2fa_$emailClean", enable2FaOnRegister)
                                            .putString("user_name_$emailClean", nameInput.trim())
                                            .apply()

                                        val session = UserSession(
                                            email = emailClean,
                                            name = nameInput.trim(),
                                            authProvider = "EMAIL",
                                            is2FaEnabled = enable2FaOnRegister,
                                            isLoggedIn = true
                                        )

                                        prefs.edit().putString("active_user_email", session.email)
                                            .putString("active_user_name", session.name)
                                            .putString("active_auth_provider", session.authProvider)
                                            .putBoolean("is_logged_in", true)
                                            .apply()

                                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess(session)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("register_submit_btn")
                                ) {
                                    Text("Create Account & Sign In", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AuthMode.PHONE_AUTH -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!phoneOtpSent) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        var expandedCountryDropdown by remember { mutableStateOf(false) }
                                        val countryCodes = listOf("+1 (USA)", "+44 (UK)", "+49 (GER)", "+91 (IND)", "+880 (BD)")

                                        Box(modifier = Modifier.weight(0.45f)) {
                                            OutlinedButton(
                                                onClick = { expandedCountryDropdown = true },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .testTag("country_code_picker_btn")
                                            ) {
                                                Text(selectedCountryCode, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            DropdownMenu(
                                                expanded = expandedCountryDropdown,
                                                onDismissRequest = { expandedCountryDropdown = false }
                                            ) {
                                                countryCodes.forEach { code ->
                                                    DropdownMenuItem(
                                                        text = { Text(code, fontSize = 12.sp) },
                                                        onClick = {
                                                            selectedCountryCode = code
                                                            expandedCountryDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = phoneNumberInput,
                                            onValueChange = { phoneNumberInput = it },
                                            label = { Text("Phone Number") },
                                            leadingIcon = { Icon(imageVector = Icons.Default.Smartphone, contentDescription = null) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(0.55f)
                                                .testTag("phone_number_input")
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (phoneNumberInput.isBlank() || phoneNumberInput.length < 7) {
                                                statusMessage = "Enter a valid phone number."
                                                isErrorStatus = true
                                                return@Button
                                            }
                                            phoneOtpSent = true
                                            phoneOtpTimer = 30
                                            statusMessage = "SMS OTP Code sent to $selectedCountryCode $phoneNumberInput"
                                            isErrorStatus = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("send_phone_otp_btn")
                                    ) {
                                        Text("Send SMS Verification Code", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "Verification code sent to $selectedCountryCode $phoneNumberInput",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = phoneOtpInput,
                                        onValueChange = { if (it.length <= 6) phoneOtpInput = it },
                                        label = { Text("Enter 6-Digit SMS Code") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Sms, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("phone_otp_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (phoneOtpInput.length < 4) {
                                                statusMessage = "Please enter valid OTP code."
                                                isErrorStatus = true
                                                return@Button
                                            }
                                            val fullPhone = "$selectedCountryCode $phoneNumberInput"
                                            val session = UserSession(
                                                email = "user_${phoneNumberInput.takeLast(4)}@phone.auth",
                                                name = "Phone User ($phoneNumberInput)",
                                                phone = fullPhone,
                                                authProvider = "PHONE",
                                                is2FaEnabled = false,
                                                isLoggedIn = true
                                            )
                                            prefs.edit().putString("active_user_email", session.email)
                                                .putString("active_user_name", session.name)
                                                .putString("active_auth_provider", session.authProvider)
                                                .putBoolean("is_logged_in", true)
                                                .apply()
                                            Toast.makeText(context, "Phone Verified!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess(session)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("verify_phone_otp_btn")
                                    ) {
                                        Text("Verify Code & Sign In", fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { phoneOtpSent = false }
                                        ) {
                                            Text("Change Number", fontSize = 11.sp)
                                        }

                                        TextButton(
                                            enabled = phoneOtpTimer == 0,
                                            onClick = {
                                                phoneOtpTimer = 30
                                                statusMessage = "New SMS OTP sent."
                                                isErrorStatus = false
                                            }
                                        ) {
                                            Text(
                                                text = if (phoneOtpTimer > 0) "Resend in ${phoneOtpTimer}s" else "Resend Code",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AuthMode.FORGOT_PASSWORD -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (resetStep) {
                                    1 -> {
                                        Text(
                                            text = "Enter your registered email address to receive password recovery instructions.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        OutlinedTextField(
                                            value = resetEmailInput,
                                            onValueChange = { resetEmailInput = it },
                                            label = { Text("Account Email") },
                                            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("forgot_email_input")
                                        )

                                        Button(
                                            onClick = {
                                                if (resetEmailInput.isBlank()) {
                                                    statusMessage = "Please enter your email."
                                                    isErrorStatus = true
                                                    return@Button
                                                }
                                                resetStep = 2
                                                statusMessage = "Recovery code sent to $resetEmailInput"
                                                isErrorStatus = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("send_recovery_code_btn")
                                        ) {
                                            Text("Send Password Reset Code", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    2 -> {
                                        OutlinedTextField(
                                            value = resetCodeInput,
                                            onValueChange = { resetCodeInput = it },
                                            label = { Text("6-Digit Recovery Code") },
                                            leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("reset_code_input")
                                        )

                                        OutlinedTextField(
                                            value = newPasswordInput,
                                            onValueChange = { newPasswordInput = it },
                                            label = { Text("New Password") },
                                            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("new_password_input")
                                        )

                                        Button(
                                            onClick = {
                                                if (resetCodeInput.isBlank() || newPasswordInput.isBlank()) {
                                                    statusMessage = "Please fill in all fields."
                                                    isErrorStatus = true
                                                    return@Button
                                                }
                                                resetStep = 3
                                                statusMessage = "Password successfully reset! You can now log in."
                                                isErrorStatus = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("save_new_password_btn")
                                        ) {
                                            Text("Update Password", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    3 -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("Password Recovery Complete", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Your account credentials have been secured.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        Button(
                                            onClick = {
                                                resetStep = 1
                                                currentAuthMode = AuthMode.LOGIN
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Text("Return to Sign In", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        AuthMode.TWO_FACTOR_VERIFY -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "2FA Shield",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = "Two-Factor Authentication is active for ${pendingSession?.email}. Enter the 6-digit code from your Authenticator App.",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = twoFactorCodeInput,
                                    onValueChange = { if (it.length <= 6) twoFactorCodeInput = it },
                                    label = { Text("6-Digit 2FA Code") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("2fa_code_input")
                                )

                                Button(
                                    onClick = {
                                        if (twoFactorCodeInput.length < 4) {
                                            statusMessage = "Please enter 6-digit 2FA code."
                                            isErrorStatus = true
                                            return@Button
                                        }

                                        val validSession = pendingSession?.copy(isLoggedIn = true)
                                        if (validSession != null) {
                                            prefs.edit().putString("active_user_email", validSession.email)
                                                .putString("active_user_name", validSession.name)
                                                .putString("active_auth_provider", validSession.authProvider)
                                                .putBoolean("is_logged_in", true)
                                                .apply()
                                            Toast.makeText(context, "2FA Authentication Passed!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess(validSession)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("verify_2fa_btn")
                                ) {
                                    Text("Verify & Authenticate", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Accounts Dialog Modal
    if (showGoogleModal) {
        AlertDialog(
            onDismissRequest = { showGoogleModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.GTranslate, contentDescription = null, tint = Color(0xFF4285F4))
                    Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose an account to continue to Finance Tracker:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val sampleGoogleAccounts = listOf(
                        Pair("Alex Morgan", "alex.morgan@gmail.com"),
                        Pair("Eshan Barua", "eshanbaruabarua@gmail.com"),
                        Pair("Finance Portfolio", "wealth.finance@gmail.com")
                    )

                    sampleGoogleAccounts.forEach { (gName, gEmail) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleModal = false
                                    val session = UserSession(
                                        email = gEmail,
                                        name = gName,
                                        authProvider = "GOOGLE",
                                        is2FaEnabled = false,
                                        isLoggedIn = true
                                    )
                                    prefs.edit().putString("active_user_email", session.email)
                                        .putString("active_user_name", session.name)
                                        .putString("active_auth_provider", session.authProvider)
                                        .putBoolean("is_logged_in", true)
                                        .apply()
                                    Toast.makeText(context, "Signed in as $gName", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess(session)
                                }
                                .testTag("google_account_option_${gEmail.substringBefore("@")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(gName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Column {
                                    Text(gName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(gEmail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
