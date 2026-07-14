import kotlinx.datetime.LocalDate

// In commonMain/kotlin
expect fun LocalDate.formatToDeviceLocale(): String
