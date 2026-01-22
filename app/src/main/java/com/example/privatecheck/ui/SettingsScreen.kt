package com.example.privatecheck.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privatecheck.data.DataStoreRepository
import com.example.privatecheck.logic.EmailService

import kotlinx.coroutines.launch
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.privatecheck.widget.CheckInWidget
import androidx.compose.ui.graphics.toArgb
import com.example.privatecheck.ui.components.ColorPickerDialog

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SettingsScreen(
    repository: DataStoreRepository,
    backgroundColor: androidx.compose.ui.graphics.Color, // Accept animated color
    onBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Email Validation Helper
    val emailPattern = remember { 
        android.util.Patterns.EMAIL_ADDRESS 
    }
    fun isValidEmail(email: String): Boolean {
        return email.isBlank() || emailPattern.matcher(email.trim()).matches()
    }
    
    // States - Use "null" as initial to indicate loading
    val savedContact1 by repository.contactEmail.collectAsState(initial = null)
    val savedContact2 by repository.contactEmail2.collectAsState(initial = null)
    val savedContact3 by repository.contactEmail3.collectAsState(initial = null)
    val savedSender by repository.senderEmail.collectAsState(initial = null)
    val savedPassword by repository.senderPassword.collectAsState(initial = null)
    val savedSmtp by repository.smtpHost.collectAsState(initial = null)

    // Block rendering until data is ready
    if (savedContact1 == null || savedContact2 == null || savedContact3 == null || 
        savedSender == null || savedPassword == null || savedSmtp == null) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor))
        return
    }

    // Now safe to bang-bang !! 
    var contactEmail1 by remember { mutableStateOf(savedContact1!!) }
    var contactEmail2 by remember { mutableStateOf(savedContact2!!) }
    var contactEmail3 by remember { mutableStateOf(savedContact3!!) }
    var senderEmail by remember { mutableStateOf(savedSender!!) }
    var senderPassword by remember { mutableStateOf(savedPassword!!) }
    
    // SMTP Logic: Auto-detected
    var smtpHost by remember { mutableStateOf(savedSmtp ?: "") }

    var isTesting by remember { mutableStateOf(false) }
    
    // Color Picker States
    var showAppThemeColorPicker by remember { mutableStateOf(false) }
    var showWidgetColorPicker by remember { mutableStateOf(false) }

    val forceWhiteTextStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface, // Pure White
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
    )

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置", 
                        color = MaterialTheme.colorScheme.onSurface 
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("紧急联系人", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("(至少填一个)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            // Validation Logic
            val emails = listOf(contactEmail1, contactEmail2, contactEmail3).map { it.trim() }
            val hasDuplicate = emails.filter { it.isNotEmpty() }.let { it.size != it.distinct().size }
            
            val isEmail1Valid = isValidEmail(contactEmail1)
            val isEmail2Valid = isValidEmail(contactEmail2)
            val isEmail3Valid = isValidEmail(contactEmail3)
            val isSenderValid = isValidEmail(senderEmail)

            OutlinedTextField(
                value = contactEmail1,
                onValueChange = { contactEmail1 = it },
                label = { Text("接收人邮箱 1", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                textStyle = forceWhiteTextStyle,
                isError = !isEmail1Valid || (contactEmail1.isNotBlank() && (contactEmail1 == contactEmail2 || contactEmail1 == contactEmail3))
            )

            OutlinedTextField(
                value = contactEmail2,
                onValueChange = { contactEmail2 = it },
                label = { Text("接收人邮箱 2", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                textStyle = forceWhiteTextStyle,
                isError = !isEmail2Valid || (contactEmail2.isNotBlank() && (contactEmail2 == contactEmail1 || contactEmail2 == contactEmail3))
            )

            OutlinedTextField(
                value = contactEmail3,
                onValueChange = { contactEmail3 = it },
                label = { Text("接收人邮箱 3", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                textStyle = forceWhiteTextStyle,
                isError = !isEmail3Valid || (contactEmail3.isNotBlank() && (contactEmail3 == contactEmail1 || contactEmail3 == contactEmail2))
            )

            if (!isEmail1Valid || !isEmail2Valid || !isEmail3Valid) {
                Text(
                    "提示：请输入正确的邮箱格式",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else if (hasDuplicate) {
                Text(
                    "提示：已填写重复的邮箱地址",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            HorizontalDivider()

            Text("发件人设置", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            
            // Removed helper text as requested

            // Auto-detect SMTP Host based on email
            LaunchedEffect(senderEmail) {
                val domain = senderEmail.substringAfter("@", "")
                smtpHost = when {
                    domain == "qq.com" || domain == "foxmail.com" -> "smtp.qq.com"
                    domain == "gmail.com" -> "smtp.gmail.com"
                    domain == "163.com" -> "smtp.163.com"
                    domain == "126.com" -> "smtp.126.com"
                    domain == "outlook.com" || domain == "hotmail.com" || domain == "live.com" -> "smtp.office365.com"
                    domain == "sina.com" -> "smtp.sina.com"
                    domain == "sohu.com" -> "smtp.sohu.com"
                    domain == "aliyun.com" -> "smtp.aliyun.com"
                    domain == "yahoo.com" -> "smtp.mail.yahoo.com"
                    domain.isNotEmpty() -> "smtp.$domain"
                    else -> ""
                }
            }

            OutlinedTextField(
                value = senderEmail,
                onValueChange = { senderEmail = it },
                label = { Text("发件人邮箱", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                textStyle = forceWhiteTextStyle,
                isError = !isSenderValid
            )
            if (!isSenderValid && senderEmail.isNotBlank()) {
                Text(
                    "提示：请输入正确的邮箱格式",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = senderPassword,
                onValueChange = { senderPassword = it },
                label = { Text("邮箱授权码 (非登录密码)", color = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = textFieldColors,
                textStyle = forceWhiteTextStyle,
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "隐藏密码" else "显示密码", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
            Text(
                text = "提示：QQ、网易、Gmail 等邮箱需在设置中开启 SMTP 服务并获取授权码。",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val emailsToSave = listOf(contactEmail1, contactEmail2, contactEmail3).map { it.trim() }
                    val filledEmails = emailsToSave.filter { it.isNotBlank() }
                    
                    if (filledEmails.isEmpty()) {
                        Toast.makeText(context, "请至少填写一个接收人邮箱", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isEmail1Valid || !isEmail2Valid || !isEmail3Valid || !isSenderValid) {
                        Toast.makeText(context, "请检查邮箱格式是否正确", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (filledEmails.size != filledEmails.distinct().size) {
                        Toast.makeText(context, "存在重复的邮箱地址，请修改", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        // Save first
                        repository.saveSettings(
                            contactEmail1.trim(), 
                            contactEmail2.trim(), 
                            contactEmail3.trim(), 
                            senderEmail, 
                            senderPassword, 
                            smtpHost
                        )
                        Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Text("保存设置", color = androidx.compose.ui.graphics.Color.White)
            }

            OutlinedButton(
                onClick = {
                    val filledEmails = listOf(contactEmail1, contactEmail2, contactEmail3)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (filledEmails.isEmpty() || senderEmail.isBlank() || senderPassword.isBlank()) {
                        Toast.makeText(context, "请先填写完整信息并保存", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    if (filledEmails.size != filledEmails.distinct().size) {
                        Toast.makeText(context, "存在重复的邮箱地址，请修改", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }

                    isTesting = true
                    scope.launch {
                        var allSuccess = true
                        filledEmails.forEach { email ->
                            val success = EmailService.sendEmail(
                                toEmail = email,
                                subject = "私了吗 - 测试邮件",
                                body = "这是来自 App 的测试邮件，配置成功！",
                                senderEmail = senderEmail,
                                senderPassword = senderPassword,
                                smtpHost = smtpHost
                            )
                            if (!success) allSuccess = false
                        }
                        isTesting = false
                        if (allSuccess) {
                            Toast.makeText(context, "发送成功，请查收", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "部分或全部发送失败，请检查配置", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发送中...", color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Text("发送测试邮件", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            

            
            Row {
                Text("应用主题色", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.alignByBaseline())
                val presets = listOf(0xFF8FBC8F, 0xFF779ECB, 0xFFFFB7B2, 0xFFB39EB5, 0xFFFFDAC1)
                val checkColor by repository.appThemeColor.collectAsState(initial = -7357297)
                // Check if custom is selected (not in presets)
                val isPreset = presets.any { it.toInt() == checkColor }
                if (!isPreset) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("再次点击切换自定义颜色", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.alignByBaseline())
                }
            }
            
            val appThemeColor by repository.appThemeColor.collectAsState(initial = -7357297)

            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Presets
                val presets = listOf(
                    0xFF8FBC8F, // Morandi Green
                    0xFF779ECB, // Pastel Blue
                    0xFFFFB7B2, // Pastel Red
                    0xFFB39EB5, // Pastel Purple
                    0xFFFFDAC1  // Pastel Orange
                )
                
                // Helper to check preset
                val isPresetSelected = presets.any { it.toInt() == appThemeColor }
                
                // Track last custom color (Initialize with current if it's custom, else default)
                var lastCustomAppThemeColor by remember { mutableIntStateOf(0xFFE2F0CB.toInt()) }
                
                // Update last custom if current is custom
                if (!isPresetSelected) {
                    lastCustomAppThemeColor = appThemeColor
                }
                
                // 1. Render Presets
                presets.forEach { colorLong ->
                    val colorInt = colorLong.toInt()
                    val isSelected = appThemeColor == colorInt
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(androidx.compose.ui.graphics.Color(colorLong), androidx.compose.foundation.shape.CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    repository.saveAppThemeColor(colorInt)
                                }
                            }
                    )
                }
                
                // 2. Render Custom Slot (6th)
                val customSlotColorValue = if (!isPresetSelected) appThemeColor else lastCustomAppThemeColor
                val customSlotColor = androidx.compose.ui.graphics.Color(customSlotColorValue)
                
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(customSlotColor, androidx.compose.foundation.shape.CircleShape)
                        .border(
                            width = if (!isPresetSelected) 3.dp else 1.dp, // Highlight if Custom is active
                            color = if (!isPresetSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .clickable {
                            if (isPresetSelected) {
                                scope.launch { repository.saveAppThemeColor(lastCustomAppThemeColor) }
                            } else {
                                showAppThemeColorPicker = true
                            }
                        }
                ) {
                    // Always show + (Bigger and Bolder)
                    Text(
                        "+", 
                        modifier = Modifier.align(Alignment.Center), 
                        color = if (!isPresetSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                        )
                    )
                }
            }
            
            if (showAppThemeColorPicker) {
                ColorPickerDialog(
                    initialColor = appThemeColor,
                    onDismiss = { showAppThemeColorPicker = false },
                    onConfirm = { newColor ->
                        scope.launch {
                            repository.saveAppThemeColor(newColor)
                        }
                        showAppThemeColorPicker = false
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(8.dp))

            Text("小组件", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            // Local States for Widget Settings
            val widgetBgType by repository.widgetBgType.collectAsState(initial = "color")
            val widgetBgColor by repository.widgetBgColor.collectAsState(initial = -7357297) // Default Green
            val widgetBgImageUri by repository.widgetBgImageUri.collectAsState(initial = null)
            val widgetSourcePath by repository.widgetSourcePath.collectAsState(initial = null)

            // Image Picker Launcher -> Saves Original Copy & Navigates to Preview (Unified Editor)
            // Image Picker Launcher -> Saves Original Copy & Navigates to Preview
            val pickImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                uri?.let { sourceUri ->
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(sourceUri)
                            val destFile = java.io.File(context.filesDir, "original_source_image.jpg")
                            val outputStream = java.io.FileOutputStream(destFile)
                            inputStream?.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            // Save Path and Launch
                            repository.saveWidgetSourcePath(destFile.absolutePath)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onNavigateToPreview(destFile.absolutePath)
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "保存图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // Permission Launcher
            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(context, "通知权限已开启，请再次点击测试", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "需要通知权限才能发送提醒", Toast.LENGTH_LONG).show()
                }
            }

            // 1. Selector: Solid Color vs Image
            Row(modifier = Modifier.fillMaxWidth()) {
                val types = listOf("纯色背景" to "color", "自定义图片" to "image")
                types.forEach { (label, type) ->
                    val isSelected = widgetBgType == type
                    OutlinedButton(
                        onClick = { 
                            // Capture current state values to stable variables to avoid Smart Cast errors
                            val currentImageUri = widgetBgImageUri
                            val currentColor = widgetBgColor
                            
                            scope.launch {
                                // 1. Save Global
                                repository.saveWidgetSettings(type, currentColor, currentImageUri)
                                
                                // 2. Sync Widget State
                                val manager = GlanceAppWidgetManager(context)
                                val glanceIds = manager.getGlanceIds(CheckInWidget::class.java)
                                glanceIds.forEach { glanceId ->
                                    updateAppWidgetState(context, glanceId) { prefs ->
                                        prefs[stringPreferencesKey("key_widget_bg_type")] = type
                                        // Ensure we push the OTHER params too, effectively "restoring" them to the widget
                                        prefs[intPreferencesKey("key_widget_bg_color")] = currentColor
                                        if (currentImageUri != null) {
                                            prefs[stringPreferencesKey("key_widget_bg_image_uri")] = currentImageUri
                                        }
                                    }
                                    CheckInWidget().update(context, glanceId)
                                }
                            } 
                        },
                        modifier = Modifier.weight(1f).padding(4.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        },
                        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                    ) {
                        Text(label, color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (widgetBgType == "color") {
                // Color Presets
                // Color Presets
                Row {
                    Text("选择背景色:", modifier = Modifier.padding(top = 8.dp).alignByBaseline(), color = MaterialTheme.colorScheme.onSurface)
                    val wPresets = listOf(0xFF8FBC8F, 0xFFE0F7FA, 0xFF212121, 0xFFFFFFFF, 0xFFFFCDD2)
                    // Check if custom is selected
                    val isWPresetSelected = wPresets.any { it.toInt() == widgetBgColor }
                    if (!isWPresetSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("再次点击切换自定义颜色", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.alignByBaseline())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wPresets = listOf(
                        0xFF8FBC8F, // Default Green
                        0xFFE0F7FA, // Light Cyan
                        0xFF212121, // Dark Grey
                        0xFFFFFFFF, // Pure White
                        0xFFFFCDD2  // Light Red
                        // 6th is Custom
                    )
                    
                    val isWPresetSelected = wPresets.any { it.toInt() == widgetBgColor }
                    var lastCustomWidgetColor by remember { mutableIntStateOf(0xFFBBDEFB.toInt()) }
                    
                    if (!isWPresetSelected) {
                        lastCustomWidgetColor = widgetBgColor
                    }
                    
                    // 1. Render Presets
                    wPresets.forEach { colorLong ->
                        val colorInt = colorLong.toInt()
                        val isSelected = widgetBgColor == colorInt
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(androidx.compose.ui.graphics.Color(colorLong), androidx.compose.foundation.shape.CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    val currentImage = widgetBgImageUri
                                    scope.launch {
                                        repository.saveWidgetSettings("color", colorInt, currentImage)
                                        // Update Widget
                                        val manager = GlanceAppWidgetManager(context)
                                        val glanceIds = manager.getGlanceIds(CheckInWidget::class.java)
                                        glanceIds.forEach { glanceId ->
                                            updateAppWidgetState(context, glanceId) { prefs ->
                                                prefs[stringPreferencesKey("key_widget_bg_type")] = "color"
                                                prefs[intPreferencesKey("key_widget_bg_color")] = colorInt
                                            }
                                            CheckInWidget().update(context, glanceId)
                                        }
                                    }
                                }
                        )
                    }
                    
                    // 2. Custom Slot Logic
                    val customSlotColorValue = if (!isWPresetSelected) widgetBgColor else lastCustomWidgetColor
                    val customSlotColor = androidx.compose.ui.graphics.Color(customSlotColorValue)
                    
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(customSlotColor, androidx.compose.foundation.shape.CircleShape)
                            .border(
                                width = if (!isWPresetSelected) 3.dp else 1.dp,
                                color = if (!isWPresetSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable {
                            val currentImage = widgetBgImageUri
                            if (isWPresetSelected) {
                                scope.launch {
                                    repository.saveWidgetSettings("color", lastCustomWidgetColor, currentImage)
                                    val manager = GlanceAppWidgetManager(context)
                                    val glanceIds = manager.getGlanceIds(CheckInWidget::class.java)
                                    glanceIds.forEach { glanceId ->
                                        updateAppWidgetState(context, glanceId) { prefs ->
                                            prefs[stringPreferencesKey("key_widget_bg_type")] = "color"
                                            prefs[intPreferencesKey("key_widget_bg_color")] = lastCustomWidgetColor
                                        }
                                        CheckInWidget().update(context, glanceId)
                                    }
                                }
                            } else {
                                showWidgetColorPicker = true
                            }
                        }
                    ) {
                         // Always show + (Bigger and Bolder)
                         Text(
                             "+", 
                             modifier = Modifier.align(Alignment.Center), 
                             color = if (!isWPresetSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Gray,
                             style = androidx.compose.ui.text.TextStyle(
                                 fontSize = 24.sp,
                                 fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                             )
                         )
                    }
                }
                
                if (showWidgetColorPicker) {
                    ColorPickerDialog(
                        initialColor = widgetBgColor,
                        onDismiss = { showWidgetColorPicker = false },
                        onConfirm = { newColor ->
                            val currentImage = widgetBgImageUri
                            scope.launch {
                                repository.saveWidgetSettings("color", newColor, currentImage)
                                // Update Widget
                                val manager = GlanceAppWidgetManager(context)
                                val glanceIds = manager.getGlanceIds(CheckInWidget::class.java)
                                glanceIds.forEach { glanceId ->
                                    updateAppWidgetState(context, glanceId) { prefs ->
                                        prefs[stringPreferencesKey("key_widget_bg_type")] = "color"
                                        prefs[intPreferencesKey("key_widget_bg_color")] = newColor
                                    }
                                    CheckInWidget().update(context, glanceId)
                                }
                            }
                            showWidgetColorPicker = false
                        }
                    )
                }
            } else {
                // Image Mode
                if (widgetBgImageUri != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             OutlinedButton(
                                 onClick = { 
                                     val source = widgetSourcePath
                                     if (source != null) {
                                         onNavigateToPreview(source) 
                                     } else {
                                         Toast.makeText(context, "找不到原图，请重新选择", Toast.LENGTH_SHORT).show()
                                     }
                                 },
                                 modifier = Modifier.weight(1f),
                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                             ) {
                                 Text("调整当前图片", color = MaterialTheme.colorScheme.onSurface)
                             }
                             
                             OutlinedButton(
                                 onClick = { pickImageLauncher.launch("image/*") },
                                 modifier = Modifier.weight(1f),
                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                             ) {
                                 Text("更换图片", color = MaterialTheme.colorScheme.onSurface)
                             }
                        }
                    }
                } else {
                    // No Image Selected
                    OutlinedButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("从相册选择图片", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Pin Widget Button (Moved from Advanced)
            var showPermissionDialog by remember { mutableStateOf(false) }

            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("提示", color = MaterialTheme.colorScheme.onSurface) },
                    text = { 
                        Text(
                            "为了成功添加桌面小组件，请确保您的设备已开启“创建桌面快捷方式”权限。",
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPermissionDialog = false
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("去开启", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showPermissionDialog = false
                                // Try to pin
                                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                val myProvider = android.content.ComponentName(context, com.example.privatecheck.widget.CheckInWidgetReceiver::class.java)
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                } else {
                                     Toast.makeText(context, "您的启动器不支持自动添加，请手动长按桌面添加", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("我已开启", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    containerColor = backgroundColor,
                    textContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedButton(
                onClick = { showPermissionDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                 Text("一键添加桌面小组件 (2x1)", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Made by 实习生摆啊糖",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}
