package commons

import java.text.SimpleDateFormat
import java.util.Date
import org.joda.time._

object DateHelper {
  private val textFormatter = new SimpleDateFormat("EEEE dd MMMM")

  private val meetingDateFormat: SimpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd")

  def readableDate(date: Date): String = textFormatter.format(date)

  def forUrl(date: Date): String = meetingDateFormat.format(date)

  def fromUrl(date: String): Date = meetingDateFormat.parse(date)

  def today: Date = new DateMidnight().toDate

  def nextDay: Date =  nextDay(new DateMidnight())

  def nextDay(date:Date): Date = nextDay(new DateMidnight(date.getTime))

  private def nextDay(dateMidnight: DateMidnight): Date = {
    val next: DateMidnight = dateMidnight.plusDays(1)
    if (next.getDayOfWeek == DateTimeConstants.SUNDAY)
      next.plusDays(1).toDate
    else if (next.getDayOfWeek == DateTimeConstants.SATURDAY)
      next.plusDays(2).toDate
    else
      next.toDate
  }

  def previousDay: Date = previousDay(new DateMidnight())
  def previousDay(date: Date): Date = previousDay(new DateMidnight(meetingDateFormat.parse(meetingDateFormat.format(date))))

  private def previousDay(midnight: DateMidnight): Date = {
    val next: DateMidnight = midnight.minusDays(1)
    if (next.getDayOfWeek == DateTimeConstants.SUNDAY)
      next.minusDays(2).toDate
    else if (next.getDayOfWeek == DateTimeConstants.SATURDAY)
      next.minusDays(1).toDate
    else
      next.toDate
  }

  private val holidays = List(
  )

  def businessDaysBetween(startDate: DateTime, endDate: DateTime): Seq[DateTime] = {
    val daysBetween = Days.daysBetween(new DateMidnight(startDate), new DateMidnight(endDate)).getDays()
    1 to daysBetween map { startDate.withFieldAdded(DurationFieldType.days(), _)} diff holidays filter { _.getDayOfWeek() match {
      case DateTimeConstants.SUNDAY | DateTimeConstants.SATURDAY => false
      case _ => true
    }}
  }
}
