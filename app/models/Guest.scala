package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import anorm.~
import org.joda.time.DateTime
import commons.DateHelper

import play.api.Play.current

case class Guest(teamName: String, userLogin: Option[String], dayNumber: Int) {

}

object Guest {
  val guestParser = {
    get[String]("team") ~
      get[Option[String]]("user_login") ~
      get[Int]("day_number") map {
      case team ~ user ~ day => Guest(team, user, day)
    }
  }

  def forMeeting(meeting: Meeting): Option[User] = {
    val iteration: Iteration = Iteration.activeAt(meeting.date).getOrElse(Iteration.it135)
    val dayNumber: Int = DateHelper.businessDaysBetween(new DateTime(iteration.start), new DateTime(meeting.date)).size + 1

    val guests: Seq[Guest] = DB.withConnection {
      implicit c =>
        SQL("select * from guest where team = {team} order by day_number asc").on('team -> meeting.teamName).as(guestParser *)
    }
    guests.filter(g => g.dayNumber == dayNumber % guests.size).map(g => User.findByLogin(g.userLogin.getOrElse(""))).headOption.getOrElse(None)
  }


  def list: Seq[Guest] = DB.withConnection {
    implicit c =>
      SQL("select * from guest").as(guestParser *)
  }

  def create(guest: Guest): Guest = {
    DB.withConnection {
      implicit c =>
        SQL("insert into guest(team, user_login, day_number) values ({team}, {user}, {day})")
          .on('team -> guest.teamName, 'user -> guest.userLogin, 'day -> guest.dayNumber).executeUpdate()
    }
    guest
  }
}
