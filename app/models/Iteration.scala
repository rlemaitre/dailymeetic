package models

import java.util.Date
import anorm.SqlParser._
import anorm._
import play.api.db.DB
import anorm.~
import org.joda.time.{DateTimeConstants, DateMidnight}

import play.api.Play.current

case class Iteration(name: String, start: Date) {

}

object Iteration {
  def create(iteration: Iteration): Iteration = {
    DB.withConnection {
      implicit c =>
        SQL("insert into iteration(name, date_start) values ({name}, {start})")
          .on('start -> iteration.start, 'name -> iteration.name).executeUpdate()
    }
    iteration
  }

  val it135 = Iteration("IT 135", new DateMidnight(2013, DateTimeConstants.SEPTEMBER, 16).toDate)
  val it136 = Iteration("IT 136", new DateMidnight(2013, DateTimeConstants.OCTOBER, 7).toDate)

  private val iteration = {
    get[String]("name") ~
      get[Date]("date_start") map {
      case name ~ start => Iteration(name, start)
    }
  }

  def activeAt(date: Date): Option[Iteration] = {
    DB.withConnection {
      implicit c =>
        SQL("select * from iteration where date_start < {start} order by date_start desc limit 1")
          .on('start -> date).as(iteration.singleOpt)
    }
  }

  def list: Seq[Iteration] = DB.withConnection {
    implicit c =>
      SQL("select * from iteration").as(iteration *)
  }
}
