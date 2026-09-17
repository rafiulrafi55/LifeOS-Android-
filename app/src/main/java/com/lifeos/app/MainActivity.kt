package com.lifeos.app

import android.os.Bundle
import android.content.Context
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person

private val Ink = Color(0xFF1C2824)
private val Moss = Color(0xFF2E6B58)
private val Coral = Color(0xFFE67B62)
private val Sand = Color(0xFFF8F6F0)
private val Teal = Color(0xFF4B9D99)
private val Orange = Color(0xFFF28A4B)
private val OrangeDeep = Color(0xFFE75B35)
private val OrangePale = Color(0xFFFFE8D8)
private const val GoogleWebClientId = "294255605490-p7nvp9s3j1d1obd93oqklprisfqh1qfa.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {
    private val repository: LifeOsRepository by lazy { FirebaseLifeOsRepository(applicationContext) }
    private val authRepository by lazy { AuthRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LifeOsApp(repository, authRepository) }
    }
}

private enum class LifeOsModule(val label: String, val icon: ImageVector) {
    Dashboard("Home", Icons.Filled.Home), Tasks("Tasks", Icons.Filled.Check), Calendar("Calendar", Icons.Filled.Event),
    Notes("Notes", Icons.Filled.Note), Expenses("Money", Icons.Filled.AttachMoney), Reminders("Reminders", Icons.Filled.Notifications), Profile("Profile", Icons.Filled.Person)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LifeOsApp(repository: LifeOsRepository, authRepository: AuthRepository) {
    var signedIn by remember { mutableStateOf(authRepository.isSignedIn) }
    DisposableEffect(authRepository) {
        val listener = authRepository.addAuthStateListener { signedIn = it }
        onDispose { authRepository.removeAuthStateListener(listener) }
    }

    if (!signedIn) {
        AuthScreen(authRepository)
        return
    }

    var selectedModule by remember { mutableStateOf(LifeOsModule.Dashboard) }
    val snapshot by repository.observeSnapshot().collectAsState(
        initial = LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )
    val context = LocalContext.current
    val profile = authRepository.currentUserId?.let { ProfileStore(context).read(it) } ?: LifeOsProfile()

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Moss, onPrimary = Color.White, background = Sand,
            surface = Color.White, onSurface = Ink, secondary = Coral
        )
    ) {
        Scaffold(
            containerColor = Sand,
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("LifeOS", fontWeight = FontWeight.Bold)
                        Text(if (selectedModule == LifeOsModule.Dashboard) currentDateLabel() else selectedModule.label, style = MaterialTheme.typography.labelMedium, color = Moss)
                    }
                })
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.padding(horizontal = 8.dp), containerColor = Color.White, tonalElevation = 0.dp) {
                    LifeOsModule.entries.forEach { module ->
                        NavigationBarItem(
                            selected = selectedModule == module,
                            onClick = { selectedModule = module },
                            icon = {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .shadow(if (selectedModule == module) 5.dp else 0.dp, CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .background(if (selectedModule == module) OrangePale else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) { Icon(module.icon, contentDescription = module.label, tint = if (selectedModule == module) OrangeDeep else Color(0xFF63706A), modifier = Modifier.size(19.dp)) }
                            },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                when (selectedModule) {
                    LifeOsModule.Dashboard -> DashboardScreen(snapshot, LocalContext.current, profile.firstName) { selectedModule = it }
                    LifeOsModule.Tasks -> TasksScreen(snapshot.tasks, repository)
                    LifeOsModule.Calendar -> CalendarScreen(snapshot.events, repository)
                    LifeOsModule.Notes -> NotesScreen(snapshot.notes, repository)
                    LifeOsModule.Expenses -> ExpensesScreen(snapshot.expenses, repository, LocalContext.current)
                    LifeOsModule.Reminders -> RemindersScreen(snapshot.reminders, repository)
                    LifeOsModule.Profile -> ProfileScreen(authRepository, repository, snapshot, LocalContext.current)
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(authRepository: AuthRepository) {
    val context = LocalContext.current
    var showSignUp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    var identifier by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val googleClient = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleWebClientId)
            .requestEmail()
            .build()
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val idToken = accountTask.result.idToken
                if (idToken == null) {
                    message = "Google did not return an ID token."
                } else {
                    busy = true
                    authRepository.signInWithGoogleIdToken(idToken) { authResult ->
                        busy = false
                        message = authResult.exceptionOrNull()?.message
                    }
                }
            } catch (error: Exception) {
                message = error.message ?: "Google sign-in was cancelled."
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFFFFF5EC), Color(0xFFFFD1B2), Color(0xFFF28A4B)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("LIFEOS", color = OrangeDeep, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(if (showSignUp) "Start your better rhythm." else "Welcome back.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (showSignUp) "One calm place for everything that matters." else "Your life, thoughtfully organized.", color = Color(0xFF63706A))
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (showSignUp) {
                        AuthField("First name", firstName, { firstName = it })
                        AuthField("Last name", lastName, { lastName = it })
                        AuthField("Username", username, { username = it })
                        AuthField("Email", email, { email = it })
                        AuthField("Password", password, { password = it }, password = true)
                        AuthField("Confirm password", confirmPassword, { confirmPassword = it }, password = true)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                message = validateSignUp(firstName, lastName, username, email, password, confirmPassword)
                                if (message == null) {
                                    acceptedTerms = false
                                    showTermsDialog = true
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)
                        ) { Text(if (busy) "Creating account..." else "Create account") }
                    } else {
                        AuthField("Email or username", identifier, { identifier = it })
                        AuthField("Password", signInPassword, { signInPassword = it }, password = true)
                        TextButton(onClick = { message = "Password reset will be added next." }, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?", color = OrangeDeep) }
                        Button(
                            onClick = {
                                message = if (identifier.isBlank() || signInPassword.isBlank()) "Enter your email or username and password." else null
                                if (message == null) {
                                    busy = true
                                    authRepository.signIn(identifier, signInPassword) { result ->
                                        busy = false
                                        message = result.exceptionOrNull()?.message
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)
                        ) { Text(if (busy) "Signing in..." else "Sign in") }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                message = null
                                googleLauncher.launch(GoogleSignIn.getClient(context, googleClient).signInIntent)
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6DED7))
                        ) {
                            GoogleBadge()
                            Spacer(Modifier.width(10.dp))
                            Text("Continue with Google", color = Ink)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    AuthModePill(showSignUp) {
                        showSignUp = !showSignUp
                        message = null
                    }

                    if (message != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(message.orEmpty(), color = Coral, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showTermsDialog = false },
            title = { Text("Terms and conditions", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "LifeOS helps you organize tasks, events, notes, expenses, reminders, and profile information. You are responsible for the information you add. By continuing, you agree to use LifeOS responsibly and allow your account data to be stored on this device and synced to Firebase when you choose manual sync.",
                        color = Color(0xFF63706A),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Justify
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                        Text("I agree to the terms and conditions", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        busy = true
                        authRepository.signUp(firstName, lastName, username, email, password) { result ->
                            busy = false
                            showTermsDialog = false
                            message = result.exceptionOrNull()?.message
                        }
                    },
                    enabled = acceptedTerms && !busy,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)
                ) { Text(if (busy) "Creating..." else "Agree and create") }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }, enabled = !busy) { Text("Cancel", color = OrangeDeep) }
            }
        )
    }
}

@Composable
private fun AuthModePill(showSignUp: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)
    ) {
        Text(
            if (showSignUp) "Already have an account?  Sign in" else "New to LifeOS?  Sign up",
            color = OrangeDeep,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoogleBadge() {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFFE6DED7), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
private fun AuthField(label: String, value: String, onValueChange: (String) -> Unit, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    )
}

private fun validateSignUp(
    firstName: String,
    lastName: String,
    username: String,
    email: String,
    password: String,
    confirmPassword: String
): String? {
    if (listOf(firstName, lastName, username, email, password, confirmPassword).any { it.isBlank() }) return "Complete every field."
    if (!email.contains("@")) return "Enter a valid email address."
    if (username.length < 3) return "Username must be at least 3 characters."
    if (password.length < 6) return "Password must be at least 6 characters."
    if (password != confirmPassword) return "Passwords do not match."
    return null
}

private fun currentDateLabel(): String =
    SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())

private fun currentDateStrip(): List<String> {
    val formatter = SimpleDateFormat("EE d", Locale.US)
    val today = Calendar.getInstance()
    return (-2..2).map { offset ->
        Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, offset)
        }.timeInMillis.let { formatter.format(Date(it)) }
    }
}

@Composable
private fun DashboardScreen(snapshot: LifeOsSnapshot, context: Context, displayName: String, onModuleSelected: (LifeOsModule) -> Unit) {
    val currency = remember(context) { CurrencyStore(context).read() }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFFF5EC), Color(0xFFFFE5D2), Sand)))) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Good morning${if (displayName.isBlank()) "" else ", $displayName"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text("A clear day starts with a clear view.", color = Color(0xFF63706A))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(OrangeDeep, Orange)), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("TODAY'S FOCUS", color = OrangePale, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Make space for what matters.", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${snapshot.tasks.count { !it.completed }} tasks", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("•", color = OrangePale)
                            Text("${snapshot.events.size} events", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { SectionHeader("Up next", "View calendar") { onModuleSelected(LifeOsModule.Calendar) } }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { snapshot.events.take(2).forEach { EventRow(it) } }
                }
            }
            item { SectionHeader("Your rhythm", "All tasks") { onModuleSelected(LifeOsModule.Tasks) } }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { snapshot.tasks.take(3).forEach { task -> TaskRow(task) {} } }
                }
            }
            item {
                SectionHeader("Money this month", "View expenses") { onModuleSelected(LifeOsModule.Expenses) }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Total spending", color = Color(0xFF63706A)); Text(formatLifeOsAmount(totalLifeOsExpenses(snapshot.expenses).toString(), currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink) }
                        Text("${snapshot.expenses.size} entries", color = OrangeDeep, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TasksScreen(tasks: List<LifeOsTask>, repository: LifeOsRepository) {
    var showAddTask by remember { mutableStateOf(false) }
    ModuleColumn("Task management", "Keep the important things moving.") {
        Button(onClick = { showAddTask = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("+  Add a task") }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { tasks.forEach { task -> TaskRow(task) { repository.toggleTask(task.id) } } }
        }
    }
    if (showAddTask) {
        TaskDialog(
            onDismiss = { showAddTask = false },
            onSave = { title, dueLabel ->
                repository.updateSnapshot { it.copy(tasks = it.tasks + LifeOsTask("task-${System.currentTimeMillis()}", title, dueLabel)) }
                showAddTask = false
            }
        )
    }
}

@Composable
private fun CalendarScreen(events: List<LifeOsEvent>, repository: LifeOsRepository) {
    var showAddEvent by remember { mutableStateOf(false) }
    ModuleColumn("Your calendar", currentDateLabel()) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                currentDateStrip().forEachIndexed { index, day ->
                    Box(Modifier.size(48.dp).clip(CircleShape).background(if (index == 2) OrangeDeep else Color.Transparent), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(day.take(1), color = if (index == 2) OrangePale else Color(0xFF63706A), fontSize = 12.sp); Text(day.drop(2), fontWeight = if (index == 2) FontWeight.Bold else FontWeight.Normal, color = if (index == 2) Color.White else Ink) }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { showAddEvent = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("+  Add an event") }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { events.forEach { EventRow(it) } }
        }
    }
    if (showAddEvent) {
        EventDialog(
            onDismiss = { showAddEvent = false },
            onSave = { title, timeLabel, colorKey ->
                repository.updateSnapshot { it.copy(events = it.events + LifeOsEvent("event-${System.currentTimeMillis()}", title, timeLabel, colorKey)) }
                showAddEvent = false
            }
        )
    }
}

@Composable
private fun NotesScreen(notes: List<LifeOsNote>, repository: LifeOsRepository) {
    var editingNote by remember { mutableStateOf(false) }
    if (editingNote) {
        NoteEditorScreen(
            onBack = { editingNote = false },
            onSave = { title, body ->
                repository.updateSnapshot { it.copy(notes = it.notes + LifeOsNote("note-${System.currentTimeMillis()}", title, body, "Edited just now")) }
                editingNote = false
            }
        )
        return
    }
    ModuleColumn("Notes", "Capture thoughts before they drift away.") {
        Button(onClick = { editingNote = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("+  New note") }
        Spacer(Modifier.height(16.dp))
        notes.forEach { note ->
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp)) { Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); Text(note.preview, color = Color(0xFF63706A)); Spacer(Modifier.height(12.dp)); Text(note.updatedLabel, style = MaterialTheme.typography.labelMedium, color = Moss) }
            }
        }
    }
}

@Composable
private fun NoteEditorScreen(onBack: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Sand)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Ink) }
                Text("New note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                TextButton(onClick = { if (title.isNotBlank() && body.isNotBlank()) onSave(title, body) }) { Text("Save", color = OrangeDeep, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink, lineHeight = 26.sp),
                    decorationBox = { innerTextField ->
                        if (body.isBlank()) Text("Start writing...", color = Color(0xFF9A928C), style = MaterialTheme.typography.bodyLarge)
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpensesScreen(expenses: List<LifeOsExpense>, repository: LifeOsRepository, context: Context) {
    var currency by remember(context) { mutableStateOf(CurrencyStore(context).read()) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    ModuleColumn("Money", "A calmer view of your spending.") {
        Card(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(OrangeDeep, Orange)), RoundedCornerShape(20.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("SEPTEMBER SPENDING", color = OrangePale, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp); Spacer(Modifier.height(7.dp)); Text(formatLifeOsAmount(totalLifeOsExpenses(expenses).toString(), currency), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("${expenses.size} recorded entries", color = Color.White.copy(alpha = 0.78f)) }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { showCurrencyDialog = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)) { Text("Currency: ${currency.code} (${currency.symbol})", color = OrangeDeep) }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { showAddExpense = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("+  Add expense") }
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                expenses.forEach { expense ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(expense.merchant, fontWeight = FontWeight.SemiBold); Text(expense.category, color = Color(0xFF63706A), style = MaterialTheme.typography.labelMedium) }; Text(formatLifeOsAmount(expense.amount, currency), fontWeight = FontWeight.Bold, color = OrangeDeep) }
                    HorizontalDivider(color = Color(0xFFE3E8E3))
                }
            }
        }
    }
    if (showAddExpense) {
        LifeOsEntryDialog("Add expense", listOf("Merchant", "Category", "Amount"), { showAddExpense = false }) { values ->
            repository.updateSnapshot { it.copy(expenses = it.expenses + LifeOsExpense("expense-${System.currentTimeMillis()}", values[0], values[1], values[2])) }
            showAddExpense = false
        }
    }
    if (showCurrencyDialog) {
        AlertDialog(onDismissRequest = { showCurrencyDialog = false }, title = { Text("Choose currency", fontWeight = FontWeight.Bold) }, text = {
            Column { LifeOsCurrencies.forEach { option ->
                TextButton(onClick = { currency = option; CurrencyStore(context).save(option); showCurrencyDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("${option.symbol}  ${option.code} - ${option.label}", color = if (option.code == currency.code) OrangeDeep else Ink) }
            } }
        }, confirmButton = {})
    }
}

@Composable
private fun RemindersScreen(reminders: List<LifeOsReminder>, repository: LifeOsRepository) {
    var showAddReminder by remember { mutableStateOf(false) }
    ModuleColumn("Reminders", "Small prompts for the things you care about.") {
        Button(onClick = { showAddReminder = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("+  Add a reminder") }
        Spacer(Modifier.height(16.dp))
        reminders.forEach { reminder ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Coral))
                    Spacer(Modifier.width(14.dp))
                    Column { Text(reminder.title, fontWeight = FontWeight.SemiBold); Text(reminder.scheduleLabel, color = Color(0xFF63706A), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
    if (showAddReminder) {
        ReminderDialog(
            onDismiss = { showAddReminder = false },
            onSave = { title, scheduleLabel ->
                repository.updateSnapshot { it.copy(reminders = it.reminders + LifeOsReminder("reminder-${System.currentTimeMillis()}", title, scheduleLabel)) }
                showAddReminder = false
            }
        )
    }
}

@Composable
private fun ProfileScreen(authRepository: AuthRepository? = null, repository: LifeOsRepository? = null, snapshot: LifeOsSnapshot = LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), context: Context? = null) {
    val profileStore = remember(context) { context?.let(::ProfileStore) }
    val userId = authRepository?.currentUserId
    var profile by remember(userId) { mutableStateOf(if (userId != null) profileStore?.read(userId) ?: LifeOsProfile() else LifeOsProfile()) }
    val activeDays = remember(userId) { userId?.let { profileStore?.recordActiveDay(it) } ?: 0 }
    val completedTasks = snapshot.tasks.count { it.completed }
    val taskRhythm = if (snapshot.tasks.isEmpty()) 0 else completedTasks * 100 / snapshot.tasks.size
    val goalsSet = if (profile.goal.isBlank()) 0 else 1
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var showConnectedServices by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var showGoalEdit by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    ModuleColumn("Profile", "Shape LifeOS around your life.") {
        Card(
            modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(OrangeDeep, Orange)), RoundedCornerShape(26.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Text(profile.firstName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${profile.firstName} ${profile.lastName}".trim().ifBlank { "Your profile" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (profile.username.isBlank()) "Set up your username" else "@${profile.username}", color = OrangePale)
                        Text(profile.email, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Making room for a more intentional life.", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat(activeDays.toString(), "DAYS ACTIVE", Modifier.weight(1f))
            ProfileStat("$taskRhythm%", "TASK RHYTHM", Modifier.weight(1f))
            ProfileStat(goalsSet.toString(), "GOALS SET", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("Personal space", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        ProfileOption("Personal details", "Name, email and account", "A") { showProfileEdit = true }
        ProfileOption("Goals and preferences", profile.goal.ifBlank { "Add your primary goal" }, "G") { showGoalEdit = true }
        ProfileOption("Notifications", if (profile.notificationsEnabled) "Reminders are enabled" else "Reminders are muted", "N") { showNotifications = true }
        ProfileOption("Connected services", "Google and other tools", "C") { showConnectedServices = !showConnectedServices }
        if (showConnectedServices) {
            FirebaseSyncPanel(
                syncing = syncing,
                restoring = restoring,
                message = syncMessage,
                onSync = {
                    syncing = true
                    syncMessage = null
                    repository?.syncToFirebase { result ->
                        syncing = false
                        syncMessage = result.fold({ "Everything is synced with Firebase." }, { it.message ?: "Sync failed. Try again." })
                    }
                },
                onRestore = {
                    restoring = true
                    syncMessage = null
                    var moduleResult: Result<Unit>? = null
                    var profileResult: Result<LifeOsProfile>? = null
                    fun finishRestoreIfReady() {
                        if (moduleResult == null || profileResult == null) return
                        profileResult?.getOrNull()?.let { profile = it }
                        restoring = false
                        syncMessage = if (moduleResult!!.isSuccess && profileResult!!.isSuccess) {
                            "Firebase data and profile restored to this device."
                        } else if (moduleResult!!.isSuccess) {
                            "LifeOS data restored. Profile restore failed."
                        } else if (profileResult!!.isSuccess) {
                            "Profile restored. No full LifeOS backup was found yet."
                        } else {
                            moduleResult!!.exceptionOrNull()?.message ?: profileResult!!.exceptionOrNull()?.message ?: "Restore failed. Try again."
                        }
                    }
                    repository?.restoreFromFirebase { result ->
                        moduleResult = result
                        finishRestoreIfReady()
                    } ?: run {
                        moduleResult = Result.failure(IllegalStateException("Local data repository unavailable."))
                    }
                    authRepository?.restoreProfile { result ->
                        profileResult = result
                        finishRestoreIfReady()
                    } ?: run {
                        profileResult = Result.failure(IllegalStateException("Profile repository unavailable."))
                        finishRestoreIfReady()
                    }
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { authRepository?.signOut() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)) { Text("Sign out", color = OrangeDeep, fontWeight = FontWeight.SemiBold) }
    }
    if (showProfileEdit) {
        ProfileEditDialog(profile, { showProfileEdit = false }) { updated -> profile = updated; userId?.let { profileStore?.save(it, updated) }; showProfileEdit = false }
    }
    if (showGoalEdit) {
        GoalEditDialog(profile.goal, { showGoalEdit = false }) { goal -> profile = profile.copy(goal = goal); userId?.let { profileStore?.save(it, profile.copy(goal = goal)) }; showGoalEdit = false }
    }
    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(profile.notificationsEnabled, { enabled -> profile = profile.copy(notificationsEnabled = enabled); userId?.let { profileStore?.save(it, profile.copy(notificationsEnabled = enabled)) } }); Text("Allow LifeOS reminders and updates", fontWeight = FontWeight.SemiBold) } },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Done", color = OrangeDeep) } }
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(92.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(Orange))
            Spacer(Modifier.height(12.dp))
            Text(value, color = OrangeDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color(0xFF63706A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ProfileOption(title: String, subtitle: String, mark: String, onClick: () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(OrangePale), contentAlignment = Alignment.Center) { Text(mark, color = OrangeDeep, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink); Text(subtitle, color = Color(0xFF63706A), style = MaterialTheme.typography.labelMedium) }
            Text(">", color = OrangeDeep, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileEditDialog(profile: LifeOsProfile, onDismiss: () -> Unit, onSave: (LifeOsProfile) -> Unit) {
    var firstName by remember { mutableStateOf(profile.firstName) }
    var lastName by remember { mutableStateOf(profile.lastName) }
    var username by remember { mutableStateOf(profile.username) }
    var email by remember { mutableStateOf(profile.email) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personal details", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AuthField("First name", firstName, { firstName = it })
                AuthField("Last name", lastName, { lastName = it })
                AuthField("Username", username, { username = it })
                AuthField("Email", email, { email = it })
            }
        },
        confirmButton = { Button(onClick = { onSave(profile.copy(firstName = firstName, lastName = lastName, username = username, email = email)) }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@Composable
private fun GoalEditDialog(currentGoal: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var goal by remember { mutableStateOf(currentGoal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Goals and preferences", fontWeight = FontWeight.Bold) },
        text = { AuthField("Primary goal", goal, { goal = it }) },
        confirmButton = { Button(onClick = { onSave(goal) }, enabled = goal.isNotBlank(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@Composable
private fun FirebaseSyncPanel(syncing: Boolean, restoring: Boolean, message: String?, onSync: () -> Unit, onRestore: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(OrangePale), contentAlignment = Alignment.Center) { Text("F", color = OrangeDeep, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Firebase sync", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Back up this device's LifeOS data", color = Color(0xFF63706A), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSync, enabled = !syncing, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) {
                Text(if (syncing) "Syncing..." else "Sync now")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRestore, enabled = !syncing && !restoring, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)) {
                Text(if (restoring) "Restoring..." else "Restore from Firebase", color = OrangeDeep)
            }
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = if (message.startsWith("Everything")) Moss else Coral, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TaskDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var dueLabel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a task", fontWeight = FontWeight.Bold) },
        text = { Column { AuthField("Task title", title, { title = it }); DateTimePickerField("Due date and time", dueLabel) { dueLabel = it } } },
        confirmButton = { Button(onClick = { onSave(title, dueLabel) }, enabled = title.isNotBlank() && dueLabel.isNotBlank(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@Composable
private fun EventDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var timeLabel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an event", fontWeight = FontWeight.Bold) },
        text = { Column { AuthField("Event title", title, { title = it }); DateTimePickerField("Event date and time", timeLabel) { timeLabel = it } } },
        confirmButton = { Button(onClick = { onSave(title, timeLabel, "coral") }, enabled = title.isNotBlank() && timeLabel.isNotBlank(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@Composable
private fun ReminderDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var scheduleLabel by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a reminder", fontWeight = FontWeight.Bold) },
        text = { Column { AuthField("Reminder", title, { title = it }); DateTimePickerField("Reminder date and time", scheduleLabel) { scheduleLabel = it } } },
        confirmButton = { Button(onClick = { onSave(title, scheduleLabel) }, enabled = title.isNotBlank() && scheduleLabel.isNotBlank(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(label: String, value: String, onValueChange: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OrangePale)) {
        Text(if (value.isBlank()) label else value, color = if (value.isBlank()) Color(0xFF63706A) else OrangeDeep)
    }
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDateMillis = state.selectedDateMillis ?: System.currentTimeMillis(); showDatePicker = false; showTimePicker = true }) { Text("Next", color = OrangeDeep) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = OrangeDeep) } }
        ) { DatePicker(state = state) }
    }
    if (showTimePicker) {
        val state = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Choose time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Date(stateToMillis(state, selectedDateMillis))
                    val formatter = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US)
                    onValueChange(formatter.format(date))
                    showTimePicker = false
                }) { Text("Done", color = OrangeDeep) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = OrangeDeep) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun stateToMillis(state: androidx.compose.material3.TimePickerState, selectedDateMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDateMillis
    calendar.set(Calendar.HOUR_OF_DAY, state.hour)
    calendar.set(Calendar.MINUTE, state.minute)
    return calendar.timeInMillis
}

@Composable
private fun LifeOsEntryDialog(title: String, fields: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var values by remember(fields) { mutableStateOf(fields.map { "" }) }
    val complete = values.all { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                fields.forEachIndexed { index, label ->
                    AuthField(label, values[index], { value -> values = values.toMutableList().also { it[index] = value } })
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(values) }, enabled = complete, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OrangeDeep) } }
    )
}

@Composable
private fun ModuleColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFFF5EC), Color(0xFFFFE5D2), Sand)))) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item { Spacer(Modifier.height(8.dp)); Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, color = Color(0xFF63706A)); Spacer(Modifier.height(22.dp)); Column(content = content); Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink); TextButton(onClick = onAction) { Text(action, color = OrangeDeep) } }
}

@Composable
private fun TaskRow(task: LifeOsTask, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = task.completed, onCheckedChange = { onToggle() }); Column(Modifier.padding(start = 8.dp)) { Text(task.title, fontWeight = FontWeight.SemiBold, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None); Text(task.dueLabel, style = MaterialTheme.typography.labelMedium, color = Color(0xFF63706A)) } }
}

@Composable
private fun EventRow(event: LifeOsEvent) {
    val color = if (event.colorKey == "coral") Coral else Teal
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(12.dp)); Column { Text(event.title, fontWeight = FontWeight.SemiBold); Text(event.timeLabel, style = MaterialTheme.typography.labelMedium, color = Color(0xFF63706A)) } }
}

private val PreviewSnapshot = LifeOsSnapshot(
    tasks = listOf(
        LifeOsTask("task-1", "Plan the week", "Today, 9:00 AM"),
        LifeOsTask("task-2", "Review product notes", "Today, 2:30 PM"),
        LifeOsTask("task-3", "Evening walk", "Today, 6:00 PM")
    ),
    events = listOf(
        LifeOsEvent("event-1", "Design sync", "10:30 AM", "coral"),
        LifeOsEvent("event-2", "Dentist appointment", "3:00 PM", "teal")
    ),
    notes = listOf(
        LifeOsNote("note-1", "Ideas for a calmer morning", "Build a gentle start that leaves room for focus...", "Edited 12 min ago"),
        LifeOsNote("note-2", "Books to read", "Deep Work, The Creative Act, Atomic Habits", "Edited yesterday")
    ),
    expenses = listOf(
        LifeOsExpense("expense-1", "Whole Foods Market", "Groceries", "$48.20"),
        LifeOsExpense("expense-2", "Spotify", "Subscriptions", "$11.99")
    ),
    reminders = listOf(
        LifeOsReminder("reminder-1", "Take a screen break", "In 25 minutes"),
        LifeOsReminder("reminder-2", "Call Mom", "Tomorrow, 7:00 PM")
    )
)

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Moss, onPrimary = Color.White, background = Sand,
            surface = Color.White, onSurface = Ink, secondary = Coral
        ),
        content = content
    )
}

@Composable
private fun AuthPreviewContent(showSignUp: Boolean) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFFF5EC), Color(0xFFFFD1B2), Color(0xFFF28A4B)))) ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("LIFEOS", color = OrangeDeep, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(if (showSignUp) "Start your better rhythm." else "Welcome back.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (showSignUp) "One calm place for everything that matters." else "Your life, thoughtfully organized.", color = Color(0xFF63706A))
            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
                Column(Modifier.padding(20.dp)) {
                    if (showSignUp) {
                        AuthField("First name", "Alex", {})
                        AuthField("Last name", "Morgan", {})
                        AuthField("Username", "alexmorgan", {})
                        AuthField("Email", "alex@example.com", {})
                        AuthField("Password", "password", {}, password = true)
                        AuthField("Confirm password", "password", {}, password = true)
                        Button(onClick = {}, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Create account") }
                    } else {
                        AuthField("Email or username", "alex@example.com", {})
                        AuthField("Password", "password", {}, password = true)
                        TextButton(onClick = {}, Modifier.align(Alignment.End)) { Text("Forgot password?", color = OrangeDeep) }
                        Button(onClick = {}, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Sign in") }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {}, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { GoogleBadge(); Spacer(Modifier.width(10.dp)); Text("Continue with Google", color = Ink) }
                    }
                    Spacer(Modifier.height(18.dp))
                    AuthModePill(showSignUp) {}
                }
            }
        }
    }
}

@Preview(name = "Sign-up terms", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpTermsPreview() {
    PreviewTheme {
        Surface(color = Sand) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sign-up form", color = Color(0xFF63706A))
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Terms and conditions", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                "LifeOS helps you organize tasks, events, notes, expenses, reminders, and profile information. You are responsible for the information you add. By continuing, you agree to use LifeOS responsibly and allow your account data to be stored on this device and synced to Firebase when you choose manual sync.",
                                color = Color(0xFF63706A),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Justify
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = {})
                                Text("I agree to the terms and conditions", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {}, enabled = false, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OrangeDeep)) { Text("Agree and create") }
                    },
                    dismissButton = { TextButton(onClick = {}) { Text("Cancel", color = OrangeDeep) } }
                )
            }
        }
    }
}

@Preview(name = "Sign in", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignInPreview() { PreviewTheme { AuthPreviewContent(showSignUp = false) } }

@Preview(name = "Sign up", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpPreview() { PreviewTheme { AuthPreviewContent(showSignUp = true) } }

@Preview(name = "Dashboard", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DashboardPreview() { PreviewTheme { DashboardScreen(PreviewSnapshot.copy(expenses = emptyList()), LocalContext.current, "Alex") {} } }

@Preview(name = "Tasks", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TasksPreview() { PreviewTheme { TasksScreen(PreviewSnapshot.tasks, InMemoryLifeOsRepository()) } }

@Preview(name = "Calendar", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalendarPreview() { PreviewTheme { CalendarScreen(PreviewSnapshot.events, InMemoryLifeOsRepository()) } }

@Preview(name = "Notes", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotesPreview() { PreviewTheme { NotesScreen(PreviewSnapshot.notes, InMemoryLifeOsRepository()) } }

@Preview(name = "Expenses", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ExpensesPreview() { PreviewTheme { ExpensesScreen(emptyList(), InMemoryLifeOsRepository(), LocalContext.current) } }

@Preview(name = "Reminders", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RemindersPreview() { PreviewTheme { RemindersScreen(PreviewSnapshot.reminders, InMemoryLifeOsRepository()) } }

@Preview(name = "Profile", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfilePreview() { PreviewTheme { ProfileScreen() } }
