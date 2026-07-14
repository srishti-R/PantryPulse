// In androidMain/kotlin
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
actual fun LocalDate.formatToDeviceLocale(): String {
    // Use the built-in localized date format style (e.g., MEDIUM or SHORT)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
    return this.toJavaLocalDate().format(formatter)
}
