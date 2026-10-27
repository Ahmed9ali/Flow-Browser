package com.example.model

import java.util.UUID

enum class AutomationActionType {
    CLICK,
    INPUT,
    SELECT_DROPDOWN,
    SELECT_COUNTRY_CODE,
    SELECT_DATE,
    UPLOAD_FILE,
    WAIT,
    SUBMIT,
    CLEAR,
    SCROLL
}

enum class AutomationStatus {
    IDLE,
    RECORDING,
    PLAYING,
    PAUSED,
    SCANNING
}

enum class ElementSemanticType {
    GENERIC_INPUT,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD,
    FIRST_NAME,
    LAST_NAME,
    FULL_NAME,
    PHONE,
    COUNTRY_CODE,
    GENDER,
    DOB,
    DOB_DAY,
    DOB_MONTH,
    DOB_YEAR,
    ADDRESS,
    ADDRESS_LINE_2,
    CITY,
    STATE,
    ZIP_PIN,
    FILE_UPLOAD,
    CHECKBOX_TERMS,
    SUBMIT_BUTTON,
    NEXT_BUTTON,
    GENERIC_BUTTON,
    DROPDOWN
}

/**
 * Real-world multi-field dataset record.
 * Supports Email, Password, Confirm Password, Phone, Country Code, Gender, DOB,
 * Address, City, State, ZIP/PIN, and Uploaded File name/path.
 */
data class AutomationDataRecord(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val countryCode: String = "+1",
    val phoneNumber: String = "",
    val gender: String = "Male",
    val dob: String = "1998-05-14",
    val dobDay: String = "14",
    val dobMonth: String = "05",
    val dobYear: String = "1998",
    val address: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val fileName: String = "id_document.pdf",
    val customFields: Map<String, String> = emptyMap()
)

data class AutomationStep(
    val id: String = UUID.randomUUID().toString(),
    val actionType: AutomationActionType = AutomationActionType.CLICK,
    val selector: String = "",
    val elementDescription: String = "",
    val value: String = "",
    val variableKey: String? = null,
    val delayMs: Long = 800L,
    val clearBeforeInput: Boolean = true,
    val isCompleted: Boolean = false
)

data class AutomationScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Comprehensive Web Workflow Automation",
    val targetUrl: String = "",
    val steps: List<AutomationStep> = emptyList(),
    val repeatCount: Int = 5,
    val humanizeDelays: Boolean = true,
    val rotateProxyOnRepeat: Boolean = false,
    val clearDataOnComplete: Boolean = true,
    val datasetRecords: List<AutomationDataRecord> = emptyList(),
    val datasetRawText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ScannedElement(
    val id: String = UUID.randomUUID().toString(),
    val tagName: String,
    val type: String = "",
    val nameAttr: String = "",
    val idAttr: String = "",
    val placeholder: String = "",
    val text: String = "",
    val selector: String,
    val isInput: Boolean = false,
    val isButton: Boolean = false,
    val semanticType: ElementSemanticType = ElementSemanticType.GENERIC_INPUT,
    val detectedOptions: List<String> = emptyList()
)

data class AutomationExecutionState(
    val status: AutomationStatus = AutomationStatus.IDLE,
    val currentIteration: Int = 1,
    val totalIterations: Int = 1,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val currentStepDescription: String = "",
    val logMessages: List<String> = emptyList(),
    val isHudVisible: Boolean = false
)

/**
 * Synthetic generator for realistic multi-field data records.
 */
object AutomationDataGenerator {
    private val firstNames = listOf(
        "Alex", "Jordan", "Taylor", "Morgan", "Sam", "Chris", "Casey", "Riley",
        "Avery", "Jamie", "Dakota", "Reese", "Harper", "Logan", "Peyton", "Cameron",
        "Liam", "Emma", "Noah", "Olivia", "Ethan", "Sophia", "Lucas", "Mia",
        "Aarav", "Priya", "Carlos", "Elena", "Wei", "Yuki", "Amara", "Tariq"
    )

    private val lastNames = listOf(
        "Miller", "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Patel", "Sharma", "Singh", "Chen", "Tanaka", "Al-Mansoor", "Kowalski", "Dubois"
    )

    private val domains = listOf("gmail.com", "outlook.com", "yahoo.com", "proton.me", "icloud.com")

    private val countryCodes = listOf("+1", "+44", "+91", "+49", "+33", "+61", "+81", "+86")

    private val cities = listOf(
        Pair("New York", "NY"),
        Pair("San Francisco", "CA"),
        Pair("Austin", "TX"),
        Pair("Seattle", "WA"),
        Pair("Chicago", "IL"),
        Pair("Miami", "FL"),
        Pair("Denver", "CO"),
        Pair("Boston", "MA"),
        Pair("Toronto", "ON"),
        Pair("London", "ENG"),
        Pair("Sydney", "NSW"),
        Pair("Berlin", "BE"),
        Pair("Mumbai", "MH"),
        Pair("Tokyo", "TYO")
    )

    private val streets = listOf(
        "Market Street", "Broadway", "Main Street", "Maple Avenue", "Oak Ridge Way",
        "Park Boulevard", "Sunset Boulevard", "Elm Street", "Highland Drive", "Pine Street"
    )

    private val files = listOf("id_document.pdf", "profile_avatar.png", "resume_cv.pdf", "utility_bill.pdf", "passport_scan.jpg")

    fun generateSampleRecords(count: Int = 10): List<AutomationDataRecord> {
        val records = mutableListOf<AutomationDataRecord>()
        for (i in 1..count) {
            val fName = firstNames[(i * 3 + 7) % firstNames.size]
            val lName = lastNames[(i * 5 + 11) % lastNames.size]
            val domain = domains[i % domains.size]
            val email = "${fName.lowercase()}.${lName.lowercase()}${(i * 97) % 899 + 100}@$domain"
            val pass = "P@ssw0rd!${(i * 123) % 8999 + 1000}"
            val countryCode = countryCodes[i % countryCodes.size]
            val phone = "${(200 + (i * 17) % 700)}${(100 + (i * 31) % 800)}${(1000 + (i * 73) % 8999)}"
            val gender = when (i % 3) {
                0 -> "Male"
                1 -> "Female"
                else -> "Non-Binary"
            }
            val birthYear = 1985 + (i * 3) % 20
            val birthMonthNum = (i % 12) + 1
            val birthMonth = if (birthMonthNum < 10) "0$birthMonthNum" else "$birthMonthNum"
            val birthDayNum = (i % 28) + 1
            val birthDay = if (birthDayNum < 10) "0$birthDayNum" else "$birthDayNum"
            val dob = "$birthYear-$birthMonth-$birthDay"

            val cityPair = cities[i % cities.size]
            val streetNum = (100 + (i * 43) % 8900)
            val street = streets[i % streets.size]
            val address = "$streetNum $street"
            val addressLine2 = if (i % 2 == 0) "Suite ${(i * 7) % 50 + 101}" else ""
            val zip = "${10000 + (i * 791) % 89999}"
            val fileName = files[i % files.size]

            records.add(
                AutomationDataRecord(
                    email = email,
                    password = pass,
                    confirmPassword = pass,
                    firstName = fName,
                    lastName = lName,
                    fullName = "$fName $lName",
                    countryCode = countryCode,
                    phoneNumber = phone,
                    gender = gender,
                    dob = dob,
                    dobDay = birthDay,
                    dobMonth = birthMonth,
                    dobYear = birthYear.toString(),
                    address = address,
                    addressLine2 = addressLine2,
                    city = cityPair.first,
                    state = cityPair.second,
                    zipCode = zip,
                    fileName = fileName,
                    customFields = mapOf("username" to "${fName.lowercase()}_$birthYear")
                )
            )
        }
        return records
    }
}
