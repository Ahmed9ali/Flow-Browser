package com.example.automation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.model.AutomationActionType
import com.example.model.AutomationDataGenerator
import com.example.model.AutomationDataRecord
import com.example.model.AutomationExecutionState
import com.example.model.AutomationScript
import com.example.model.AutomationStatus
import com.example.model.AutomationStep
import com.example.model.ElementSemanticType
import com.example.model.ScannedElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.random.Random

class AutomationEngine(
    private val scope: CoroutineScope
) {
    private val _executionState = MutableStateFlow(AutomationExecutionState())
    val executionState: StateFlow<AutomationExecutionState> = _executionState.asStateFlow()

    private val initialRecords = AutomationDataGenerator.generateSampleRecords(10)

    private val _currentScript = MutableStateFlow(
        AutomationScript(
            name = "Account Creation & Full Form Automation",
            steps = listOf(
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[name*=\"first_name\"], #firstName, input[placeholder*=\"First\" i]",
                    elementDescription = "Fill First Name",
                    value = "{{firstName}}",
                    variableKey = "firstName",
                    delayMs = 700L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[name*=\"last_name\"], #lastName, input[placeholder*=\"Last\" i]",
                    elementDescription = "Fill Last Name",
                    value = "{{lastName}}",
                    variableKey = "lastName",
                    delayMs = 700L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[type=\"email\"], input[name*=\"email\"], #email",
                    elementDescription = "Fill Email Address",
                    value = "{{email}}",
                    variableKey = "email",
                    delayMs = 900L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[type=\"password\"]:not([name*=\"confirm\"]):not([id*=\"confirm\"]), #password",
                    elementDescription = "Fill Strong Password",
                    value = "{{password}}",
                    variableKey = "password",
                    delayMs = 800L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[name*=\"confirm\"], #confirmPassword, input[placeholder*=\"Confirm\" i]",
                    elementDescription = "Confirm Password",
                    value = "{{confirmPassword}}",
                    variableKey = "confirmPassword",
                    delayMs = 800L
                ),
                AutomationStep(
                    actionType = AutomationActionType.SELECT_COUNTRY_CODE,
                    selector = ".iti__selected-flag, select[name*=\"country_code\"], #country_code",
                    elementDescription = "Select Country Code",
                    value = "{{countryCode}}",
                    variableKey = "countryCode",
                    delayMs = 800L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[type=\"tel\"], input[name*=\"phone\"], #phone",
                    elementDescription = "Fill Phone Number",
                    value = "{{phone}}",
                    variableKey = "phone",
                    delayMs = 800L
                ),
                AutomationStep(
                    actionType = AutomationActionType.SELECT_DROPDOWN,
                    selector = "select[name*=\"gender\"], #gender, [role=\"combobox\"][name*=\"gender\"]",
                    elementDescription = "Select Gender",
                    value = "{{gender}}",
                    variableKey = "gender",
                    delayMs = 750L
                ),
                AutomationStep(
                    actionType = AutomationActionType.SELECT_DATE,
                    selector = "input[type=\"date\"], [data-datepicker], input[name*=\"dob\"]",
                    elementDescription = "Select Date of Birth (Calendar)",
                    value = "{{dob}}",
                    variableKey = "dob",
                    delayMs = 850L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[name*=\"address\"], #address, input[placeholder*=\"Street\" i]",
                    elementDescription = "Fill Street Address",
                    value = "{{address}}",
                    variableKey = "address",
                    delayMs = 750L
                ),
                AutomationStep(
                    actionType = AutomationActionType.INPUT,
                    selector = "input[name*=\"zip\"], input[name*=\"postal\"], input[name*=\"pin\"], #zip",
                    elementDescription = "Fill ZIP / PIN Code",
                    value = "{{zipCode}}",
                    variableKey = "zipCode",
                    delayMs = 700L
                ),
                AutomationStep(
                    actionType = AutomationActionType.UPLOAD_FILE,
                    selector = "input[type=\"file\"], .dropzone",
                    elementDescription = "Upload Document / Avatar",
                    value = "{{fileName}}",
                    variableKey = "fileName",
                    delayMs = 1000L
                ),
                AutomationStep(
                    actionType = AutomationActionType.CLICK,
                    selector = "input[type=\"checkbox\"], #terms, [name*=\"terms\"], [name*=\"agree\"]",
                    elementDescription = "Accept Terms & Policies",
                    delayMs = 600L
                ),
                AutomationStep(
                    actionType = AutomationActionType.SUBMIT,
                    selector = "button[type=\"submit\"], .btn-primary, #submit-btn, button.submit",
                    elementDescription = "Click Submit / Create Account",
                    delayMs = 1500L
                )
            ),
            repeatCount = 5,
            rotateProxyOnRepeat = true,
            humanizeDelays = true,
            clearDataOnComplete = true,
            datasetRecords = initialRecords
        )
    )
    val currentScript: StateFlow<AutomationScript> = _currentScript.asStateFlow()

    private val _scannedElements = MutableStateFlow<List<ScannedElement>>(emptyList())
    val scannedElements: StateFlow<List<ScannedElement>> = _scannedElements.asStateFlow()

    private var playbackJob: Job? = null
    var onRotateProxy: (() -> Unit)? = null
    var webViewProvider: (() -> WebView?)? = null

    // JavaScript Bridge exposed to WebView
    inner class AutomationJsInterface {
        @JavascriptInterface
        fun onUserAction(actionType: String, selector: String, description: String, value: String) {
            if (_executionState.value.status != AutomationStatus.RECORDING) return

            val type = when (actionType.uppercase()) {
                "INPUT" -> AutomationActionType.INPUT
                "SELECT_DROPDOWN" -> AutomationActionType.SELECT_DROPDOWN
                "SELECT_DATE" -> AutomationActionType.SELECT_DATE
                "UPLOAD_FILE" -> AutomationActionType.UPLOAD_FILE
                "SUBMIT" -> AutomationActionType.SUBMIT
                else -> AutomationActionType.CLICK
            }

            val semanticVar = detectVariableKeyFromSelector(selector, description)

            val newStep = AutomationStep(
                actionType = type,
                selector = selector,
                elementDescription = description,
                value = if (semanticVar != null) "{{$semanticVar}}" else value,
                variableKey = semanticVar,
                delayMs = 800L
            )

            scope.launch {
                val updatedSteps = _currentScript.value.steps + newStep
                _currentScript.value = _currentScript.value.copy(steps = updatedSteps)
                addLog("Captured action: $description ($selector)")
            }
        }

        @JavascriptInterface
        fun onScanResults(jsonString: String) {
            scope.launch {
                try {
                    val jsonArray = JSONArray(jsonString)
                    val elements = mutableListOf<ScannedElement>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val semanticStr = obj.optString("semanticType", "GENERIC_INPUT")
                        val semanticEnum = try {
                            ElementSemanticType.valueOf(semanticStr)
                        } catch (e: Exception) {
                            ElementSemanticType.GENERIC_INPUT
                        }

                        val optionsList = mutableListOf<String>()
                        val optsArray = obj.optJSONArray("detectedOptions")
                        if (optsArray != null) {
                            for (j in 0 until optsArray.length()) {
                                optionsList.add(optsArray.optString(j))
                            }
                        }

                        elements.add(
                            ScannedElement(
                                tagName = obj.optString("tagName", ""),
                                type = obj.optString("type", ""),
                                nameAttr = obj.optString("nameAttr", ""),
                                idAttr = obj.optString("idAttr", ""),
                                placeholder = obj.optString("placeholder", ""),
                                text = obj.optString("text", ""),
                                selector = obj.optString("selector", ""),
                                isInput = obj.optBoolean("isInput", false),
                                isButton = obj.optBoolean("isButton", false),
                                semanticType = semanticEnum,
                                detectedOptions = optionsList
                            )
                        )
                    }
                    _scannedElements.value = elements
                    addLog("Scanner identified ${elements.size} form elements (inputs, dropdowns, calendar, file uploads, buttons)")
                } catch (e: Exception) {
                    addLog("Scanner parse error: ${e.message}")
                }
            }
        }
    }

    val jsInterface = AutomationJsInterface()

    private fun detectVariableKeyFromSelector(selector: String, description: String): String? {
        val combined = "$selector $description".lowercase()
        return when {
            combined.contains("email") -> "email"
            combined.contains("confirm") && combined.contains("pass") -> "confirmPassword"
            combined.contains("pass") -> "password"
            combined.contains("first") && combined.contains("name") -> "firstName"
            combined.contains("last") && combined.contains("name") -> "lastName"
            combined.contains("country") && (combined.contains("code") || combined.contains("dial")) -> "countryCode"
            combined.contains("phone") || combined.contains("mobile") || combined.contains("tel") -> "phone"
            combined.contains("gender") || combined.contains("sex") -> "gender"
            combined.contains("birth") || combined.contains("dob") -> "dob"
            combined.contains("zip") || combined.contains("postal") || combined.contains("pin") -> "zipCode"
            combined.contains("city") -> "city"
            combined.contains("state") -> "state"
            combined.contains("address") -> "address"
            combined.contains("file") || combined.contains("upload") || combined.contains("resume") -> "fileName"
            else -> null
        }
    }

    // Control HUD visibility
    fun toggleHud() {
        val next = !_executionState.value.isHudVisible
        _executionState.value = _executionState.value.copy(isHudVisible = next)
    }

    fun setHudVisible(visible: Boolean) {
        _executionState.value = _executionState.value.copy(isHudVisible = visible)
    }

    // Recording
    fun toggleRecording() {
        if (_executionState.value.status == AutomationStatus.RECORDING) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.RECORDING,
            isHudVisible = true
        )
        addLog("Recording started. Click, input, select dropdowns or dates on page.")
        injectJs(AutomationScripts.RECORDING_INJECTION)
    }

    fun stopRecording() {
        _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
        addLog("Recording stopped. Total steps: ${_currentScript.value.steps.size}")
    }

    // Scanner
    fun triggerScan() {
        _executionState.value = _executionState.value.copy(status = AutomationStatus.SCANNING)
        addLog("Scanning DOM for modern inputs, gender dropdowns, country codes, DOB calendar, file uploads...")
        injectJs(AutomationScripts.DOM_SCANNER)
    }

    fun clearScanHighlights() {
        injectJs(AutomationScripts.CLEAR_SCAN_HIGHLIGHTS)
        _scannedElements.value = emptyList()
        if (_executionState.value.status == AutomationStatus.SCANNING) {
            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
        }
    }

    fun addScannedElementToScript(element: ScannedElement) {
        val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
        val newStep = AutomationStep(
            actionType = actionType,
            selector = element.selector,
            elementDescription = desc,
            value = defaultVal,
            variableKey = varKey,
            delayMs = 850L
        )
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + newStep)
        addLog("Added $desc into script ($actionType)")
    }

    /**
     * One-Tap Auto Map:
     * Takes scanned elements on page and builds a clean, fully-mapped automation workflow.
     */
    fun autoMapAllScannedElements() {
        val elements = _scannedElements.value
        if (elements.isEmpty()) return

        val newSteps = elements.map { element ->
            val (actionType, varKey, defaultVal, desc) = mapScannedElementDetails(element)
            AutomationStep(
                actionType = actionType,
                selector = element.selector,
                elementDescription = desc,
                value = defaultVal,
                variableKey = varKey,
                delayMs = 800L
            )
        }

        _currentScript.value = _currentScript.value.copy(steps = newSteps)
        addLog("Auto-mapped ${newSteps.size} form steps from page elements.")
    }

    private fun mapScannedElementDetails(element: ScannedElement): Quad<AutomationActionType, String?, String, String> {
        return when (element.semanticType) {
            ElementSemanticType.EMAIL -> Quad(AutomationActionType.INPUT, "email", "{{email}}", "Fill Email")
            ElementSemanticType.PASSWORD -> Quad(AutomationActionType.INPUT, "password", "{{password}}", "Fill Password")
            ElementSemanticType.CONFIRM_PASSWORD -> Quad(AutomationActionType.INPUT, "confirmPassword", "{{confirmPassword}}", "Confirm Password")
            ElementSemanticType.FIRST_NAME -> Quad(AutomationActionType.INPUT, "firstName", "{{firstName}}", "Fill First Name")
            ElementSemanticType.LAST_NAME -> Quad(AutomationActionType.INPUT, "lastName", "{{lastName}}", "Fill Last Name")
            ElementSemanticType.FULL_NAME -> Quad(AutomationActionType.INPUT, "fullName", "{{fullName}}", "Fill Full Name")
            ElementSemanticType.PHONE -> Quad(AutomationActionType.INPUT, "phone", "{{phone}}", "Fill Phone Number")
            ElementSemanticType.COUNTRY_CODE -> Quad(AutomationActionType.SELECT_COUNTRY_CODE, "countryCode", "{{countryCode}}", "Select Country Code")
            ElementSemanticType.GENDER -> Quad(AutomationActionType.SELECT_DROPDOWN, "gender", "{{gender}}", "Select Gender")
            ElementSemanticType.DOB -> Quad(AutomationActionType.SELECT_DATE, "dob", "{{dob}}", "Select Date of Birth")
            ElementSemanticType.DOB_DAY -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobDay", "{{dobDay}}", "Select DOB Day")
            ElementSemanticType.DOB_MONTH -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobMonth", "{{dobMonth}}", "Select DOB Month")
            ElementSemanticType.DOB_YEAR -> Quad(AutomationActionType.SELECT_DROPDOWN, "dobYear", "{{dobYear}}", "Select DOB Year")
            ElementSemanticType.ADDRESS -> Quad(AutomationActionType.INPUT, "address", "{{address}}", "Fill Address")
            ElementSemanticType.ADDRESS_LINE_2 -> Quad(AutomationActionType.INPUT, "addressLine2", "{{addressLine2}}", "Fill Apt/Suite")
            ElementSemanticType.CITY -> Quad(AutomationActionType.INPUT, "city", "{{city}}", "Fill City")
            ElementSemanticType.STATE -> Quad(AutomationActionType.INPUT, "state", "{{state}}", "Fill State")
            ElementSemanticType.ZIP_PIN -> Quad(AutomationActionType.INPUT, "zipCode", "{{zipCode}}", "Fill ZIP / PIN Code")
            ElementSemanticType.FILE_UPLOAD -> Quad(AutomationActionType.UPLOAD_FILE, "fileName", "{{fileName}}", "Upload Document")
            ElementSemanticType.CHECKBOX_TERMS -> Quad(AutomationActionType.CLICK, null, "", "Accept Policies & Terms")
            ElementSemanticType.SUBMIT_BUTTON -> Quad(AutomationActionType.SUBMIT, null, "", "Submit / Create Account")
            ElementSemanticType.NEXT_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click Next / Continue")
            ElementSemanticType.DROPDOWN -> Quad(AutomationActionType.SELECT_DROPDOWN, null, element.detectedOptions.firstOrNull() ?: "", "Select Dropdown Option")
            ElementSemanticType.GENERIC_BUTTON -> Quad(AutomationActionType.CLICK, null, "", "Click ${element.text.ifBlank { "Button" }}")
            ElementSemanticType.GENERIC_INPUT -> Quad(AutomationActionType.INPUT, null, "sample_data", "Fill ${element.placeholder.ifBlank { element.nameAttr.ifBlank { "Input" } }}")
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // Playback Execution
    fun startPlayback() {
        if (_currentScript.value.steps.isEmpty()) {
            addLog("No script steps to execute. Record or add steps first.")
            return
        }

        stopPlayback()
        _executionState.value = _executionState.value.copy(
            status = AutomationStatus.PLAYING,
            currentIteration = 1,
            totalIterations = _currentScript.value.repeatCount,
            currentStepIndex = 0,
            totalSteps = _currentScript.value.steps.size,
            isHudVisible = true
        )

        val script = _currentScript.value
        val records = if (script.datasetRecords.isNotEmpty()) {
            script.datasetRecords
        } else {
            // Fallback: parse datasetRawText or generate
            val lines = script.datasetRawText.lines().filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                lines.mapIndexed { idx, email ->
                    AutomationDataRecord(
                        email = email.trim(),
                        password = "P@ssw0rd!${idx + 1000}",
                        confirmPassword = "P@ssw0rd!${idx + 1000}",
                        firstName = "User${idx + 1}",
                        lastName = "Test",
                        phoneNumber = "555019${1000 + idx}",
                        countryCode = "+1",
                        gender = if (idx % 2 == 0) "Male" else "Female",
                        dob = "1995-06-15",
                        dobDay = "15",
                        dobMonth = "06",
                        dobYear = "1995",
                        address = "${100 + idx} Main Street",
                        city = "New York",
                        state = "NY",
                        zipCode = "10001",
                        fileName = "id_document.pdf"
                    )
                }
            } else {
                AutomationDataGenerator.generateSampleRecords(script.repeatCount)
            }
        }

        playbackJob = scope.launch(Dispatchers.Main) {
            addLog("Starting automation execution (${script.repeatCount} iterations with ${records.size} data profiles)...")

            for (iteration in 1..script.repeatCount) {
                if (_executionState.value.status != AutomationStatus.PLAYING) break

                _executionState.value = _executionState.value.copy(
                    currentIteration = iteration,
                    totalIterations = script.repeatCount
                )

                // Rotate proxy at every count if configured
                if (script.rotateProxyOnRepeat) {
                    onRotateProxy?.invoke()
                    addLog("[Iteration $iteration] Rotating proxy node for clean session")
                    delay(500)
                }

                val currentRecord = records[(iteration - 1) % records.size]
                addLog("[Iteration $iteration] Using Profile: ${currentRecord.fullName.ifBlank { currentRecord.email }} (${currentRecord.email})")

                for ((index, step) in script.steps.withIndex()) {
                    if (_executionState.value.status != AutomationStatus.PLAYING) break

                    _executionState.value = _executionState.value.copy(
                        currentStepIndex = index + 1,
                        currentStepDescription = step.elementDescription
                    )

                    // Humanize wait delay
                    val actualDelay = if (script.humanizeDelays) {
                        (step.delayMs * (Random.nextDouble(0.85, 1.25))).toLong()
                    } else step.delayMs

                    delay(actualDelay)

                    // Resolve variable placeholders
                    val resolvedValue = resolveVariables(step.value, step.variableKey, currentRecord)

                    // Execute corresponding JS in WebView
                    when (step.actionType) {
                        AutomationActionType.CLICK -> {
                            val js = AutomationScripts.createClickScript(step.selector)
                            injectJs(js)
                            addLog("[Iter $iteration] Clicked: ${step.elementDescription}")
                        }
                        AutomationActionType.INPUT -> {
                            val js = AutomationScripts.createInputScript(step.selector, resolvedValue, step.clearBeforeInput)
                            injectJs(js)
                            addLog("[Iter $iteration] Inputted '$resolvedValue' into ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_DROPDOWN -> {
                            val js = AutomationScripts.createSelectDropdownScript(step.selector, resolvedValue)
                            injectJs(js)
                            addLog("[Iter $iteration] Selected '$resolvedValue' in dropdown ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_COUNTRY_CODE -> {
                            val js = AutomationScripts.createSelectCountryCodeScript(step.selector, currentRecord.countryCode)
                            injectJs(js)
                            addLog("[Iter $iteration] Selected Country Code '${currentRecord.countryCode}' for ${step.elementDescription}")
                        }
                        AutomationActionType.SELECT_DATE -> {
                            val js = AutomationScripts.createSelectDateScript(
                                step.selector,
                                currentRecord.dob,
                                currentRecord.dobDay,
                                currentRecord.dobMonth,
                                currentRecord.dobYear
                            )
                            injectJs(js)
                            addLog("[Iter $iteration] Selected DOB '${currentRecord.dob}' (Calendar/Date picker)")
                        }
                        AutomationActionType.UPLOAD_FILE -> {
                            val js = AutomationScripts.createFileUploadScript(step.selector, currentRecord.fileName)
                            injectJs(js)
                            addLog("[Iter $iteration] Attached file '${currentRecord.fileName}' into ${step.elementDescription}")
                        }
                        AutomationActionType.SUBMIT -> {
                            val js = AutomationScripts.createSubmitScript(step.selector)
                            injectJs(js)
                            addLog("[Iter $iteration] Submitted form: ${step.elementDescription}")
                        }
                        AutomationActionType.WAIT -> {
                            addLog("[Iter $iteration] Pausing for ${step.delayMs}ms")
                        }
                        AutomationActionType.CLEAR -> {
                            val js = AutomationScripts.createInputScript(step.selector, "", true)
                            injectJs(js)
                            addLog("[Iter $iteration] Cleared data in ${step.elementDescription}")
                        }
                        AutomationActionType.SCROLL -> {
                            injectJs("window.scrollBy({ top: 350, behavior: 'smooth' });")
                            addLog("[Iter $iteration] Scrolled page")
                        }
                    }
                }

                // If clearDataOnComplete is set, clear form fields after step sequence completes
                if (script.clearDataOnComplete) {
                    delay(700)
                    script.steps.filter { it.actionType == AutomationActionType.INPUT }.forEach { inputStep ->
                        val clearJs = AutomationScripts.createInputScript(inputStep.selector, "", true)
                        injectJs(clearJs)
                    }
                    addLog("[Iter $iteration] Form fields cleared for next session.")
                }

                delay(1200)
            }

            _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
            addLog("Automation completed all ${script.repeatCount} iterations successfully.")
        }
    }

    private fun resolveVariables(rawText: String, varKey: String?, record: AutomationDataRecord): String {
        var text = rawText
        if (text.isBlank() && varKey != null) {
            text = "{{$varKey}}"
        }

        return text
            .replace("{{email}}", record.email)
            .replace("{{password}}", record.password)
            .replace("{{confirmPassword}}", record.confirmPassword)
            .replace("{{firstName}}", record.firstName)
            .replace("{{lastName}}", record.lastName)
            .replace("{{fullName}}", record.fullName)
            .replace("{{phone}}", record.phoneNumber)
            .replace("{{phoneNumber}}", record.phoneNumber)
            .replace("{{countryCode}}", record.countryCode)
            .replace("{{gender}}", record.gender)
            .replace("{{dob}}", record.dob)
            .replace("{{dobDay}}", record.dobDay)
            .replace("{{dobMonth}}", record.dobMonth)
            .replace("{{dobYear}}", record.dobYear)
            .replace("{{address}}", record.address)
            .replace("{{addressLine2}}", record.addressLine2)
            .replace("{{city}}", record.city)
            .replace("{{state}}", record.state)
            .replace("{{zipCode}}", record.zipCode)
            .replace("{{fileName}}", record.fileName)
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _executionState.value = _executionState.value.copy(status = AutomationStatus.IDLE)
        addLog("Automation playback stopped.")
    }

    // Dataset Management
    fun addDataRecord(record: AutomationDataRecord) {
        val updated = _currentScript.value.datasetRecords + record
        _currentScript.value = _currentScript.value.copy(datasetRecords = updated)
        addLog("Added data record: ${record.email}")
    }

    fun updateDataRecord(record: AutomationDataRecord) {
        val updated = _currentScript.value.datasetRecords.map { if (it.id == record.id) record else it }
        _currentScript.value = _currentScript.value.copy(datasetRecords = updated)
    }

    fun deleteDataRecord(recordId: String) {
        val updated = _currentScript.value.datasetRecords.filter { it.id != recordId }
        _currentScript.value = _currentScript.value.copy(datasetRecords = updated)
        addLog("Deleted data record.")
    }

    fun generateSyntheticRecords(count: Int) {
        val records = AutomationDataGenerator.generateSampleRecords(count)
        _currentScript.value = _currentScript.value.copy(
            datasetRecords = records,
            repeatCount = count
        )
        addLog("Generated $count realistic dataset profiles (Email, Pass, Phone, Gender, DOB, Address, PIN, File).")
    }

    fun importDatasetText(text: String) {
        val lines = text.lines().filter { it.isNotBlank() }
        val generated = lines.mapIndexed { idx, line ->
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 4) {
                AutomationDataRecord(
                    email = parts[0],
                    password = parts[1],
                    confirmPassword = parts[1],
                    firstName = parts.getOrNull(2) ?: "User$idx",
                    phoneNumber = parts.getOrNull(3) ?: "555019${1000 + idx}",
                    gender = parts.getOrNull(4) ?: "Male",
                    dob = parts.getOrNull(5) ?: "1995-05-15",
                    zipCode = parts.getOrNull(6) ?: "10001"
                )
            } else {
                AutomationDataRecord(
                    email = line.trim(),
                    password = "P@ssw0rd!${idx + 1000}",
                    confirmPassword = "P@ssw0rd!${idx + 1000}",
                    firstName = "User${idx + 1}",
                    lastName = "Profile",
                    phoneNumber = "555019${1000 + idx}",
                    countryCode = "+1",
                    gender = if (idx % 2 == 0) "Male" else "Female",
                    dob = "1996-08-20",
                    dobDay = "20",
                    dobMonth = "08",
                    dobYear = "1996",
                    address = "${200 + idx} Broadway",
                    city = "San Francisco",
                    state = "CA",
                    zipCode = "94105",
                    fileName = "id_document.pdf"
                )
            }
        }
        _currentScript.value = _currentScript.value.copy(
            datasetRecords = generated,
            datasetRawText = text
        )
        addLog("Imported ${generated.size} records from input.")
    }

    // Script Management
    fun updateScript(script: AutomationScript) {
        _currentScript.value = script
    }

    fun addStep(step: AutomationStep) {
        _currentScript.value = _currentScript.value.copy(steps = _currentScript.value.steps + step)
    }

    fun removeStep(stepId: String) {
        _currentScript.value = _currentScript.value.copy(
            steps = _currentScript.value.steps.filter { it.id != stepId }
        )
    }

    fun clearAllSteps() {
        _currentScript.value = _currentScript.value.copy(steps = emptyList())
        addLog("All script steps cleared.")
    }

    fun setRepeatCount(count: Int) {
        _currentScript.value = _currentScript.value.copy(repeatCount = count.coerceAtLeast(1))
    }

    private fun addLog(message: String) {
        val currentLogs = _executionState.value.logMessages.takeLast(30)
        _executionState.value = _executionState.value.copy(
            logMessages = currentLogs + message
        )
    }

    private fun injectJs(script: String) {
        scope.launch(Dispatchers.Main) {
            val webView = webViewProvider?.invoke()
            webView?.evaluateJavascript(script, null)
        }
    }
}
