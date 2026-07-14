// In iosMain/kotlin
import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun LocalDate.formatToDeviceLocale(): String {
    val dateFormatter = NSDateFormatter().apply {
        setDateStyle(NSDateFormatterMediumStyle) // Use a predefined style
        setLocale(NSLocale.currentLocale) // Use the current device locale
    }

    // Convert LocalDate to NSDate for NSDateFormatter
    val calendar = NSCalendar.currentCalendar
    val components = NSDateComponents().apply {
        year = this@formatToDeviceLocale.year.toLong()
        month = this@formatToDeviceLocale.monthNumber.toLong()
        day = this@formatToDeviceLocale.dayOfMonth.toLong()
    }
    val nsDate = calendar.dateFromComponents(components) ?: NSDate()

    return dateFormatter.stringFromDate(nsDate)
}
